package com.sujal.wallet.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomJwtVerificationFilter extends OncePerRequestFilter {

	private final JwtUtils jwtUtils;

	public CustomJwtVerificationFilter(JwtUtils jwtUtils) {
		this.jwtUtils = jwtUtils;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String header = request.getHeader("Authorization");

		if (header != null && header.startsWith("Bearer ")) {

			String token = header.substring(7);

			try {
				Claims claims = jwtUtils.validateTokenAndGetClaims(token);

				Object userIdValue = claims.get("user_id");

				if (!(userIdValue instanceof Number)) {
					throw new IllegalArgumentException("Invalid user ID");
				}

				Long userId = ((Number) userIdValue).longValue();

				String email = claims.get("user_email", String.class);
				String role = claims.get("user_role", String.class);

				if (email == null || role == null || !email.equals(claims.getSubject())
						|| !(role.equals("USER") || role.equals("ADMIN"))) {

					throw new IllegalArgumentException("Invalid JWT claims");
				}

				JwtPrincipal principal = new JwtPrincipal(userId, email);

				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal,
						null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

				SecurityContextHolder.getContext().setAuthentication(authentication);

			} catch (JwtException | IllegalArgumentException ex) {

				SecurityContextHolder.clearContext();
			}
		}

		filterChain.doFilter(request, response);
	}
}