package com.banking.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LoanCalculator {

    /**
     * Calculate monthly payment using the formula:
     * M = P * [r(1+r)^n] / [(1+r)^n - 1]
     * Where:
     * M = Monthly payment
     * P = Principal amount
     * r = Monthly interest rate (annual rate / 12 / 100)
     * n = Number of months
     */
    public static BigDecimal calculateMonthlyPayment(
            BigDecimal principal,
            BigDecimal annualInterestRate,
            int termMonths) {

        if (termMonths == 0) {
            return BigDecimal.ZERO;
        }

        // Convert annual interest rate to monthly decimal
        BigDecimal monthlyRate = annualInterestRate
                .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP)
                .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP);
        }

        // Calculate (1 + r)^n
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powerTerm = onePlusRate.pow(termMonths);

        // Calculate numerator: P * r * (1+r)^n
        BigDecimal numerator = principal
                .multiply(monthlyRate)
                .multiply(powerTerm);

        // Calculate denominator: (1+r)^n - 1
        BigDecimal denominator = powerTerm.subtract(BigDecimal.ONE);

        // Monthly payment
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate how much of a payment goes to principal vs interest
     * Returns array: [principalPortion, interestPortion]
     */
    public static BigDecimal[] calculatePaymentPortions(
            BigDecimal paymentAmount,
            BigDecimal outstandingBalance,
            BigDecimal annualInterestRate) {

        // Calculate monthly interest
        BigDecimal monthlyRate = annualInterestRate
                .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP)
                .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);

        BigDecimal interestPortion = outstandingBalance
                .multiply(monthlyRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal principalPortion = paymentAmount
                .subtract(interestPortion)
                .setScale(2, RoundingMode.HALF_UP);

        // Ensure principal doesn't exceed outstanding balance
        if (principalPortion.compareTo(outstandingBalance) > 0) {
            principalPortion = outstandingBalance;
        }

        return new BigDecimal[]{principalPortion, interestPortion};
    }

    /**
     * Calculate total interest to be paid over the life of the loan
     */
    public static BigDecimal calculateTotalInterest(
            BigDecimal principal,
            BigDecimal monthlyPayment,
            int termMonths) {

        BigDecimal totalPayments = monthlyPayment.multiply(new BigDecimal(termMonths));
        return totalPayments.subtract(principal);
    }
}