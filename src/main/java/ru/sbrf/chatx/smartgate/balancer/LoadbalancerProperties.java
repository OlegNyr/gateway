package ru.sbrf.chatx.smartgate.balancer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "chatx.loadbalancer")
public class LoadbalancerProperties {

    private StickyProperties sticky = new StickyProperties();

    private ProxyErrorProperties proxyError = new ProxyErrorProperties();

    private Map<String, InstanceProperties> instance = Map.of();

    private HealthProperties health = new HealthProperties();

    @Data
    public static class StickyProperties {
        String cookiesName = "sticky-cookies";
    }

    @Data
    public static class ProxyErrorProperties {
        List<Integer> code = List.of();
    }

    @Data
    public static class HealthProperties {
        public static final HealthProperties EMPTY = new HealthProperties();
        String healthPath;
        Duration interval;
        Duration initialDelay;
        Integer errorCount;
        Duration timeOut;
    }

    @Data
    public static class InstanceProperties {
        private HealthProperties health;
        private List<ServiceInstanceProperties> hosts;
    }

    @Data
    public static class ServiceInstanceProperties {
        String zoneId;
        URI uri;
    }
}
