package com.banking.service;

import com.banking.dto.*;
import com.banking.entity.*;
import com.banking.exception.BadRequestException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.exception.UnauthorizedException;
import com.banking.repository.*;
import com.banking.util.LoanCalculator;
import com.banking.util.LoanNumberGenerator;
import com.banking.util.PaymentRefGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;

    @Transactional
    public LoanApplicationResponse applyForLoan(LoanApplicationRequest request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Validate account ownership
        if (!account.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You don't have access to this account");
        }

        // Generate loan number
        String loanNumber = LoanNumberGenerator.generate();

        // Calculate interest rate based on loan type (simplified logic)
        BigDecimal interestRate = determineInterestRate(request.getLoanType());

        // Calculate monthly payment
        BigDecimal monthlyPayment = LoanCalculator.calculateMonthlyPayment(
                request.getPrincipalAmount(),
                interestRate,
                request.getTermMonths()
        );

        Loan loan = Loan.builder()
                .loanNumber(loanNumber)
                .user(user)
                .account(account)
                .loanType(request.getLoanType())
                .principalAmount(request.getPrincipalAmount())
                .interestRate(interestRate)
                .termMonths(request.getTermMonths())
                .monthlyPayment(monthlyPayment)
                .outstandingBalance(request.getPrincipalAmount())
                .status(Loan.LoanStatus.PENDING)
                .purpose(request.getPurpose())
                .build();

        Loan savedLoan = loanRepository.save(loan);

        return LoanApplicationResponse.builder()
                .loanNumber(savedLoan.getLoanNumber())
                .status(savedLoan.getStatus())
                .principalAmount(savedLoan.getPrincipalAmount())
                .interestRate(savedLoan.getInterestRate())
                .termMonths(savedLoan.getTermMonths())
                .estimatedMonthlyPayment(savedLoan.getMonthlyPayment())
                .applicationDate(savedLoan.getApplicationDate())
                .build();
    }

    @Transactional(readOnly = true)
    public LoanListResponse getUserLoans() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<LoanDto> loans = loanRepository.findByUserIdOrderByApplicationDateDesc(user.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return LoanListResponse.builder()
                .loans(loans)
                .build();
    }

    @Transactional(readOnly = true)
    public LoanDto getLoanDetails(Long loanId) {
        Loan loan = getLoanAndValidateOwnership(loanId);
        return mapToDto(loan);
    }

    @Transactional
    public LoanRepaymentResponse makeLoanRepayment(Long loanId, LoanRepaymentRequest request) {
        Loan loan = getLoanAndValidateOwnership(loanId);

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new BadRequestException("Loan is not active");
        }

        if (request.getAmount().compareTo(loan.getOutstandingBalance()) > 0) {
            throw new BadRequestException("Payment amount exceeds outstanding balance");
        }

        // Calculate principal and interest portions
        BigDecimal[] portions = LoanCalculator.calculatePaymentPortions(
                request.getAmount(),
                loan.getOutstandingBalance(),
                loan.getInterestRate()
        );

        BigDecimal principalPortion = portions[0];
        BigDecimal interestPortion = portions[1];

        // Create repayment record
        LoanRepayment repayment = LoanRepayment.builder()
                .loan(loan)
                .paymentRef(PaymentRefGenerator.generate())
                .amount(request.getAmount())
                .principalAmount(principalPortion)
                .interestAmount(interestPortion)
                .paymentMethod(request.getPaymentMethod())
                .status(LoanRepayment.PaymentStatus.COMPLETED)
                .build();

        // Update loan balance
        loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(principalPortion));

        // Check if loan is fully paid
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(Loan.LoanStatus.CLOSED);
        }

        loanRepaymentRepository.save(repayment);
        loanRepository.save(loan);

        return LoanRepaymentResponse.builder()
                .paymentRef(repayment.getPaymentRef())
                .amount(repayment.getAmount())
                .remainingBalance(loan.getOutstandingBalance())
                .status(repayment.getStatus())
                .paymentDate(repayment.getPaymentDate())
                .build();
    }

    @Transactional(readOnly = true)
    public LoanRepaymentListResponse getLoanRepayments(Long loanId) {
        Loan loan = getLoanAndValidateOwnership(loanId);

        List<LoanRepaymentDto> repayments = loanRepaymentRepository.findRepaymentsForLoan(loanId)
                .stream()
                .map(this::mapRepaymentToDto)
                .collect(Collectors.toList());

        BigDecimal totalRepaid = loanRepaymentRepository.getTotalRepaidAmount(loanId);
        if (totalRepaid == null) totalRepaid = BigDecimal.ZERO;

        return LoanRepaymentListResponse.builder()
                .repayments(repayments)
                .totalRepaid(totalRepaid)
                .remainingBalance(loan.getOutstandingBalance())
                .build();
    }

    private Loan getLoanAndValidateOwnership(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!loan.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You don't have access to this loan");
        }

        return loan;
    }

    private BigDecimal determineInterestRate(Loan.LoanType loanType) {
        return switch (loanType) {
            case PERSONAL -> new BigDecimal("12.50");
            case BUSINESS -> new BigDecimal("10.00");
            case MORTGAGE -> new BigDecimal("7.50");
        };
    }

    private LoanDto mapToDto(Loan loan) {
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

    private LoanRepaymentDto mapRepaymentToDto(LoanRepayment repayment) {
        return LoanRepaymentDto.builder()
                .id(repayment.getId())
                .paymentRef(repayment.getPaymentRef())
                .amount(repayment.getAmount())
                .principalAmount(repayment.getPrincipalAmount())
                .interestAmount(repayment.getInterestAmount())
                .paymentDate(repayment.getPaymentDate())
                .paymentMethod(repayment.getPaymentMethod())
                .status(repayment.getStatus())
                .build();
    }
}