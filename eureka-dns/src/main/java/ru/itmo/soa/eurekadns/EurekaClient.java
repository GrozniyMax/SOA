package ru.itmo.soa.eurekadns;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** HTTP-клиент Eureka: опрашивает /apps/{app} и возвращает UP-инстансы с выбранным портом. */
public final class EurekaClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final String baseUrl;
    private final HttpClient http;

    public EurekaClient(String baseUrl) {
        this(baseUrl, HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    EurekaClient(String baseUrl, HttpClient http) {
        this.baseUrl = baseUrl;
        this.http = http;
    }

    public List<Instance> fetch(String application, String portField) throws Exception {
        String url = baseUrl.replaceAll("/+$", "") + "/apps/" + application;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .timeout(TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return parse(response.body(), application, portField);
    }

    static List<Instance> parse(String json, String application, String portField) {
        List<Instance> instances = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject app = root.getAsJsonObject("application");
        if (app == null) {
            return instances;
        }
        JsonElement instanceEl = app.get("instance");
        if (instanceEl == null) {
            return instances;
        }
        JsonArray array;
        if (instanceEl.isJsonArray()) {
            array = instanceEl.getAsJsonArray();
        } else if (instanceEl.isJsonObject()) {
            // Eureka отдаёт объект, когда инстанс один.
            array = new JsonArray();
            array.add(instanceEl.getAsJsonObject());
        } else {
            return instances;
        }
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            if (!"UP".equals(obj.get("status").getAsString())) {
                continue;
            }
            JsonObject portObj = obj.getAsJsonObject(portField);
            if (portObj == null || !"true".equalsIgnoreCase(portObj.get("@enabled").getAsString())) {
                continue;
            }
            int port = portObj.get("$").getAsInt();
            InetAddress address;
            try {
                address = InetAddress.getByName(obj.get("ipAddr").getAsString());
            } catch (Exception e) {
                continue;
            }
            instances.add(new Instance(obj.get("instanceId").getAsString(), address, port));
        }
        return instances;
    }
}