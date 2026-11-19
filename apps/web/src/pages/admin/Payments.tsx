import { useQueryClient } from "@tanstack/react-query";
import { ReceiptText } from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router";
import { MarkPaidModal } from "../../components/MarkPaidModal";
import { Button, Card, EmptyState, ErrorState, FilterChips, Loading, MonthPicker, PageHeader, StatusPill, TableHead, TotalBar } from "../../components/ui";
import { errorMessage } from "../../lib/api";
import { money, plural, shortDate } from "../../lib/format";
import { useAdminInvoices } from "../../lib/queries";
import type { AdminInvoiceItem, Invoice } from "../../lib/types";
import { useMonthParam } from "../../lib/useMonth";

type Filter = "all" | "paid" | "pending" | "late";
const methodLabel = { pix: "Pix", boleto: "Boleto", card: "Cartão" } as const;

export default function AdminPayments() {
  const qc = useQueryClient();
  const [month, shift] = useMonthParam();
  const [filter, setFilter] = useState<Filter>("all");
  const [marking, setMarking] = useState<AdminInvoiceItem | null>(null);
  const { data, isLoading, error, refetch } = useAdminInvoices({ month });

  const valid = useMemo(() => (data ?? []).filter((i) => i.invoice.status !== "cancelled"), [data]);
  const count = (f: Filter) => (f === "all" ? valid.length : valid.filter((i) => i.invoice.display_status === f).length);
  const sum = (f: Filter) => valid.filter((i) => f === "all" || i.invoice.display_status === f).reduce((s, i) => s + i.invoice.amount_cents, 0);
  const shown = filter === "all" ? valid : valid.filter((i) => i.invoice.display_status === filter);

  return (
    <>
      <PageHeader
        eyebrow="// Admin · pagamentos"
        title="Pagamentos"
        subtitle="Faturas de todas as contas no mês, com o que já entrou e o que falta."
        actions={<MonthPicker month={month} onChange={shift} className="w-full sm:w-auto" />}
      />

      <div className="mb-6">
        <FilterChips<Filter>
          value={filter}
          onChange={setFilter}
          options={[
            { value: "all", label: "Todas", count: data ? count("all") : undefined },
            { value: "paid", label: "Pagas", count: data ? count("paid") : undefined },
            { value: "pending", label: "A vencer", count: data ? count("pending") : undefined },
            { value: "late", label: "Em atraso", count: data ? count("late") : undefined },
          ]}
        />
      </div>

      {isLoading ? (
        <Loading />
      ) : error ? (
        <ErrorState message={errorMessage(error)} onRetry={refetch} />
      ) : (
        <Card>
          {shown.length === 0 ? (
            <EmptyState icon={ReceiptText} title="Nenhuma fatura" description="Não há faturas com essa situação neste mês." />
          ) : (
            <>
              <TableHead className="grid-cols-[70px_1.6fr_110px_90px_130px_120px_170px] gap-4">
                <span>Vence</span>
                <span>Conta</span>
                <span>Fatura</span>
                <span>Forma</span>
                <span>Status</span>
                <span className="text-right">Valor</span>
                <span />
              </TableHead>
              <ul>
                {shown.map((item) => {
                  const i: Invoice = item.invoice;
                  return (
                    <li key={i.id} className="grid grid-cols-[1fr_auto] items-center gap-x-4 gap-y-2 border-b-2 border-navy px-4 py-4 last:border-b-0 sm:px-6 md:grid-cols-[70px_1.6fr_110px_90px_130px_120px_170px] md:gap-4">
                      <span className="hidden font-mono text-sm md:block">{shortDate(i.due_date)}</span>
                      <Link to={`/admin/contas/${item.account_id}`} className="min-w-0 font-bold hover:underline">
                        {item.account_name}
                      </Link>
                      <span className="hidden font-mono text-xs text-muted md:block">{i.number}</span>
                      <span className="hidden text-sm text-muted md:block">{i.method ? methodLabel[i.method] : "—"}</span>
                      <span className="hidden md:block">
                        <StatusPill kind="invoice" status={i.display_status} />
                      </span>
                      <span className="tabular text-right text-lg font-extrabold">{money(i.amount_cents)}</span>
                      <span className="flex flex-wrap items-center gap-2 md:hidden">
                        <StatusPill kind="invoice" status={i.display_status} />
                        <span className="font-mono text-xs text-muted">
                          {i.number} · vence {shortDate(i.due_date)}
                        </span>
                      </span>
                      <span className="text-right">
                        {i.status === "pending" && (
                          <Button size="sm" onClick={() => setMarking(item)}>
                            Marcar como pago
                          </Button>
                        )}
                      </span>
                    </li>
                  );
                })}
              </ul>
            </>
          )}
          <TotalBar
            label={`Total · ${plural(valid.length, "fatura", "faturas")}`}
            sub={`Pagas ${money(sum("paid"))} · A vencer ${money(sum("pending"))} · Em atraso ${money(sum("late"))}`}
            value={money(sum("all"))}
          />
        </Card>
      )}

      <MarkPaidModal
        invoice={marking?.invoice ?? null}
        accountName={marking?.account_name}
        onClose={() => setMarking(null)}
        onDone={() => qc.invalidateQueries({ queryKey: ["admin"] })}
      />
    </>
  );
}
