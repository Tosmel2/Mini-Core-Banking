package com.banking.repository;

import com.banking.entity.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {
    List<LoanRepayment> findByLoanIdOrderByPaymentDateDesc(Long loanId);

    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.loan.id = :loanId ORDER BY lr.paymentDate DESC")
    List<LoanRepayment> findRepaymentsForLoan(@Param("loanId") Long loanId);

    @Query("SELECT SUM(lr.amount) FROM LoanRepayment lr WHERE lr.loan.id = :loanId AND lr.status = 'COMPLETED'")
    java.math.BigDecimal getTotalRepaidAmount(@Param("loanId") Long loanId);
}