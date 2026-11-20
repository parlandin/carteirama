import { Check, NotebookPen, Pencil, ReceiptText, Undo2, X } from "lucide-react";
import { useState, type ReactNode } from "react";
import { Link, useParams } from "react-router";
import { Button, Card, ErrorState, Field, FormError, GroupChip, Input, LinkButton, Loading, PageHeader, Pill, StatusPill } from "../../components/ui";
import { api, errorMessage } from "../../lib/api";
import { isReadOnly, useMe } from "../../lib/auth";
import { currentMonth, date, money, monthLabel, today } from "../../lib/format";
import { useCharge, useInvalidateData } from "../../lib/queries";
import { useHere, useOrigin } from "../../lib/origin";
import { useToast } from "../../lib/toast";

function Info({ label, value, mono }: { label: string; value: ReactNode; mono?: boolean }) {
  return (
    <div className="min-w-0">
      <dt className="font-mono text-[11px] tracking-[0.14em] text-muted uppercase">{label}</dt>
      <dd className={mono ? "mt-1 font-mono break-words" : "mt-1 text-[16px] break-words"}>{value || "—"}</dd>
    </div>
  );
}

export default function ChargeDetail() {
  const { id = "" } = useParams();
  const toast = useToast();
  const invalidate = useInvalidateData();
  const { data: me } = useMe();
  const readOnly = isReadOnly(me);
  const { data: charge, isLoading, error, refetch } = useCharge(id);
  const chargeMonth = charge?.reference_month.slice(0, 7);
  const origin = useOrigin({ to: !chargeMonth || chargeMonth === currentMonth() ? "/app/cobrancas" : `/app/cobrancas?mes=${chargeMonth}`, label: "Cobranças" });
  const here = useHere(charge?.description ?? "Cobrança");
  const [paidAt, setPaidAt] = useState(today());
  const [busy, setBusy] = useState<"pay" | "unpay" | "cancel" | null>(null);
  const [payError, setPayError] = useState<string | null>(null);
  const [confirmCancel, setConfirmCancel] = useState(false);

  if (isLoading) return <Loading />;
  if (error || !charge) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  const month = charge.reference_month.slice(0, 7);
  const oneOff = charge.kind === "one_off";
  const pending = charge.status === "pending";
  const title = charge.description ?? `Mensalidade de ${monthLabel(month).toLowerCase()}`;

  async function pay() {
    setBusy("pay");
    setPayError(null);
    try {
      await api.post(`/charges/${id}/pay`, { paid_at: paidAt || null });
      await invalidate();
      toast.show("Pagamento registrado.");
    } catch (e) {
      setPayError(errorMessage(e));
    } finally {
      setBusy(null);
    }
  }

  async function unpay() {
    setBusy("unpay");
    try {
      await api.post(`/charges/${id}/unpay`);
      await invalidate();
      toast.show("Pagamento desfeito.");
    } catch (e) {
      toast.show(errorMessage(e), "error");
    } finally {
      setBusy(null);
    }
  }

  async function cancel() {
    setBusy("cancel");
    try {
      await api.delete(`/charges/${id}`);
      await invalidate();
      toast.show("Cobrança cancelada.");
      origin.goBack();
    } catch (e) {
      toast.show(errorMessage(e), "error");
      setBusy(null);
    }
  }

  return (
    <>
      <PageHeader
        back={origin.back}
        eyebrow={oneOff ? "// Cobrança avulsa" : "// Mensalidade"}
        title={title}
        badge={<StatusPill kind="charge" status={charge.display_status} />}
        subtitle={
          <span>
            {charge.client_id ? (
              <Link to={`/app/clientes/${charge.client_id}`} state={here} className="font-semibold text-ink hover:underline">
                {charge.client_name}
              </Link>
            ) : (
              <span className="font-semibold text-ink">{charge.client_name}</span>
            )}{" "}
            · vence {date(charge.due_date)}
          </span>
        }
        actions={
          oneOff &&
          pending && (
            <LinkButton to={`/app/cobrancas/${id}/editar`} state={here} icon={Pencil} disabled={readOnly} className="w-full sm:w-auto">
              Editar cobrança
            </LinkButton>
          )
        }
      />

      <div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_340px]">
        <div className="flex flex-col gap-7">
          <Card className="p-5 sm:p-7">
            <div className="mb-5 flex items-center gap-3">
              <span className="grid size-10 place-items-center rounded-xl border-2 border-navy bg-blue">
                <ReceiptText className="size-5 text-white" />
              </span>
              <h2 className="text-xl font-bold">Cobrança</h2>
              {oneOff && <Pill tone="blue">Avulsa</Pill>}
            </div>
            <dl className="grid gap-5 sm:grid-cols-2">
              <Info label="Para quem" value={charge.client_id ? <Link to={`/app/clientes/${charge.client_id}`} state={here} className="hover:underline">{charge.client_name}</Link> : `${charge.client_name} (sem cadastro)`} />
              <Info label="Grupo" value={charge.group_id && charge.group_name ? <Link to={`/app/grupos/${charge.group_id}`} state={here}><GroupChip name={charge.group_name} color={charge.group_color} /></Link> : null} />
              <Info label="Vencimento" value={date(charge.due_date)} mono />
              <Info label="Mês" value={monthLabel(month)} />
              {charge.status === "paid" && <Info label="Pago em" value={date(charge.paid_at)} mono />}
            </dl>
          </Card>

          <Card className="p-5 sm:p-7">
            <div className="mb-3 flex items-center gap-3">
              <NotebookPen className="size-5" />
              <h2 className="text-xl font-bold">Nota</h2>
            </div>
            <p className="whitespace-pre-line text-muted">{charge.note || "Nenhuma nota."}</p>
          </Card>

          {oneOff && pending && !readOnly && (
            <div className="flex flex-col items-start gap-3">
              {confirmCancel ? (
                <Card className="w-full border-red p-5 sm:p-6">
                  <p className="font-bold">Cancelar esta cobrança?</p>
                  <p className="mt-1 text-sm text-muted">Ela sai do total do mês. Não dá para desfazer.</p>
                  <div className="mt-4 flex flex-col-reverse gap-3 sm:flex-row">
                    <Button onClick={() => setConfirmCancel(false)}>Voltar</Button>
                    <Button variant="danger" loading={busy === "cancel"} onClick={cancel}>
                      Cancelar cobrança
                    </Button>
                  </div>
                </Card>
              ) : (
                <Button variant="ghost" size="sm" icon={X} onClick={() => setConfirmCancel(true)} className="text-red">
                  Cancelar cobrança
                </Button>
              )}
            </div>
          )}
        </div>

        <div className="flex flex-col gap-7">
          <Card dark className="p-6">
            <p className="font-mono text-[11px] font-bold tracking-[0.14em] text-amber uppercase">[Valor]</p>
            <p className="tabular mt-2 text-[44px] leading-none font-extrabold tracking-tight text-amber">{money(charge.total_cents)}</p>
            {(charge.bonus_cents > 0 || charge.cost_cents > 0) && (
              <dl className="mt-6 divide-y divide-white/20 border-t border-white/20 text-[15px]">
                {[
                  ["Serviço", money(charge.amount_cents)],
                  ["Bônus", `+ ${money(charge.bonus_cents)}`],
                  ["Custos", `− ${money(charge.cost_cents)}`],
                  ["Líquido", <span className="text-amber">{money(charge.net_cents)}</span>],
                ].map(([k, v], i) => (
                  <div key={i} className="flex items-center justify-between gap-3 py-3">
                    <dt className="text-white/80">{k}</dt>
                    <dd className="text-right font-bold">{v}</dd>
                  </div>
                ))}
              </dl>
            )}
          </Card>

          {!readOnly && pending && (
            <Card className="p-5 sm:p-6">
              <h2 className="mb-4 text-xl font-bold">Registrar pagamento</h2>
              <div className="flex flex-col gap-4">
                <FormError message={payError} />
                <Field label="Pago em" htmlFor="paid-at" hint="A data não pode ser futura.">
                  <Input id="paid-at" type="date" max={today()} value={paidAt} onChange={(e) => setPaidAt(e.target.value)} />
                </Field>
                <Button variant="primary" icon={Check} block loading={busy === "pay"} onClick={pay}>
                  Marcar como pago
                </Button>
              </div>
            </Card>
          )}

          {!readOnly && charge.status === "paid" && (
            <Card className="p-5 sm:p-6">
              <p className="font-bold">Pago em {date(charge.paid_at)}</p>
              <Button variant="ghost" size="sm" icon={Undo2} loading={busy === "unpay"} onClick={unpay} className="-ml-2 mt-2 text-muted">
                Desfazer pagamento
              </Button>
            </Card>
          )}
        </div>
      </div>
    </>
  );
}
