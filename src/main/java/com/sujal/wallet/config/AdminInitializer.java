
package com.sujal.wallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sujal.wallet.entity.Role;
import com.sujal.wallet.service.AuthService;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner createAdmin(
            AuthService authService,
            @Value("${app.admin.email}") String email,
            @Value("${app.admin.password}") String password) {

        return args -> {
            authService.createUserIfAbsent(
                    email,
                    password,
                    Role.ADMIN
            );
        };
    }
}
