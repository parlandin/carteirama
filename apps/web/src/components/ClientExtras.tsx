import { CalendarDays, Minus, Plus, Repeat, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { api, errorMessage } from "../lib/api";
import { cx } from "../lib/cx";
import { addMonths, currentMonth, maskMoneyInput, money, monthLabel, parseMoney } from "../lib/format";
import { useInvalidateData } from "../lib/queries";
import { useToast } from "../lib/toast";
import type { ClientExtra, ExtraKind } from "../lib/types";
import { Button, Card, Field, FormError, Input, Modal, Select } from "./ui";

/* ------------------------------------------------------------------ Rascunho e validação */

export interface ExtraDraft {
  kind: ExtraKind;
  description: string;
  amount: string;
  /** recurring: todo mês, a partir do atual; month: só no mês escolhido. */
  when: "recurring" | "month";
  month: string;
}

export const emptyExtra = (kind: ExtraKind = "bonus"): ExtraDraft => ({ kind, description: "", amount: "", when: "month", month: currentMonth() });

export type ExtraErrors = Partial<Record<"description" | "amount_cents" | "month", string>>;

/** Valida o rascunho; devolve os erros ou o corpo pronto para POST /clients/{id}/extras. */
export function toExtraBody(d: ExtraDraft): { errors: ExtraErrors } | { body: Record<string, unknown> } {
  const errors: ExtraErrors = {};
  const amount = parseMoney(d.amount);
  if (!d.description.trim()) errors.description = "Descreva o item.";
  if (amount === null || amount < 1) errors.amount_cents = "Informe um valor maior que zero.";
  if (d.when === "month" && !d.month) errors.month = "Escolha o mês.";
  if (Object.keys(errors).length > 0) return { errors };
  return {
    body: { kind: d.kind, description: d.description.trim(), amount_cents: amount, month: d.when === "month" ? d.month : null },
  };
}

const KINDS: { kind: ExtraKind; title: string; hint: string; icon: typeof Plus }[] = [
  { kind: "bonus", title: "Bônus", hint: "Soma no valor cobrado do cliente.", icon: Plus },
  { kind: "cost", title: "Custo", hint: "Sai do seu ganho; o cliente paga o mesmo.", icon: Minus },
];

/** Campos de um bônus ou custo. Usado no modal e no cadastro do cliente. */
export function ExtraFields({ id, value, onChange, errors = {} }: { id: string; value: ExtraDraft; onChange: (v: ExtraDraft) => void; errors?: ExtraErrors }) {
  const set = <K extends keyof ExtraDraft>(k: K, v: ExtraDraft[K]) => onChange({ ...value, [k]: v });
  const now = currentMonth();
  return (
    <div className="flex flex-col gap-5">
      <fieldset>
        <legend className="mb-1.5 text-sm font-semibold">Tipo</legend>
        <div className="grid grid-cols-2 gap-3">
          {KINDS.map((k) => (
            <label
              key={k.kind}
              className={cx(
                "flex cursor-pointer flex-col gap-0.5 rounded-xl border-2 border-navy px-3.5 py-3 transition",
                value.kind === k.kind ? (k.kind === "bonus" ? "bg-green-soft" : "bg-red-soft") : "bg-white hover:bg-paper-2",
              )}
            >
              <span className="flex items-center gap-2 font-bold">
                <input type="radio" name={`${id}-kind`} className="size-4 accent-blue" checked={value.kind === k.kind} onChange={() => set("kind", k.kind)} />
                <k.icon className={cx("size-4", k.kind === "bonus" ? "text-green" : "text-red")} strokeWidth={3} />
                {k.title}
              </span>
              <span className="text-[13px] leading-snug text-muted">{k.hint}</span>
            </label>
          ))}
        </div>
      </fieldset>
      <div className="grid gap-5 sm:grid-cols-[1fr_160px]">
        <Field label="Descrição" required htmlFor={`${id}-description`} error={errors.description}>
          <Input
            id={`${id}-description`}
            maxLength={120}
            placeholder={value.kind === "bonus" ? "Ex.: Fio 2,5 mm, ferramenta" : "Ex.: Hospedagem, material"}
            value={value.description}
            onChange={(e) => set("description", e.target.value)}
            invalid={!!errors.description}
          />
        </Field>
        <Field label="Valor (R$)" required htmlFor={`${id}-amount`} error={errors.amount_cents}>
          <Input id={`${id}-amount`} inputMode="decimal" placeholder="0,00" value={value.amount} onChange={(e) => set("amount", maskMoneyInput(e.target.value))} invalid={!!errors.amount_cents} />
        </Field>
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="Quando vale" htmlFor={`${id}-when`}>
          <Select id={`${id}-when`} value={value.when} onChange={(e) => set("when", e.target.value as ExtraDraft["when"])}>
            <option value="month">Só em um mês</option>
            <option value="recurring">Todo mês</option>
          </Select>
        </Field>
        {value.when === "month" ? (
          <Field label="Mês" required htmlFor={`${id}-month`} error={errors.month}>
            <Input id={`${id}-month`} type="month" min={addMonths(now, -12)} max={addMonths(now, 12)} value={value.month} onChange={(e) => set("month", e.target.value)} invalid={!!errors.month} />
          </Field>
        ) : (
          <p className="self-end pb-3 text-[13px] text-muted">Entra todo mês, a partir de {monthLabel(now).toLowerCase()}, até você remover.</p>
        )}
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ Modal de inclusão */

export function AddExtraModal({ clientId, open, onClose }: { clientId: string; open: boolean; onClose: () => void }) {
  const toast = useToast();
  const invalidate = useInvalidateData();
  const [draft, setDraft] = useState(emptyExtra());
  const [errors, setErrors] = useState<ExtraErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (open) {
      setDraft(emptyExtra());
      setErrors({});
      setFormError(null);
    }
  }, [open]);

  async function save() {
    const r = toExtraBody(draft);
    if ("errors" in r) {
      setErrors(r.errors);
      return;
    }
    setBusy(true);
    setFormError(null);
    try {
      await api.post(`/clients/${clientId}/extras`, r.body);
      await invalidate();
      toast.show(draft.kind === "bonus" ? "Bônus adicionado." : "Custo adicionado.");
      onClose();
    } catch (e) {
      setFormError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Adicionar bônus ou custo"
      footer={
        <>
          <Button onClick={onClose}>Cancelar</Button>
          <Button variant="primary" loading={busy} onClick={save}>
            Adicionar
          </Button>
        </>
      }
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          save();
        }}
        className="flex flex-col gap-5"
      >
        <FormError message={formError} />
        <ExtraFields id="extra" value={draft} onChange={(v) => { setDraft(v); setErrors({}); }} errors={errors} />
        <button type="submit" hidden />
      </form>
    </Modal>
  );
}

