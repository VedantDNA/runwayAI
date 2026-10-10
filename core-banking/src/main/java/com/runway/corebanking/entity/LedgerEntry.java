package com.runway.corebanking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single line in the immutable double-entry ledger.
 *
 * <p><b>Key invariant</b>: For every Transaction, the sum of all DEBIT
 * LedgerEntry amounts must equal the sum of all CREDIT LedgerEntry amounts.
 * This is enforced by {@code TransferService} inside a single @Transactional
 * boundary — if either insert fails, both roll back.
 *
 * <p>@Immutable tells Hibernate to skip dirty-checking for this entity,
 * which is both a performance win and a correctness guard (ledger rows
 * must never be mutated after settlement).
 */
@Entity
@Immutable                        // Hibernate: skip dirty-check, enforce read-only after insert
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, updatable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10, updatable = false)
    private EntryType entryType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    /**
     * Running balance of the account AFTER this entry is applied.
     * Stored for fast statement rendering without re-aggregating all prior rows.
     */
    @Column(name = "running_balance", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal runningBalance;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // No @Setter on purpose — immutable after creation.

    // ── Enum ──────────────────────────────────────────────────────────────────
    public enum EntryType {
        DEBIT,  // money leaving an account (balance decreases)
        CREDIT  // money entering an account (balance increases)
    }
}
