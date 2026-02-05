
package com.banking.dto;

import com.banking.entity.Account;
import com.banking.entity.Transaction;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ============ Account DTOs ============

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDto {
    private Long id;
    private String accountNumber;
    private Account.AccountType accountType;
    private BigDecimal balance;
    private String currency;
    private Account.AccountStatus status;
    private LocalDateTime createdAt;
}

