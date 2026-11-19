package com.sujal.wallet.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wallets" ,uniqueConstraints = @UniqueConstraint(name="uk_wallets_user",columnNames = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal balance=BigDecimal.ZERO.setScale(2);

	/** Optimistic-locking version: Hibernate increments it on every update. */
	@Version
	private Long version;

	public void credit(BigDecimal amount) {
		this.balance = this.balance.add(amount);
	}

	public void debit(BigDecimal amount) {
		this.balance = this.balance.subtract(amount);
	}

	public boolean hasSufficientBalance(BigDecimal amount) {
		return this.balance.compareTo(amount) >= 0;
	}
}
