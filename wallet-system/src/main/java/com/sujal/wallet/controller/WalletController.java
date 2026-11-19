package com.sujal.wallet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.sujal.wallet.dto.AddMoneyRequest;
import com.sujal.wallet.dto.PageResponse;
import com.sujal.wallet.dto.TransactionResponse;
import com.sujal.wallet.dto.TransferRequest;
import com.sujal.wallet.dto.WalletResponse;
import com.sujal.wallet.entity.TransactionStatus;
import com.sujal.wallet.exception.ApiException;
import com.sujal.wallet.security.JwtPrincipal;
import com.sujal.wallet.service.WalletService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(
            @AuthenticationPrincipal JwtPrincipal currentUser) {

        return ResponseEntity.ok(
                walletService.getWallet(currentUser.getEmail())
        );
    }

    @PostMapping("/add")
    public ResponseEntity<TransactionResponse> addMoney(
            @AuthenticationPrincipal JwtPrincipal currentUser,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AddMoneyRequest request) {

        return ResponseEntity.ok(
                walletService.addMoney(
                        currentUser.getEmail(),
                        idempotencyKey,
                        request.getAmount()
                )
        );
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @AuthenticationPrincipal JwtPrincipal currentUser,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        TransactionResponse result = walletService.transfer(
                currentUser.getEmail(),
                idempotencyKey,
                request.getReceiverEmail(),
                request.getAmount()
        );

        // First-time FAILED attempt: already saved (the service transaction has
        // committed), so report it as a normal error response. A replay of the
        // same key returns the stored result (200) instead of failing again.
        if (result.getStatus() == TransactionStatus.FAILED && !result.isReplayed()) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insufficient balance",
                    "INSUFFICIENT_FUNDS",
                    List.of("transactionId: " + result.getId(),
                            "availableBalance: " + result.getAvailableBalance())
            );
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal JwtPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                walletService.history(
                        currentUser.getEmail(),
                        page,
                        size
                )
        );
    }
}