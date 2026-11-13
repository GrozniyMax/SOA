package ru.itmo.soa.oscar.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InstanceHeaderConfig implements WebMvcConfigurer {

    public static final String INSTANCE_HEADER = "X-Instance";

    private final String instanceId;

    public InstanceHeaderConfig(@Value("${instance.id:}") String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (instanceId.isEmpty()) {
            return;
        }
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader(INSTANCE_HEADER, instanceId);
                return true;
            }
        });
    }
}
