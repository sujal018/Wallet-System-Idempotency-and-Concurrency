package com.sujal.wallet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sujal.wallet.dto.PageResponse;
import com.sujal.wallet.dto.TransactionResponse;
import com.sujal.wallet.dto.WalletResponse;
import com.sujal.wallet.service.AdminService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/wallets")
    public ResponseEntity<PageResponse<WalletResponse>> getAllWallets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                adminService.getAllWallets(page, size)
        );
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                adminService.getAllTransactions(page, size)
        );
    }
}