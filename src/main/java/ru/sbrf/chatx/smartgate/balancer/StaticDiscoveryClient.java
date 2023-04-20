package ru.sbrf.chatx.smartgate.balancer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StaticDiscoveryClient {

    private final LoadbalancerProperties loadbalancerProperties;

    public List<HostServiceInstance> getInstances(String serviceId) {
        LoadbalancerProperties.InstanceProperties instanceProperties
                = loadbalancerProperties.getInstance().get(serviceId);
        Objects.requireNonNull(instanceProperties);
        return instanceProperties.getHosts().stream()
                .map(h->new HostServiceInstance(h.getZoneId(), h.getUri()))
                .toList();
    }
}