/* ------------------------------------------------------------------ Card no detalhe do cliente */

function whenLabel(e: ClientExtra): string {
  const month = e.start_month.slice(0, 7);
  if (!e.recurring) return `Só em ${monthLabel(month).toLowerCase()}`;
  return month > currentMonth() ? `Todo mês, a partir de ${monthLabel(month).toLowerCase()}` : "Todo mês";
}

/** Soma dos recorrentes que já valem neste mês. */
export function recurringTotals(extras: ClientExtra[]) {
  const now = currentMonth();
  const live = extras.filter((e) => e.recurring && e.start_month.slice(0, 7) <= now);
  const sum = (k: ExtraKind) => live.filter((e) => e.kind === k).reduce((s, e) => s + e.amount_cents, 0);
  return { bonus: sum("bonus"), cost: sum("cost") };
}

export function ExtrasCard({ clientId, extras, canEdit }: { clientId: string; extras: ClientExtra[]; canEdit: boolean }) {
  const toast = useToast();
  const invalidate = useInvalidateData();
  const [adding, setAdding] = useState(false);
  const [removing, setRemoving] = useState<ClientExtra | null>(null);
  const [busy, setBusy] = useState(false);
  const now = currentMonth();
  // Os de um mês que já passou ficam no histórico das cobranças; aqui, só o que vale agora ou adiante.
  const shown = extras.filter((e) => e.recurring || e.start_month.slice(0, 7) >= now);
  const past = extras.length - shown.length;

  async function remove() {
    if (!removing) return;
    setBusy(true);
    try {
      await api.delete(`/clients/${clientId}/extras/${removing.id}`);
      await invalidate();
      toast.show("Item removido.");
      setRemoving(null);
    } catch (e) {
      toast.show(errorMessage(e), "error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-3 border-b-2 border-navy px-5 py-5 sm:px-7">
        <div>
          <h2 className="text-xl font-bold">Bônus e custos</h2>
          <p className="text-sm text-muted">O que entra além do serviço e o que sai do seu ganho.</p>
        </div>
        {canEdit && (
          <Button size="sm" icon={Plus} onClick={() => setAdding(true)}>
            Adicionar
          </Button>
        )}
      </div>
      {shown.length === 0 ? (
        <p className="px-5 py-8 text-center text-sm text-muted sm:px-7">
          Nenhum bônus ou custo{past > 0 ? " para este mês em diante" : ""}. Ex.: um fio usado num serviço (bônus, só naquele mês) ou a hospedagem que você paga (custo, todo mês).
        </p>
      ) : (
        <ul>
          {shown.map((e) => (
            <li key={e.id} className="flex items-center gap-3 border-b-2 border-navy px-5 py-3.5 last:border-b-0 sm:px-7">
              <span className={cx("grid size-8 shrink-0 place-items-center rounded-lg border-2 border-navy", e.kind === "bonus" ? "bg-green-soft text-green" : "bg-red-soft text-red")}>
                {e.kind === "bonus" ? <Plus className="size-4" strokeWidth={3} /> : <Minus className="size-4" strokeWidth={3} />}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate font-bold">{e.description}</p>
                <p className="flex items-center gap-1 text-xs text-muted">
                  {e.recurring ? <Repeat className="size-3" /> : <CalendarDays className="size-3" />}
                  {whenLabel(e)}
                </p>
              </div>
              <span className={cx("tabular shrink-0 font-extrabold", e.kind === "bonus" ? "text-green" : "text-red")}>
                {e.kind === "bonus" ? "+" : "−"} {money(e.amount_cents)}
              </span>
              {canEdit && (
                <button type="button" onClick={() => setRemoving(e)} className="shrink-0 rounded-lg p-1.5 text-muted hover:bg-paper-2 hover:text-red" aria-label={`Remover ${e.description}`}>
                  <Trash2 className="size-4" />
                </button>
              )}
            </li>
          ))}
        </ul>
      )}

      <AddExtraModal clientId={clientId} open={adding} onClose={() => setAdding(false)} />
      <Modal
        open={!!removing}
        onClose={() => setRemoving(null)}
        title={`Remover ${removing?.kind === "cost" ? "custo" : "bônus"}?`}
        footer={
          <>
            <Button onClick={() => setRemoving(null)}>Voltar</Button>
            <Button variant="danger" loading={busy} onClick={remove}>
              Remover
            </Button>
          </>
        }
      >
        {removing && (
          <p>
            <strong>{removing.description}</strong> ({money(removing.amount_cents)}){" "}
            {removing.recurring
              ? "deixa de entrar a partir deste mês. Os meses anteriores continuam com ele."
              : `sai de ${monthLabel(removing.start_month.slice(0, 7)).toLowerCase()}.`}
          </p>
        )}
      </Modal>
    </Card>
  );
}
