-- Cobranças avulsas: valem só para um mês e não vêm da mensalidade do cliente.
-- A regra "uma cobrança por cliente por mês" passa a valer só para as recorrentes.
CREATE TYPE charge_kind AS ENUM ('recurring', 'one_off');

ALTER TABLE charges
    ADD COLUMN kind        charge_kind NOT NULL DEFAULT 'recurring',
    ADD COLUMN description text,
    ADD CONSTRAINT charges_description_chk CHECK (
        (kind = 'recurring' AND description IS NULL)
        OR (kind = 'one_off' AND description IS NOT NULL AND length(description) BETWEEN 1 AND 120)
    );

ALTER TABLE charges DROP CONSTRAINT charges_client_month_uq;
CREATE UNIQUE INDEX charges_client_month_uq ON charges (client_id, reference_month) WHERE kind = 'recurring';
