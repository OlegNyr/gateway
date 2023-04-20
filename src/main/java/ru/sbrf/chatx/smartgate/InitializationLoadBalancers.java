package ru.sbrf.chatx.smartgate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import ru.sbrf.chatx.smartgate.balancer.LoadbalancerProperties;

@RequiredArgsConstructor
@Slf4j
public class InitializationLoadBalancers implements InitializingBean {
    private final LoadbalancerProperties loadbalancerProperties;
    private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;
    @Override
    public void afterPropertiesSet() throws Exception {
        for (String serviceId : loadbalancerProperties.getInstance().keySet()) {
            ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerFactory.getInstance(serviceId);
            log.info("Инициализация сервиса {} -> {}", serviceId, loadBalancer);
        }
    }
}
