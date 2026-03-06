package com.farmeazy.repository;

import com.farmeazy.entity.PlatformWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PlatformWalletRepository extends JpaRepository<PlatformWallet, Long> {

    List<PlatformWallet> findByTransactionType(PlatformWallet.TransactionType transactionType);

    List<PlatformWallet> findByReferenceTypeAndReferenceId(PlatformWallet.ReferenceType referenceType, Long referenceId);

    @Query("SELECT pw FROM PlatformWallet pw ORDER BY pw.createdAt DESC LIMIT 1")
    PlatformWallet findLatestTransaction();

    @Query("SELECT SUM(pw.amount) FROM PlatformWallet pw WHERE pw.transactionType = 'CREDIT'")
    BigDecimal getTotalCredits();

    @Query("SELECT SUM(pw.amount) FROM PlatformWallet pw WHERE pw.transactionType = 'DEBIT'")
    BigDecimal getTotalDebits();

    @Query("SELECT COALESCE((SELECT SUM(pw.amount) FROM PlatformWallet pw WHERE pw.transactionType = 'CREDIT'), 0) - " +
           "COALESCE((SELECT SUM(pw.amount) FROM PlatformWallet pw WHERE pw.transactionType = 'DEBIT'), 0)")
    BigDecimal getCurrentBalance();
}
