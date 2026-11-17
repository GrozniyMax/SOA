package ru.itmo.soa.eurekadns;

import org.junit.jupiter.api.Test;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayAResolutionTest {

    @Test
    void gatewayInstanceResolvesAddressForItsSrvTarget() throws Exception {
        String instanceId = "1529c9719ea7:api-gateway:8443";
        Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();
        snapshots.put("gateway", new Snapshot(System.currentTimeMillis(),
                List.of(new Instance(instanceId, InetAddress.getByName("172.18.0.11"), 8443))));
        EurekaRegistry registry = new EurekaRegistry(System::currentTimeMillis, snapshots);
        DiscoveryResolver resolver = new DiscoveryResolver(registry, 30_000);

        Message srv = resolver.resolve(query("_gateway._tcp.discovery.internal.", Type.SRV));
        assertEquals(Rcode.NOERROR, srv.getRcode());
        SRVRecord srvRec = (SRVRecord) srv.getSection(Section.ANSWER).get(0);
        String target = srvRec.getTarget().toString();

        Message a = resolver.resolve(query(target, Type.A));
        assertEquals(Rcode.NOERROR, a.getRcode(), "A-запрос для " + target + " должен быть NOERROR");
        List<Record> ans = a.getSection(Section.ANSWER);
        assertEquals(1, ans.size());
        assertTrue(ans.get(0) instanceof ARecord);
        assertEquals(InetAddress.getByName("172.18.0.11"), ((ARecord) ans.get(0)).getAddress());
    }

    private static Message query(String name, int type) throws Exception {
        Message q = new Message();
        q.addRecord(Record.newRecord(Name.fromString(name), type, DClass.IN), Section.QUESTION);
        return q;
    }
}