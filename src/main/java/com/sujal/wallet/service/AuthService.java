package com.sujal.wallet.service;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sujal.wallet.dto.LoginRequest;
import com.sujal.wallet.dto.LoginResponse;
import com.sujal.wallet.dto.RegisterRequest;
import com.sujal.wallet.dto.RegisterResponse;
import com.sujal.wallet.entity.Role;
import com.sujal.wallet.entity.User;
import com.sujal.wallet.entity.Wallet;
import com.sujal.wallet.exception.ApiException;
import com.sujal.wallet.repository.UserRepository;
import com.sujal.wallet.repository.WalletRepository;
import com.sujal.wallet.security.CustomUserDetailsImpl;
import com.sujal.wallet.security.JwtUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    /*
     * Register a new USER and create the wallet
     * in the same database transaction.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = normalize(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Email is already registered",
                    "EMAIL_ALREADY_REGISTERED"
            );
        }

        return create(email, request.getPassword(), Role.USER);
    }

    /*
     * Used for creating an initial ADMIN account.
     */
    @Transactional
    public void createUserIfAbsent(
            String email,
            String rawPassword,
            Role role) {

        String normalizedEmail = normalize(email);

        if (!userRepository.existsByEmail(normalizedEmail)) {
            create(normalizedEmail, rawPassword, role);
        }
    }

    /*
     * Authenticate user using Spring Security.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = normalize(request.getEmail());

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                request.getPassword()
                        )
                );

        CustomUserDetailsImpl userDetails =
                (CustomUserDetailsImpl) authentication.getPrincipal();

        User user = userDetails.getUser();

        String token = jwtUtils.generateJwt(user);

        return new LoginResponse(
                token,
                "Bearer",
                expirationMs,
                user.getId(),
                user.getRole()
        );
    }

    /*
     * Common user and wallet creation method.
     */
    private RegisterResponse create(
            String email,
            String rawPassword,
            Role role) {

        User user = new User();

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);

        User savedUser = userRepository.save(user);

        Wallet wallet = new Wallet();

        wallet.setUserId(savedUser.getId());
        wallet.setBalance(BigDecimal.ZERO.setScale(2));

        Wallet savedWallet = walletRepository.save(wallet);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedWallet.getId()
        );
    }

    private String normalize(String email) {

        if (email == null || email.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required",
                    "INVALID_EMAIL"
            );
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}