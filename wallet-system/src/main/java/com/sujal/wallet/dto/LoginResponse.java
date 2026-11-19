package com.sujal.wallet.dto;

import com.sujal.wallet.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

	private final String accessToken;
	private final String tokenType;
	private final long expiresInMs;
	private final Long userId;
	private final Role role;
}
