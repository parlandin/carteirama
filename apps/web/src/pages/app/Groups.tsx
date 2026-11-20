import { ChevronRight, Layers, Lock, Plus } from "lucide-react";
import { Link } from "react-router";
import { Card, EmptyState, ErrorState, GroupAvatar, LinkButton, Loading, MonthPicker, PageHeader, TableHead, TotalBar } from "../../components/ui";
import { isReadOnly, useMe } from "../../lib/auth";
import { groupColor } from "../../lib/colors";
import { errorMessage } from "../../lib/api";
import { money, monthShort, plural } from "../../lib/format";
import { useAutoGenerateCharges, useGroups } from "../../lib/queries";
import { useHere } from "../../lib/origin";
import { useMonthParam } from "../../lib/useMonth";

function Share({ pct, color }: { pct: number; color: string | null }) {
  return (
    <div className="w-full max-w-[110px]">
      <div className="h-3 overflow-hidden rounded-full border-2 border-navy bg-white">
        <div className="h-full" style={{ width: `${Math.max(pct, pct > 0 ? 3 : 0)}%`, background: groupColor(color) }} />
      </div>
      <p className="mt-1 font-mono text-[11px] text-muted">{pct}%</p>
    </div>
  );
}

export default function Groups() {
  const { data: me } = useMe();
  const readOnly = isReadOnly(me);
  const [month, shift] = useMonthParam();
  const here = useHere("Grupos");
  useAutoGenerateCharges(!readOnly);
  const { data: groups, isLoading, error, refetch } = useGroups(month);

  const total = groups?.reduce((s, g) => s + g.month_total_cents, 0) ?? 0;
  const clients = groups?.reduce((s, g) => s + g.client_count, 0) ?? 0;

  return (
    <>
      <PageHeader
        eyebrow="// Recebimentos · por grupo"
        title="Grupos de clientes"
        badge={
          readOnly && (
            <span className="inline-flex items-center gap-2 rounded-full bg-navy px-4 py-2 font-mono text-xs font-bold tracking-[0.14em] text-white uppercase shadow-[3px_3px_0_0_var(--color-amber)]">
              <Lock className="size-3.5 text-amber" /> Conta suspensa
            </span>
          )
        }
        subtitle="Quanto cada grupo rende por mês e o total que entra no caixa."
        actions={
          <>
            <MonthPicker month={month} onChange={shift} className="w-full sm:w-auto" />
            <LinkButton to="/app/grupos/novo" state={here} disabled={readOnly} className="flex-1 sm:flex-none">
              Novo grupo
            </LinkButton>
            <LinkButton to="/app/clientes/novo" state={here} variant="primary" icon={Plus} disabled={readOnly} className="flex-1 sm:flex-none">
              Novo cliente
            </LinkButton>
          </>
        }
      />

      {isLoading ? (
        <Loading />
      ) : error ? (
        <ErrorState message={errorMessage(error)} onRetry={refetch} />
      ) : !groups || groups.length === 0 ? (
        <Card>
          <EmptyState
            icon={Layers}
            title="Nenhum grupo ainda"
            description="Crie grupos como “Assinatura SaaS” ou “Hospedagem” e depois cadastre os clientes de cada um."
            action={
              <LinkButton to="/app/grupos/novo" state={here} variant="primary" icon={Plus} disabled={readOnly}>
                Criar primeiro grupo
              </LinkButton>
            }
          />
        </Card>
      ) : (
        <Card>
          <TableHead className="grid-cols-[1fr_110px_150px_170px_24px] gap-6">
            <span>Grupo</span>
            <span className="text-right">Clientes</span>
            <span>Participação</span>
            <span className="text-right">Valor / mês</span>
            <span />
          </TableHead>
          <ul>
            {groups.map((g) => {
              const pct = total > 0 ? Math.round((g.month_total_cents / total) * 100) : 0;
              return (
                <li key={g.id} className="border-b-2 border-navy">
                  <Link
                    to={`/app/grupos/${g.id}`}
                    state={here}
                    className="grid grid-cols-[auto_1fr_auto] items-center gap-x-4 gap-y-2 px-4 py-4 transition hover:bg-paper-2 sm:px-6 md:grid-cols-[1fr_110px_150px_170px_24px] md:gap-6 md:py-5"
                  >
                    <div className="contents md:flex md:min-w-0 md:items-center md:gap-4">
                      <GroupAvatar name={g.name} color={g.color} />
                      <div className="min-w-0">
                        <p className="truncate text-lg font-bold">{g.name}</p>
                        {g.notes && <p className="truncate text-sm text-muted">{g.notes}</p>}
                        <p className="font-mono text-xs text-muted md:hidden">{plural(g.client_count, "cliente", "clientes")} · {pct}%</p>
                      </div>
                    </div>
                    <span className="hidden text-right font-mono text-sm text-muted md:block">{plural(g.client_count, "cliente", "clientes")}</span>
                    <div className="hidden md:block">
                      <Share pct={pct} color={g.color} />
                    </div>
                    <span className="tabular text-right text-lg font-extrabold whitespace-nowrap sm:text-[22px]">{money(g.month_total_cents)}</span>
                    <ChevronRight className="hidden size-5 md:block" strokeWidth={2.5} />
                  </Link>
                </li>
              );
            })}
          </ul>
          {!readOnly && (
            <Link to="/app/grupos/novo" state={here} className="flex items-center justify-center gap-2 border-t-2 border-dashed border-navy bg-paper-2 py-4 font-semibold text-blue hover:bg-blue-soft" style={{ marginTop: -2 }}>
              <Plus className="size-4" strokeWidth={2.5} /> Criar novo grupo
            </Link>
          )}
          <TotalBar
            label={`Total a receber · ${monthShort(month)}`}
            sub={`Soma de ${plural(clients, "cliente", "clientes")} em ${plural(groups.length, "grupo", "grupos")}`}
            value={money(total)}
          />
        </Card>
      )}
    </>
  );
}
