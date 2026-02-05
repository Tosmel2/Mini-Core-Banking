package com.banking.service;

import com.banking.dto.*;
import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.exception.ResourceNotFoundException;
import com.banking.exception.UnauthorizedException;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import com.banking.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    @Transactional
    public AccountDto createAccount(CreateAccountRequest request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Generate unique account number
        String accountNumber = AccountNumberGenerator.generate();
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            accountNumber = AccountNumberGenerator.generate();
        }

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .user(user)
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);

        // Process initial deposit if provided
        if (request.getInitialDeposit() != null && request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            DepositRequest depositRequest = new DepositRequest(
                    savedAccount.getId(),
                    request.getInitialDeposit(),
                    "Initial deposit"
            );
            transactionService.deposit(depositRequest);
            savedAccount = accountRepository.findById(savedAccount.getId()).orElseThrow();
        }

        return mapToDto(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountListResponse getUserAccounts() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<AccountDto> accounts = accountRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return AccountListResponse.builder()
                .accounts(accounts)
                .build();
    }

    @Transactional(readOnly = true)
    public AccountBalanceResponse getAccountBalance(Long accountId) {
        Account account = getAccountAndValidateOwnership(accountId);

        return AccountBalanceResponse.builder()
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .availableBalance(account.getBalance()) // Can add logic for holds/pending
                .build();
    }

    @Transactional(readOnly = true)
    public Account getAccountAndValidateOwnership(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!account.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You don't have access to this account");
        }

        return account;
    }

    private AccountDto mapToDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }
}