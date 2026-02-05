package com.banking.dto;

import com.banking.entity.Transaction;
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
public class TransactionResponse {
    private String transactionRef;
    private Long accountId;
    private Transaction.TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal newBalance;
    private Transaction.TransactionStatus status;
    private LocalDateTime timestamp;
}
