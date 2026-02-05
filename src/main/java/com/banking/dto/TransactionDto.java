package com.banking.dto;

import com.banking.entity.Account;
import com.banking.entity.Transaction;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {
    private Long id;
    private String transactionRef;
    private Long sourceAccountId;
    private Long destinationAccountId;
    private Transaction.TransactionType transactionType;
    private BigDecimal amount;
    private String currency;
    private String description;
    private Transaction.TransactionStatus status;
    private LocalDateTime createdAt;
}

