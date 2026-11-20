import { CreditCard, Lock, ReceiptText } from "lucide-react";
import { useState } from "react";
import { ResumeSubscriptionButton } from "../../components/ResumeSubscriptionButton";
import { Button, Card, EmptyState, ErrorState, Eyebrow, LinkButton, Loading, Modal, PageHeader, StatusPill, TableHead, TotalBar } from "../../components/ui";
import { api, errorMessage } from "../../lib/api";
import { useMe } from "../../lib/auth";
import { date, daysBetween, formatDocument, instantDate, instantDay, money, plural, today } from "../../lib/format";
import { keys, useInvoices, useSubscription } from "../../lib/queries";
import { useToast } from "../../lib/toast";
import type { Subscription as Sub } from "../../lib/types";
import { useQueryClient } from "@tanstack/react-query";

const methodLabel = { pix: "Pix", boleto: "Boleto", card: "Cartão" } as const;

function planLine(sub: Sub) {
  if (sub.account_status === "trial" && sub.trial_ends_at) {
    const end = instantDay(sub.trial_ends_at)!;
    const days = Math.max(0, daysBetween(today(), end));
    return `Teste grátis até ${date(end)} (${plural(days, "dia restante", "dias restantes")})`;
  }
  if (sub.status === "cancelled") return "Assinatura cancelada";
  if (sub.cancel_at_period_end) return sub.next_due_date ? `Cancelamento agendado. Acesso até ${date(sub.next_due_date)}` : "Cancelamento agendado";
  return sub.next_due_date ? `Próxima fatura em ${date(sub.next_due_date)}` : null;
}

