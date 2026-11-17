package ru.itmo.soa.eurekadns;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EurekaRefresherTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void serveEureka(String json) {
        server.createContext("/apps/OSCAR-SERVICE", exchange -> {
            byte[] body = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
    }

    @Test
    void refresherPollsEurekaAndUpdatesRegistrySnapshot() throws Exception {
        serveEureka("{"
                + "\"application\": {\"name\":\"OSCAR-SERVICE\",\"instance\":["
                + "  {\"instanceId\":\"oscar-1\",\"status\":\"UP\",\"ipAddr\":\"10.0.0.11\","
                + "   \"port\":{\"@enabled\":\"true\",\"$\":8081}}"
                + "]}}");

        Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();
        EurekaRegistry registry = new EurekaRegistry(System::currentTimeMillis, snapshots);
        EurekaClient client = new EurekaClient(baseUrl);
        EurekaRefresher refresher = new EurekaRefresher(
                client, registry,
                Map.of("oscar", new ServiceSpec("OSCAR-SERVICE", "port")),
                50);

        refresher.start();
        try {
            await(() -> snapshots.containsKey("oscar") && !snapshots.get("oscar").instances().isEmpty());
            Snapshot snapshot = registry.snapshot("oscar");
            assertNotNull(snapshot);
            assertEquals(1, snapshot.instances().size());
            assertEquals("oscar-1", snapshot.instances().get(0).instanceId());
            assertEquals(8081, snapshot.instances().get(0).port());
        } finally {
            refresher.stop();
        }
    }

    @Test
    void refresherStartsEmptySnapshotWhenAppMissing() throws Exception {
        server.createContext("/apps/NOPE", exchange -> {
            byte[] body = "{\"application\":{\"name\":\"NOPE\"}}"
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();
        EurekaRegistry registry = new EurekaRegistry(System::currentTimeMillis, snapshots);
        EurekaClient client = new EurekaClient(baseUrl);
        EurekaRefresher refresher = new EurekaRefresher(
                client, registry,
                Map.of("oscar", new ServiceSpec("NOPE", "port")),
                50);

        refresher.start();
        try {
            await(() -> snapshots.containsKey("oscar"));
            assertNotNull(registry.snapshot("oscar"));
            assertEquals(0, registry.snapshot("oscar").instances().size());
        } finally {
            refresher.stop();
        }
    }

    private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("condition not met within timeout");
    }
}