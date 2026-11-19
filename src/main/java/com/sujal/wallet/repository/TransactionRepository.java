package com.sujal.wallet.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sujal.wallet.entity.TransactionType;
import com.sujal.wallet.entity.WalletTransaction;

public interface TransactionRepository
        extends JpaRepository<WalletTransaction, Long> {

    Optional<WalletTransaction> findByOwnerWalletIdAndTypeAndIdempotencyKey(
            Long ownerWalletId,
            TransactionType type,
            String idempotencyKey
    );

    Page<WalletTransaction> findByFromWalletIdOrToWalletId(
            Long fromWalletId,
            Long toWalletId,
            Pageable pageable
    );
}