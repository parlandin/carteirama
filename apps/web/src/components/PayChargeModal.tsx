import { useState } from "react";
import { api, errorMessage } from "../lib/api";
import { date, money, today } from "../lib/format";
import { useInvalidateData } from "../lib/queries";
import { useToast } from "../lib/toast";
import type { Charge } from "../lib/types";
import { Button, Field, FormError, Input, Modal } from "./ui";

export function PayChargeModal({ charge, onClose }: { charge: Charge | null; onClose: () => void }) {
  const toast = useToast();
  const invalidate = useInvalidateData();
  const [paidAt, setPaidAt] = useState(today());
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const close = () => {
    setError(null);
    setPaidAt(today());
    onClose();
  };

  async function confirm() {
    if (!charge) return;
    setBusy(true);
    setError(null);
    try {
      await api.post(`/charges/${charge.id}/pay`, { paid_at: paidAt || null });
      await invalidate();
      toast.show(`Pagamento de ${charge.client_name} registrado.`);
      close();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={!!charge}
      onClose={close}
      title="Marcar como pago"
      footer={
        <>
          <Button onClick={close}>Cancelar</Button>
          <Button variant="primary" loading={busy} onClick={confirm}>
            Confirmar pagamento
          </Button>
        </>
      }
    >
      {charge && (
        <div className="flex flex-col gap-5">
          <FormError message={error} />
          <dl className="grid grid-cols-2 gap-4 rounded-xl border-2 border-navy bg-paper-2 p-4">
            <div className="col-span-2">
              <dt className="font-mono text-[11px] tracking-widest text-muted uppercase">Cliente</dt>
              <dd className="font-bold">{charge.client_name}</dd>
              {charge.kind === "one_off" && <dd className="text-sm text-muted">Avulsa · {charge.description}</dd>}
            </div>
            <div>
              <dt className="font-mono text-[11px] tracking-widest text-muted uppercase">Vencimento</dt>
              <dd className="font-mono">{date(charge.due_date)}</dd>
            </div>
            <div>
              <dt className="font-mono text-[11px] tracking-widest text-muted uppercase">Valor</dt>
              <dd className="font-extrabold">{money(charge.total_cents)}</dd>
              {charge.bonus_cents > 0 && <dd className="text-xs text-muted">serviço {money(charge.amount_cents)} + bônus {money(charge.bonus_cents)}</dd>}
            </div>
          </dl>
          <Field label="Pago em" htmlFor="paid-at" hint="A data não pode ser futura.">
            <Input id="paid-at" type="date" max={today()} value={paidAt} onChange={(e) => setPaidAt(e.target.value)} />
          </Field>
        </div>
      )}
    </Modal>
  );
}
