import { Archive, Check, Mail, NotebookPen, Pencil, Plus, ReceiptText, Undo2, UserRound } from "lucide-react";
import { useState, type ReactNode } from "react";
import { Link, useParams } from "react-router";
import { ExtrasCard, recurringTotals } from "../../components/ClientExtras";
import { PayChargeModal } from "../../components/PayChargeModal";
import { Button, Card, ErrorState, LinkButton, Loading, Modal, PageHeader, Pill, StatusPill, GroupChip } from "../../components/ui";
import { api, errorMessage } from "../../lib/api";
import { isReadOnly, useMe } from "../../lib/auth";
import { currentMonth, date, formatDocument, formatPhone, instantDay, monthLabel, money, shortDate } from "../../lib/format";
import { useClient, useInvalidateData } from "../../lib/queries";
import { useHere, useOrigin } from "../../lib/origin";
import { useToast } from "../../lib/toast";
import type { Charge } from "../../lib/types";

function InfoCard({ icon: Icon, color, title, children }: { icon: typeof Mail; color: string; title: string; children: ReactNode }) {
  return (
    <Card className="p-5 sm:p-7">
      <div className="mb-5 flex items-center gap-3">
        <span className="grid size-10 place-items-center rounded-xl border-2 border-navy" style={{ background: color }}>
          <Icon className="size-5" color={color === "#FBAF24" ? "#111b35" : "#fff"} />
        </span>
        <h2 className="text-xl font-bold">{title}</h2>
      </div>
      <dl className="grid gap-5 sm:grid-cols-2">{children}</dl>
    </Card>
  );
}

function Info({ label, value, mono }: { label: string; value: ReactNode; mono?: boolean }) {
  return (
    <div className="min-w-0">
      <dt className="font-mono text-[11px] tracking-[0.14em] text-muted uppercase">{label}</dt>
      <dd className={mono ? "mt-1 font-mono break-words" : "mt-1 text-[16px] break-words"}>{value || "—"}</dd>
    </div>
  );
}

