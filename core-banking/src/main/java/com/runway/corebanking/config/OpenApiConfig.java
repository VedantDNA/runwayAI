package com.runway.corebanking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI runwayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Runway Core Banking API")
                        .description("Autonomous, Predictive Neobanking Engine API with immutable double-entry ledger.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Runway Team")
                                .email("dev@runway.local"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
