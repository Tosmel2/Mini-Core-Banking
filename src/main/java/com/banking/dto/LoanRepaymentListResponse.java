package com.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRepaymentListResponse {
    private List<LoanRepaymentDto> repayments;
    private BigDecimal totalRepaid;
    private BigDecimal remainingBalance;
}
