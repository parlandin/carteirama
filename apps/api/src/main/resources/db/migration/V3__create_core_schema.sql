-- Modelo de dados do MVP (ver carteirama-banco-de-dados.md)
-- Dinheiro em centavos (integer); datas sem hora em date; momentos em timestamptz (UTC).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- Enums
-- ---------------------------------------------------------------------------
CREATE TYPE person_type         AS ENUM ('pf', 'pj');
CREATE TYPE account_status      AS ENUM ('trial', 'active', 'suspended', 'cancelled');
CREATE TYPE subscription_status AS ENUM ('trialing', 'active', 'cancelled');
CREATE TYPE invoice_status      AS ENUM ('pending', 'paid', 'cancelled');
CREATE TYPE payment_method      AS ENUM ('pix', 'boleto', 'card');
CREATE TYPE client_status       AS ENUM ('active', 'paused', 'archived');
CREATE TYPE charge_status       AS ENUM ('pending', 'paid', 'cancelled');

-- updated_at atualizado a cada alteração
CREATE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------------
-- Acesso
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    email         citext      NOT NULL UNIQUE CHECK (char_length(email) <= 254),
    password_hash text        NOT NULL,
    name          text        NOT NULL CHECK (char_length(name) BETWEEN 2 AND 120),
    is_admin      boolean     NOT NULL DEFAULT false,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);

-- Sessões de login ficam no Spring Session JDBC (SPRING_SESSION, migration V2),
-- no lugar da tabela "sessions" descrita no documento.

