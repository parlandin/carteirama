import { Plus, Search, Users } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router";
import { Card, EmptyState, ErrorState, FilterChips, GroupChip, LinkButton, Loading, PageHeader, StatusPill, TableHead, TotalBar } from "../../components/ui";
import { errorMessage } from "../../lib/api";
import { isReadOnly, useMe } from "../../lib/auth";
import { currentMonth, formatDocument, formatPhone, money, plural } from "../../lib/format";
import { useCharges, useClients, useGroups } from "../../lib/queries";
import { useHere } from "../../lib/origin";

const ARCHIVED = "__archived";

export default function Clients() {
  const { data: me } = useMe();
  const readOnly = isReadOnly(me);
  const [params, setParams] = useSearchParams();
  const here = useHere("Clientes");
  const filter = params.get("grupo") ?? "all";
  const [search, setSearch] = useState(params.get("q") ?? "");
  const [q, setQ] = useState(search);

  useEffect(() => {
    const t = setTimeout(() => setQ(search.trim()), 300);
    return () => clearTimeout(t);
  }, [search]);

  const archived = filter === ARCHIVED;
  const { data: clients, isLoading, isFetching, error, refetch } = useClients({ q: q || undefined, status: archived ? "archived" : undefined });
  const { data: groups } = useGroups(currentMonth());
  const { data: late } = useCharges({ month: currentMonth(), status: "late" });
  const lateIds = useMemo(() => new Set(late?.map((c) => c.client_id)), [late]);

  const shown = useMemo(() => {
    if (!clients) return [];
    if (filter === "all" || archived) return clients;
    return clients.filter((c) => c.group_id === filter);
  }, [clients, filter, archived]);

  const counts = useMemo(() => {
    const m = new Map<string, number>();
    for (const c of archived ? [] : (clients ?? [])) m.set(c.group_id, (m.get(c.group_id) ?? 0) + 1);
    return m;
  }, [clients, archived]);

  const total = shown.filter((c) => c.status === "active").reduce((s, c) => s + c.amount_cents, 0);

  const setFilter = (v: string) =>
    setParams(
      (p) => {
        const n = new URLSearchParams(p);
        if (v === "all") n.delete("grupo");
        else n.set("grupo", v);
        return n;
      },
      { replace: true },
    );

  return (
    <>
      <PageHeader
        eyebrow="// Cadastro · todos os grupos"
        title="Clientes"
        subtitle="Todos os clientes cadastrados e quanto cada um paga por mês."
        actions={
          <LinkButton to="/app/clientes/novo" state={here} variant="primary" icon={Plus} disabled={readOnly} className="w-full sm:w-auto">
            Novo cliente
          </LinkButton>
        }
      />

      <div className="mb-4 max-w-xl">
        <label className="relative block">
          <span className="sr-only">Buscar clientes</span>
          <Search className="pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2 text-muted" />
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar por nome, CPF/CNPJ ou e-mail"
            className="h-13 w-full rounded-xl border-2 border-navy bg-white pr-4 pl-12 text-[15px] placeholder:text-[#8792a8] focus:ring-3 focus:ring-blue/30 focus:outline-none"
          />
        </label>
      </div>

      <div className="mb-6">
        <FilterChips
          value={filter}
          onChange={setFilter}
          options={[
            { value: "all", label: "Todos", count: archived ? undefined : clients?.length },
            ...(groups ?? []).map((g) => ({ value: g.id, label: g.name, count: counts.get(g.id) ?? 0 })),
            { value: ARCHIVED, label: "Arquivados" },
          ]}
        />
      </div>

      {isLoading ? (
        <Loading />
      ) : error ? (
        <ErrorState message={errorMessage(error)} onRetry={refetch} />
      ) : (
        <Card className={isFetching ? "opacity-80 transition-opacity" : undefined}>
          {shown.length === 0 ? (
            <EmptyState
              icon={Users}
              title={q ? "Nenhum cliente encontrado" : archived ? "Nenhum cliente arquivado" : "Nenhum cliente ainda"}
              description={q ? "Tente buscar por outro nome, documento ou e-mail." : archived ? undefined : "Cadastre os clientes e escolha o grupo de cada um."}
              action={
                !q &&
                !archived && (
                  <LinkButton to="/app/clientes/novo" state={here} variant="primary" icon={Plus} disabled={readOnly}>
                    Cadastrar cliente
                  </LinkButton>
                )
              }
            />
          ) : (
            <>
              <TableHead className="grid-cols-[1.5fr_1.1fr_1.4fr_70px_120px_120px] gap-5">
                <span>Cliente</span>
                <span>Grupo</span>
                <span>Contato</span>
                <span>Vence</span>
                <span>Status</span>
                <span className="text-right">Valor</span>
              </TableHead>
              <ul>
                {shown.map((c) => (
                  <li key={c.id} className="border-b-2 border-navy last:border-b-0">
                    <Link
                      to={`/app/clientes/${c.id}`}
                      state={here}
                      className="grid grid-cols-[1fr_auto] items-start gap-x-4 gap-y-2 px-4 py-4 hover:bg-paper-2 sm:px-6 md:grid-cols-[1.5fr_1.1fr_1.4fr_70px_120px_120px] md:items-center md:gap-5"
                    >
                      <div className="min-w-0">
                        <p className="text-base font-bold">{c.name}</p>
                        {c.document && <p className="font-mono text-xs text-muted">{formatDocument(c.document)}</p>}
                      </div>
                      <div className="hidden min-w-0 md:block">
                        <GroupChip name={c.group_name} color={c.group_color} />
                      </div>
                      <div className="hidden min-w-0 text-sm text-muted md:block">
                        <p className="truncate">{c.email ?? "—"}</p>
                        {c.phone && <p>{formatPhone(c.phone)}</p>}
                      </div>
                      <span className="hidden font-mono text-sm md:block">dia {c.due_day}</span>
                      <span className="hidden md:block">
                        <StatusPill kind="client" status={lateIds.has(c.id) && c.status === "active" ? "late" : c.status} />
                      </span>
                      <span className="tabular text-right text-base font-extrabold sm:text-lg">{money(c.amount_cents)}</span>
                      <div className="col-span-2 flex flex-wrap items-center gap-2 md:hidden">
                        <GroupChip name={c.group_name} color={c.group_color} />
                        <StatusPill kind="client" status={lateIds.has(c.id) && c.status === "active" ? "late" : c.status} />
                        <span className="font-mono text-xs text-muted">dia {c.due_day}</span>
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            </>
          )}
          {!archived && (
            <TotalBar label={`Total · ${plural(shown.length, "cliente", "clientes")}`} sub={shown.some((c) => c.status === "paused") ? "Clientes pausados não entram na soma" : undefined} value={money(total)} suffix="/mês" />
          )}
        </Card>
      )}
    </>
  );
}
