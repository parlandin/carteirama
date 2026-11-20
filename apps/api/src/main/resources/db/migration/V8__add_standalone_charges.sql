-- Cobranças avulsas sem cliente cadastrado: um serviço único, sem mensalidade.
-- Nesse caso client_id fica nulo e payer_name guarda para quem é a cobrança.
ALTER TABLE charges
    ALTER COLUMN client_id DROP NOT NULL,
    ADD COLUMN payer_name text,
    ADD CONSTRAINT charges_payer_chk CHECK (
        (client_id IS NOT NULL AND payer_name IS NULL)
        OR (client_id IS NULL AND kind = 'one_off' AND payer_name IS NOT NULL
            AND char_length(payer_name) BETWEEN 2 AND 120)
    );
