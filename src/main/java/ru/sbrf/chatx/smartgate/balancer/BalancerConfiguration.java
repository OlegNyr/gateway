package ru.sbrf.chatx.smartgate.balancer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LoadbalancerProperties.class)
public class BalancerConfiguration {
}