export default function Subscription() {
  const toast = useToast();
  const qc = useQueryClient();
  const { data: me } = useMe();
  const { data: sub, isLoading, error, refetch } = useSubscription();
  const { data: invoices } = useInvoices();
  const [checkingOut, setCheckingOut] = useState(false);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  if (isLoading) return <Loading />;
  if (error || !sub || !me?.account) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  const account = me.account;
  const paid = invoices?.filter((i) => i.status === "paid") ?? [];
  const paidTotal = paid.reduce((s, i) => s + i.amount_cents, 0);
  const openInvoice = invoices?.find((i) => i.status === "pending");
  const canCancel = sub.status !== "cancelled" && !sub.cancel_at_period_end;
  const payable = sub.status !== "cancelled" && !sub.cancel_at_period_end;

  async function checkout() {
    setCheckingOut(true);
    try {
      const { url } = await api.post<{ url: string }>("/subscription/checkout");
      window.location.href = url;
    } catch (e) {
      toast.show(errorMessage(e), "error");
      setCheckingOut(false);
    }
  }

  async function cancel() {
    setCancelling(true);
    try {
      const updated = await api.post<Sub>("/subscription/cancel");
      qc.setQueryData(keys.subscription, updated);
      toast.show("Cancelamento agendado para o fim do período.");
      setConfirmCancel(false);
    } catch (e) {
      toast.show(errorMessage(e), "error");
    } finally {
      setCancelling(false);
    }
  }

  return (
    <>
      <PageHeader eyebrow="// Conta · assinatura" title="Assinatura" subtitle="Seu plano, o uso e as faturas da Carteirama." />

      <div className="grid gap-6 lg:grid-cols-2">
        <Card dark className="flex flex-col p-6 sm:p-8">
          <div className="flex items-start justify-between gap-3">
            <p className="font-mono text-[11px] font-bold tracking-[0.14em] text-amber uppercase">[Plano atual]</p>
            <StatusPill kind="account" status={sub.account_status} />
          </div>
          <p className="mt-2 text-3xl font-extrabold">{sub.plan.name}</p>
          <p className="tabular mt-2 text-[44px] leading-none font-extrabold tracking-tight text-amber">
            {money(sub.price_cents)}
            <span className="ml-1.5 font-mono text-sm font-medium text-white/80">/mês</span>
          </p>
          <div className="mt-5 space-y-1 text-[15px] text-white/90">
            {planLine(sub) && <p>{planLine(sub)}</p>}
          </div>
          <div className="mt-auto pt-6">
            {canCancel ? (
              <button type="button" onClick={() => setConfirmCancel(true)} className="text-sm font-semibold text-white underline underline-offset-2 hover:text-amber">
                Cancelar assinatura
              </button>
            ) : (
              sub.cancel_at_period_end &&
              sub.status !== "cancelled" && (
                <div className="flex flex-col items-start gap-3">
                  <p className="text-sm text-white/80">Mudou de ideia? Desfaça o cancelamento para continuar usando e voltar a pagar as faturas.</p>
                  <ResumeSubscriptionButton variant="amber" className="w-full sm:w-auto" />
                </div>
              )
            )}
          </div>
        </Card>

        <Card className="flex flex-col p-6 sm:p-8">
          <Eyebrow>[Uso da conta]</Eyebrow>
          <div className="mt-4 flex gap-12">
            <div>
              <p className="text-5xl font-extrabold">{sub.usage.clients}</p>
              <p className="mt-1 text-muted">clientes cadastrados</p>
            </div>
            <div>
              <p className="text-5xl font-extrabold">{sub.usage.users}</p>
              <p className="mt-1 text-muted">{sub.usage.users === 1 ? "usuário na conta" : "usuários na conta"}</p>
            </div>
          </div>
          <p className="mt-auto flex items-center gap-2 border-t-2 border-navy pt-4 text-sm text-muted">
            <Lock className="size-4" /> Backup diário na infraestrutura da Parlandim TI.
          </p>
        </Card>

        <Card className="flex flex-col p-6 sm:p-8">
          <Eyebrow>[Forma de pagamento]</Eyebrow>
          <div className="mt-4 flex items-center gap-4">
            <span className="grid size-14 shrink-0 place-items-center rounded-xl border-2 border-navy bg-blue text-white">
              <CreditCard className="size-6" />
            </span>
            <div>
              <p className="text-lg font-bold">{sub.account_status === "trial" ? "Teste grátis" : "Cobrança mensal"}</p>
              <p className="text-sm text-muted">
                {sub.account_status === "trial" ? "Cadastre o pagamento para continuar depois do teste." : "Pague com cartão, Pix ou boleto. Você recebe o recibo por e-mail."}
              </p>
            </div>
          </div>
          {sub.cancel_at_period_end && sub.status !== "cancelled" && (
            <p className="mt-6 rounded-xl border-2 border-navy bg-amber-soft px-4 py-3 text-sm font-semibold">
              Pagamentos ficam indisponíveis enquanto o cancelamento está agendado. Mantenha a assinatura para voltar a pagar.
            </p>
          )}
          {payable && (
            <div className="mt-6 flex flex-wrap gap-3">
              {openInvoice && (
                <LinkButton to={`/app/assinatura/faturas/${openInvoice.id}`} variant="primary">
                  Pagar fatura em aberto
                </LinkButton>
              )}
              <Button icon={CreditCard} loading={checkingOut} onClick={checkout}>
                {sub.account_status === "trial" ? "Assinar agora" : "Configurar cartão"}
              </Button>
            </div>
          )}
        </Card>

        <Card className="p-6 sm:p-8">
          <div className="flex items-center justify-between gap-3">
            <Eyebrow>[Dados de cobrança]</Eyebrow>
            <LinkButton to="/app/conta" size="sm">
              Editar
            </LinkButton>
          </div>
          <dl className="mt-4 divide-y-2 divide-navy border-t-2 border-navy">
            {[
              [account.person_type === "pj" ? "Empresa" : "Nome", account.company_name || account.legal_name],
              [account.person_type === "pj" ? "CNPJ" : "CPF", formatDocument(account.document)],
              ["E-mail das faturas", me.user.email],
            ].map(([k, v]) => (
              <div key={k} className="flex flex-col gap-1 py-3.5 sm:flex-row sm:items-center sm:justify-between">
                <dt className="font-mono text-[11px] tracking-[0.14em] text-muted uppercase">{k}</dt>
                <dd className="font-semibold break-all sm:text-right">{v}</dd>
              </div>
            ))}
          </dl>
        </Card>
      </div>

      <Card className="mt-8">
        <div className="px-5 pt-6 pb-4 sm:px-6">
          <Eyebrow>[Faturas]</Eyebrow>
        </div>
        {!invoices || invoices.length === 0 ? (
          <EmptyState icon={ReceiptText} title="Nenhuma fatura ainda" description={sub.account_status === "trial" ? "A primeira fatura sai quando o teste acabar." : undefined} />
        ) : (
          <>
            <TableHead className="grid-cols-[130px_130px_1fr_130px_120px_150px] gap-5 rounded-none border-t-2">
              <span>Fatura</span>
              <span>Vencimento</span>
              <span>Forma</span>
              <span>Status</span>
              <span className="text-right">Valor</span>
              <span />
            </TableHead>
            <ul>
              {invoices.map((i) => (
                <li key={i.id} className="grid grid-cols-[1fr_auto] items-center gap-x-4 gap-y-2 border-t-2 border-navy px-4 py-4 sm:px-6 md:grid-cols-[130px_130px_1fr_130px_120px_150px] md:gap-5 md:first:border-t-0">
                  <span className="font-mono text-sm font-bold">{i.number}</span>
                  <span className="hidden font-mono text-sm md:block">{date(i.due_date)}</span>
                  <span className="hidden text-sm text-muted md:block">{i.method ? methodLabel[i.method] : "—"}</span>
                  <span className="hidden md:block">
                    <StatusPill kind="invoice" status={i.display_status} />
                  </span>
                  <span className="tabular text-right text-lg font-extrabold">{money(i.amount_cents)}</span>
                  <span className="flex items-center gap-2 md:hidden">
                    <StatusPill kind="invoice" status={i.display_status} />
                    <span className="font-mono text-xs text-muted">{date(i.due_date)}</span>
                  </span>
                  <span className="text-right">
                    {i.status === "pending" && payable ? (
                      <LinkButton to={`/app/assinatura/faturas/${i.id}`} size="sm" variant="primary">
                        Pagar
                      </LinkButton>
                    ) : i.paid_at ? (
                      <span className="font-mono text-xs text-muted">pago {instantDate(i.paid_at)}</span>
                    ) : null}
                  </span>
                </li>
              ))}
            </ul>
          </>
        )}
        <TotalBar label="Total pago" sub={plural(paid.length, "fatura paga", "faturas pagas")} value={money(paidTotal)} />
      </Card>

      <Modal
        open={confirmCancel}
        onClose={() => setConfirmCancel(false)}
        title="Cancelar assinatura?"
        footer={
          <>
            <Button onClick={() => setConfirmCancel(false)}>Manter assinatura</Button>
            <Button variant="danger" loading={cancelling} onClick={cancel}>
              Cancelar assinatura
            </Button>
          </>
        }
      >
        <p>O acesso continua até o fim do período já pago. Depois disso, a conta fica só para consulta e os dados são apagados após 90 dias.</p>
      </Modal>
    </>
  );
}
