package ru.itmo.soa.eurekadns;

import java.net.InetAddress;

/** Один UP-инстанс сервиса, полученный из Eureka. */
public final class Instance {
    private final String instanceId;
    private final InetAddress address;
    private final int port;

    public Instance(String instanceId, InetAddress address, int port) {
        this.instanceId = instanceId;
        this.address = address;
        this.port = port;
    }

    public String instanceId() {
        return instanceId;
    }

    public InetAddress address() {
        return address;
    }

    public int port() {
        return port;
    }
}