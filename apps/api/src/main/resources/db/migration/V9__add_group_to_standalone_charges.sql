ALTER TABLE charges
    ADD COLUMN group_id uuid,
    ADD CONSTRAINT charges_group_fk FOREIGN KEY (group_id, account_id)
        REFERENCES groups (id, account_id) ON DELETE RESTRICT,
    ADD CONSTRAINT charges_group_chk CHECK (group_id IS NULL OR client_id IS NULL);

CREATE INDEX charges_group_month_idx ON charges (group_id, reference_month) WHERE group_id IS NOT NULL;
