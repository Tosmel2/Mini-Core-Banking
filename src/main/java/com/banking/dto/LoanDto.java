package com.banking.dto;

import com.banking.entity.Loan;
import com.banking.entity.LoanRepayment;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ============ Loan DTOs ============

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDto {
    private Long id;
    private String loanNumber;
    private Loan.LoanType loanType;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer termMonths;
    private BigDecimal monthlyPayment;
    private BigDecimal outstandingBalance;
    private Loan.LoanStatus status;
    private LocalDateTime applicationDate;
    private LocalDateTime disbursementDate;
    private LocalDate maturityDate;
}

// ============ Loan Repayment DTOs ============

