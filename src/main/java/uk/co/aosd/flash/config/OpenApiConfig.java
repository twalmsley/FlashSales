package uk.co.aosd.flash.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI / Swagger configuration.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI flashSalesOpenApi(
        @Value("${spring.application.name:Flash Sales Demo App}") final String appName,
        @Value("${spring.profiles.active:}") final String activeProfiles
    ) {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
            .info(new Info()
                .title(appName)
                .description("REST APIs for managing products, flash sales, and customer orders."
                    + (activeProfiles == null || activeProfiles.isBlank()
                    ? ""
                    : " Active profiles: " + activeProfiles))
                .version("v1")
                .contact(new Contact().name("Flash Sales Demo App"))
                .license(new License().name("UNLICENSED")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token authentication. Get token from /api/v1/auth/login or /api/v1/auth/register")));
    }

    /**
     * Endpoints exposed by the admin service profile.
     */
    @Bean
    public GroupedOpenApi adminServiceApi() {
        return GroupedOpenApi.builder()
            .group("admin-service")
            .pathsToMatch(
                "/api/v1/admin/**",
                "/api/v1/products/**"
            )
            .build();
    }

    /**
     * Endpoints exposed by the API service profile (client-facing endpoints).
     */
    @Bean
    public GroupedOpenApi apiServiceApi() {
        return GroupedOpenApi.builder()
            .group("api-service")
            .pathsToMatch("/api/v1/clients/**")
            .build();
    }
}

