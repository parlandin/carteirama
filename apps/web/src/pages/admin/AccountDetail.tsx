import { useQueryClient } from "@tanstack/react-query";
import { Ban, RotateCcw, TriangleAlert } from "lucide-react";
import { useState, type ReactNode } from "react";
import { MarkPaidModal } from "../../components/MarkPaidModal";
import { useParams } from "react-router";
import { Button, Card, ErrorState, Eyebrow, Field, FormError, Loading, Modal, PageHeader, StatusPill, TableHead, Textarea, TotalBar } from "../../components/ui";
import { api, errorMessage } from "../../lib/api";
import { useMe } from "../../lib/auth";
import { date, daysBetween, formatDocument, formatPhone, instantDate, maskZip, money, plural, today } from "../../lib/format";
import { useAdminAccount } from "../../lib/queries";
import { useToast } from "../../lib/toast";
import type { AdminAccountDetail, Invoice } from "../../lib/types";

const ACTIONS: Record<string, string> = {
  "account.suspend": "Conta suspensa",
  "account.reactivate": "Conta reativada",
  "invoice.mark_paid": "Fatura marcada como paga",
  "plan.price_change": "Preço do plano alterado",
};

function Row({ k, v }: { k: string; v: ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5 py-3 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
      <dt className="font-mono text-[11px] tracking-[0.14em] text-muted uppercase">{k}</dt>
      <dd className="font-semibold break-all sm:text-right">{v || "—"}</dd>
    </div>
  );
}

