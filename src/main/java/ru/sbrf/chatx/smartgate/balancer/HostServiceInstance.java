package ru.sbrf.chatx.smartgate.balancer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Value
@AllArgsConstructor
public class HostServiceInstance implements ServiceInstance {
    String serviceId;
    String host;
    int port;
    boolean secure;
    Map<String, String> metadata = new LinkedHashMap<>();
    URI uri;
    String zoneId;

    public HostServiceInstance(String zoneId, URI uri) {
        this.serviceId = UUID.randomUUID().toString();
        this.uri = uri;
        this.host = this.uri.getHost();
        this.port = this.uri.getPort();
        String scheme = this.uri.getScheme();
        this.secure = "https".equals(scheme);
        this.zoneId = zoneId;
    }

    public static URI getUri(ServiceInstance instance) {
        String scheme = (instance.isSecure()) ? "https" : "http";
        int port = instance.getPort();
        if (port <= 0) {
            port = (instance.isSecure()) ? 443 : 80;
        }
        String uri = String.format("%s://%s:%s", scheme, instance.getHost(), port);
        return URI.create(uri);
    }
}
