package com.runway.corebanking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Runway Core Banking Engine entry point.
 *
 * <p>@SpringBootApplication is a convenience annotation that enables:
 * <ul>
 *   <li>@Configuration  — marks this class as a bean source</li>
 *   <li>@EnableAutoConfiguration — Spring Boot wires up DataSource, JPA, Security, etc.
 *       based on what's on the classpath</li>
 *   <li>@ComponentScan — scans all sub-packages for @Component, @Service, @Repository</li>
 * </ul>
 *
 * <p>@EnableScheduling activates Spring's task scheduler so @Scheduled methods
 * (e.g. interest accrual) are picked up without any extra config.
 */
@SpringBootApplication
@EnableScheduling
public class CoreBankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreBankingApplication.class, args);
    }
}
