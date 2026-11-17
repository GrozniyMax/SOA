package ru.itmo.soa.eurekadns;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

/** Снапшоты по имени сервиса с часами для проверки свежести. */
public final class EurekaRegistry {
    private final LongSupplier clock;
    private final Map<String, Snapshot> snapshots;

    public EurekaRegistry(LongSupplier clock, Map<String, Snapshot> snapshots) {
        this.clock = clock;
        this.snapshots = snapshots;
    }

    public Snapshot snapshot(String service) {
        return snapshots.get(service);
    }

    public void update(String service, List<Instance> instances, long updatedAtMillis) {
        snapshots.put(service, new Snapshot(updatedAtMillis, instances));
    }

    public Set<String> services() {
        return snapshots.keySet();
    }

    public long nowMillis() {
        return clock.getAsLong();
    }
}