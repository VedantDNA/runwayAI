-- =============================================================================
-- V1__init_schema.sql
-- Runway Core Banking — Initial Schema
--
-- INTERVIEW CONTEXT: Double-entry bookkeeping
--   Every financial movement is recorded as two ledger entries: one DEBIT and
--   one CREDIT. This ensures that money is never created or destroyed — only
--   moved. The invariant SUM(DEBIT amounts) == SUM(CREDIT amounts) must hold
--   at all times across the entire ledger.
--
--   accounts.cached_balance is a denormalized read-cache updated inside the
--   same transaction as the ledger entries. It is NEVER the source of truth.
--   True balance = SELECT SUM(amount) FROM ledger_entries WHERE entry_type=CREDIT
--                  - SUM(amount) WHERE entry_type=DEBIT for a given account.
-- =============================================================================

-- Enable UUID generation without the pgcrypto extension (pg 13+)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================================================
-- ENUM TYPES
-- =============================================================================

CREATE TYPE account_type_enum AS ENUM (
    'CHECKING',
    'SAVINGS_VAULT'
);

CREATE TYPE entry_type_enum AS ENUM (
    'DEBIT',
    'CREDIT'
);

CREATE TYPE transaction_status_enum AS ENUM (
    'PENDING',
    'COMPLETED',
    'FAILED',
    'REVERSED'
);

CREATE TYPE transaction_category_enum AS ENUM (
    'TRANSFER',
    'SUBSCRIPTION',
    'GROCERIES',
    'DINING',
    'UTILITIES',
    'RENT',
    'SALARY',
    'INTEREST',
    'INVESTMENT',
    'HEALTHCARE',
    'ENTERTAINMENT',
    'TRAVEL',
    'SHOPPING',
    'OTHER'
);

CREATE TYPE frequency_enum AS ENUM (
    'DAILY',
    'WEEKLY',
    'MONTHLY',
    'ANNUALLY'
);

CREATE TYPE vault_rule_type_enum AS ENUM (
    'AUTO_SWEEP_EXCESS',       -- sweep balance above threshold to savings
    'MAINTAIN_MINIMUM',        -- keep checking above a floor
    'PERCENTAGE_SAVE'          -- save X% of every incoming credit
);

CREATE TYPE notification_type_enum AS ENUM (
    'OVERDRAFT_RISK',
    'PRICE_CREEP',
    'BUDGET_EXCEEDED',
    'AUTOPILOT_SWEEP'
);

-- =============================================================================
-- USERS
-- =============================================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,   -- bcrypt hash, never plaintext
    full_name       VARCHAR(255) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- =============================================================================
-- ACCOUNTS
-- =============================================================================

-- WHY cached_balance exists:
--   Computing SUM(ledger_entries) on every balance read is correct but slow at
--   scale. cached_balance is updated atomically inside the transfer transaction
--   so it always reflects committed state. If they diverge (bug), the ledger is
--   authoritative — cached_balance is just an optimisation.

CREATE TABLE accounts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_type    account_type_enum NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    cached_balance  NUMERIC(15, 2) NOT NULL DEFAULT 0.00,  -- read-cache only!
    apy             NUMERIC(5, 4) NOT NULL DEFAULT 0.0000, -- annual % yield (e.g. 0.0450 = 4.5%)
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);

-- =============================================================================
-- TRANSACTIONS
-- =============================================================================

-- A transaction is the business-level event (e.g. "Netflix subscription charge").
-- Each transaction spawns exactly two ledger_entries (DEBIT + CREDIT).

CREATE TABLE transactions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id      UUID NOT NULL REFERENCES accounts(id),
    counterpart_account_id UUID REFERENCES accounts(id), -- null for external
    amount          NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    category        transaction_category_enum NOT NULL DEFAULT 'OTHER',
    merchant_name   VARCHAR(255),
    description     TEXT,
    status          transaction_status_enum NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    settled_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_category   ON transactions(category);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);

-- =============================================================================
-- LEDGER ENTRIES
-- =============================================================================

-- The immutable double-entry ledger. Rows are NEVER updated or deleted.
-- WHY IMMUTABLE: Any modification after settlement would break the audit trail.
-- Reversals are expressed as new CREDIT entries, not deletes.

CREATE TABLE ledger_entries (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    account_id      UUID NOT NULL REFERENCES accounts(id),
    entry_type      entry_type_enum NOT NULL,
    amount          NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    running_balance NUMERIC(15, 2) NOT NULL,  -- balance AFTER this entry
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Unique constraint: each transaction produces exactly one DEBIT + one CREDIT per account
CREATE UNIQUE INDEX idx_ledger_txn_account_type
    ON ledger_entries(transaction_id, account_id, entry_type);

CREATE INDEX idx_ledger_account_created ON ledger_entries(account_id, created_at DESC);

-- =============================================================================
-- RECURRING OBLIGATIONS
-- =============================================================================

CREATE TABLE recurring_obligations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    merchant_name   VARCHAR(255) NOT NULL,
    amount          NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    frequency       frequency_enum NOT NULL DEFAULT 'MONTHLY',
    category        transaction_category_enum NOT NULL DEFAULT 'SUBSCRIPTION',
    next_due_date   DATE NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_recurring_user_id ON recurring_obligations(user_id);

-- =============================================================================
-- VAULT RULES
-- =============================================================================

CREATE TABLE vault_rules (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rule_type           vault_rule_type_enum NOT NULL,
    threshold_amount    NUMERIC(15, 2) NOT NULL CHECK (threshold_amount >= 0),
    target_account_id   UUID NOT NULL REFERENCES accounts(id),
    percentage          NUMERIC(5, 2) CHECK (percentage BETWEEN 0 AND 100), -- for PERCENTAGE_SAVE
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vault_rules_user_id ON vault_rules(user_id);

-- =============================================================================
-- REFRESH TOKENS  (Granule 2-C — JWT refresh token rotation)
-- =============================================================================

-- WHY HASH: If the DB is breached, raw tokens can't be replayed.
-- Same reasoning as storing bcrypt(password) instead of plaintext.

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,  -- SHA-256 of the actual token
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at  TIMESTAMP WITH TIME ZONE,       -- NULL = still valid
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- =============================================================================
-- BUDGET GOALS  (per-category monthly spending targets)
-- =============================================================================

CREATE TABLE budget_goals (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category        transaction_category_enum NOT NULL,
    monthly_limit   NUMERIC(12, 2) NOT NULL CHECK (monthly_limit > 0),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- One budget goal per category per user
    CONSTRAINT uq_budget_user_category UNIQUE (user_id, category)
);

-- =============================================================================
-- PASSWORD RESET TOKENS
-- =============================================================================

-- Short-lived single-use tokens for the forgot-password flow.
-- token_hash is stored (not plaintext); used_at is set when consumed.

CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE,    -- NULL = not yet consumed
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- NOTIFICATIONS
-- =============================================================================

CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        notification_type_enum NOT NULL,
    message     TEXT NOT NULL,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_unread
    ON notifications(user_id, is_read)
    WHERE is_read = FALSE;
