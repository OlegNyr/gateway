package ru.sbrf.chatx.smartgate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import ru.sbrf.chatx.smartgate.balancer.BalancerConfiguration;

@Import(BalancerConfiguration.class)
@SpringBootApplication
public class SmartgateApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartgateApplication.class, args);
    }

}
