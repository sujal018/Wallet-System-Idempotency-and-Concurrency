package com.sujal.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sujal.wallet.entity.TransactionStatus;
import com.sujal.wallet.entity.TransactionType;
import com.sujal.wallet.entity.WalletTransaction;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionResponse {

	private final Long id;
	private final Long fromWalletId;
	private final Long toWalletId;
	private final BigDecimal amount;
	private final TransactionType type;
	private final TransactionStatus status;
	private final String idempotencyKey;

	private final Instant createdAt;

	/**
	 * The caller's wallet balance after the operation. Only filled for add and
	 * transfer responses; omitted from transaction history and admin lists.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final BigDecimal availableBalance;

	/**
	 * True when this is the stored result of an earlier request with the same
	 * Idempotency-Key (nothing was processed again).
	 */
	private final boolean replayed;

	public static TransactionResponse from(WalletTransaction t) {
		return from(t, null);
	}

	public static TransactionResponse from(WalletTransaction t, BigDecimal availableBalance) {
		return new TransactionResponse(t.getId(), t.getFromWalletId(), t.getToWalletId(), t.getAmount(), t.getType(),
				t.getStatus(), t.getIdempotencyKey(), t.getCreatedAt(), availableBalance, false);
	}

	/** Same result, marked as a replay of an earlier request. */
	public TransactionResponse asReplay() {
		return new TransactionResponse(id, fromWalletId, toWalletId, amount, type, status, idempotencyKey, createdAt,
				availableBalance, true);
	}
}
