package com.hresources.hr.policy_assistant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides OpenAPI metadata for the Policy Assistant API.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the OpenAPI definition exposed by Swagger.
     *
     * @return configured OpenAPI metadata
     */
    @Bean
    public OpenAPI policyAssistantOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HR Policy Assistant API")
                        .description("REST API for asking HR policy questions and serving future RAG-based policy answers.")
                        .version("v1")
                        .contact(new Contact()
                                .name("HR Policy Assistant")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Internal Use")));
    }
}
