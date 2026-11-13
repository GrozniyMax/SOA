"""Publish the Oscar instances from Eureka as DNS SRV and address records."""

import hashlib
import json
import logging
import os
import struct
import time
from http.client import HTTPException
from ipaddress import ip_address
from socketserver import StreamRequestHandler, ThreadingTCPServer
from threading import Thread
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from dnslib import A, AAAA, DNSRecord, QTYPE, RCODE, RR, SRV, DNSError

ZONE = "discovery.internal."
SERVICE = f"_oscar._tcp.{ZONE}"
POLL_INTERVAL = 5
MAX_SNAPSHOT_AGE = 30
LOG = logging.getLogger(__name__)


def fetch_instances(url):
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
        port = instance["port"]
        if str(port["@enabled"]).lower() != "true":
            continue
        number = int(port["$"])
        if not 1 <= number <= 65535:
            raise ValueError(f"Invalid instance port: {number}")
        address = ip_address(instance["ipAddr"])
        instance_key = hashlib.sha256(instance["instanceId"].encode()).hexdigest()[:24]
        endpoints[f"oscar-{instance_key}.{ZONE}"] = (address, number)
    return endpoints


class OscarResolver:
    def __init__(self):
        self.snapshot = (float("-inf"), {})

    def refresh(self, url):
        while True:
            try:
                endpoints = fetch_instances(url)
                if endpoints != self.snapshot[1]:
                    LOG.info("Oscar endpoints: %s", list(endpoints.values()))
                self.snapshot = (time.monotonic(), endpoints)
            except (OSError, HTTPException, KeyError, TypeError, ValueError) as error:
                LOG.warning("Could not refresh Oscar endpoints: %s", error)
            time.sleep(POLL_INTERVAL)

    def resolve(self, request):
        response = request.reply(ra=0)
        name = str(request.q.qname).lower()
        qtype = request.q.qtype
        updated_at, endpoints = self.snapshot
        if time.monotonic() - updated_at > MAX_SNAPSHOT_AGE:
            response.header.rcode = RCODE.SERVFAIL
        elif name == SERVICE and endpoints:
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


class DNSHandler(StreamRequestHandler):
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


class DNSServer(ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True

    def __init__(self, resolver):
        super().__init__(("0.0.0.0", 8053), DNSHandler)
        self.resolver = resolver


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
    url = os.environ["EUREKA_URL"].rstrip("/") + "/apps/OSCAR-SERVICE"
    resolver = OscarResolver()
    with DNSServer(resolver) as server:
        Thread(target=resolver.refresh, args=(url,), daemon=True).start()
        server.serve_forever()
