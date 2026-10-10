package com.runway.corebanking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The business-level event: "Netflix charged $15.99 on 2024-03-01".
 *
 * <p>Each Transaction spawns exactly two {@link LedgerEntry} rows:
 * a DEBIT on the source account and a CREDIT on the destination.
 * The transaction itself records the high-level metadata; the ledger
 * records the immutable accounting detail.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** Null when the counterpart is an external party (e.g. a merchant). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterpart_account_id")
    private Account counterpartAccount;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    @Builder.Default
    private Category category = Category.OTHER;

    @Column(name = "merchant_name", length = 255)
    private String merchantName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;

    // ── Associations ─────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    // ── Enums ─────────────────────────────────────────────────────────────────
    public enum Status {
        PENDING, COMPLETED, FAILED, REVERSED
    }

    public enum Category {
        TRANSFER, SUBSCRIPTION, GROCERIES, DINING, UTILITIES, RENT,
        SALARY, INTEREST, INVESTMENT, HEALTHCARE, ENTERTAINMENT,
        TRAVEL, SHOPPING, OTHER
    }
}
