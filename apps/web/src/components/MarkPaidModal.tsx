import { useState } from "react";
import { api, errorMessage } from "../lib/api";
import { date, money, today } from "../lib/format";
import { useToast } from "../lib/toast";
import type { Invoice, PaymentMethod } from "../lib/types";
import { Button, Field, FormError, Input, Modal, Select } from "./ui";

const METHODS: { value: PaymentMethod; label: string }[] = [
  { value: "pix", label: "Pix" },
  { value: "boleto", label: "Boleto" },
  { value: "card", label: "Cartão" },
];

export function MarkPaidModal({ invoice, accountName, onClose, onDone }: { invoice: Invoice | null; accountName?: string; onClose: () => void; onDone: () => void }) {
  const toast = useToast();
  const [paidAt, setPaidAt] = useState(today());
  const [method, setMethod] = useState<PaymentMethod>("pix");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    if (!invoice) return;
    setBusy(true);
    setError(null);
    try {
      await api.post(`/admin/invoices/${invoice.id}/mark-paid`, { paid_at: paidAt, method });
      toast.show(`Fatura ${invoice.number} marcada como paga.`);
      onDone();
      onClose();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={!!invoice}
      onClose={onClose}
      title="Marcar fatura como paga"
      footer={
        <>
          <Button onClick={onClose}>Cancelar</Button>
          <Button variant="primary" loading={busy} onClick={submit}>
            Confirmar
          </Button>
        </>
      }
    >
      {invoice && (
        <div className="flex flex-col gap-5">
          <FormError message={error} />
          <p>
            Fatura <strong className="font-mono">{invoice.number}</strong>
            {accountName && <> de {accountName}</>}, {money(invoice.amount_cents)}, vencimento {date(invoice.due_date)}.
          </p>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Pago em" htmlFor="mp-date">
              <Input id="mp-date" type="date" max={today()} value={paidAt} onChange={(e) => setPaidAt(e.target.value)} />
            </Field>
            <Field label="Forma" htmlFor="mp-method">
              <Select id="mp-method" value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)}>
                {METHODS.map((m) => (
                  <option key={m.value} value={m.value}>
                    {m.label}
                  </option>
                ))}
              </Select>
            </Field>
          </div>
        </div>
      )}
    </Modal>
  );
}
