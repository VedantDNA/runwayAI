package com.runway.corebanking.repository;

import com.runway.corebanking.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    /**
     * The global ledger invariant query.
     * SUM(all DEBITs) must equal SUM(all CREDITs) across the entire ledger.
     * A non-zero result indicates a bug in TransferService.
     * Used in the JUnit correctness test (Granule 2-J).
     */
    @Query("""
           SELECT
               COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT'  THEN le.amount ELSE 0 END), 0)
             - COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0)
           FROM LedgerEntry le
           """)
    BigDecimal computeGlobalImbalance();

    /**
     * Per-account sum of debits for test assertions.
     */
    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le WHERE le.account.id = :accountId AND le.entryType = 'DEBIT'")
    BigDecimal sumDebitsByAccount(@Param("accountId") UUID accountId);

    /**
     * Per-account sum of credits for test assertions.
     */
    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le WHERE le.account.id = :accountId AND le.entryType = 'CREDIT'")
    BigDecimal sumCreditsByAccount(@Param("accountId") UUID accountId);
}
