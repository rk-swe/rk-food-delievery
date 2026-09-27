package com.rk.fooddelivery.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  OpenAPI foodDeliveryOpenApi() {
    return new OpenAPI()
        .info(new Info().title("Food Delivery API").version("v1"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .tags(
            List.of(
                new io.swagger.v3.oas.models.tags.Tag().name("Account"),
                new io.swagger.v3.oas.models.tags.Tag().name("Authentication"),
                new io.swagger.v3.oas.models.tags.Tag().name("Cities"),
                new io.swagger.v3.oas.models.tags.Tag().name("Cuisines"),
                new io.swagger.v3.oas.models.tags.Tag().name("Delivery Partners"),
                new io.swagger.v3.oas.models.tags.Tag().name("Restaurants")));
  }

  @Bean
  GroupedOpenApi api() {
    return GroupedOpenApi.builder().group("api").pathsToMatch("/api/**").build();
  }
}
