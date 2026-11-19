import { Building2, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router";
import { Card, EmptyState, ErrorState, FilterChips, Loading, PageHeader, StatusPill, TableHead, TotalBar } from "../../components/ui";
import { errorMessage } from "../../lib/api";
import { date, formatDocument, instantDate, instantDay, money, plural } from "../../lib/format";
import { useAdminAccounts, useAdminSummary } from "../../lib/queries";
import type { AdminAccountFilter, AdminAccountItem } from "../../lib/types";

function nextEvent(a: AdminAccountItem) {
  switch (a.display_status) {
    case "trial":
      return `Teste termina em ${instantDate(a.trial_ends_at)}`;
    case "suspended":
      return `Suspensa em ${instantDate(a.suspended_at)}`;
    case "cancelled":
      return `Cancelada em ${instantDate(a.cancelled_at)}`;
    case "late":
      return "Fatura vencida";
    default:
      return `Conta criada em ${date(instantDay(a.created_at))}`;
  }
}

type Filter = "all" | AdminAccountFilter;

export default function AdminAccounts() {
  const [params, setParams] = useSearchParams();
  const raw = params.get("situacao");
  const filter: Filter = raw && ["trial", "active", "late", "suspended", "cancelled"].includes(raw) ? (raw as Filter) : "all";
  const [search, setSearch] = useState("");
  const [q, setQ] = useState("");
  useEffect(() => {
    const t = setTimeout(() => setQ(search.trim()), 300);
    return () => clearTimeout(t);
  }, [search]);

  const { data: summary } = useAdminSummary();
  const { data: accounts, isLoading, error, refetch, isFetching } = useAdminAccounts({ status: filter === "all" ? undefined : filter, q: q || undefined });
  const by = summary?.accounts_by_status ?? {};
  const total = (by.trial ?? 0) + (by.active ?? 0) + (by.suspended ?? 0) + (by.cancelled ?? 0);

  const setFilter = (v: Filter) =>
    setParams(
      (p) => {
        const n = new URLSearchParams(p);
        if (v === "all") n.delete("situacao");
        else n.set("situacao", v);
        return n;
      },
      { replace: true },
    );

  return (
    <>
      <PageHeader eyebrow="// Admin · contas" title="Contas" subtitle="Todas as contas que usam a Carteirama e a situação de cada uma." />

      <div className="mb-4 max-w-xl">
        <label className="relative block">
          <span className="sr-only">Buscar contas</span>
          <Search className="pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2 text-muted" />
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar por empresa, responsável ou e-mail"
            className="h-13 w-full rounded-xl border-2 border-navy bg-white pr-4 pl-12 text-[15px] placeholder:text-[#8792a8] focus:ring-3 focus:ring-blue/30 focus:outline-none"
          />
        </label>
      </div>
      <div className="mb-6">
        <FilterChips<Filter>
          value={filter}
          onChange={setFilter}
          options={[
            { value: "all", label: "Todas", count: summary ? total : undefined },
            { value: "active", label: "Ativas", count: by.active },
            { value: "trial", label: "Em teste", count: by.trial },
            { value: "late", label: "Em atraso", count: by.late },
            { value: "suspended", label: "Suspensas", count: by.suspended },
            { value: "cancelled", label: "Canceladas", count: by.cancelled },
          ]}
        />
      </div>

      {isLoading ? (
        <Loading />
      ) : error ? (
        <ErrorState message={errorMessage(error)} onRetry={refetch} />
      ) : (
        <Card className={isFetching ? "opacity-80" : undefined}>
          {!accounts || accounts.length === 0 ? (
            <EmptyState icon={Building2} title="Nenhuma conta encontrada" />
          ) : (
            <>
              <TableHead className="grid-cols-[1.6fr_170px_130px_1.1fr] gap-5">
                <span>Conta</span>
                <span>Documento</span>
                <span>Situação</span>
                <span>Próximo evento</span>
              </TableHead>
              <ul>
                {accounts.map((a) => (
                  <li key={a.id} className="border-b-2 border-navy last:border-b-0">
                    <Link to={`/admin/contas/${a.id}`} className="grid grid-cols-[1fr_auto] items-start gap-x-4 gap-y-2 px-4 py-4 hover:bg-paper-2 sm:px-6 md:grid-cols-[1.6fr_170px_130px_1.1fr] md:items-center md:gap-5">
                      <div className="min-w-0">
                        <p className="font-bold">{a.company_name || a.legal_name}</p>
                        <p className="truncate font-mono text-xs text-muted">
                          {a.user_name} · {a.email}
                        </p>
                      </div>
                      <span className="hidden font-mono text-sm md:block">{formatDocument(a.document)}</span>
                      <span>
                        <StatusPill kind="account" status={a.display_status} />
                      </span>
                      <span className="col-span-2 text-sm text-muted md:col-span-1">{nextEvent(a)}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </>
          )}
          {summary && <TotalBar label={`MRR · ${plural(total, "conta", "contas")}`} value={money(summary.mrr_cents)} suffix="/mês" />}
        </Card>
      )}
    </>
  );
}
