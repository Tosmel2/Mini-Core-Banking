package com.banking.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDisbursementRequest {
    private String comments; // Optional comments about disbursement
}