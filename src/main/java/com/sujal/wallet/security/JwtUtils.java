package com.sujal.wallet.security;

import com.sujal.wallet.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtils {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtils(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {

        this.secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(base64Secret));

        this.expirationMs = expirationMs;
    }

    public String generateJwt(User user) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("user_id", user.getId())
                .claim("user_email", user.getEmail())
                .claim("user_role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(secretKey)
                .compact();
    }

    public Claims validateTokenAndGetClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}