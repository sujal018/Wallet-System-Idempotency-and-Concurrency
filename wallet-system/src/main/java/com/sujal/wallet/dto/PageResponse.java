package com.sujal.wallet.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageResponse<T> {

	private final List<T> content;
	private final int page;
	private final int size;
	private final long totalElements;
	private final int totalPages;

	public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
		return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
				page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
