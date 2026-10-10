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
 * A bank account owned by a User (CHECKING or SAVINGS_VAULT).
 *
 * <p><b>Interview Q: Why keep cached_balance at all?</b>
 * Computing the true balance via SUM(ledger_entries) requires reading potentially
 * thousands of rows. {@code cached_balance} is a denormalized optimisation updated
 * atomically in the same @Transactional boundary as the ledger write, so it always
 * reflects committed state. The ledger is still the source of truth — a reconciliation
 * job can verify them at any time.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    /**
     * Read-cache. Updated within the same transaction as ledger entries.
     * Never use this as the financial source of truth.
     */
    @Column(name = "cached_balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cachedBalance = BigDecimal.ZERO;

    /**
     * Annual Percentage Yield — e.g. 0.0450 = 4.5%.
     * Only meaningful for SAVINGS_VAULT accounts.
     */
    @Column(name = "apy", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal apy = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // ── Associations ─────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    // ── Enum ──────────────────────────────────────────────────────────────────
    public enum AccountType {
        CHECKING, SAVINGS_VAULT
    }
}
