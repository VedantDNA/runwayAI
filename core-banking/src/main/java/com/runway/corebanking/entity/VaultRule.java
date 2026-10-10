package com.runway.corebanking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A rule that drives autonomous vault management.
 *
 * <p>Examples:
 * <ul>
 *   <li>AUTO_SWEEP_EXCESS: whenever checking balance exceeds $3,000, sweep the
 *       overage to the savings vault.</li>
 *   <li>PERCENTAGE_SAVE: on every incoming salary credit, save 20% to savings.</li>
 * </ul>
 */
@Entity
@Table(name = "vault_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 30)
    private RuleType ruleType;

    /** Threshold amount (meaning varies by rule_type). */
    @Column(name = "threshold_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal thresholdAmount;

    /** The savings account that receives swept funds. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id", nullable = false)
    private Account targetAccount;

    /** Used only for PERCENTAGE_SAVE (0–100). */
    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // ── Enum ──────────────────────────────────────────────────────────────────
    public enum RuleType {
        AUTO_SWEEP_EXCESS, MAINTAIN_MINIMUM, PERCENTAGE_SAVE
    }
}
