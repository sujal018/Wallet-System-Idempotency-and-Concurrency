package com.sujal.wallet.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pagination {

    private static final int MAX_PAGE_SIZE = 100;

    private Pagination() {
    }

    public static Pageable of(int page, int size, Sort sort) {

        int validPage = Math.max(page, 0);

        int validSize = Math.min(
                Math.max(size, 1),
                MAX_PAGE_SIZE
        );

        return PageRequest.of(validPage, validSize, sort);
    }
}