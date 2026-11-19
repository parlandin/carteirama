import { ChevronRight } from "lucide-react";
import type { ReactNode } from "react";
import { Link } from "react-router";
import { Card, ErrorState, Eyebrow, LinkButton, Loading, PageHeader, StatusPill, TotalBar } from "../../components/ui";
import { errorMessage } from "../../lib/api";
import { cx } from "../../lib/cx";
import { MONTHS, date, daysBetween, instantDay, money, monthShort, plural, today } from "../../lib/format";
import { useAdminAccounts, useAdminSummary } from "../../lib/queries";
import type { AdminAccountItem } from "../../lib/types";

function Kpi({ label, value, sub, tag, className }: { label: string; value: string; sub: string; tag?: ReactNode; className?: string }) {
  return (
    <Card className={cx("p-4 sm:p-6", className)}>
      <div className="flex items-start justify-between gap-2">
        <p className="font-mono text-[11px] tracking-[0.14em] text-muted uppercase">[{label}]</p>
        {tag && <span className="hidden sm:block">{tag}</span>}
      </div>
      <p className="tabular mt-2 text-2xl font-extrabold tracking-tight sm:text-[32px]">{value}</p>
      <p className="mt-1.5 text-sm text-muted">{sub}</p>
    </Card>
  );
}

function MrrChart({ data }: { data: { month: string; cents: number }[] }) {
  const max = Math.max(1, ...data.map((d) => d.cents));
  const last = data[data.length - 1];
  const prev = data[data.length - 2];
  const delta = prev && prev.cents > 0 ? ((last.cents - prev.cents) / prev.cents) * 100 : null;
  return (
    <Card className="p-5 sm:p-7">
      <div className="mb-6 flex flex-wrap items-baseline justify-between gap-2">
        <Eyebrow>[MRR por mês]</Eyebrow>
        {delta !== null && (
          <p className="text-sm text-muted">
            {delta >= 0 ? "+" : ""}
            {delta.toFixed(1).replace(".", ",")}% em relação a {MONTHS[Number(prev.month.slice(5)) - 1].toLowerCase()}
          </p>
        )}
      </div>
      <div className="flex h-56 items-end gap-2 border-b-2 border-navy sm:gap-4" role="img" aria-label="Faturamento recorrente dos últimos meses">
        {data.map((d, i) => {
          const isLast = i === data.length - 1;
          return (
            <div key={d.month} className="flex h-full min-w-0 flex-1 flex-col items-center justify-end">
              <span className="mb-1 font-mono text-[11px] font-bold">{Math.round(d.cents / 100)}</span>
              <div
                className={cx("w-full rounded-t-lg border-2 border-b-0 border-navy", isLast ? "bg-amber" : "bg-blue")}
                style={{ height: `${Math.max(2, (d.cents / max) * 80)}%` }}
                title={`${monthShort(d.month)}: ${money(d.cents)}`}
              />
            </div>
          );
        })}
      </div>
      <div className="mt-1.5 flex gap-2 sm:gap-4">
        {data.map((d) => (
          <span key={d.month} className="flex-1 text-center font-mono text-[10px] text-muted">
            {MONTHS[Number(d.month.slice(5)) - 1].slice(0, 3).toUpperCase()}
          </span>
        ))}
      </div>
    </Card>
  );
}

function StatusBreakdown({ by }: { by: Record<string, number> }) {
  const late = by.late ?? 0;
  const rows = [
    { key: "active", label: "Ativas", sub: "pagando em dia", n: Math.max(0, (by.active ?? 0) - late), color: "#167A3C" },
    { key: "trial", label: "Em teste", sub: "ainda sem cobrança", n: by.trial ?? 0, color: "#2553E6" },
    { key: "late", label: "Em atraso", sub: "fatura vencida", n: late, color: "#FBAF24" },
    { key: "suspended", label: "Suspensas", sub: "sem acesso de escrita", n: by.suspended ?? 0, color: "#C53030" },
    { key: "cancelled", label: "Canceladas", sub: "sem acesso", n: by.cancelled ?? 0, color: "#111B35" },
  ];
  const total = rows.reduce((s, r) => s + r.n, 0);
  return (
    <Card className="p-5 sm:p-7">
      <Eyebrow className="mb-5">[Contas por situação]</Eyebrow>
      <div className="flex h-8 overflow-hidden rounded-full border-2 border-navy bg-paper">
        {total > 0 && rows.filter((r) => r.n > 0).map((r) => <div key={r.key} style={{ width: `${(r.n / total) * 100}%`, background: r.color }} title={`${r.label}: ${r.n}`} />)}
      </div>
      <ul className="mt-5 divide-y-2 divide-navy border-t-2 border-navy">
        {rows.map((r) => (
          <li key={r.key} className="flex items-center gap-3 py-3">
            <span className="size-4 shrink-0 rounded border-2 border-navy" style={{ background: r.color }} />
            <div className="flex-1">
              <p className="font-bold">{r.label}</p>
              <p className="text-xs text-muted">{r.sub}</p>
            </div>
            <div className="text-right">
              <p className="text-lg font-extrabold">{r.n}</p>
              <p className="font-mono text-[11px] text-muted">{total > 0 ? Math.round((r.n / total) * 100) : 0}%</p>
            </div>
          </li>
        ))}
      </ul>
    </Card>
  );
}

