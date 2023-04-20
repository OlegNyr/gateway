package ru.sbrf.chatx.smartgate.balancer;

import io.micrometer.core.instrument.Tag;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;


@ToString
public class HealthInstance {
    @Getter
    private final String serviceId;
    private final Duration interval;
    @Getter
    private final Tag tagPath;
    @Getter
    private volatile boolean allowed;
    private final AtomicInteger countError;
    private final Integer maxCountError;
    @Getter
    private final URI healthCheckPath;
    private volatile Instant lastStart;
    @Getter
    private final Duration timeOut;

    public HealthInstance(String serviceId,
                          HostServiceInstance instance,
                          Integer errorCount,
                          Duration initialDelay,
                          Duration interval,
                          Duration timeOut,
                          String healthPath) {
        this.serviceId = serviceId;
        this.healthCheckPath = UriComponentsBuilder
                .fromUriString(getUri(instance, healthPath))
                .build()
                .toUri();
        this.maxCountError = errorCount;
        this.countError = new AtomicInteger(this.maxCountError);
        this.lastStart = Instant.now()
                .plus(initialDelay)
                .minus(interval);
        this.interval = interval;
        this.timeOut = timeOut;
        this.tagPath = Tag.of("url", healthCheckPath.toString());
    }

    static String getUri(HostServiceInstance serviceInstance, String healthCheckPath) {
        if (StringUtils.hasText(healthCheckPath)) {
            String path = healthCheckPath.startsWith("/") ? healthCheckPath : "/" + healthCheckPath;
            return serviceInstance.getUri().toString() + path;
        }
        return serviceInstance.getUri().toString();
    }

    public void failure() {
        int i = countError.decrementAndGet();
        if (i <= 0) {
            allowed = false;
            countError.set(0);
        }
    }

    public void success() {
        countError.set(maxCountError);
        allowed = true;
    }

    public boolean isAfterInterval(Instant now) {
        return now.minus(interval).isAfter(lastStart);
    }

    public void updateLastStart() {
        lastStart = Instant.now();
    }
}
