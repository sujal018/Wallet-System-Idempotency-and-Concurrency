package com.sujal.wallet.dto;

import com.sujal.wallet.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {

	private final Long userId;
	private final String email;
	private final Role role;
	private final Long walletId;
}
