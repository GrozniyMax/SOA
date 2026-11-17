package ru.itmo.soa.eurekadns;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Точка входа eureka-dns: публикует Eureka-инстансы как DNS SRV/A записи.
 *
 * <p>Читает переменные окружения:
 * <ul>
 *   <li>{@code EUREKA_URL} — базовый URL Eureka (по умолчанию http://localhost:8761/eureka);</li>
 *   <li>{@code DNS_PORT} — порт DNS-сервера (по умолчанию 8053);</li>
 *   <li>{@code POLL_INTERVAL_MS} — период опроса Eureka (по умолчанию 5000).</li>
 * </ul>
 */
public final class EurekaDnsApplication {

    /** Локальное имя сервиса (ключ DNS-имён) → приложение в Eureka + поле порта. */
    private static Map<String, ServiceSpec> defaultServices() {
        Map<String, ServiceSpec> services = new LinkedHashMap<>();
        services.put("oscar", new ServiceSpec("OSCAR-SERVICE", "port"));
        services.put("gateway", new ServiceSpec("API-GATEWAY", "securePort"));
        return services;
    }

    public static void main(String[] args) throws Exception {
        String eurekaUrl = env("EUREKA_URL", "http://localhost:8761/eureka");
        int port = Integer.parseInt(env("DNS_PORT", "8053"));
        long pollInterval = Long.parseLong(env("POLL_INTERVAL_MS", "5000"));
        long maxSnapshotAgeMillis = Long.parseLong(env("MAX_SNAPSHOT_AGE_MS", "30000"));

        Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();
        EurekaRegistry registry = new EurekaRegistry(System::currentTimeMillis, snapshots);

        EurekaClient client = new EurekaClient(eurekaUrl);
        EurekaRefresher refresher = new EurekaRefresher(client, registry, defaultServices(), pollInterval);
        refresher.start();

        DiscoveryResolver resolver = new DiscoveryResolver(registry, maxSnapshotAgeMillis);
        DnsServer dnsServer = new DnsServer(resolver::resolve, port);
        dnsServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            dnsServer.stop();
            refresher.stop();
        }));

        System.out.println("eureka-dns listening on port " + dnsServer.getPort()
                + ", polling " + eurekaUrl + " every " + pollInterval + "ms");

        // Потоки DNS-сервера и рефрешера — daemon: держим главный поток живым,
        // иначе JVM завершится сразу после старта.
        Thread.currentThread().join();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}