
package com.sujal.wallet.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

// JWT Bearer Token Authentication
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")

@OpenAPIDefinition(info = @Info(title = "Wallet System API", version = "v1.0", description = "REST APIs for Wallet Management System"))
@Configuration
public class SwaggerConfig {
}
