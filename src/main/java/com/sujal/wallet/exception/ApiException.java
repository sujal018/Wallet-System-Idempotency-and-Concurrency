package com.sujal.wallet.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Business error that maps directly to an HTTP status. */
@Getter
public class ApiException extends RuntimeException {

	private final String code;
	private final HttpStatus status;
	private final List<String> details;

	public ApiException(HttpStatus status, String message, String code) {
		this(status, message, code, List.of());
	}

	public ApiException(HttpStatus status, String message, String code, List<String> details) {
		super(message);
		this.status = status;
		this.code = code;
		this.details = details;
	}
}
