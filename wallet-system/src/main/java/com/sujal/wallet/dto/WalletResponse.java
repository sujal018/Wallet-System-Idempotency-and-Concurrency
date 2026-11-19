package com.sujal.wallet.dto;

import java.math.BigDecimal;

import com.sujal.wallet.entity.Wallet;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WalletResponse {

	private final Long walletId;
	private final Long userId;
	private final BigDecimal balance;
	private final Long version;

	public static WalletResponse from(Wallet w) {
		return new WalletResponse(w.getId(), w.getUserId(), w.getBalance(), w.getVersion());
	}
}
