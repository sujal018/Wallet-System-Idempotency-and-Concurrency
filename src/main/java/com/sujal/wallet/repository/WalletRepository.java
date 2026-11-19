package com.sujal.wallet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import com.sujal.wallet.entity.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

	Optional<Wallet> findByUserId(Long userId);

	/**
	 * Id-only lookup. Used before locking so the Wallet entity is not cached in the
	 * persistence context with possibly stale state before SELECT ... FOR UPDATE.
	 */
	@Query("select w.id from Wallet w where w.userId = :userId")
	Optional<Long> findIdByUserId(@Param("userId") Long userId);

	/** SELECT ... FOR UPDATE on the user's wallet. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from Wallet w where w.userId = :userId")
	Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);

	/**
	 * Locks several wallets in ascending id order. Every transfer takes its locks in
	 * the same order, so two opposite transfers (A to B and B to A) cannot deadlock.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from Wallet w where w.id in :ids order by w.id asc")
	List<Wallet> findAllByIdForUpdateOrdered(@Param("ids") List<Long> ids);
}
