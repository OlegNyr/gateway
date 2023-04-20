package ru.sbrf.chatx.smartgate.balancer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class SberLoaderBalancer implements ReactorServiceInstanceLoadBalancer,
        LoadBalancerLifecycle<RequestDataContext, ResponseData, ServiceInstance> {

    private final HealthServiceManager healthServiceManager;
    private final String cookiesName;

    public SberLoaderBalancer(String serviceId,
                              HealthServiceManager healthServiceManager,
                              LoadbalancerProperties loadbalancerProperties) {
        this.healthServiceManager = healthServiceManager;
        LoadbalancerProperties.StickyProperties stickyProperties = loadbalancerProperties.getSticky();

        cookiesName = stickyProperties.getCookiesName();
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        HealthService healthService = healthServiceManager.getHealthService(request);
        int[] allowedIndexHostLocal = healthService.getAllowedIndexHost();
        if (allowedIndexHostLocal.length == 0) {
            throw NotFoundException.create(false, "Не найдено одного сервиса, не зарегистрировано");
        }
        String valueCookies = getValueCookies(request);
        if (!StringUtils.isEmpty(valueCookies)) {
            int indexAllowed = allowedIndexHostLocal[valueCookies.hashCode() % allowedIndexHostLocal.length];
            int index = HealthService.getHostIndex(allowedIndexHostLocal, indexAllowed);
            return createResponse(healthService, index);
        } else {
            int indexAllowed = ThreadLocalRandom.current().nextInt(allowedIndexHostLocal.length);
            int index = HealthService.getHostIndex(allowedIndexHostLocal, indexAllowed);
            return createResponse(healthService, index);
        }
    }

    private Mono<Response<ServiceInstance>> createResponse(HealthService healthService, int index) {
        HostServiceInstance hostServiceInstance = healthService.getInstance(index);
        return Mono.just(new DefaultResponse(hostServiceInstance));
    }

    private String getValueCookies(Request<?> request) {
        if (request.getContext() instanceof RequestDataContext rq) {
            List<String> values = rq.getClientRequest().getCookies().get(cookiesName);
            if (values == null) {
                return null;
            } else {
                if (values.isEmpty()) {
                    return null;
                }
                if (values.size() == 1) {
                    return values.get(0);
                }
                return String.join("", values);
            }
        } else {
            return null;
        }
    }

    @Override
    public void onStart(Request<RequestDataContext> request) {
        log.debug("onStart {}", request);
    }

    @Override
    public void onStartRequest(Request<RequestDataContext> request, Response<ServiceInstance> lbResponse) {
        log.debug("onStartRequest {}", lbResponse);
    }

    @Override
    public void onComplete(CompletionContext<ResponseData, ServiceInstance, RequestDataContext> completionContext) {
        log.debug("onComplete {}", completionContext);
    }
}
