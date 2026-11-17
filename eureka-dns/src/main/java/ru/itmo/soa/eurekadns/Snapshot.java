package ru.itmo.soa.eurekadns;

import java.util.List;

/** Снапшот инстансов одного сервиса: момент обновления + список UP-инстансов. */
public final class Snapshot {
    private final long updatedAtMillis;
    private final List<Instance> instances;

    public Snapshot(long updatedAtMillis, List<Instance> instances) {
        this.updatedAtMillis = updatedAtMillis;
        this.instances = List.copyOf(instances);
    }

    public long updatedAtMillis() {
        return updatedAtMillis;
    }

    public List<Instance> instances() {
        return instances;
    }
}