package ru.sbrf.chatx.smartgate.balancer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestTemplate;
import ru.sbrf.chatx.smartgate.balancer.LoadbalancerProperties.HealthProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class HealthServiceImpl implements HealthService {
    public static final int MAGIC_SEED = 4231;
    public static final int FAIL_INDEX = -1;
    //Списко хостов которые обрабатываем
    private final List<HealthInstance> healthInstance;
    private final int[] templateAllowedHost;
    private final ScheduledExecutorService scheduledExecutorService;
    private final List<HostServiceInstance> instances;
    private final Tag tagName;
    private volatile int[] allowedIndexHost;
    private ScheduledFuture<?> scheduledFuture;
    private final Executor executor;
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;

    public HealthServiceImpl(String name,
                             List<HostServiceInstance> instances,
                             LoadbalancerProperties loadbalancerProperties,
                             RestTemplate restTemplate,
                             MeterRegistry meterRegistry) {
        this.instances = instances;
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
        this.tagName = Tag.of("loadBalancer", name);
        healthInstance = instances
                .stream()
                .map(instance -> createHealInstance(name, instance, loadbalancerProperties))
                .toList();
        templateAllowedHost = createTemplateAllowed(instances);
        allowedIndexHost = new int[templateAllowedHost.length];
        Arrays.fill(allowedIndexHost, FAIL_INDEX);


        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        executor = Executors.newFixedThreadPool(instances.size());
    }


    @Override
    public int[] getAllowedIndexHost() {
        return Arrays.copyOf(allowedIndexHost, allowedIndexHost.length);
    }

    @Override
    public void start() {
        scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
                    final Instant now = Instant.now();
                    List<CompletableFuture<Void>> completableFutureList = healthInstance.stream()
                            .filter(instance -> instance.isAfterInterval(now))
                            .peek(HealthInstance::updateLastStart)
                            .map(instance -> CompletableFuture
                                    .runAsync(() -> healthServiceInternal(instance), executor)
                                    .orTimeout(instance.getTimeOut().toSeconds(), TimeUnit.SECONDS)
                                    .whenComplete((c, t) -> {
                                        if (t != null) {
                                            if (log.isDebugEnabled()) {
                                                log.debug("Ошибка обработки таймаута {} -> {}",
                                                        ExceptionUtils.getMessage(t),
                                                        instance.getHealthCheckPath());
                                            }
                                            instance.failure();
                                            createMetric(instance);
                                        }
                                    })
                            )
                            .toList();

                    try {
                        CompletableFuture
                                .allOf(completableFutureList.toArray(new CompletableFuture[0]))
                                .get(15, TimeUnit.SECONDS);
                    } catch (Exception ignore) {
                        log.warn("time out complete services");
                    }
                    createAllowedList();
                },
                2, 2, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        scheduledFuture.cancel(true);
    }

    private static <T> T mapProp(T val1, T val2, T def) {
        if (val1 != null) {
            return val1;
        }
        if (val2 != null) {
            return val2;
        }
        return def;
    }

    private static HealthInstance createHealInstance(String name,
                                                     HostServiceInstance instance,
                                                     LoadbalancerProperties loadbalancerProperties) {
        HealthProperties healthInstProp
                = Optional.ofNullable(loadbalancerProperties.getInstance())
                .map(m -> m.get(name))
                .map(LoadbalancerProperties.InstanceProperties::getHealth)
                .orElse(HealthProperties.EMPTY);

        HealthProperties healthProp = Optional.ofNullable(loadbalancerProperties.getHealth())
                .orElse(HealthProperties.EMPTY);

        return new HealthInstance(instance.getServiceId(),
                instance,
                mapProp(healthInstProp.getErrorCount(), healthProp.getErrorCount(), 1),
                mapProp(healthInstProp.getInitialDelay(), healthProp.getInitialDelay(), Duration.ofSeconds(5)),
                mapProp(healthInstProp.getInterval(), healthProp.getInterval(), Duration.ofSeconds(5)),
                mapProp(healthInstProp.getTimeOut(), healthProp.getTimeOut(), Duration.ofSeconds(10)),
                mapProp(healthInstProp.getHealthPath(), healthProp.getHealthPath(), "/health")
        );
    }

    private void createAllowedList() {
        int[] result = new int[templateAllowedHost.length];
        for (int i = 0; i < templateAllowedHost.length; i++) {
            if (healthInstance.get(templateAllowedHost[i]).isAllowed()) {
                result[i] = templateAllowedHost[i];
            } else {
                result[i] = FAIL_INDEX;
            }
        }
        allowedIndexHost = result;
    }

    private int[] createTemplateAllowed(List<HostServiceInstance> instances) {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < instances.size(); j++) {
                all.add(j);
            }
        }
        Collections.shuffle(all, new Random(MAGIC_SEED)); //перемешивание всегда одинаково
        return ArrayUtils.toPrimitive(all.toArray(new Integer[0]));
    }

    private void healthServiceInternal(HealthInstance instance) {
        try {
            log.trace("запрос хелчека {}", instance.getHealthCheckPath());
            HttpStatusCode statusCode = restTemplate
                    .getForEntity(instance.getHealthCheckPath(), Void.class)
                    .getStatusCode();
            if (HttpStatus.OK.equals(statusCode)) {
                log.trace("успех {}", instance.getHealthCheckPath());
                instance.success();
                createMetric(instance);
            } else {
                log.trace("ошибка {} {}", statusCode, instance.getHealthCheckPath());
                instance.failure();
                createMetric(instance);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Ошибка {} обработки {}", ExceptionUtils.getMessage(e), instance.getHealthCheckPath());
            }
            instance.failure();
            createMetric(instance);
        }
    }

    private void createMetric(HealthInstance instance) {
        meterRegistry.gauge("chatx.loadbalancer.instance",
                Tags.of(instance.getTagPath(), tagName),
                instance.isAllowed() ? 1 : 0);
    }

    @Override
    public HostServiceInstance getInstance(int index) {
        return instances.get(index);
    }

}
