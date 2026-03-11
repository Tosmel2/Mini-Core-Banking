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

    // ===== Newly added methods =====

    @Transactional(readOnly = true)
    public AccountDto getAccount(Long accountId) {
        Account account = getAccountAndValidateOwnership(accountId);
        return mapToDto(account);
    }

    @Transactional(readOnly = true)
    public AccountDto getCurrentAccount() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Account current = user.getCurrentAccount();
        if (current == null) {
            return null;
        }
        if (!current.getUser().getId().equals(user.getId())) {
            return null;
        }
        return mapToDto(current);
    }

    @Transactional
    public void setCurrentAccount(SetCurrentAccountRequest request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = getAccountAndValidateOwnership(request.getAccountId());
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new UnauthorizedException("Cannot select a non-active account as current");
        }
        user.setCurrentAccount(account);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public BeneficiaryLookupResponse lookupBeneficiary(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResourceNotFoundException("Destination account not found");
        }
        User holder = account.getUser();
        String display = buildHolderDisplayName(holder);
        return BeneficiaryLookupResponse.builder()
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .holderDisplayName(display)
                .build();
    }

    private String buildHolderDisplayName(User user) {
        String first = user.getFirstName();
        String last = user.getLastName();
        if (first == null && last == null) {
            return "Account Holder";
        }
        if (first == null) {
            return last;
        }
        if (last == null || last.isBlank()) {
            return first;
        }
        return first + " " + last.charAt(0) + ".";
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