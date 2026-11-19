package com.sujal.wallet.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddMoneyRequest {

	@NotNull(message = "Amount is required")
	@DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
	@Digits(integer = 15, fraction = 2, message = "Amount can have at most 2 decimal places")
	private BigDecimal amount;
}
