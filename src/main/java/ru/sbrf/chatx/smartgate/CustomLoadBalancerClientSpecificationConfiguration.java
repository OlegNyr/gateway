package ru.sbrf.chatx.smartgate;


import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sbrf.chatx.smartgate.balancer.CustomDefaultLoadBalancer;
import ru.sbrf.chatx.smartgate.balancer.LoadbalancerProperties;

@Configuration
public class CustomLoadBalancerClientSpecificationConfiguration {

    @Bean
    public LoadBalancerClientSpecification customloadBalancerClientSpecification() {
        LoadBalancerClientSpecification loadBalancerClientSpecification = new LoadBalancerClientSpecification();
        loadBalancerClientSpecification.setName("default.");
        loadBalancerClientSpecification.setConfiguration(new Class[]{CustomDefaultLoadBalancer.class});
        return loadBalancerClientSpecification;
    }

    @Bean
    public InitializationLoadBalancers initializationLoadBalancers(LoadbalancerProperties loadbalancerProperties,
                                                                   ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory) {
        return new InitializationLoadBalancers(loadbalancerProperties, loadBalancerFactory);
    }

}
