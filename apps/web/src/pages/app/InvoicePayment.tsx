import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Barcode, Check, CircleCheckBig, Copy, CreditCard, QrCode, RefreshCw, ShieldCheck, Smartphone, TriangleAlert, type LucideIcon } from "lucide-react";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { useParams } from "react-router";
import { PixQrCode } from "../../components/PixQrCode";
import { ResumeSubscriptionButton } from "../../components/ResumeSubscriptionButton";
import { Button, Card, ErrorState, Eyebrow, FormError, LinkButton, Loading, PageHeader, StatusPill } from "../../components/ui";
import { api, errorMessage } from "../../lib/api";
import { ME_KEY, useMe } from "../../lib/auth";
import { date, daysBetween, formatDocument, instantDate, money, plural, today } from "../../lib/format";
import { keys, useInvoices, useSubscription } from "../../lib/queries";
import { useToast } from "../../lib/toast";
import type { PaymentLink } from "../../lib/types";

type Method = "pix" | "card" | "boleto";

const methodInfo: Record<Method, { label: string; icon: LucideIcon; hint: string }> = {
  pix: { label: "Pix", icon: QrCode, hint: "Aprovação na hora" },
  card: { label: "Cartão", icon: CreditCard, hint: "Em breve" },
  boleto: { label: "Boleto", icon: Barcode, hint: "Em breve" },
};

const methodLabel = { pix: "Pix", boleto: "Boleto", card: "Cartão" } as const;

function MethodTab({ method, active, available, onSelect }: { method: Method; active: boolean; available: boolean; onSelect: () => void }) {
  const { label, icon: Icon, hint } = methodInfo[method];
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      disabled={!available}
      onClick={onSelect}
      className={
        "flex min-w-0 flex-1 items-center gap-3 rounded-xl border-2 border-navy px-3 py-3 text-left transition sm:px-4 " +
        (active ? "bg-navy text-white shadow-hard-sm" : available ? "bg-white hover:bg-paper-2" : "cursor-not-allowed bg-paper-2 text-muted opacity-70")
      }
    >
      <span className={"grid size-9 shrink-0 place-items-center rounded-lg border-2 " + (active ? "border-amber bg-amber text-navy" : "border-navy bg-white")}>
        <Icon className="size-4" strokeWidth={2.5} />
      </span>
      <span className="min-w-0">
        <span className="block font-bold">{label}</span>
        <span className={"block truncate font-mono text-[11px] tracking-wider uppercase " + (active ? "text-white/70" : "text-muted")}>{available ? (method === "pix" ? hint : "Disponível") : hint}</span>
      </span>
    </button>
  );
}

function Step({ n, title, children }: { n: number; title: string; children: ReactNode }) {
  return (
    <li className="flex gap-4">
      <span className="grid size-8 shrink-0 place-items-center rounded-full border-2 border-navy bg-amber font-mono text-sm font-bold">{n}</span>
      <div>
        <p className="font-bold">{title}</p>
        <p className="text-sm text-muted">{children}</p>
      </div>
    </li>
  );
}

function PixPanel({ code, amount, refreshing, onRefresh }: { code: string; amount: number; refreshing: boolean; onRefresh: () => void }) {
  const toast = useToast();
  const [copied, setCopied] = useState(false);

  function copy() {
    navigator.clipboard.writeText(code).then(() => {
      setCopied(true);
      toast.show("Código Pix copiado.");
      window.setTimeout(() => setCopied(false), 2500);
    });
  }

  return (
    <div className="grid gap-8 md:grid-cols-[260px_1fr] md:items-start">
      <div className="flex flex-col items-center">
        <PixQrCode code={code} />
        <p className="tabular text-center text-3xl font-extrabold">{money(amount)}</p>
        <p className="mt-1 text-center font-mono text-[11px] tracking-widest text-muted uppercase">Escaneie com o app do banco</p>
      </div>

      <div className="flex min-w-0 flex-col gap-6">
        <div>
          <p className="mb-2 font-mono text-[11px] tracking-[0.14em] text-muted uppercase">Pix copia e cola</p>
          <div className="rounded-xl border-2 border-navy bg-paper-2 p-3 font-mono text-xs break-all select-all">{code}</div>
          <div className="mt-3 flex flex-wrap gap-3">
            <Button variant="primary" icon={copied ? Check : Copy} onClick={copy}>
              {copied ? "Copiado" : "Copiar código"}
            </Button>
            <Button icon={RefreshCw} loading={refreshing} onClick={onRefresh}>
              Gerar novo código
            </Button>
          </div>
        </div>

        <ol className="flex flex-col gap-4 border-t-2 border-navy pt-6">
          <Step n={1} title="Abra o app do seu banco">
            Entre na área Pix e escolha pagar com QR code ou Pix copia e cola.
          </Step>
          <Step n={2} title="Escaneie ou cole o código">
            Aponte a câmera para o QR code, ou cole o código copiado acima.
          </Step>
          <Step n={3} title={`Confira o valor de ${money(amount)} e confirme`}>
            A confirmação chega em segundos e esta página atualiza sozinha.
          </Step>
        </ol>
      </div>
    </div>
  );
}

