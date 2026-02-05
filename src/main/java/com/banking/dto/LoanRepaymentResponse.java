package com.banking.dto;

import com.banking.entity.LoanRepayment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRepaymentResponse {
    private String paymentRef;
    private BigDecimal amount;
    private BigDecimal remainingBalance;
    private LoanRepayment.PaymentStatus status;
    private LocalDateTime paymentDate;
}