export default function ClientDetail() {
  const { id = "" } = useParams();
  const toast = useToast();
  const invalidate = useInvalidateData();
  const { data: me } = useMe();
  const readOnly = isReadOnly(me);
  const { data, isLoading, error, refetch } = useClient(id);
  const origin = useOrigin({ to: "/app/clientes", label: "Clientes" });
  const here = useHere(data?.client.name ?? "Cliente");
  const [paying, setPaying] = useState<Charge | null>(null);
  const [unpaying, setUnpaying] = useState<string | null>(null);
  const [confirmArchive, setConfirmArchive] = useState(false);
  const [archiving, setArchiving] = useState(false);

  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  const { client, charges, extras } = data;
  const fixed = recurringTotals(extras);
  const archived = client.status === "archived";
  const month = currentMonth();
  const openCharge = charges.find((c) => c.status === "pending" && c.reference_month.startsWith(month)) ?? charges.find((c) => c.display_status === "late");
  const since = instantDay(client.created_at);

  async function unpay(charge: Charge) {
    setUnpaying(charge.id);
    try {
      await api.post(`/charges/${charge.id}/unpay`);
      await invalidate();
      toast.show("Pagamento desfeito.");
    } catch (e) {
      toast.show(errorMessage(e), "error");
    } finally {
      setUnpaying(null);
    }
  }

  async function archive() {
    setArchiving(true);
    try {
      await api.delete(`/clients/${id}`);
      await invalidate();
      toast.show("Cliente arquivado.");
      origin.goBack();
    } catch (e) {
      toast.show(errorMessage(e), "error");
      setConfirmArchive(false);
    } finally {
      setArchiving(false);
    }
  }

  return (
    <>
      <PageHeader
        back={origin.back}
        title={client.name}
        badge={<StatusPill kind="client" status={client.status} />}
        subtitle={
          <span className="font-mono text-sm">
            {client.document && <>{client.document.length > 11 ? "CNPJ" : "CPF"} {formatDocument(client.document)} · </>}cliente desde {since ? monthLabel(since.slice(0, 7)).toLowerCase() : "—"}
          </span>
        }
        actions={
          !archived && (
            <>
              <LinkButton to={`/app/clientes/${id}/editar`} state={here} icon={Pencil} disabled={readOnly} className="flex-1 sm:flex-none">
                Editar cliente
              </LinkButton>
              <Button variant="primary" icon={Check} disabled={readOnly || !openCharge} onClick={() => openCharge && setPaying(openCharge)} className="flex-1 sm:flex-none" title={openCharge ? undefined : "Nenhuma cobrança em aberto"}>
                Registrar pagamento
              </Button>
            </>
          )
        }
      />

      <div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_340px]">
        <div className="flex flex-col gap-7">
          <InfoCard icon={UserRound} color="#2553E6" title="Cliente">
            <Info label="Nome" value={client.name} />
            <Info label={client.document && client.document.length <= 11 ? "CPF" : "CNPJ"} value={client.document ? formatDocument(client.document) : null} mono />
          </InfoCard>
          <InfoCard icon={Mail} color="#FBAF24" title="Como falar">
            <Info label="E-mail" value={client.email && <a href={`mailto:${client.email}`} className="hover:underline">{client.email}</a>} />
            <Info
              label="Telefone / WhatsApp"
              value={
                client.phone && (
                  <a href={`https://wa.me/55${client.phone}`} target="_blank" rel="noreferrer" className="hover:underline">
                    {formatPhone(client.phone)}
                  </a>
                )
              }
            />
          </InfoCard>
          <ExtrasCard clientId={client.id} extras={extras} canEdit={!readOnly && !archived} />
          <Card className="p-5 sm:p-7">
            <div className="mb-3 flex items-center gap-3">
              <NotebookPen className="size-5" />
              <h2 className="text-xl font-bold">Observações</h2>
            </div>
            <p className="whitespace-pre-line text-muted">{client.notes || "Nenhuma observação."}</p>
          </Card>
          {!archived && (
            <div className="flex justify-start">
              <Button variant="ghost" size="sm" icon={Archive} disabled={readOnly} onClick={() => setConfirmArchive(true)} className="text-red">
                Arquivar cliente
              </Button>
            </div>
          )}
        </div>

        <div className="flex flex-col gap-7">
          <Card dark className="p-6">
            <p className="font-mono text-[11px] font-bold tracking-[0.14em] text-amber uppercase">[Assinatura]</p>
            <p className="tabular mt-2 text-[44px] leading-none font-extrabold tracking-tight text-amber">
              {money(client.amount_cents)}
              <span className="ml-1.5 font-mono text-sm font-medium text-white/80">/mês</span>
            </p>
            <dl className="mt-6 divide-y divide-white/20 border-t border-white/20 text-[15px]">
              {[
                ...(fixed.bonus > 0 || fixed.cost > 0
                  ? [
                      ["Bônus todo mês", `+ ${money(fixed.bonus)}`],
                      ["Custos todo mês", `− ${money(fixed.cost)}`],
                      ["Líquido por mês", <span className="text-amber">{money(client.amount_cents + fixed.bonus - fixed.cost)}</span>],
                    ]
                  : []),
                ["Grupo", <Link to={`/app/grupos/${client.group_id}`} state={here} className="hover:underline">{client.group_name}</Link>],
                ["Vencimento", `Todo dia ${client.due_day}`],
                ["Situação", client.status === "active" ? "Ativo" : client.status === "paused" ? "Pausado" : "Arquivado"],
                ["Cliente desde", date(since)],
              ].map(([k, v], i) => (
                <div key={i} className="flex items-center justify-between gap-3 py-3">
                  <dt className="text-white/80">{k}</dt>
                  <dd className="text-right font-bold">{v}</dd>
                </div>
              ))}
            </dl>
          </Card>

          <Card>
            <div className="flex items-center justify-between border-b-2 border-navy px-5 py-5 sm:px-6">
              <h2 className="text-xl font-bold">Últimas cobranças</h2>
              <GroupChip name={client.group_name} color={client.group_color} />
            </div>
            {client.status === "active" && (
              <div className="border-b-2 border-navy px-5 py-3 sm:px-6">
                <LinkButton to={`/app/cobrancas/nova?cliente=${client.id}`} state={here} size="sm" variant="ghost" icon={Plus} disabled={readOnly} className="-ml-2 text-blue">
                  Nova cobrança avulsa
                </LinkButton>
              </div>
            )}
            {charges.length === 0 ? (
              <div className="flex flex-col items-center gap-2 px-6 py-10 text-center text-muted">
                <ReceiptText className="size-6" />
                <p className="text-sm">As cobranças aparecem aqui a partir do mês do cadastro.</p>
              </div>
            ) : (
              <ul>
                {charges.slice(0, 12).map((c) => (
                  <li key={c.id} className="flex items-center justify-between gap-3 border-b-2 border-navy px-5 py-3.5 last:border-b-0 sm:px-6">
                    <div className="min-w-0">
                      {c.kind === "one_off" ? (
                        <>
                          <Link to={`/app/cobrancas/${c.id}`} state={here} className="block truncate font-bold hover:underline">
                            {c.description}
                          </Link>
                          <p className="mt-0.5 flex items-center gap-1.5 text-xs text-muted">
                            <Pill tone="blue" className="py-0.5">Avulsa</Pill>
                            {monthLabel(c.reference_month.slice(0, 7))}
                          </p>
                        </>
                      ) : (
                        <p className="font-bold">{monthLabel(c.reference_month.slice(0, 7))}</p>
                      )}
                      <p className="font-mono text-xs text-muted">
                        {c.status === "paid" ? `pago em ${shortDate(c.paid_at)}` : `vence ${shortDate(c.due_date)}`} · {money(c.total_cents)}
                        {c.cost_cents > 0 && <> · líq. {money(c.net_cents)}</>}
                      </p>
                    </div>
                    <div className="flex shrink-0 flex-col items-end gap-1.5">
                      <StatusPill kind="charge" status={c.display_status} />
                      {!readOnly && (c.status === "pending" ? (
                        <button type="button" className="text-xs font-bold text-blue hover:underline" onClick={() => setPaying(c)}>
                          Marcar pago
                        </button>
                      ) : c.status === "paid" ? (
                        <button type="button" disabled={unpaying === c.id} className="inline-flex items-center gap-1 text-xs font-semibold text-muted hover:text-ink" onClick={() => unpay(c)}>
                          <Undo2 className="size-3" /> Desfazer
                        </button>
                      ) : null)}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>
      </div>

      <PayChargeModal charge={paying} onClose={() => setPaying(null)} />
      <Modal
        open={confirmArchive}
        onClose={() => setConfirmArchive(false)}
        title="Arquivar cliente?"
        footer={
          <>
            <Button onClick={() => setConfirmArchive(false)}>Cancelar</Button>
            <Button variant="danger" loading={archiving} onClick={archive}>
              Arquivar cliente
            </Button>
          </>
        }
      >
        <p>
          <strong>{client.name}</strong> sai da lista e do total do grupo, e as cobranças em aberto são canceladas. O histórico de pagamentos continua guardado.
        </p>
      </Modal>
    </>
  );
}
