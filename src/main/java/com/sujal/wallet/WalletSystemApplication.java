package com.sujal.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Users live in our own table and log in through /auth/login, so Boot's default
// in-memory user (with the generated password in the log) is switched off.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class WalletSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(WalletSystemApplication.class, args);
	}
}
