CREATE TABLE abacatepay_pix_charges (
    id           text        PRIMARY KEY,
    invoice_id   uuid        NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    amount_cents integer     NOT NULL,
    br_code      text        NOT NULL,
    status       text        NOT NULL,
    expires_at   timestamptz NOT NULL,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX abacatepay_pix_charges_invoice_idx ON abacatepay_pix_charges (invoice_id, created_at DESC);

CREATE TRIGGER abacatepay_pix_charges_set_updated_at BEFORE UPDATE ON abacatepay_pix_charges FOR EACH ROW EXECUTE FUNCTION set_updated_at();
