import { Pencil, Plus, ReceiptText, Users } from "lucide-react";
import { Link, useParams } from "react-router";
import { Card, EmptyState, ErrorState, GroupAvatar, LinkButton, Loading, PageHeader, Pill, StatusPill, TableHead, TotalBar } from "../../components/ui";
import { errorMessage } from "../../lib/api";
import { isReadOnly, useMe } from "../../lib/auth";
import { currentMonth, date, money, monthShort, plural } from "../../lib/format";
import { useCharges, useGroup } from "../../lib/queries";
import { useHere, useOrigin } from "../../lib/origin";

export default function GroupDetail() {
  const { id = "" } = useParams();
  const { data: me } = useMe();
  const readOnly = isReadOnly(me);
  const month = currentMonth();
  const { data: group, isLoading, error, refetch } = useGroup(id);
  const { data: charges } = useCharges({ month, group_id: id });
  const origin = useOrigin({ to: "/app", label: "Grupos" });
  const here = useHere(group?.name ?? "Grupo");

  if (isLoading) return <Loading />;
  if (error || !group) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  const active = group.clients.filter((c) => c.status !== "archived");
  const valid = charges?.filter((c) => c.status !== "cancelled") ?? [];
  const monthTotal = valid.reduce((s, c) => s + c.total_cents, 0);
  const late = valid.filter((c) => c.display_status === "late");
  const lateIds = new Set(late.map((c) => c.client_id));
  const lateTotal = late.reduce((s, c) => s + c.total_cents, 0);
  const standalone = valid.filter((c) => !c.client_id);
  const subscriptionTotal = active.filter((c) => c.status === "active").reduce((s, c) => s + c.amount_cents, 0);
  const payingCount = active.filter((c) => c.status === "active").length;
  const ticket = payingCount > 0 ? Math.round(subscriptionTotal / payingCount) : 0;

  return (
    <>
      <PageHeader
        back={origin.back}
        title={
          <span className="flex items-center gap-4">
            <span className="hidden sm:inline-flex">
              <GroupAvatar name={group.name} color={group.color} size="lg" />
            </span>
            {group.name}
          </span>
        }
        subtitle={group.notes}
        actions={
          <>
            <LinkButton to={`/app/grupos/${id}/editar`} state={here} icon={Pencil} disabled={readOnly} className="flex-1 sm:flex-none">
              Editar grupo
            </LinkButton>
            <LinkButton to={`/app/clientes/novo?grupo=${id}`} state={here} variant="primary" icon={Plus} disabled={readOnly} className="flex-1 sm:flex-none">
              Adicionar cliente
            </LinkButton>
          </>
        }
      />

      <Card dark className="mb-6 flex flex-col gap-6 px-5 py-6 sm:px-8 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="font-mono text-[11px] font-bold tracking-[0.14em] text-amber uppercase">[Total do grupo · {monthShort(month)}]</p>
          <p className="tabular mt-1 text-[40px] leading-none font-extrabold tracking-tight text-amber sm:text-[52px]">{money(monthTotal)}</p>
        </div>
        <dl className="grid grid-cols-3 gap-4 sm:gap-12">
          {[
            ["Clientes", String(active.length)],
            ["Ticket médio", money(ticket)],
            ["Em atraso", money(lateTotal)],
          ].map(([k, v]) => (
            <div key={k}>
              <dt className="font-mono text-[10px] tracking-[0.14em] text-white/70 uppercase sm:text-[11px]">{k}</dt>
              <dd className="tabular mt-1 text-lg font-extrabold whitespace-nowrap sm:text-[26px]">{v}</dd>
            </div>
          ))}
        </dl>
      </Card>

      <Card>
        {active.length === 0 ? (
          <EmptyState
            icon={Users}
            title="Nenhum cliente neste grupo"
            description="Adicione clientes para ver quanto este grupo rende por mês."
            action={
              <LinkButton to={`/app/clientes/novo?grupo=${id}`} state={here} variant="primary" icon={Plus} disabled={readOnly}>
                Adicionar cliente
              </LinkButton>
            }
          />
        ) : (
          <>
            <TableHead className="grid-cols-[1.4fr_1.4fr_90px_130px_140px] gap-6">
              <span>Cliente</span>
              <span>Contato</span>
              <span>Vence</span>
              <span>Status</span>
              <span className="text-right">Assinatura</span>
            </TableHead>
            <ul>
              {active.map((c) => (
                <li key={c.id} className="border-b-2 border-navy last:border-b-0">
                  <Link
                    to={`/app/clientes/${c.id}`}
                    state={here}
                    className="grid grid-cols-[1fr_auto] items-center gap-x-4 gap-y-1.5 px-4 py-4 hover:bg-paper-2 sm:px-6 md:grid-cols-[1.4fr_1.4fr_90px_130px_140px] md:gap-6"
                  >
                    <span className="min-w-0 text-[17px] font-bold">{c.name}</span>
                    <span className="hidden min-w-0 truncate text-sm text-muted md:block">{c.email ?? "—"}</span>
                    <span className="hidden font-mono text-sm md:block">dia {c.due_day}</span>
                    <span className="hidden md:block">
                      <StatusPill kind="client" status={lateIds.has(c.id) ? "late" : c.status} />
                    </span>
                    <span className="tabular row-span-2 text-right text-lg font-extrabold md:row-span-1">{money(c.amount_cents)}</span>
                    <span className="flex flex-wrap items-center gap-2 md:hidden">
                      <StatusPill kind="client" status={lateIds.has(c.id) ? "late" : c.status} />
                      <span className="font-mono text-xs text-muted">dia {c.due_day}</span>
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </>
        )}
        <TotalBar label={`Total · ${plural(active.length, "cliente", "clientes")}`} sub={payingCount !== active.length ? `${plural(payingCount, "cliente ativo", "clientes ativos")}; pausados não entram na soma` : undefined} value={money(subscriptionTotal)} suffix="/mês" />
      </Card>

      <Card className="mt-7">
        <div className="flex flex-col gap-3 border-b-2 border-navy px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <div>
            <h2 className="text-xl font-bold">Cobranças avulsas · {monthShort(month)}</h2>
            <p className="text-sm text-muted">Serviços únicos sem cliente cadastrado neste grupo.</p>
          </div>
          <LinkButton to={`/app/cobrancas/nova?grupo=${id}`} state={here} size="sm" icon={Plus} disabled={readOnly} className="w-full sm:w-auto">
            Cobrança avulsa
          </LinkButton>
        </div>
        {standalone.length === 0 ? (
          <div className="flex flex-col items-center gap-2 px-6 py-10 text-center text-muted">
            <ReceiptText className="size-6" />
            <p className="text-sm">Nenhuma cobrança avulsa neste mês.</p>
          </div>
        ) : (
          <ul>
            {standalone.map((c) => (
              <li key={c.id} className="border-b-2 border-navy last:border-b-0">
                <Link to={`/app/cobrancas/${c.id}`} state={here} className="grid grid-cols-[1fr_auto] items-center gap-x-4 gap-y-1.5 px-4 py-4 hover:bg-paper-2 sm:px-6">
                  <span className="min-w-0">
                    <span className="block truncate text-[17px] font-bold">{c.client_name}</span>
                    <span className="mt-1 flex min-w-0 items-center gap-1.5">
                      <Pill tone="blue">Avulsa</Pill>
                      <span className="truncate text-sm text-muted">{c.description}</span>
                    </span>
                  </span>
                  <span className="tabular text-right text-lg font-extrabold">{money(c.total_cents)}</span>
                  <span className="flex flex-wrap items-center gap-2">
                    <StatusPill kind="charge" status={c.display_status} />
                    <span className="font-mono text-xs text-muted">vence {date(c.due_date)}</span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </>
  );
}
