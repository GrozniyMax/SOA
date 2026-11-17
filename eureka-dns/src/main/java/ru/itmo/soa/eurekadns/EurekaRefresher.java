package ru.itmo.soa.eurekadns;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Фоновый опрос Eureka: для каждого сервиса периодически перечитывает список UP-инстансов
 * и обновляет снапшот в registry. Ошибки одиночного опроса не роняют цикл.
 */
public final class EurekaRefresher {
    private final EurekaClient client;
    private final EurekaRegistry registry;
    private final Map<String, ServiceSpec> services;
    private final long pollIntervalMillis;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public EurekaRefresher(EurekaClient client, EurekaRegistry registry,
                           Map<String, ServiceSpec> services, long pollIntervalMillis) {
        this.client = client;
        this.registry = registry;
        this.services = services;
        this.pollIntervalMillis = pollIntervalMillis;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        thread = new Thread(this::loop, "eureka-refresher");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void loop() {
        while (running.get()) {
            for (Map.Entry<String, ServiceSpec> entry : services.entrySet()) {
                String service = entry.getKey();
                ServiceSpec spec = entry.getValue();
                try {
                    java.util.List<Instance> instances = client.fetch(spec.appName(), spec.portField());
                    registry.update(service, instances, registry.nowMillis());
                } catch (Exception e) {
                    System.err.println("Could not refresh " + spec.appName() + ": "
                            + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                }
            }
            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}