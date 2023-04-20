package ru.sbrf.chatx.smartgate.balancer;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

/**
 * поднимается после запроса балансера, для каждого балансера свой
 */
public class CustomDefaultLoadBalancer {
    @Bean
    public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(Environment environment,
                                                                                   HealthServiceManager healthServiceManager,
                                                                                   LoadbalancerProperties loadbalancerProperties) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new SberLoaderBalancer(name, healthServiceManager, loadbalancerProperties);
    }

    @Bean
    public HealthServiceManager healthServiceManager(Environment environment,
                                                     StaticDiscoveryClient staticDiscoveryClient,
                                                     RestTemplate restTemplateHealthChecks,
                                                     LoadbalancerProperties loadbalancerProperties,
                                                     MeterRegistry meterRegistry) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new HealthServiceManager(name,
                staticDiscoveryClient,
                loadbalancerProperties,
                restTemplateHealthChecks,
                meterRegistry);
    }

    @Bean
    public RestTemplate restTemplateHealthChecks() {
        return new RestTemplate();
    }
}