CREATE TABLE password_resets (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  text        NOT NULL UNIQUE,
    expires_at  timestamptz NOT NULL,
    used_at     timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX password_resets_user_id_idx ON password_resets (user_id);

-- ---------------------------------------------------------------------------
-- Conta e cobrança do SaaS
-- ---------------------------------------------------------------------------
CREATE TABLE accounts (
    id                  uuid           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             uuid           NOT NULL UNIQUE REFERENCES users (id) ON DELETE RESTRICT,
    person_type         person_type    NOT NULL,
    document            varchar(14)    NOT NULL UNIQUE CHECK (document ~ '^([0-9]{11}|[0-9]{14})$'),
    legal_name          text           NOT NULL CHECK (char_length(legal_name) BETWEEN 2 AND 120),
    company_name        text,
    phone               varchar(11)    NOT NULL CHECK (phone ~ '^[0-9]+$'),
    zip                 varchar(8)     CHECK (zip ~ '^[0-9]{8}$'),
    address             text,
    city                text,
    state               char(2),
    status              account_status NOT NULL DEFAULT 'trial',
    trial_ends_at       timestamptz    NOT NULL,
    suspended_at        timestamptz,
    cancelled_at        timestamptz,
    gateway_customer_id text           UNIQUE,
    created_at          timestamptz    NOT NULL DEFAULT now(),
    updated_at          timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT accounts_pj_company_name_chk CHECK (person_type <> 'pj' OR company_name IS NOT NULL)
);
CREATE INDEX accounts_status_idx ON accounts (status);

CREATE TABLE plans (
    code        text        PRIMARY KEY,
    name        text        NOT NULL,
    price_cents integer     NOT NULL CHECK (price_cents > 0),
    trial_days  integer     NOT NULL DEFAULT 14,
    active      boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE subscriptions (
    id                      uuid                PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id              uuid                NOT NULL UNIQUE REFERENCES accounts (id) ON DELETE RESTRICT,
    plan_code               text                NOT NULL REFERENCES plans (code) ON DELETE RESTRICT,
    price_cents             integer             NOT NULL CHECK (price_cents > 0),
    status                  subscription_status NOT NULL,
    gateway_subscription_id text                UNIQUE,
    next_due_date           date,
    cancel_at_period_end    boolean             NOT NULL DEFAULT false,
    created_at              timestamptz         NOT NULL DEFAULT now(),
    updated_at              timestamptz         NOT NULL DEFAULT now()
);

CREATE SEQUENCE invoice_number_seq;

CREATE TABLE invoices (
    id                uuid           PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id        uuid           NOT NULL REFERENCES accounts (id) ON DELETE RESTRICT,
    number            text           NOT NULL UNIQUE DEFAULT ('FAT-' || nextval('invoice_number_seq')),
    amount_cents      integer        NOT NULL CHECK (amount_cents > 0),
    due_date          date           NOT NULL,
    status            invoice_status NOT NULL DEFAULT 'pending',
    paid_at           timestamptz,
    method            payment_method,
    gateway_charge_id text           UNIQUE,
    created_at        timestamptz    NOT NULL DEFAULT now(),
    updated_at        timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT invoices_paid_at_chk CHECK (status <> 'paid' OR paid_at IS NOT NULL)
);
ALTER SEQUENCE invoice_number_seq OWNED BY invoices.number;
CREATE INDEX invoices_account_due_date_idx ON invoices (account_id, due_date DESC);
CREATE INDEX invoices_status_due_date_idx  ON invoices (status, due_date);

CREATE TABLE webhook_events (
    id               uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    gateway_event_id text        NOT NULL UNIQUE,
    type             text        NOT NULL,
    payload          jsonb       NOT NULL,
    processed_at     timestamptz,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now()
);

-- Sem updated_at: o log nunca é editado
CREATE TABLE audit_log (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id    uuid        REFERENCES accounts (id) ON DELETE SET NULL,
    actor_user_id uuid        REFERENCES users (id) ON DELETE SET NULL,
    action        text        NOT NULL,
    meta          jsonb       NOT NULL DEFAULT '{}'::jsonb,
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX audit_log_account_created_at_idx ON audit_log (account_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- Produto do usuário
-- ---------------------------------------------------------------------------
CREATE TABLE groups (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id  uuid        NOT NULL REFERENCES accounts (id) ON DELETE RESTRICT,
    name        text        NOT NULL CHECK (char_length(name) BETWEEN 1 AND 60),
    color       char(7)     CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    notes       text,
    archived_at timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT groups_id_account_uq UNIQUE (id, account_id)
);
CREATE UNIQUE INDEX groups_account_name_active_uq ON groups (account_id, lower(name)) WHERE archived_at IS NULL;

CREATE TABLE clients (
    id           uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id   uuid          NOT NULL REFERENCES accounts (id) ON DELETE RESTRICT,
    group_id     uuid          NOT NULL,
    name         text          NOT NULL CHECK (char_length(name) BETWEEN 2 AND 120),
    document     varchar(14)   CHECK (document ~ '^([0-9]{11}|[0-9]{14})$'),
    email        citext,
    phone        varchar(11)   CHECK (phone ~ '^[0-9]+$'),
    amount_cents integer       NOT NULL CHECK (amount_cents BETWEEN 1 AND 99999999),
    due_day      smallint      NOT NULL CHECK (due_day BETWEEN 1 AND 28),
    status       client_status NOT NULL DEFAULT 'active',
    notes        text,
    created_at   timestamptz   NOT NULL DEFAULT now(),
    updated_at   timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT clients_id_account_uq UNIQUE (id, account_id),
    CONSTRAINT clients_group_fk FOREIGN KEY (group_id, account_id)
        REFERENCES groups (id, account_id) ON DELETE RESTRICT
);
CREATE INDEX clients_account_group_idx  ON clients (account_id, group_id);
CREATE INDEX clients_account_status_idx ON clients (account_id, status);

CREATE TABLE charges (
    id              uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      uuid          NOT NULL REFERENCES accounts (id) ON DELETE RESTRICT,
    client_id       uuid          NOT NULL,
    reference_month date          NOT NULL CHECK (extract(day FROM reference_month) = 1),
    amount_cents    integer       NOT NULL CHECK (amount_cents BETWEEN 1 AND 99999999),
    due_date        date          NOT NULL,
    status          charge_status NOT NULL DEFAULT 'pending',
    paid_at         date,
    note            text,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT charges_client_month_uq UNIQUE (client_id, reference_month),
    CONSTRAINT charges_client_fk FOREIGN KEY (client_id, account_id)
        REFERENCES clients (id, account_id) ON DELETE RESTRICT,
    CONSTRAINT charges_paid_at_chk CHECK (status <> 'paid' OR paid_at IS NOT NULL)
    -- "paid_at não pode ser futura" é validado na aplicação: depende do fuso
    -- America/Sao_Paulo e now() não pode ser usado num CHECK imutável.
);
CREATE INDEX charges_account_month_idx      ON charges (account_id, reference_month);
CREATE INDEX charges_account_status_due_idx ON charges (account_id, status, due_date);

-- ---------------------------------------------------------------------------
-- Triggers de updated_at
-- ---------------------------------------------------------------------------
CREATE TRIGGER users_set_updated_at           BEFORE UPDATE ON users           FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER password_resets_set_updated_at BEFORE UPDATE ON password_resets FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER accounts_set_updated_at        BEFORE UPDATE ON accounts        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER plans_set_updated_at           BEFORE UPDATE ON plans           FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER subscriptions_set_updated_at   BEFORE UPDATE ON subscriptions   FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER invoices_set_updated_at        BEFORE UPDATE ON invoices        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER webhook_events_set_updated_at  BEFORE UPDATE ON webhook_events  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER groups_set_updated_at          BEFORE UPDATE ON groups          FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER clients_set_updated_at         BEFORE UPDATE ON clients         FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER charges_set_updated_at         BEFORE UPDATE ON charges         FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- Seed: plano cloud (idempotente)
-- ---------------------------------------------------------------------------
INSERT INTO plans (code, name, price_cents, trial_days, active)
VALUES ('cloud', 'Cloud', 5900, 14, true)
ON CONFLICT (code) DO NOTHING;
