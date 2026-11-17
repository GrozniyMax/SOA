package ru.itmo.soa.eurekadns;

import org.junit.jupiter.api.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Два TCP-клиента должны обслуживаться параллельно (один не должен блокировать другого). */
class DnsServerConcurrencyTest {

    @Test
    void twoConcurrentTcpClientsBothGetAnswers() throws Exception {
        Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();
        snapshots.put("oscar", new Snapshot(System.currentTimeMillis(),
                List.of(new Instance("o1", InetAddress.getByName("10.0.0.11"), 8081))));
        EurekaRegistry registry = new EurekaRegistry(System::currentTimeMillis, snapshots);
        DiscoveryResolver resolver = new DiscoveryResolver(registry, 30_000);

        DnsServer server = new DnsServer(resolver::resolve, 0);
        server.start();
        try {
            AtomicInteger ok = new AtomicInteger();
            Thread[] threads = new Thread[2];
            for (int i = 0; i < 2; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        Message r = tcpQuery(server.getPort());
                        if (r.getRcode() == Rcode.NOERROR) {
                            ok.incrementAndGet();
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
            // Первый клиент держит соединение открытым и шлёт много запросов,
            // имитируя HAProxy, монополизирующего поток.
            try (Socket holder = new Socket()) {
                holder.connect(new InetSocketAddress("127.0.0.1", server.getPort()), 2000);
                holder.setSoTimeout(5000);
                DataOutputStream holderOut = new DataOutputStream(holder.getOutputStream());
                DataInputStream holderIn = new DataInputStream(holder.getInputStream());
                byte[] q = srvQuery().toWire();
                holderOut.writeShort(q.length);
                holderOut.write(q);
                holderOut.flush();
                int len = holderIn.readUnsignedShort();
                byte[] resp = new byte[len];
                holderIn.readFully(resp);
                // Держим соединение открытым, пока второй клиент стучится.
                threads[0].start();
                Thread.sleep(200);
                threads[1].start();
                for (Thread t : threads) {
                    t.join(3000);
                }
            }
            assertEquals(2, ok.get(), "оба TCP-клиента должны получить ответ, даже когда один держит соединение");
        } finally {
            server.stop();
        }
    }

    private static Message tcpQuery(int port) throws Exception {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", port), 2000);
            s.setSoTimeout(3000);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());
            byte[] q = srvQuery().toWire();
            out.writeShort(q.length);
            out.write(q);
            out.flush();
            int len = in.readUnsignedShort();
            byte[] resp = new byte[len];
            in.readFully(resp);
            return new Message(resp);
        }
    }

    private static Message srvQuery() throws Exception {
        Message q = new Message();
        q.addRecord(Record.newRecord(Name.fromString("_oscar._tcp.discovery.internal."),
                Type.SRV, DClass.IN), Section.QUESTION);
        return q;
    }
}