function WaitingBar() {
  return (
    <div className="flex items-center gap-3 rounded-xl border-2 border-navy bg-amber-soft px-4 py-3">
      <span className="relative flex size-3">
        <span className="absolute inline-flex size-full animate-ping rounded-full bg-amber opacity-75" />
        <span className="relative inline-flex size-3 rounded-full bg-amber" />
      </span>
      <p className="text-sm font-semibold">Aguardando o pagamento. Verificamos automaticamente a cada poucos segundos.</p>
    </div>
  );
}

function SummaryRow({ label, value, strong }: { label: ReactNode; value: ReactNode; strong?: boolean }) {
  return (
    <div className="flex items-baseline justify-between gap-4 py-3">
      <dt className="text-white/80">{label}</dt>
      <dd className={strong ? "tabular text-right text-lg font-extrabold" : "tabular text-right font-semibold"}>{value}</dd>
    </div>
  );
}

export default function InvoicePayment() {
  const { id = "" } = useParams();
  const qc = useQueryClient();
  const toast = useToast();
  const { data: me } = useMe();
  const { data: sub } = useSubscription();
  const { data: invoices, isLoading, error, refetch } = useInvoices();
  const invoice = invoices?.find((i) => i.id === id);
  const blocked = !!sub && (sub.status === "cancelled" || sub.cancel_at_period_end);
  const pending = invoice?.status === "pending" && !!sub && !blocked;
  const [method, setMethod] = useState<Method>("pix");
  const wasPending = useRef(false);

  const link = useQuery({
    queryKey: ["payment-link", id],
    queryFn: () => api.post<PaymentLink>(`/subscription/invoices/${id}/payment-link`),
    enabled: pending,
    retry: false,
    staleTime: Infinity,
    gcTime: 0,
  });

  useEffect(() => {
    if (!pending) return;
    const timer = window.setInterval(() => qc.invalidateQueries({ queryKey: keys.invoices }), 5000);
    return () => window.clearInterval(timer);
  }, [pending, qc]);

  useEffect(() => {
    if (pending) {
      wasPending.current = true;
    } else if (wasPending.current && invoice?.status === "paid") {
      wasPending.current = false;
      toast.show("Pagamento confirmado. Obrigado!");
      qc.invalidateQueries({ queryKey: keys.subscription });
      qc.invalidateQueries({ queryKey: ME_KEY });
    }
  }, [pending, invoice?.status, qc, toast]);

  if (isLoading) return <Loading />;
  if (error) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;
  if (!invoice) return <ErrorState message="Fatura não encontrada." />;

  const back = { to: "/app/assinatura", label: "Assinatura" };
  const late = invoice.display_status === "late";
  const lateDays = late ? daysBetween(invoice.due_date, today()) : 0;
  const account = me?.account;
  const available: Record<Method, boolean> = {
    pix: !!link.data?.pix_code,
    card: !!link.data?.card_url,
    boleto: !!link.data?.boleto_url,
  };
  const selected: Method = available[method] ? method : "pix";

  return (
    <>
      <PageHeader
        back={back}
        eyebrow="// Assinatura · pagamento"
        title={`Fatura ${invoice.number}`}
        badge={<StatusPill kind="invoice" status={invoice.display_status} />}
        subtitle={invoice.status === "paid" ? `Paga em ${instantDate(invoice.paid_at)}` : `Vencimento em ${date(invoice.due_date)}`}
      />

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px] lg:items-start">
        <div className="flex min-w-0 flex-col gap-6">
          {invoice.status === "paid" && (
            <Card className="p-6 sm:p-10">
              <div className="flex flex-col items-center text-center">
                <span className="grid size-16 place-items-center rounded-2xl border-2 border-navy bg-green text-white shadow-hard-sm">
                  <CircleCheckBig className="size-8" />
                </span>
                <h2 className="mt-5 text-3xl font-extrabold">Pagamento confirmado</h2>
                <p className="mt-2 max-w-md text-muted">
                  Recebemos {money(invoice.amount_cents)}
                  {invoice.method ? ` via ${methodLabel[invoice.method]}` : ""} em {instantDate(invoice.paid_at)}. Sua assinatura está em dia.
                </p>
                <div className="mt-6 flex flex-wrap justify-center gap-3">
                  <LinkButton to="/app" variant="primary">
                    Ir para o painel
                  </LinkButton>
                  <LinkButton to="/app/assinatura">Ver assinatura</LinkButton>
                </div>
              </div>
            </Card>
          )}

          {invoice.status === "cancelled" && (
            <Card className="p-6 sm:p-8">
              <h2 className="text-2xl font-extrabold">Fatura cancelada</h2>
              <p className="mt-2 text-muted">Esta fatura foi cancelada e não precisa ser paga.</p>
            </Card>
          )}

          {invoice.status === "pending" && blocked && sub && (
            <Card className="p-6 sm:p-8">
              {sub.status === "cancelled" ? (
                <>
                  <h2 className="text-2xl font-extrabold">Assinatura cancelada</h2>
                  <p className="mt-2 text-muted">Esta fatura não pode mais ser paga porque a assinatura foi encerrada.</p>
                </>
              ) : (
                <>
                  <h2 className="text-2xl font-extrabold">Cancelamento agendado</h2>
                  <p className="mt-2 text-muted">
                    Você pediu o cancelamento da assinatura, então esta fatura não precisa ser paga. Se quiser continuar usando a Carteirama, mantenha a assinatura e pague por aqui.
                  </p>
                  <ResumeSubscriptionButton className="mt-5 w-full sm:w-auto" />
                </>
              )}
            </Card>
          )}

          {pending && (
            <>
              {late && (
                <div className="flex items-start gap-3 rounded-xl border-2 border-navy bg-red-soft px-4 py-3 text-red">
                  <TriangleAlert className="mt-0.5 size-5 shrink-0" />
                  <p className="font-semibold">
                    Vencida há {plural(lateDays, "dia", "dias")}. Pague agora para manter o acesso completo à conta.
                  </p>
                </div>
              )}

              <Card className="p-5 sm:p-8">
                <Eyebrow>[Forma de pagamento]</Eyebrow>
                <div role="tablist" className="mt-4 flex flex-col gap-3 sm:flex-row">
                  {(["pix", "card", "boleto"] as Method[]).map((m) => (
                    <MethodTab key={m} method={m} active={selected === m} available={m === "pix" || available[m]} onSelect={() => setMethod(m)} />
                  ))}
                </div>

                <div className="mt-8">
                  <FormError message={link.error ? errorMessage(link.error) : null} />
                  {link.error && (
                    <Button className="mt-4" icon={RefreshCw} onClick={() => link.refetch()}>
                      Tentar de novo
                    </Button>
                  )}
                  {link.isLoading && <Loading label="Gerando o Pix…" />}
                  {selected === "pix" && link.data?.pix_code && (
                    <PixPanel
                      code={link.data.pix_code}
                      amount={invoice.amount_cents}
                      refreshing={link.isFetching}
                      onRefresh={() => link.refetch()}
                    />
                  )}
                  {selected === "card" && link.data?.card_url && (
                    <LinkButton to={link.data.card_url} target="_blank" rel="noreferrer" variant="primary" icon={CreditCard}>
                      Pagar com cartão
                    </LinkButton>
                  )}
                  {selected === "boleto" && link.data?.boleto_url && (
                    <LinkButton to={link.data.boleto_url} target="_blank" rel="noreferrer" variant="primary" icon={Barcode}>
                      Abrir boleto
                    </LinkButton>
                  )}
                </div>
              </Card>

              {link.data && <WaitingBar />}
            </>
          )}
        </div>

        <aside className="flex flex-col gap-6">
          <Card dark className="p-6 sm:p-7">
            <p className="font-mono text-[11px] font-bold tracking-[0.14em] text-amber uppercase">[Resumo]</p>
            <dl className="mt-3 divide-y divide-white/20">
              <SummaryRow label="Fatura" value={invoice.number} />
              <SummaryRow label={`Plano ${sub?.plan.name ?? ""}`.trim()} value="Mensal" />
              <SummaryRow label="Vencimento" value={date(invoice.due_date)} />
              <SummaryRow label="Situação" value={invoice.status === "paid" ? "Paga" : invoice.status === "cancelled" ? "Cancelada" : blocked ? "Não precisa ser paga" : late ? "Em atraso" : "Em aberto"} />
            </dl>
            <div className="mt-4 border-t-2 border-white/30 pt-4">
              <p className="text-white/80">Total</p>
              <p className="tabular text-[44px] leading-none font-extrabold tracking-tight text-amber">{money(invoice.amount_cents)}</p>
            </div>
          </Card>

          {account && me && (
            <Card className="p-6 sm:p-7">
              <Eyebrow>[Dados de cobrança]</Eyebrow>
              <dl className="mt-3 divide-y-2 divide-navy border-t-2 border-navy">
                {[
                  [account.person_type === "pj" ? "Empresa" : "Nome", account.company_name || account.legal_name],
                  [account.person_type === "pj" ? "CNPJ" : "CPF", formatDocument(account.document)],
                  ["E-mail", me.user.email],
                ].map(([k, v]) => (
                  <div key={k} className="py-3">
                    <dt className="font-mono text-[11px] tracking-[0.14em] text-muted uppercase">{k}</dt>
                    <dd className="font-semibold break-all">{v}</dd>
                  </div>
                ))}
              </dl>
            </Card>
          )}

          <div className="flex gap-3 rounded-xl border-2 border-navy bg-white px-4 py-4 text-sm">
            <ShieldCheck className="size-5 shrink-0 text-green" />
            <p>Pagamento processado com segurança. A Carteirama não guarda dados bancários.</p>
          </div>
          <div className="flex gap-3 rounded-xl border-2 border-navy bg-white px-4 py-4 text-sm">
            <Smartphone className="size-5 shrink-0 text-blue" />
            <p>Pode fechar esta página depois de pagar: a fatura é baixada automaticamente quando o pagamento é confirmado.</p>
          </div>
        </aside>
      </div>
    </>
  );
}
