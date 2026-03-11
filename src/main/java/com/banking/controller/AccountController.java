package com.banking.controller;

import com.banking.dto.*;
import com.banking.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Accounts", description = "Account management APIs")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "Get all accounts for authenticated user")
    public ResponseEntity<AccountListResponse> getUserAccounts() {
        AccountListResponse response = accountService.getUserAccounts();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new account")
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountDto response = accountService.createAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get details of a single account")
    public ResponseEntity<AccountDto> getAccount(@PathVariable Long accountId) {
        AccountDto response = accountService.getAccount(accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current")
    @Operation(summary = "Get current selected account for authenticated user")
    public ResponseEntity<AccountDto> getCurrentAccount() {
        AccountDto response = accountService.getCurrentAccount();
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/current")
    @Operation(summary = "Set current selected account for authenticated user")
    public ResponseEntity<Void> setCurrentAccount(@Valid @RequestBody SetCurrentAccountRequest request) {
        accountService.setCurrentAccount(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/lookup/{accountNumber}")
    @Operation(summary = "Lookup beneficiary details by account number for transfer confirmation")
    public ResponseEntity<BeneficiaryLookupResponse> lookupBeneficiary(@PathVariable String accountNumber) {
        BeneficiaryLookupResponse response = accountService.lookupBeneficiary(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balance")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(@PathVariable Long accountId) {
        AccountBalanceResponse response = accountService.getAccountBalance(accountId);
        return ResponseEntity.ok(response);
    }
}