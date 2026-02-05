package com.banking.service;

import com.banking.dto.*;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import com.banking.exception.BadRequestException;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import com.banking.util.TransactionRefGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        validateAccountOwnership(account);
        validateAccountStatus(account);

        // Create transaction
        Transaction transaction = Transaction.builder()
                .transactionRef(TransactionRefGenerator.generate())
                .destinationAccount(account)
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .currency(account.getCurrency())
                .description(request.getDescription())
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();

        // Update account balance
        account.credit(request.getAmount());

        transactionRepository.save(transaction);
        accountRepository.save(account);

        return TransactionResponse.builder()
                .transactionRef(transaction.getTransactionRef())
                .accountId(account.getId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .newBalance(account.getBalance())
                .status(transaction.getStatus())
                .timestamp(transaction.getCreatedAt())
                .build();
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        validateAccountOwnership(account);
        validateAccountStatus(account);

        // Check sufficient balance
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for withdrawal");
        }

        // Create transaction
        Transaction transaction = Transaction.builder()
                .transactionRef(TransactionRefGenerator.generate())
                .sourceAccount(account)
                .transactionType(Transaction.TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .currency(account.getCurrency())
                .description(request.getDescription())
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();

        // Update account balance
        account.debit(request.getAmount());

        transactionRepository.save(transaction);
        accountRepository.save(account);

        return TransactionResponse.builder()
                .transactionRef(transaction.getTransactionRef())
                .accountId(account.getId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .newBalance(account.getBalance())
                .status(transaction.getStatus())
                .timestamp(transaction.getCreatedAt())
                .build();
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        // Get source account
        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        validateAccountOwnership(sourceAccount);
        validateAccountStatus(sourceAccount);

        // Get destination account
        Account destinationAccount = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        validateAccountStatus(destinationAccount);

        // Validate transfer
        if (sourceAccount.getId().equals(destinationAccount.getId())) {
            throw new BadRequestException("Cannot transfer to the same account");
        }

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for transfer");
        }

        // Create transaction
        Transaction transaction = Transaction.builder()
                .transactionRef(TransactionRefGenerator.generate())
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .transactionType(Transaction.TransactionType.TRANSFER)
                .amount(request.getAmount())
                .currency(sourceAccount.getCurrency())
                .description(request.getDescription())
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();

        // Update balances
        sourceAccount.debit(request.getAmount());
        destinationAccount.credit(request.getAmount());

        transactionRepository.save(transaction);
        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        return TransactionResponse.builder()
                .transactionRef(transaction.getTransactionRef())
                .accountId(sourceAccount.getId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .newBalance(sourceAccount.getBalance())
                .status(transaction.getStatus())
                .timestamp(transaction.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public TransactionListResponse getTransactions(Long accountId,
                                                   LocalDateTime startDate,
                                                   LocalDateTime endDate,
                                                   int page, int size) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage;

        if (accountId != null) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            validateAccountOwnership(account);

            if (startDate != null && endDate != null) {
                transactionPage = transactionRepository.findByAccountIdAndDateRange(
                        accountId, startDate, endDate, pageable);
            } else {
                transactionPage = transactionRepository.findByAccountId(accountId, pageable);
            }
        } else {
            transactionPage = transactionRepository.findByUserId(user.getId(), pageable);
        }

        List<TransactionDto> transactions = transactionPage.getContent()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        PaginationDto pagination = PaginationDto.builder()
                .page(transactionPage.getNumber())
                .size(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .build();

        return TransactionListResponse.builder()
                .transactions(transactions)
                .pagination(pagination)
                .build();
    }

    private void validateAccountOwnership(Account account) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!account.getUser().getEmail().equals(userEmail)) {
            throw new BadRequestException("You don't have access to this account");
        }
    }

    private void validateAccountStatus(Account account) {
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }
    }

    private TransactionDto mapToDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .transactionRef(transaction.getTransactionRef())
                .sourceAccountId(transaction.getSourceAccount() != null ?
                        transaction.getSourceAccount().getId() : null)
                .destinationAccountId(transaction.getDestinationAccount() != null ?
                        transaction.getDestinationAccount().getId() : null)
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}