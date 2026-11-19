import { useState } from "react";
import { api, errorMessage } from "../lib/api";
import { date, money } from "../lib/format";
import { useInvalidateData } from "../lib/queries";
import { useToast } from "../lib/toast";
import type { Charge } from "../lib/types";
import { Button, Modal } from "./ui";

/** Confirma o cancelamento de uma cobrança pendente (usado nas avulsas criadas por engano). */
export function CancelChargeModal({ charge, onClose }: { charge: Charge | null; onClose: () => void }) {
  const toast = useToast();
  const invalidate = useInvalidateData();
  const [busy, setBusy] = useState(false);

  async function confirm() {
    if (!charge) return;
    setBusy(true);
    try {
      await api.delete(`/charges/${charge.id}`);
      await invalidate();
      toast.show("Cobrança cancelada.");
      onClose();
    } catch (e) {
      toast.show(errorMessage(e), "error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={!!charge}
      onClose={onClose}
      title="Cancelar cobrança?"
      footer={
        <>
          <Button onClick={onClose}>Voltar</Button>
          <Button variant="danger" loading={busy} onClick={confirm}>
            Cancelar cobrança
          </Button>
        </>
      }
    >
      {charge && (
        <p>
          <strong>{charge.description ?? "Mensalidade"}</strong> de {charge.client_name}, {money(charge.total_cents)} com vencimento em {date(charge.due_date)}, sai do total do mês. Não dá para desfazer.
        </p>
      )}
    </Modal>
  );
}
