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
public class LoanRepaymentDto {
    private Long id;
    private String paymentRef;
    private BigDecimal amount;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private LocalDateTime paymentDate;
    private LoanRepayment.PaymentMethod paymentMethod;
    private LoanRepayment.PaymentStatus status;
}
