package com.sujal.wallet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.sujal.wallet.security.CustomAccessDeniedHandler;
import com.sujal.wallet.security.CustomAuthenticationEntryPoint;
import com.sujal.wallet.security.CustomJwtVerificationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomJwtVerificationFilter jwtFilter;
	private final CustomAuthenticationEntryPoint authenticationEntryPoint;

	private final CustomAccessDeniedHandler accessDeniedHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable())

				// H2 console renders inside frames
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))

				.authorizeHttpRequests(auth -> auth

						// Swagger APIs - Public access
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

						// Health check (used by Render / Railway) and H2 console (dev only)
						.requestMatchers("/actuator/health", "/actuator/info", "/h2-console/**").permitAll()

						// Authentication APIs - Public access
						.requestMatchers("/auth/**").permitAll()

						// Admin APIs
						.requestMatchers("/admin/**").hasRole("ADMIN")

						// Wallet APIs
						.requestMatchers("/wallet/**").hasRole("USER")

						.anyRequest().authenticated())

				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

		return configuration.getAuthenticationManager();
	}
}