package ru.sbrf.chatx.smartgate.balancer;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class HealthServiceManager implements InitializingBean, DisposableBean {

    private final Map<String, HealthService> zoneIdHealthServices;

    public HealthServiceManager(String serviceId,
                                StaticDiscoveryClient staticDiscoveryClient,
                                LoadbalancerProperties loadbalancerProperties,
                                RestTemplate restTemplate,
                                MeterRegistry meterRegistry) {

        Map<String, List<HostServiceInstance>> collect = staticDiscoveryClient.getInstances(serviceId)
                .stream()
                .collect(Collectors.groupingBy(HostServiceInstance::getZoneId, Collectors.toUnmodifiableList()));

        Map<String, HealthService> map = new HashMap<>();
        for (Map.Entry<String, List<HostServiceInstance>> listEntry : collect.entrySet()) {
            map.put(listEntry.getKey(),
                    new HealthServiceImpl(serviceId,
                            listEntry.getValue(),
                            loadbalancerProperties,
                            restTemplate,
                            meterRegistry));
        }
        zoneIdHealthServices = Map.copyOf(map);
    }

    public HealthService getHealthService(Request<?> request) {
        //1)здесь поиде мы можем выбрать какой стенд является основным
        //      и отдать сервис который работает для этого стенда
        //2)дальше мы здесь можем на основании request
        //      выбирать стенд, например на основании custom заголовка
        //
        if (request.getContext() instanceof RequestDataContext rq) {
            //rq.getClientRequest().getHeaders() здесь можем
            //          обрабатывать запрос close если в заголовке есть close_operator
        }
        return zoneIdHealthServices.get("MAIN");
    }

    @Override
    public void afterPropertiesSet() {
        for (Map.Entry<String, HealthService> stringHealthServiceEntry : zoneIdHealthServices.entrySet()) {
            try {
                stringHealthServiceEntry.getValue().start();
            } catch (Exception e) {
                log.warn("Ошибка инициализации {}", stringHealthServiceEntry.getKey(), e);
            }
        }
    }

    @Override
    public void destroy() {
        for (Map.Entry<String, HealthService> stringHealthServiceEntry : zoneIdHealthServices.entrySet()) {
            try {
                stringHealthServiceEntry.getValue().stop();
            } catch (Exception e) {
                log.warn("Ошибка дэинициализации {}", stringHealthServiceEntry.getKey(), e);
            }
        }
    }
}