function attentionLine(a: AdminAccountItem) {
  if (a.display_status === "late") return "Fatura vencida e não paga";
  if (a.display_status === "trial" && a.trial_ends_at) {
    const end = instantDay(a.trial_ends_at)!;
    const d = daysBetween(today(), end);
    return `O teste termina em ${date(end)}${d >= 0 ? `, em ${plural(d, "dia", "dias")}` : ""}`;
  }
  if (a.display_status === "suspended") return `Suspensa em ${date(instantDay(a.suspended_at))}`;
  return "";
}

export default function AdminOverview() {
  const { data: s, isLoading, error, refetch } = useAdminSummary();
  const { data: late } = useAdminAccounts({ status: "late", limit: 5 });
  const { data: trial } = useAdminAccounts({ status: "trial", limit: 5 });
  const { data: suspended } = useAdminAccounts({ status: "suspended", limit: 5 });

  if (isLoading) return <Loading />;
  if (error || !s) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  const by = s.accounts_by_status;
  const totalAccounts = (by.trial ?? 0) + (by.active ?? 0) + (by.suspended ?? 0) + (by.cancelled ?? 0);
  const inv = s.invoices_month;
  const billed = inv.paid_cents + inv.pending_cents + inv.late_cents;
  const trialEnd = trial?.map((t) => instantDay(t.trial_ends_at)).filter(Boolean).sort().at(-1);
  const attention = [...(late ?? []), ...(suspended ?? []), ...(trial ?? [])].slice(0, 8);
  const monthTag = monthShort(inv.month).split(" ")[0];

  return (
    <>
      <PageHeader
        eyebrow="// Admin · visão geral"
        title="Visão geral"
        subtitle="A receita recorrente e a saúde das contas da Carteirama."
        actions={
          <LinkButton to="/admin/contas" className="w-full sm:w-auto">
            Ver contas
          </LinkButton>
        }
      />

      <div className="mb-7 grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-4">
        <Kpi label={`Recebido em ${monthTag.toLowerCase()}`} value={money(inv.paid_cents)} sub={`de ${money(billed)} em faturas do mês`} />
        <Kpi label="Contas ativas" value={String(by.active ?? 0)} sub={`de ${totalAccounts} contas no total`} />
        <Kpi label="Em teste" value={String(by.trial ?? 0)} sub={trialEnd ? `terminam até ${date(trialEnd)}` : "nenhum teste em andamento"} tag={<StatusPill kind="account" status="trial" />} />
        <Kpi label="Em atraso" value={String(by.late ?? 0)} sub={`${money(inv.late_cents)} em aberto`} tag={<StatusPill kind="account" status="late" />} />
      </div>

      <div className="mb-7 grid gap-6 lg:grid-cols-[1.6fr_1fr]">
        <MrrChart data={s.mrr_by_month.slice(-6)} />
        <StatusBreakdown by={by} />
      </div>

      <Card>
        <div className="border-b-2 border-navy px-5 py-5 sm:px-6">
          <Eyebrow>[Precisa de atenção]</Eyebrow>
        </div>
        {attention.length === 0 ? (
          <p className="px-6 py-10 text-center text-muted">Tudo em dia. Nenhuma conta em atraso, suspensa ou em teste.</p>
        ) : (
          <ul>
            {attention.map((a) => (
              <li key={a.id} className="border-b-2 border-navy last:border-b-0">
                <Link to={`/admin/contas/${a.id}`} className="grid grid-cols-[1fr_auto] items-center gap-x-4 gap-y-1.5 px-4 py-4 hover:bg-paper-2 sm:grid-cols-[140px_1fr_auto] sm:px-6">
                  <span className="sm:order-none">
                    <StatusPill kind="account" status={a.display_status} />
                  </span>
                  <ChevronRight className="size-5 sm:order-last" strokeWidth={2.5} />
                  <div className="col-span-2 min-w-0 sm:col-span-1">
                    <p className="font-bold">{a.company_name || a.legal_name}</p>
                    <p className="text-sm text-muted">{attentionLine(a)}</p>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
        <TotalBar label={`MRR · ${monthShort(inv.month)}`} sub="Soma das assinaturas pagantes, ativas e em atraso" value={money(s.mrr_cents)} />
      </Card>
    </>
  );
}
