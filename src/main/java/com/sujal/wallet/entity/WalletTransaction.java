package com.sujal.wallet.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transactions",
		uniqueConstraints = @UniqueConstraint(name = "uk_owner_idempotency",
				columnNames = { "owner_wallet_id","type", "idempotency_key" }),
		indexes = { @Index(name = "idx_tx_from", columnList = "from_wallet_id"),
				@Index(name = "idx_tx_to", columnList = "to_wallet_id") })
@Getter
@Setter
@NoArgsConstructor

public class WalletTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Wallet of the user who initiated the operation (scope of the idempotency key). */
	@Column(name = "owner_wallet_id", nullable = false)
	private Long ownerWalletId;

	/** Null for ADD (money enters from outside the system). */
	@Column(name = "from_wallet_id")
	private Long fromWalletId;

	@Column(name = "to_wallet_id", nullable = false)
	private Long toWalletId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TransactionType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TransactionStatus status;

	@Column(name = "idempotency_key", nullable = false, length = 100)
	private String idempotencyKey;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	
}
