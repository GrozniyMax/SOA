"""Publish Eureka instances as DNS SRV and address records."""

import hashlib
import json
import logging
import os
import struct
import time
from http.client import HTTPException
from ipaddress import ip_address
from socketserver import BaseRequestHandler, StreamRequestHandler, ThreadingTCPServer, UDPServer
from threading import Thread
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from dnslib import A, AAAA, DNSRecord, QTYPE, RCODE, RR, SRV, DNSError

ZONE = "discovery.internal."
SERVICES = {
    "oscar": ("OSCAR-SERVICE", "port"),
    "gateway": ("API-GATEWAY", "securePort"),
}
POLL_INTERVAL = 5
MAX_SNAPSHOT_AGE = 30
LOG = logging.getLogger(__name__)


def fetch_instances(url, service, port_field):
    request = Request(url, headers={"Accept": "application/json"})
    try:
        with urlopen(request, timeout=3) as response:
            instances = json.load(response)["application"]["instance"]
    except HTTPError as error:
        error.close()
        if error.code == 404:
            return {}
        raise

    endpoints = {}
    for instance in instances:
        if instance["status"] != "UP":
            continue
        port = instance[port_field]
        if str(port["@enabled"]).lower() != "true":
            continue
        number = int(port["$"])
        if not 1 <= number <= 65535:
            raise ValueError(f"Invalid instance port: {number}")
        address = ip_address(instance["ipAddr"])
        instance_key = hashlib.sha256(instance["instanceId"].encode()).hexdigest()[:24]
        endpoints[f"{service}-{instance_key}.{ZONE}"] = (address, number)
    return endpoints


class EurekaResolver:
    def __init__(self):
        self.snapshots = {service: (float("-inf"), {}) for service in SERVICES}

    def refresh(self, base_url, service):
        application, port_field = SERVICES[service]
        url = f"{base_url}/apps/{application}"
        while True:
            try:
                endpoints = fetch_instances(url, service, port_field)
                if endpoints != self.snapshots[service][1]:
                    LOG.info("%s endpoints: %s", application, list(endpoints.values()))
                self.snapshots[service] = (time.monotonic(), endpoints)
            except (OSError, HTTPException, KeyError, TypeError, ValueError) as error:
                LOG.warning("Could not refresh %s endpoints: %s", application, error)
            time.sleep(POLL_INTERVAL)

    def resolve(self, request):
        response = request.reply(ra=0)
        name = str(request.q.qname).lower()
        for service in SERVICES:
            srv_name = f"_{service}._tcp.{ZONE}"
            if name == srv_name or (name.startswith(f"{service}-") and name.endswith(f".{ZONE}")):
                return self.resolve_service(response, name, request.q.qtype, service, srv_name)
        response.header.rcode = RCODE.NXDOMAIN
        return response

    def resolve_service(self, response, name, qtype, service, srv_name):
        updated_at, endpoints = self.snapshots[service]
        if time.monotonic() - updated_at > MAX_SNAPSHOT_AGE:
            response.header.rcode = RCODE.SERVFAIL
        elif name == srv_name and endpoints:
            if qtype in (QTYPE.SRV, QTYPE.ANY):
                for target, (address, port) in endpoints.items():
                    response.add_answer(RR(name, QTYPE.SRV, ttl=POLL_INTERVAL, rdata=SRV(0, 1, port, target)))
                    response.add_ar(address_record(target, address))
        elif name in endpoints:
            record = address_record(name, endpoints[name][0])
            if qtype in (record.rtype, QTYPE.ANY):
                response.add_answer(record)
        else:
            response.header.rcode = RCODE.NXDOMAIN
        return response


def address_record(name, address):
    if address.version == 4:
        return RR(name, QTYPE.A, ttl=POLL_INTERVAL, rdata=A(str(address)))
    return RR(name, QTYPE.AAAA, ttl=POLL_INTERVAL, rdata=AAAA(str(address)))


class TCPHandler(StreamRequestHandler):
    timeout = 10

    def handle(self):
        # HAProxy can send several length-prefixed DNS queries on one TCP connection.
        try:
            while length := self.rfile.read(2):
                size = struct.unpack("!H", length)[0]
                query = DNSRecord.parse(self.rfile.read(size))
                response = self.server.resolver.resolve(query).pack()
                self.wfile.write(struct.pack("!H", len(response)) + response)
        except (OSError, DNSError, struct.error):
            return


class UDPHandler(BaseRequestHandler):
    def handle(self):
        data, socket = self.request
        try:
            query = DNSRecord.parse(data)
            response = self.server.resolver.resolve(query).pack()
        except DNSError:
            return
        if len(response) > 512:
            # Nginx retries a truncated response over TCP.
            truncated = query.reply(ra=0)
            truncated.header.tc = 1
            response = truncated.pack()
        socket.sendto(response, self.client_address)


class TCPServer(ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
    url = os.environ["EUREKA_URL"].rstrip("/")
    resolver = EurekaResolver()
    address = ("0.0.0.0", 8053)
    with TCPServer(address, TCPHandler) as tcp, UDPServer(address, UDPHandler) as udp:
        tcp.resolver = udp.resolver = resolver
        for service in SERVICES:
            Thread(target=resolver.refresh, args=(url, service), daemon=True).start()
        Thread(target=tcp.serve_forever, daemon=True).start()
        udp.serve_forever()
