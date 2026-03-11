package com.banking.dto;

import com.banking.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryLookupResponse {
    private String accountNumber;
    private Account.AccountType accountType;
    private String currency;
    private Account.AccountStatus status;
    private String holderDisplayName;
}
