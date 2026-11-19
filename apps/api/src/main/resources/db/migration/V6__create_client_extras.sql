-- Extras do cliente, além do valor do serviço.
--   bonus: soma ao valor cobrado do cliente (um fio usado, uma ferramenta comprada).
--   cost:  não muda o que o cliente paga; sai do líquido (o ganho) do mês (hospedagem, material).
-- Recorrente: vale de start_month em diante; end_month é preenchido quando ele é removido.
-- Só de um mês: start_month = end_month.
-- Os valores entram na mensalidade (cobrança recorrente) do cliente nos meses em que valem.
CREATE TYPE extra_kind AS ENUM ('bonus', 'cost');

CREATE TABLE client_extras (
    id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id   uuid        NOT NULL REFERENCES accounts (id) ON DELETE RESTRICT,
    client_id    uuid        NOT NULL,
    kind         extra_kind  NOT NULL,
    description  text        NOT NULL CHECK (length(description) BETWEEN 1 AND 120),
    amount_cents integer     NOT NULL CHECK (amount_cents BETWEEN 1 AND 99999999),
    recurring    boolean     NOT NULL,
    start_month  date        NOT NULL CHECK (extract(day FROM start_month) = 1),
    end_month    date        CHECK (extract(day FROM end_month) = 1),
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT client_extras_client_fk FOREIGN KEY (client_id, account_id)
        REFERENCES clients (id, account_id) ON DELETE RESTRICT,
    CONSTRAINT client_extras_months_chk CHECK (end_month IS NULL OR end_month >= start_month),
    CONSTRAINT client_extras_one_month_chk CHECK (recurring OR end_month = start_month)
);

CREATE INDEX client_extras_client_month_idx ON client_extras (client_id, start_month);

CREATE TRIGGER client_extras_set_updated_at BEFORE UPDATE ON client_extras FOR EACH ROW EXECUTE FUNCTION set_updated_at();
