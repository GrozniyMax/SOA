package ru.itmo.soa.eurekadns;

/** Приложение в Eureka и поле порта ("port"|"securePort") для DNS-сервиса. */
public final class ServiceSpec {
    private final String appName;
    private final String portField;

    public ServiceSpec(String appName, String portField) {
        this.appName = appName;
        this.portField = portField;
    }

    public String appName() {
        return appName;
    }

    public String portField() {
        return portField;
    }
}