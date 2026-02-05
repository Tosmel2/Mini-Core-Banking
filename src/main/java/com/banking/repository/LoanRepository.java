package com.banking.repository;

import com.banking.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserId(Long userId);
    List<Loan> findByStatus(Loan.LoanStatus status);
    Page<Loan> findByStatus(Loan.LoanStatus status, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.user.id = :userId ORDER BY l.applicationDate DESC")
    List<Loan> findByUserIdOrderByApplicationDateDesc(@Param("userId") Long userId);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = :status")
    long countByStatus(@Param("status") Loan.LoanStatus status);

    @Query("SELECT SUM(l.outstandingBalance) FROM Loan l WHERE l.status = 'ACTIVE'")
    java.math.BigDecimal getTotalOutstandingLoans();
}