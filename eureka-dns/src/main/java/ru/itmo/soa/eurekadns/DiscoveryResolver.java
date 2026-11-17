package ru.itmo.soa.eurekadns;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Резолвит DNS-запросы (SRV/A) из снапшотов Eureka. */
public final class DiscoveryResolver {
    private static final String ZONE = "discovery.internal.";
    private static final String SRV_PREFIX = "_";
    private static final String TCP_SUFFIX = "._tcp." + ZONE;
    private static final int TTL = 5;

    private final EurekaRegistry registry;
    private final long maxAgeMillis;

    public DiscoveryResolver(EurekaRegistry registry, long maxAgeMillis) {
        this.registry = registry;
        this.maxAgeMillis = maxAgeMillis;
    }

    public Message resolve(Message query) {
        Record question = query.getQuestion();
        String qname = question.getName().toString();
        int qtype = question.getType();

        Message response = new Message(query.getHeader().getID());
        response.addRecord(question, Section.QUESTION);

        String service = serviceFromSrv(qname);
        if (service != null && qtype == Type.SRV) {
            resolveSrv(response, service);
        } else {
            resolveAddress(response, qname, qtype);
        }
        return response;
    }

    private void resolveSrv(Message response, String service) {
        Snapshot snapshot = registry.snapshot(service);
        if (snapshot == null || snapshot.instances().isEmpty()) {
            response.getHeader().setRcode(Rcode.NXDOMAIN);
            return;
        }
        if (isStale(snapshot)) {
            response.getHeader().setRcode(Rcode.SERVFAIL);
            return;
        }
        Name owner = questionName(service);
        for (Instance instance : snapshot.instances()) {
            Name target = name(targetName(service, instance));
            response.addRecord(new SRVRecord(owner, DClass.IN, TTL, 0, 1,
                    instance.port(), target), Section.ANSWER);
            response.addRecord(new ARecord(target, DClass.IN, TTL,
                    instance.address()), Section.ADDITIONAL);
        }
    }

    private void resolveAddress(Message response, String qname, int qtype) {
        if (qtype != Type.A && qtype != Type.ANY) {
            response.getHeader().setRcode(Rcode.NXDOMAIN);
            return;
        }
        for (String service : registry.services()) {
            Snapshot snapshot = registry.snapshot(service);
            if (snapshot == null) {
                continue;
            }
            if (isStale(snapshot)) {
                response.getHeader().setRcode(Rcode.SERVFAIL);
                return;
            }
            for (Instance instance : snapshot.instances()) {
                if (targetName(service, instance).equals(qname)) {
                    response.addRecord(new ARecord(name(qname), DClass.IN, TTL,
                            instance.address()), Section.ANSWER);
                    return;
                }
            }
        }
        response.getHeader().setRcode(Rcode.NXDOMAIN);
    }

    /** "_oscar._tcp.discovery.internal." → "oscar", иначе null. Имя — с завершающей точкой. */
    private static String serviceFromSrv(String qname) {
        if (!qname.startsWith(SRV_PREFIX) || !qname.endsWith(TCP_SUFFIX)) {
            return null;
        }
        return qname.substring(SRV_PREFIX.length(), qname.length() - TCP_SUFFIX.length());
    }

    /** Владелец SRV-записи = сам запрошенный "_{service}._tcp.{zone}". */
    private static Name questionName(String service) {
        return name(SRV_PREFIX + service + TCP_SUFFIX);
    }

    private static String targetName(String service, Instance instance) {
        return service + "-" + hash(instance.instanceId()) + "." + ZONE;
    }

    /** SHA-256(instanceId), первые 24 hex-символа — стабильное уникальное имя инстанса. */
    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private boolean isStale(Snapshot snapshot) {
        return registry.nowMillis() - snapshot.updatedAtMillis() > maxAgeMillis;
    }

    private static Name name(String s) {
        return Name.fromConstantString(s);
    }
}