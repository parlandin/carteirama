import { Check, Clock, Plus, ReceiptText, TriangleAlert, Undo2, X } from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router";
import { CancelChargeModal } from "../../components/CancelChargeModal";
import { OneOffChargeModal } from "../../components/OneOffChargeModal";
import { PayChargeModal } from "../../components/PayChargeModal";
import { Button, Card, EmptyState, ErrorState, GroupChip, Loading, MonthPicker, PageHeader, Pill, StatusPill, TableHead, TotalBar } from "../../components/ui";
import { api, errorMessage } from "../../lib/api";
import { isReadOnly, useMe } from "../../lib/auth";
import { cx } from "../../lib/cx";
import { date, money, monthLabel, monthShort, plural } from "../../lib/format";
import { useAutoGenerateCharges, useCharges, useGroups, useInvalidateData, useSummary } from "../../lib/queries";
import { useToast } from "../../lib/toast";
import type { Charge } from "../../lib/types";
import { useMonthParam } from "../../lib/useMonth";

type Filter = "all" | "paid" | "pending" | "late";

export default function Charges() {
  const { data: me } = useMe();
  const readOnly = isReadOnly(me);
  const toast = useToast();
  const invalidate = useInvalidateData();
  const [month, shift] = useMonthParam();
  useAutoGenerateCharges(!readOnly);
  const [filter, setFilter] = useState<Filter>("all");
  const [paying, setPaying] = useState<Charge | null>(null);
  const [cancelling, setCancelling] = useState<Charge | null>(null);
  const [creating, setCreating] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);

  const { data: summary } = useSummary(month);
  const { data: charges, isLoading, error, refetch } = useCharges({ month });
  const { data: groups } = useGroups(month);
  const groupById = useMemo(() => new Map(groups?.map((g) => [g.id, g])), [groups]);

  const visible = useMemo(() => {
    const list = (charges ?? []).filter((c) => c.status !== "cancelled");
    return filter === "all" ? list : list.filter((c) => c.display_status === filter);
  }, [charges, filter]);

  const pct = summary && summary.total_cents > 0 ? Math.round((summary.paid_cents / summary.total_cents) * 100) : 0;

  async function unpay(c: Charge) {
    setBusyId(c.id);
    try {
      await api.post(`/charges/${c.id}/unpay`);
      await invalidate();
      toast.show("Pagamento desfeito.");
    } catch (e) {
      toast.show(errorMessage(e), "error");
    } finally {
      setBusyId(null);
    }
  }

  async function generate() {
    setGenerating(true);
    try {
      const r = await api.post<{ created: number }>("/charges/generate", { month });
      await invalidate();
      toast.show(r.created > 0 ? `${plural(r.created, "cobrança criada", "cobranças criadas")}.` : "Nenhum cliente ativo sem cobrança neste mês.", r.created > 0 ? "success" : "info");
    } catch (e) {
      toast.show(errorMessage(e), "error");
    } finally {
      setGenerating(false);
    }
  }

  const cards: { key: Filter; label: string; icon?: typeof Check; cents: number; count: number; className: string }[] = summary
    ? [
        { key: "paid", label: "Recebido", icon: Check, cents: summary.paid_cents, count: summary.paid_count, className: "bg-white" },
        { key: "pending", label: "A vencer", icon: Clock, cents: summary.pending_cents, count: summary.pending_count, className: "bg-white" },
        { key: "late", label: "Em atraso", icon: TriangleAlert, cents: summary.late_cents, count: summary.late_count, className: "bg-amber-soft" },
      ]
    : [];

  return (
    <>
      <PageHeader
        eyebrow={`// Cobranças · ${monthShort(month)}`}
        title="Cobranças do mês"
        subtitle="Quem já pagou, quem vence nos próximos dias e quem está em atraso."
        actions={
          <>
            <MonthPicker month={month} onChange={shift} className="w-full sm:w-auto" />
            <Button variant="primary" icon={Plus} disabled={readOnly} onClick={() => setCreating(true)} className="w-full sm:w-auto">
              Cobrança avulsa
            </Button>
          </>
        }
      />

      {summary && (
        <div className="mb-7 grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-4">
          <button
            type="button"
            onClick={() => setFilter("all")}
            className={cx("col-span-2 rounded-2xl border-2 border-navy bg-navy p-5 text-left text-white shadow-hard sm:p-6 lg:col-span-1", filter === "all" && "ring-3 ring-amber ring-offset-2 ring-offset-paper")}
          >
            <p className="font-mono text-[11px] font-bold tracking-[0.14em] text-amber uppercase">[Previsto]</p>
            <p className="tabular mt-1 text-[28px] font-extrabold tracking-tight text-amber">{money(summary.total_cents)}</p>
            <div className="mt-3 h-2.5 overflow-hidden rounded-full bg-white/20">
              <div className="h-full bg-amber" style={{ width: `${pct}%` }} />
            </div>
            <p className="mt-2 font-mono text-xs">{pct}% recebido</p>
          </button>
          {cards.map((c) => (
            <button
              key={c.key}
              type="button"
              aria-pressed={filter === c.key}
              onClick={() => setFilter(filter === c.key ? "all" : c.key)}
              className={cx("rounded-2xl border-2 border-navy p-4 text-left shadow-hard transition hover:-translate-y-0.5 sm:p-6", c.className, filter === c.key && "ring-3 ring-blue ring-offset-2 ring-offset-paper")}
            >
              <p className={cx("flex items-center gap-1.5 font-mono text-[11px] font-bold tracking-[0.14em] uppercase", c.key === "late" ? "text-[#8a5300]" : "text-muted")}>
                {c.icon && <c.icon className={cx("size-4", c.key === "paid" && "text-green")} />}[{c.label}]
              </p>
              <p className="tabular mt-1 text-xl font-extrabold tracking-tight sm:text-[28px]">{money(c.cents)}</p>
              <p className="mt-2 font-mono text-xs text-muted">{plural(c.count, "cobrança", "cobranças")}</p>
            </button>
          ))}
        </div>
      )}

      {isLoading ? (
        <Loading />
      ) : error ? (
        <ErrorState message={errorMessage(error)} onRetry={refetch} />
      ) : (
        <Card>
          {visible.length === 0 ? (
            filter === "all" ? (
              <EmptyState
                icon={ReceiptText}
                title={`Nenhuma cobrança em ${monthLabel(month).toLowerCase()}`}
                description="As mensalidades são criadas a partir dos clientes ativos, com o valor e o dia de vencimento de cada um. Cobranças avulsas você cria pelo botão acima."
                action={
                  <Button variant="primary" loading={generating} disabled={readOnly} onClick={generate}>
                    Gerar cobranças de {monthLabel(month)}
                  </Button>
                }
              />
            ) : (
              <EmptyState icon={ReceiptText} title="Nada por aqui" description="Nenhuma cobrança com essa situação neste mês." action={<Button size="sm" onClick={() => setFilter("all")}>Ver todas</Button>} />
            )
          ) : (
            <>
              <TableHead className="grid-cols-[1.5fr_1.2fr_110px_120px_120px_210px] gap-5">
                <span>Cliente</span>
                <span>Grupo</span>
                <span>Vence em</span>
                <span className="text-right">Valor</span>
                <span>Situação</span>
                <span />
              </TableHead>
              <ul>
                {visible.map((c) => {
                  const g = groupById.get(c.group_id);
                  const action =
                    readOnly ? null : c.status === "pending" ? (
                      <span className="flex items-center justify-end gap-1">
                        <Button size="sm" onClick={() => setPaying(c)} className="md:w-auto">
                          Marcar como pago
                        </Button>
                        {c.kind === "one_off" && (
                          <Button size="sm" variant="ghost" icon={X} onClick={() => setCancelling(c)} className="px-2 text-muted" aria-label={`Cancelar cobrança avulsa ${c.description}`} title="Cancelar cobrança avulsa" />
                        )}
                      </span>
                    ) : c.status === "paid" ? (
                      <Button size="sm" variant="ghost" icon={Undo2} loading={busyId === c.id} onClick={() => unpay(c)} className="text-muted">
                        Desfazer
                      </Button>
                    ) : null;
                  return (
                    <li key={c.id} className="grid grid-cols-[1fr_auto] items-center gap-x-4 gap-y-2.5 border-b-2 border-navy px-4 py-4 last:border-b-0 sm:px-6 md:grid-cols-[1.5fr_1.2fr_110px_120px_120px_210px] md:gap-5 md:py-3.5">
                      <span className="min-w-0">
                        <Link to={`/app/clientes/${c.client_id}`} className="font-bold hover:underline">
                          {c.client_name}
                        </Link>
                        {c.kind === "one_off" && (
                          <span className="mt-1 flex min-w-0 items-center gap-1.5">
                            <Pill tone="blue">Avulsa</Pill>
                            <span className="truncate text-sm text-muted">{c.description}</span>
                          </span>
                        )}
                      </span>
                      <span className="hidden min-w-0 md:block">{g && <GroupChip name={g.name} color={g.color} />}</span>
                      <span className="hidden font-mono text-sm md:block">{date(c.due_date)}</span>
                      <span className="tabular text-right text-lg font-extrabold">{money(c.amount_cents)}</span>
                      <span className="flex flex-wrap items-center gap-2 md:contents">
                        <StatusPill kind="charge" status={c.display_status} />
                        <span className="font-mono text-xs text-muted md:hidden">vence {date(c.due_date)}</span>
                      </span>
                      <span className="flex justify-end md:block md:text-right">{action}</span>
                    </li>
                  );
                })}
              </ul>
            </>
          )}
          {summary && <TotalBar label={`Total previsto · ${plural(summary.total_count, "cobrança", "cobranças")}`} value={money(summary.total_cents)} />}
        </Card>
      )}

      <PayChargeModal charge={paying} onClose={() => setPaying(null)} />
      <CancelChargeModal charge={cancelling} onClose={() => setCancelling(null)} />
      <OneOffChargeModal open={creating} onClose={() => setCreating(false)} month={month} />
    </>
  );
}
