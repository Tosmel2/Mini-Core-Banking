package com.banking.service;

import com.banking.dto.*;
import com.banking.entity.Account;
import com.banking.entity.Loan;
import com.banking.entity.User;
import com.banking.exception.BadRequestException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banking.entity.Transaction;

import java.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserDto> getAllUsers(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage;

        if (search != null && !search.trim().isEmpty()) {
            userPage = userRepository.searchUsers(search, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        return userPage.map(this::mapUserToDto);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUserStatus(Long userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsActive(isActive);
        User updatedUser = userRepository.save(user);

        return mapUserToDto(updatedUser);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<LoanDto> getPendingLoans(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Loan> loanPage = loanRepository.findByStatus(Loan.LoanStatus.PENDING, pageable);

        return loanPage.map(this::mapLoanToDto);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOAN_OFFICER')")
    public LoanDto approveLoan(Long loanId, LoanApprovalRequest request) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.PENDING) {
            throw new IllegalStateException("Loan is not in pending status");
        }

        // Update interest rate if provided
        if (request.getInterestRate() != null) {
            loan.setInterestRate(request.getInterestRate());

            // Recalculate monthly payment with new interest rate
            BigDecimal monthlyPayment = com.banking.util.LoanCalculator.calculateMonthlyPayment(
                    loan.getPrincipalAmount(),
                    request.getInterestRate(),
                    loan.getTermMonths()
            );
            loan.setMonthlyPayment(monthlyPayment);
        }

        loan.setStatus(Loan.LoanStatus.APPROVED);
        loan.setApprovalDate(LocalDateTime.now());

        // Set maturity date
        loan.setMaturityDate(LocalDateTime.now().plusMonths(loan.getTermMonths()).toLocalDate());

        Loan updatedLoan = loanRepository.save(loan);
        return mapLoanToDto(updatedLoan);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOAN_OFFICER')")
    public LoanDto disburseLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        // Validate loan can be disbursed
        if (loan.getStatus() != Loan.LoanStatus.APPROVED) {
            throw new BadRequestException("Only approved loans can be disbursed. Current status: " + loan.getStatus());
        }

        // Get the linked account
        Account account = loan.getAccount();
        if (account == null) {
            throw new BadRequestException("Loan has no linked account for disbursement");
        }

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BadRequestException("Account is not active. Cannot disburse loan.");
        }

        // Credit the loan amount to the account
        account.credit(loan.getPrincipalAmount());
        accountRepository.save(account);

        // Update loan status to ACTIVE
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        loan.setDisbursementDate(LocalDateTime.now());
        loan.setOutstandingBalance(loan.getPrincipalAmount());

        // Calculate and set maturity date
        LocalDate maturityDate = LocalDate.now().plusMonths(loan.getTermMonths());
        loan.setMaturityDate(maturityDate);

        Loan updatedLoan = loanRepository.save(loan);

        // Optional: Create a transaction record for the disbursement
        createDisbursementTransaction(loan, account);

        return mapLoanToDto(updatedLoan);
    }

    // Helper method to create transaction record for disbursement
    private void createDisbursementTransaction(Loan loan, Account account) {
        Transaction transaction = Transaction.builder()
                .transactionRef(generateTransactionRef())
                .destinationAccount(account)
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .amount(loan.getPrincipalAmount())
                .currency(account.getCurrency())
                .description("Loan disbursement - " + loan.getLoanNumber())
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();

        transactionRepository.save(transaction);
    }

    // Helper method to generate transaction reference
    private String generateTransactionRef() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int randomNum = 100 + new java.security.SecureRandom().nextInt(900);
        return "TXN" + timestamp + randomNum;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOAN_OFFICER')")
    public LoanDto rejectLoan(Long loanId, LoanRejectionRequest request) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.PENDING) {
            throw new IllegalStateException("Loan is not in pending status");
        }

        loan.setStatus(Loan.LoanStatus.REJECTED);
        loan.setRejectionReason(request.getReason());

        Loan updatedLoan = loanRepository.save(loan);
        return mapLoanToDto(updatedLoan);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // User statistics
        long totalUsers = userRepository.count();
        stats.put("totalUsers", totalUsers);

        // Account statistics
        long totalAccounts = accountRepository.count();
        long activeAccounts = accountRepository.countActiveAccounts();
        BigDecimal totalDeposits = accountRepository.getTotalDeposits();
        if (totalDeposits == null) totalDeposits = BigDecimal.ZERO;

        stats.put("totalAccounts", totalAccounts);
        stats.put("activeAccounts", activeAccounts);
        stats.put("totalDeposits", totalDeposits);

        // Loan statistics
        long pendingLoans = loanRepository.countByStatus(Loan.LoanStatus.PENDING);
        long activeLoans = loanRepository.countByStatus(Loan.LoanStatus.ACTIVE);
        long approvedLoans = loanRepository.countByStatus(Loan.LoanStatus.APPROVED);
        BigDecimal totalOutstandingLoans = loanRepository.getTotalOutstandingLoans();
        if (totalOutstandingLoans == null) totalOutstandingLoans = BigDecimal.ZERO;

        stats.put("pendingLoans", pendingLoans);
        stats.put("activeLoans", activeLoans);
        stats.put("approvedLoans", approvedLoans);
        stats.put("totalLoans", totalOutstandingLoans);

        return stats;
    }

    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .address(user.getAddress())
                .isActive(user.getIsActive())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .build();
    }

    private LoanDto mapLoanToDto(Loan loan) {
        return LoanDto.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .loanType(loan.getLoanType())
                .principalAmount(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .termMonths(loan.getTermMonths())
                .monthlyPayment(loan.getMonthlyPayment())
                .outstandingBalance(loan.getOutstandingBalance())
                .status(loan.getStatus())
                .applicationDate(loan.getApplicationDate())
                .disbursementDate(loan.getDisbursementDate())
                .maturityDate(loan.getMaturityDate())
                .build();
    }
}