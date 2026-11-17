package ru.itmo.soa.eurekadns;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EurekaClientTest {

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
        serveEureka("OSCAR-SERVICE", json);
    }

    private void serveEureka(String appName, String json) {
        server.createContext("/apps/" + appName, exchange -> {
            byte[] body = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
    }

    @Test
    void parsesUpInstancesWithSelectedPortField() throws Exception {
        serveEureka("{"
                + "\"application\": {"
                + "\"name\": \"OSCAR-SERVICE\","
                + "\"instance\": ["
                + "  {\"instanceId\": \"oscar-1:oscar-service:8081\", \"status\": \"UP\","
                + "   \"ipAddr\": \"10.0.0.11\","
                + "   \"port\": {\"@enabled\": \"true\", \"$\": 8081},"
                + "   \"securePort\": {\"@enabled\": \"false\", \"$\": 8443}},"
                + "  {\"instanceId\": \"oscar-2:oscar-service:8082\", \"status\": \"UP\","
                + "   \"ipAddr\": \"10.0.0.12\","
                + "   \"port\": {\"@enabled\": \"true\", \"$\": 8082},"
                + "   \"securePort\": {\"@enabled\": \"false\", \"$\": 8443}}"
                + "]}}");

        List<Instance> instances = new EurekaClient(baseUrl)
                .fetch("OSCAR-SERVICE", "port");

        assertEquals(2, instances.size());
        assertEquals("oscar-1:oscar-service:8081", instances.get(0).instanceId());
        assertEquals(InetAddress.getByName("10.0.0.11"), instances.get(0).address());
        assertEquals(8081, instances.get(0).port());
    }

    @Test
    void filtersOutDownInstancesAndDisabledPorts() throws Exception {
        serveEureka("{"
                + "\"application\": {"
                + "\"name\": \"OSCAR-SERVICE\","
                + "\"instance\": ["
                + "  {\"instanceId\": \"up-1\", \"status\": \"UP\","
                + "   \"ipAddr\": \"10.0.0.11\","
                + "   \"port\": {\"@enabled\": \"true\", \"$\": 8081}},"
                + "  {\"instanceId\": \"down-1\", \"status\": \"DOWN\","
                + "   \"ipAddr\": \"10.0.0.12\","
                + "   \"port\": {\"@enabled\": \"true\", \"$\": 8082}},"
                + "  {\"instanceId\": \"port-disabled\", \"status\": \"UP\","
                + "   \"ipAddr\": \"10.0.0.13\","
                + "   \"port\": {\"@enabled\": \"false\", \"$\": 8083}}"
                + "]}}");

        List<Instance> instances = new EurekaClient(baseUrl)
                .fetch("OSCAR-SERVICE", "port");

        assertEquals(1, instances.size());
        assertEquals("up-1", instances.get(0).instanceId());
    }

    @Test
    void selectSecurePortWhenRequested() throws Exception {
        serveEureka("API-GATEWAY", "{"
                + "\"application\": {"
                + "\"name\": \"API-GATEWAY\","
                + "\"instance\": ["
                + "  {\"instanceId\": \"gateway-1\", \"status\": \"UP\","
                + "   \"ipAddr\": \"10.0.0.9\","
                + "   \"port\": {\"@enabled\": \"true\", \"$\": 8080},"
                + "   \"securePort\": {\"@enabled\": \"true\", \"$\": 8443}}"
                + "]}}");

        List<Instance> instances = new EurekaClient(baseUrl)
                .fetch("API-GATEWAY", "securePort");

        assertEquals(1, instances.size());
        assertEquals(8443, instances.get(0).port());
    }

    @Test
    void noInstancesWhenAppMissingReturnsEmpty() throws Exception {
        server.createContext("/apps/NOPE", exchange -> {
            byte[] body = "{\"application\":{\"name\":\"NOPE\"}}"
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        List<Instance> instances = new EurekaClient(baseUrl).fetch("NOPE", "port");

        assertTrue(instances.isEmpty());
    }
}