export default function AdminAccountDetailPage() {
  const { id = "" } = useParams();
  const qc = useQueryClient();
  const toast = useToast();
  const { data: me } = useMe();
  const { data, isLoading, error, refetch } = useAdminAccount(id);
  const [marking, setMarking] = useState<Invoice | null>(null);
  const [suspending, setSuspending] = useState(false);
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  const a = data.account;
  const name = a.company_name || a.legal_name;
  const lateInvoice = data.invoices.find((i) => i.display_status === "late");
  const paid = data.invoices.filter((i) => i.status === "paid");
  const openTotal = data.invoices.filter((i) => i.status === "pending").reduce((s, i) => s + i.amount_cents, 0);
  const isOwn = me?.account?.id === a.id;

  const refresh = () => qc.invalidateQueries({ queryKey: ["admin"] });

  async function doSuspend() {
    setBusy(true);
    setActionError(null);
    try {
      const updated = await api.post<AdminAccountDetail>(`/admin/accounts/${id}/suspend`, { reason: reason.trim() });
      qc.setQueryData(["admin", "account", id], updated);
      refresh();
      toast.show("Conta suspensa.");
      setSuspending(false);
      setReason("");
    } catch (e) {
      setActionError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function doReactivate() {
    setBusy(true);
    try {
      const updated = await api.post<AdminAccountDetail>(`/admin/accounts/${id}/reactivate`);
      qc.setQueryData(["admin", "account", id], updated);
      refresh();
      toast.show("Conta reativada.");
    } catch (e) {
      toast.show(errorMessage(e), "error");
    } finally {
      setBusy(false);
    }
  }

  const location = [data.city, data.state].filter(Boolean).join("/");
  const sub = data.subscription;

  return (
    <>
      <PageHeader
        back={{ to: "/admin/contas", label: "Contas" }}
        eyebrow="// Admin · conta"
        title={name}
        badge={<StatusPill kind="account" status={a.display_status} />}
        subtitle={`${sub ? "Cloud · " : ""}cliente desde ${instantDate(a.created_at)}`}
        actions={
          a.status === "suspended" ? (
            <Button variant="primary" icon={RotateCcw} loading={busy} onClick={doReactivate} className="w-full sm:w-auto">
              Reativar conta
            </Button>
          ) : a.status !== "cancelled" && !isOwn ? (
            <Button variant="danger" icon={Ban} onClick={() => setSuspending(true)} className="w-full sm:w-auto">
              Suspender conta
            </Button>
          ) : null
        }
      />

      {lateInvoice && (
        <div className="mb-7 flex items-start gap-3 rounded-2xl border-2 border-navy bg-amber-soft px-5 py-4">
          <TriangleAlert className="mt-0.5 size-5 shrink-0" />
          <div>
            <p className="font-bold">Fatura vencida há {plural(Math.max(1, daysBetween(lateInvoice.due_date, today())), "dia", "dias")}</p>
            <p className="text-sm">
              {lateInvoice.number} de {money(lateInvoice.amount_cents)}, vencida em {date(lateInvoice.due_date)}. A conta é suspensa automaticamente alguns dias após o vencimento.
            </p>
          </div>
        </div>
      )}

      <div className="mb-7 grid gap-6 lg:grid-cols-2">
        <Card className="p-5 sm:p-7">
          <Eyebrow className="mb-3">[Dados da conta]</Eyebrow>
          <dl className="divide-y-2 divide-navy border-t-2 border-navy">
            <Row k="Responsável" v={a.user_name} />
            <Row k="E-mail" v={<a href={`mailto:${a.email}`} className="hover:underline">{a.email}</a>} />
            <Row k="Telefone" v={formatPhone(data.phone)} />
            <Row k={a.person_type === "pj" ? "CNPJ" : "CPF"} v={formatDocument(a.document)} />
            {a.company_name && <Row k="Razão social" v={a.legal_name} />}
            <Row k="Endereço" v={[data.address, data.zip && maskZip(data.zip)].filter(Boolean).join(" · ")} />
            <Row k="Local" v={location} />
            <Row k="Conta criada em" v={instantDate(a.created_at)} />
          </dl>
        </Card>
        <Card className="p-5 sm:p-7">
          <Eyebrow className="mb-3">[Assinatura]</Eyebrow>
          {sub ? (
            <dl className="divide-y-2 divide-navy border-t-2 border-navy">
              <Row k="Plano" v={`${sub.plan_code === "cloud" ? "Cloud" : sub.plan_code} · ${money(sub.price_cents)}/mês`} />
              <Row k="Situação" v={{ trialing: "Em teste", active: "Ativa", cancelled: "Cancelada" }[sub.status]} />
              <Row k="Próximo vencimento" v={date(sub.next_due_date)} />
              {a.trial_ends_at && <Row k="Fim do teste" v={instantDate(a.trial_ends_at)} />}
              {sub.cancel_at_period_end && <Row k="Cancelamento" v="Agendado para o fim do período" />}
              {a.suspended_at && <Row k="Suspensa em" v={instantDate(a.suspended_at)} />}
              {a.cancelled_at && <Row k="Cancelada em" v={instantDate(a.cancelled_at)} />}
            </dl>
          ) : (
            <p className="text-muted">Sem assinatura.</p>
          )}
        </Card>
      </div>

      <Card className="mb-7">
        <div className="px-5 pt-6 pb-4 sm:px-6">
          <Eyebrow>[Histórico de pagamentos]</Eyebrow>
        </div>
        {data.invoices.length === 0 ? (
          <p className="border-t-2 border-navy px-6 py-8 text-center text-muted">Nenhuma fatura ainda.</p>
        ) : (
          <>
            <TableHead className="grid-cols-[130px_130px_140px_1fr_180px] gap-5 rounded-none border-t-2">
              <span>Vencimento</span>
              <span>Fatura</span>
              <span>Status</span>
              <span>Valor</span>
              <span className="text-right">Ações</span>
            </TableHead>
            <ul>
              {data.invoices.map((i) => (
                <li key={i.id} className="grid grid-cols-[1fr_auto] items-center gap-x-4 gap-y-2 border-t-2 border-navy px-4 py-4 sm:px-6 md:grid-cols-[130px_130px_140px_1fr_180px] md:gap-5 md:first:border-t-0">
                  <span className="font-mono text-sm">{date(i.due_date)}</span>
                  <span className="text-right font-mono text-sm md:text-left">{i.number}</span>
                  <StatusPill kind="invoice" status={i.display_status} className="justify-self-start" />
                  <span className="tabular text-right text-lg font-extrabold md:text-left">{money(i.amount_cents)}</span>
                  <span className="col-span-2 md:col-span-1 md:text-right">
                    {i.status === "pending" ? (
                      <Button size="sm" variant="primary" onClick={() => setMarking(i)} className="w-full md:w-auto">
                        Marcar como pago
                      </Button>
                    ) : i.paid_at ? (
                      <span className="font-mono text-xs text-muted">pago {instantDate(i.paid_at)}</span>
                    ) : null}
                  </span>
                </li>
              ))}
            </ul>
          </>
        )}
        <TotalBar label="Total pago na conta" sub={`${plural(paid.length, "fatura paga", "faturas pagas")}${openTotal > 0 ? ` e ${money(openTotal)} em aberto` : ""}`} value={money(paid.reduce((s, i) => s + i.amount_cents, 0))} />
      </Card>

      {data.audit_log.length > 0 && (
        <Card className="p-5 sm:p-7">
          <Eyebrow className="mb-3">[Registro de ações]</Eyebrow>
          <ul className="divide-y-2 divide-navy border-t-2 border-navy">
            {data.audit_log.map((l) => (
              <li key={l.id} className="flex flex-col gap-0.5 py-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-semibold">{ACTIONS[l.action] ?? l.action}</p>
                  {typeof l.meta?.reason === "string" && <p className="text-sm text-muted">Motivo: {l.meta.reason}</p>}
                </div>
                <p className="font-mono text-xs text-muted">
                  {instantDate(l.created_at)} · {l.actor_name ?? "sistema"}
                </p>
              </li>
            ))}
          </ul>
        </Card>
      )}

      <MarkPaidModal invoice={marking} accountName={name} onClose={() => setMarking(null)} onDone={refresh} />
      <Modal
        open={suspending}
        onClose={() => setSuspending(false)}
        title="Suspender conta"
        footer={
          <>
            <Button onClick={() => setSuspending(false)}>Cancelar</Button>
            <Button variant="danger" loading={busy} disabled={reason.trim().length < 3} onClick={doSuspend}>
              Suspender
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-4">
          <FormError message={actionError} />
          <p>
            <strong>{name}</strong> continua vendo os dados, mas não consegue cadastrar nem editar nada até ser reativada.
          </p>
          <Field label="Motivo" required htmlFor="reason" hint="Fica registrado no histórico da conta.">
            <Textarea id="reason" maxLength={200} value={reason} onChange={(e) => setReason(e.target.value)} className="min-h-20" />
          </Field>
        </div>
      </Modal>
    </>
  );
}

