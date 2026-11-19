import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { currentMonth, maskMoneyInput, parseMoney, today } from "../lib/format";
import { useClients, useInvalidateData } from "../lib/queries";
import { useToast } from "../lib/toast";
import { useForm } from "../lib/useForm";
import { Button, Field, FormError, Input, Modal, Select, Textarea } from "./ui";

/** Vencimento sugerido: hoje no mês atual; dia 1 em outro mês. */
function defaultDueDate(month?: string): string {
  return !month || month === currentMonth() ? today() : `${month}-01`;
}

const empty = (month?: string) => ({ client_id: "", description: "", amount: "", due_date: defaultDueDate(month), note: "" });

/**
 * Cria uma cobrança avulsa: vale só para o mês do vencimento e não altera a mensalidade do cliente.
 * Com `client`, o cliente fica fixo; sem ele, aparece a lista de clientes ativos.
 */
export function OneOffChargeModal({ open, onClose, client, month }: { open: boolean; onClose: () => void; client?: { id: string; name: string }; month?: string }) {
  const toast = useToast();
  const invalidate = useInvalidateData();
  const { values, set, setValues, errors, setErrors, formError, fail, reset } = useForm(empty(month));
  const [busy, setBusy] = useState(false);
  const { data: clients } = useClients({ status: "active" });

  useEffect(() => {
    if (open) {
      setValues(empty(month));
      reset();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, month]);

  async function save() {
    const amount = parseMoney(values.amount);
    const local: Record<string, string> = {};
    if (!client && !values.client_id) local.client_id = "Escolha o cliente.";
    if (!values.description.trim()) local.description = "Descreva a cobrança.";
    if (amount === null || amount < 1) local.amount_cents = "Informe um valor maior que zero.";
    if (!values.due_date) local.due_date = "Informe o vencimento.";
    if (Object.keys(local).length > 0) {
      setErrors(local);
      return;
    }
    setBusy(true);
    try {
      await api.post("/charges", {
        kind: "one_off",
        client_id: client?.id ?? values.client_id,
        description: values.description.trim(),
        amount_cents: amount,
        due_date: values.due_date,
        note: values.note.trim() || null,
      });
      await invalidate();
      toast.show("Cobrança avulsa criada.");
      onClose();
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Nova cobrança avulsa"
      footer={
        <>
          <Button onClick={onClose}>Cancelar</Button>
          <Button variant="primary" loading={busy} onClick={save}>
            Criar cobrança
          </Button>
        </>
      }
    >
      <form
        className="flex flex-col gap-5"
        onSubmit={(e) => {
          e.preventDefault();
          save();
        }}
      >
        <p className="text-[15px] text-muted">Vale só para o mês do vencimento e não muda a mensalidade do cliente. Use para instalação, serviço extra, visita técnica…</p>
        <FormError message={formError} />
        {client ? (
          <div className="rounded-xl border-2 border-navy bg-paper-2 px-4 py-3">
            <p className="font-mono text-[11px] tracking-widest text-muted uppercase">Cliente</p>
            <p className="font-bold">{client.name}</p>
          </div>
        ) : (
          <Field label="Cliente" required htmlFor="oneoff-client" error={errors.client_id}>
            <Select id="oneoff-client" value={values.client_id} onChange={(e) => set("client_id", e.target.value)} invalid={!!errors.client_id}>
              <option value="">Escolha um cliente ativo</option>
              {clients?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </Select>
          </Field>
        )}
        <Field label="Descrição" required htmlFor="oneoff-description" error={errors.description}>
          <Input
            id="oneoff-description"
            maxLength={120}
            placeholder="Ex.: Instalação, visita técnica"
            value={values.description}
            onChange={(e) => set("description", e.target.value)}
            invalid={!!errors.description}
            autoFocus
          />
        </Field>
        <div className="grid gap-5 sm:grid-cols-2">
          <Field label="Valor (R$)" required htmlFor="oneoff-amount" error={errors.amount_cents}>
            <Input id="oneoff-amount" inputMode="decimal" placeholder="0,00" value={values.amount} onChange={(e) => set("amount", maskMoneyInput(e.target.value))} invalid={!!errors.amount_cents} />
          </Field>
          <Field label="Vencimento" required htmlFor="oneoff-due" error={errors.due_date}>
            <Input id="oneoff-due" type="date" value={values.due_date} onChange={(e) => set("due_date", e.target.value)} invalid={!!errors.due_date} />
          </Field>
        </div>
        <Field label="Nota" htmlFor="oneoff-note" hint="Opcional." error={errors.note}>
          <Textarea id="oneoff-note" maxLength={500} className="min-h-20" value={values.note} onChange={(e) => set("note", e.target.value)} />
        </Field>
        <button type="submit" hidden />
      </form>
    </Modal>
  );
}
