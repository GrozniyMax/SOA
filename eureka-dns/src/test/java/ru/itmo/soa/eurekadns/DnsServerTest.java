package ru.itmo.soa.eurekadns;

import org.junit.jupiter.api.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DnsServerTest {

    private static final String ZONE = "discovery.internal.";

    private static Message srvQuery(String name) throws Exception {
        Message query = new Message();
        query.addRecord(Record.newRecord(Name.fromString(name), Type.SRV, DClass.IN), Section.QUESTION);
        return query;
    }

    private static DnsServer startServer(DnsServer server) throws Exception {
        server.start();
        return server;
    }

    @Test
    void tcpQueryReturnsLengthPrefixedResponse() throws Exception {
        Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();
        snapshots.put("oscar", new Snapshot(System.currentTimeMillis(), List.of(
                new Instance("o1", InetAddress.getByName("10.0.0.11"), 8081),
                new Instance("o2", InetAddress.getByName("10.0.0.12"), 8082))));
        EurekaRegistry registry = new EurekaRegistry(System::currentTimeMillis, snapshots);
        DiscoveryResolver resolver = new DiscoveryResolver(registry, 30_000);
        DnsServer server = startServer(new DnsServer(resolver::resolve, 0));

        try {
            Message query = srvQuery("_oscar._tcp." + ZONE);
            byte[] wire = query.toWire();

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", server.getPort()), 2000);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream());
                out.writeShort(wire.length);
                out.write(wire);
                out.flush();

                int len = in.readUnsignedShort();
                byte[] responseBytes = in.readNBytes(len);
                Message response = new Message(responseBytes);

                assertEquals(Rcode.NOERROR, response.getRcode());
                assertEquals(2, response.getSection(Section.ANSWER).size());
                assertTrue(response.getSection(Section.ANSWER).stream()
                        .allMatch(r -> r instanceof SRVRecord));
            }
        } finally {
            server.stop();
        }
    }

    @Test
    void udpResponseSetsTruncatedFlagWhenTooLarge() throws Exception {
        List<Instance> many = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            many.add(new Instance("oscar-" + i, InetAddress.getByName("10.0.0." + (i + 1)), 8080 + i));
        }
        Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();
        snapshots.put("oscar", new Snapshot(System.currentTimeMillis(), many));
        EurekaRegistry registry = new EurekaRegistry(System::currentTimeMillis, snapshots);
        DiscoveryResolver resolver = new DiscoveryResolver(registry, 30_000);
        DnsServer server = startServer(new DnsServer(resolver::resolve, 0));

        try {
            byte[] wire = srvQuery("_oscar._tcp." + ZONE).toWire();
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(2000);
                socket.send(new DatagramPacket(wire, wire.length,
                        new InetSocketAddress("127.0.0.1", server.getPort())));

                byte[] buf = new byte[4096];
                DatagramPacket reply = new DatagramPacket(buf, buf.length);
                socket.receive(reply);
                Message response = new Message(java.util.Arrays.copyOf(reply.getData(), reply.getLength()));

                assertTrue(reply.getLength() <= 512, "UDP ответ должен быть <= 512 байт");
                assertTrue(response.getHeader().getFlag(org.xbill.DNS.Flags.TC),
                        "при большом ответе должен быть установлен флаг TC");
            }
        } finally {
            server.stop();
        }
    }
}