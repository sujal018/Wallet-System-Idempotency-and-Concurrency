package com.sujal.wallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sujal.wallet.dto.PageResponse;
import com.sujal.wallet.dto.TransactionResponse;
import com.sujal.wallet.dto.WalletResponse;
import com.sujal.wallet.entity.TransactionStatus;
import com.sujal.wallet.entity.TransactionType;
import com.sujal.wallet.entity.User;
import com.sujal.wallet.entity.Wallet;
import com.sujal.wallet.entity.WalletTransaction;
import com.sujal.wallet.exception.ApiException;
import com.sujal.wallet.repository.TransactionRepository;
import com.sujal.wallet.repository.UserRepository;
import com.sujal.wallet.repository.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final int MAX_KEY_LENGTH = 100;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    /*
     * Get logged-in user's wallet.
     */
    @Transactional(readOnly = true)
    public WalletResponse getWallet(String email) {

        User user = userOf(email);

        Wallet wallet = walletOf(user);

        return WalletResponse.from(wallet);
    }

    /*
     * Add money to the user's wallet.
     */
    @Transactional
    public TransactionResponse addMoney(
            String email,
            String idempotencyKey,
            BigDecimal amount) {

        String key = requireKey(idempotencyKey);

        User user = userOf(email);

        Long walletId = walletIdOf(user);

        // Lock wallet before modifying its balance.
        Wallet wallet = walletRepository
                .findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found",
                        "WALLET_NOT_FOUND"
                ));

        BigDecimal value = scale(amount);

        // Check whether this request was already processed.
        Optional<WalletTransaction> existing =
                find(walletId, TransactionType.ADD, key);

        if (existing.isPresent()) {
            return replay(existing.get(), value, wallet.getId(), wallet.getBalance());
        }

        wallet.credit(value);

        WalletTransaction transaction = createTransaction(
                walletId,
                null,
                wallet.getId(),
                value,
                TransactionType.ADD,
                TransactionStatus.SUCCESS,
                key
        );

        WalletTransaction savedTransaction =
                transactionRepository.saveAndFlush(transaction);

        return TransactionResponse.from(savedTransaction, wallet.getBalance());
    }

    /*
     * Transfer money between two users.
     */
    @Transactional
    public TransactionResponse transfer(
            String senderEmail,
            String idempotencyKey,
            String receiverEmail,
            BigDecimal amount) {

        String key = requireKey(idempotencyKey);

        User sender = userOf(senderEmail);

        Long senderWalletId = walletIdOf(sender);

        // Get receiver.
        User receiver = userRepository
                .findByEmail(normalize(receiverEmail))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Receiver not found",
                        "RECEIVER_NOT_FOUND"
                ));

        if (receiver.getId().equals(sender.getId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot transfer to yourself",
                    "SELF_TRANSFER"
            );
        }

        Long receiverWalletId = walletIdOf(receiver);

        /*
         * Lock both wallets in ascending ID order.
         * This reduces the risk of deadlocks during opposite transfers.
         */
        List<Long> walletIds = List.of(
                Math.min(senderWalletId, receiverWalletId),
                Math.max(senderWalletId, receiverWalletId)
        );

        List<Wallet> lockedWallets =
                walletRepository.findAllByIdForUpdateOrdered(walletIds);

        if (lockedWallets.size() != 2) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "One or both wallets were not found",
                    "WALLET_NOT_FOUND"
            );
        }

        Wallet from = lockedWallets.stream()
                .filter(w -> w.getId().equals(senderWalletId))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Sender wallet not found",
                        "WALLET_NOT_FOUND"
                ));

        Wallet to = lockedWallets.stream()
                .filter(w -> w.getId().equals(receiverWalletId))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Receiver wallet not found",
                        "WALLET_NOT_FOUND"
                ));

        BigDecimal value = scale(amount);

        // Recheck idempotency after acquiring locks.
        Optional<WalletTransaction> existing =
                find(senderWalletId, TransactionType.TRANSFER, key);

        if (existing.isPresent()) {
            return replay(existing.get(), value, receiverWalletId, from.getBalance());
        }

        if (!from.hasSufficientBalance(value)) {
            // Record the failed attempt (balances untouched) and return it.
            // The controller turns a first-time FAILED result into HTTP 422;
            // a replay of it returns the stored result instead.
            WalletTransaction failed = createTransaction(
                    senderWalletId,
                    from.getId(),
                    to.getId(),
                    value,
                    TransactionType.TRANSFER,
                    TransactionStatus.FAILED,
                    key
            );

            return TransactionResponse.from(
                    transactionRepository.saveAndFlush(failed),
                    from.getBalance()
            );
        }

        // Debit sender and credit receiver.
        from.debit(value);
        to.credit(value);

        WalletTransaction transaction = createTransaction(
                senderWalletId,
                from.getId(),
                to.getId(),
                value,
                TransactionType.TRANSFER,
                TransactionStatus.SUCCESS,
                key
        );

        WalletTransaction savedTransaction =
                transactionRepository.saveAndFlush(transaction);

        return TransactionResponse.from(savedTransaction, from.getBalance());
    }

    /*
     * Get transactions belonging to the logged-in user's wallet.
     */
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> history(
            String email,
            int page,
            int size) {

        User user = userOf(email);

        Wallet wallet = walletOf(user);

        var pageable = Pagination.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        var transactions =
                transactionRepository.findByFromWalletIdOrToWalletId(
                        wallet.getId(),
                        wallet.getId(),
                        pageable
                );

        return PageResponse.of(
                transactions,
                TransactionResponse::from
        );
    }

    /*
     * Find transaction by wallet ID, type and idempotency key.
     */
    private Optional<WalletTransaction> find(
            Long walletId,
            TransactionType type,
            String key) {

        return transactionRepository
                .findByOwnerWalletIdAndTypeAndIdempotencyKey(
                        walletId,
                        type,
                        key
                );
    }

    /*
     * A key that was already used: return the stored result if the request is
     * the same, reject it if the same key is reused for a different request.
     */
    private TransactionResponse replay(
            WalletTransaction existing,
            BigDecimal amount,
            Long toWalletId,
            BigDecimal availableBalance) {

        boolean samePayload =
                existing.getAmount().compareTo(amount) == 0
                        && existing.getToWalletId().equals(toWalletId);

        if (!samePayload) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Idempotency-Key was already used for a different request",
                    "IDEMPOTENCY_KEY_REUSED"
            );
        }

        return TransactionResponse.from(existing, availableBalance).asReplay();
    }

    /*
     * Create a transaction entity.
     */
    private WalletTransaction createTransaction(
            Long ownerWalletId,
            Long fromWalletId,
            Long toWalletId,
            BigDecimal amount,
            TransactionType type,
            TransactionStatus status,
            String key) {

        WalletTransaction transaction = new WalletTransaction();

        transaction.setOwnerWalletId(ownerWalletId);
        transaction.setFromWalletId(fromWalletId);
        transaction.setToWalletId(toWalletId);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setIdempotencyKey(key);

        return transaction;
    }

    private User userOf(String email) {

        return userRepository
                .findByEmail(normalize(email))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found",
                        "USER_NOT_FOUND"
                ));
    }

    private Wallet walletOf(User user) {

        return walletRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found",
                        "WALLET_NOT_FOUND"
                ));
    }

    private Long walletIdOf(User user) {

        return walletRepository
                .findIdByUserId(user.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found",
                        "WALLET_NOT_FOUND"
                ));
    }

    private String requireKey(String key) {

        if (key == null || key.isBlank() ||
                key.length() > MAX_KEY_LENGTH) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must contain 1 to "
                            + MAX_KEY_LENGTH + " characters",
                    "INVALID_IDEMPOTENCY_KEY"
            );
        }

        return key;
    }

    private BigDecimal scale(BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Amount must be positive",
                    "INVALID_AMOUNT"
            );
        }

        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Amount cannot have more than 2 decimal places",
                    "INVALID_AMOUNT_SCALE"
            );
        }
    }

    private String normalize(String email) {

        if (email == null || email.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required",
                    "INVALID_EMAIL"
            );
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}