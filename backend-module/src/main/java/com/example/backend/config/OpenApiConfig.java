package com.example.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI beneficioOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Beneficio API")
                        .version("1.0.0")
                        .description("API REST para gerenciamento de benefícios — CRUD completo e transferência de valores com controle transacional.")
                        .contact(new Contact()
                                .name("BIP Desafio Fullstack")
                                .email("dev@example.com")));
    }
}
