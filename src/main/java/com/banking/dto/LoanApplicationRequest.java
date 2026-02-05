package com.banking.dto;

import com.banking.entity.Loan;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Loan type is required")
    private Loan.LoanType loanType;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is 1000")
    @DecimalMax(value = "1000000.00", message = "Maximum loan amount is 1,000,000")
    private BigDecimal principalAmount;

    @NotNull(message = "Term is required")
    @Min(value = 6, message = "Minimum term is 6 months")
    @Max(value = 360, message = "Maximum term is 360 months")
    private Integer termMonths;

    @NotBlank(message = "Purpose is required")
    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    private String purpose;
}
