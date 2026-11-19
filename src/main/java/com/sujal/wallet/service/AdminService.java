package com.sujal.wallet.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sujal.wallet.dto.PageResponse;
import com.sujal.wallet.dto.TransactionResponse;
import com.sujal.wallet.dto.WalletResponse;
import com.sujal.wallet.repository.TransactionRepository;
import com.sujal.wallet.repository.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

	private final WalletRepository walletRepository;
	private final TransactionRepository transactionRepository;

	public PageResponse<WalletResponse> getAllWallets(int page, int size) {

		var pageable = Pagination.of(page, size, Sort.by("id"));

		return PageResponse.of(walletRepository.findAll(pageable), WalletResponse::from);
	}

	public PageResponse<TransactionResponse> getAllTransactions(int page, int size) {

		var pageable = Pagination.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

		return PageResponse.of(transactionRepository.findAll(pageable), TransactionResponse::from);
	}
}