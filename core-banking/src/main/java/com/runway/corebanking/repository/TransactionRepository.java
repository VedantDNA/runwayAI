package com.runway.corebanking.repository;

import com.runway.corebanking.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /** Paginated list of all transactions for an account, newest first. */
    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    /**
     * Paginated + category-filtered — used by GET /api/v1/transactions?category=.
     * When category is null this falls back to the query above via a custom query.
     */
    @Query("""
           SELECT t FROM Transaction t
           WHERE t.account.id = :accountId
             AND (:category IS NULL OR t.category = :category)
           ORDER BY t.createdAt DESC
           """)
    Page<Transaction> findByAccountIdAndCategory(
            @Param("accountId") UUID accountId,
            @Param("category") Transaction.Category category,
            Pageable pageable);

    /**
     * Monthly spend by category — used to check budget_goals after each transfer.
     */
    @Query("""
           SELECT COALESCE(SUM(t.amount), 0)
           FROM Transaction t
           WHERE t.account.id    = :accountId
             AND t.category      = :category
             AND t.status        = 'COMPLETED'
             AND t.createdAt    >= :from
             AND t.createdAt     < :to
           """)
    BigDecimal sumByAccountCategoryAndPeriod(
            @Param("accountId") UUID accountId,
            @Param("category")  Transaction.Category category,
            @Param("from")      OffsetDateTime from,
            @Param("to")        OffsetDateTime to);
}
