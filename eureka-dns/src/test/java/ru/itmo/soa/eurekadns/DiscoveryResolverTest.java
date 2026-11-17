package ru.itmo.soa.eurekadns;

import org.junit.jupiter.api.Test;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryResolverTest {

    private static final String ZONE = "discovery.internal.";

    private final Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();

    private DiscoveryResolver resolver(long maxAgeMillis) {
        EurekaRegistry registry = new EurekaRegistry(
                () -> System.currentTimeMillis(),
                snapshots);
        return new DiscoveryResolver(registry, maxAgeMillis);
    }

    private static Message query(String name, int type) throws Exception {
        Message query = new Message();
        query.addRecord(Record.newRecord(Name.fromString(name), type, DClass.IN), Section.QUESTION);
        return query;
    }

    @Test
    void srvQueryReturnsPortsAndAddressesOfAllUpInstances() throws Exception {
        snapshots.put("oscar", new Snapshot(now(), List.of(
                new Instance("oscar-1:oscar-service:8081", ip("10.0.0.11"), 8081),
                new Instance("oscar-2:oscar-service:8082", ip("10.0.0.12"), 8082))));

        Message response = resolver(30_000).resolve(query("_oscar._tcp." + ZONE, Type.SRV));

        assertEquals(Rcode.NOERROR, response.getRcode());
        assertTrue(response.getHeader().getFlag(org.xbill.DNS.Flags.QR),
                "ответ должен иметь установленный флаг QR (иначе nginx отвергает его)");
        List<Record> answer = response.getSection(Section.ANSWER);
        assertEquals(2, answer.size());
        assertTrue(answer.stream().allMatch(r -> r instanceof SRVRecord));

        List<Integer> ports = answer.stream()
                .map(r -> ((SRVRecord) r).getPort())
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        assertEquals(List.of(8081, 8082), ports);

        // Каждому SRV сопоставлена A-запись в ADDITIONAL с адресом инстанса.
        List<Record> additional = response.getSection(Section.ADDITIONAL);
        assertEquals(2, additional.size());
        assertTrue(additional.stream().allMatch(r -> r instanceof ARecord));
    }

    @Test
    void aQueryResolvesInstanceHostnameToAddress() throws Exception {
        String id = "oscar-1:oscar-service:8081";
        snapshots.put("oscar", new Snapshot(now(), List.of(
                new Instance(id, ip("10.0.0.11"), 8081))));
        String hostname = targetName("oscar", id);

        Message response = resolver(30_000).resolve(query(hostname, Type.A));

        assertEquals(Rcode.NOERROR, response.getRcode());
        assertTrue(response.getHeader().getFlag(org.xbill.DNS.Flags.QR),
                "ответ должен иметь установленный флаг QR (иначе nginx отвергает его)");
        List<Record> answer = response.getSection(Section.ANSWER);
        assertEquals(1, answer.size());
        ARecord a = (ARecord) answer.get(0);
        assertEquals(ip("10.0.0.11"), a.getAddress());
    }

    @Test
    void aaaaQueryReturnsEmptyNoerrorForIpv4Instance() throws Exception {
        // AAAA-запрос для IPv4-only должен дать пустой NOERROR, а не A-запись — nginx её отвергает.
        String id = "oscar-1:oscar-service:8081";
        snapshots.put("oscar", new Snapshot(now(), List.of(
                new Instance(id, ip("10.0.0.11"), 8081))));
        String hostname = targetName("oscar", id);

        Message response = resolver(30_000).resolve(query(hostname, Type.AAAA));

        assertEquals(Rcode.NOERROR, response.getRcode());
        assertTrue(response.getHeader().getFlag(org.xbill.DNS.Flags.QR),
                "ответ должен иметь установленный флаг QR");
        assertEquals(0, response.getSection(Section.ANSWER).size(),
                "AAAA-запрос для IPv4-инстанса должен вернуть пустой NOERROR, а не A-запись");
    }

    @Test
    void unknownNameReturnsNxdomain() throws Exception {
        snapshots.put("oscar", new Snapshot(now(), List.of(
                new Instance("oscar-1", ip("10.0.0.11"), 8081))));

        Message response = resolver(30_000).resolve(query("no-such." + ZONE, Type.A));

        assertEquals(Rcode.NXDOMAIN, response.getRcode());
    }

    @Test
    void staleSnapshotReturnsServfail() throws Exception {
        snapshots.put("oscar", new Snapshot(now() - 60_000, List.of(
                new Instance("oscar-1", ip("10.0.0.11"), 8081))));

        Message response = resolver(30_000).resolve(query("_oscar._tcp." + ZONE, Type.SRV));

        assertEquals(Rcode.SERVFAIL, response.getRcode());
    }

    @Test
    void noSnapshotForServiceReturnsNxdomain() throws Exception {
        Message response = resolver(30_000).resolve(query("_movie._tcp." + ZONE, Type.SRV));

        assertEquals(Rcode.NXDOMAIN, response.getRcode());
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static InetAddress ip(String s) throws Exception {
        return InetAddress.getByName(s);
    }

    private static String targetName(String service, String instanceId) {
        return service + "-" + sha256hex(instanceId) + "." + ZONE;
    }

    private static String sha256hex(String value) {
        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}