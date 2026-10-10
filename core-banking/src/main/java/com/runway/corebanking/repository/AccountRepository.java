package com.runway.corebanking.repository;

import com.runway.corebanking.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Derives the true balance by summing ledger entries.
     * Credits increase the balance; debits decrease it.
     *
     * WHY JPQL not native SQL: portability + Hibernate maps field names,
     * not column names — `le.entryType` not `entry_type`.
     *
     * This query is the correctness check for cached_balance.
     */
    @Query("""
           SELECT
               COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0)
             - COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT'  THEN le.amount ELSE 0 END), 0)
           FROM LedgerEntry le
           WHERE le.account.id = :accountId
           """)
    BigDecimal computeTrueBalance(@Param("accountId") UUID accountId);
}
