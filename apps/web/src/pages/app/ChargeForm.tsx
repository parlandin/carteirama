import { Check } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router";
import { Button, Card, ErrorState, Field, FilterChips, FormError, FormSection, Input, Loading, PageHeader, Select, Textarea } from "../../components/ui";
import { api, errorMessage } from "../../lib/api";
import { isReadOnly, useMe } from "../../lib/auth";
import { centsToInput, currentMonth, isValidMonth, maskMoneyInput, parseMoney, today } from "../../lib/format";
import { useCharge, useClients, useGroups, useInvalidateData } from "../../lib/queries";
import { useToast } from "../../lib/toast";
import type { Charge } from "../../lib/types";
import { useOrigin } from "../../lib/origin";
import { useForm } from "../../lib/useForm";

type Payer = "client" | "name";

export default function ChargeFormPage() {
  const { id } = useParams();
  if (id) return <EditCharge id={id} />;
  return <ChargeForm />;
}

function EditCharge({ id }: { id: string }) {
  const { data, isLoading, error, refetch } = useCharge(id);
  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;
  return <ChargeForm initial={data} />;
}

function defaultDueDate(month: string | null): string {
  return !isValidMonth(month) || month === currentMonth() ? today() : `${month}-01`;
}

function ChargeForm({ initial }: { initial?: Charge }) {
  const navigate = useNavigate();
  const toast = useToast();
  const invalidate = useInvalidateData();
  const [params] = useSearchParams();
  const { data: me } = useMe();
  const readOnly = isReadOnly(me);
  const editing = !!initial;
  const presetClient = params.get("cliente");
  const { data: clients } = useClients({ status: "active" });
  const { data: groups } = useGroups(currentMonth());
  const [busy, setBusy] = useState(false);

  const { values, set, errors, setErrors, formError, setFormError, fail, reset } = useForm({
    payer: (initial ? (initial.client_id ? "client" : "name") : presetClient || !params.get("grupo") ? "client" : "name") as Payer,
    client_id: initial?.client_id ?? presetClient ?? "",
    payer_name: initial && !initial.client_id ? initial.client_name : "",
    group_id: initial ? (initial.client_id ? "" : (initial.group_id ?? "")) : (params.get("grupo") ?? ""),
    description: initial?.description ?? "",
    amount: initial ? centsToInput(initial.amount_cents) : "",
    due_date: initial?.due_date ?? defaultDueDate(params.get("mes")),
    note: initial?.note ?? "",
  });

  useEffect(() => {
    if (readOnly) setFormError("Sua conta está suspensa. Regularize a assinatura para criar ou editar cobranças.");
  }, [readOnly, setFormError]);

  const locked = editing && initial!.status !== "pending";
  const byName = values.payer === "name";
  const fixedClient = editing ? (initial!.client_id ? initial!.client_name : null) : (presetClient && clients?.find((c) => c.id === presetClient)?.name) || null;
  const presetGroup = params.get("grupo");
  const origin = useOrigin(
    editing
      ? { to: `/app/cobrancas/${initial!.id}`, label: initial!.description ?? "Cobrança" }
      : presetClient
        ? { to: `/app/clientes/${presetClient}`, label: fixedClient ?? "Cliente" }
        : presetGroup
          ? { to: `/app/grupos/${presetGroup}`, label: groups?.find((g) => g.id === presetGroup)?.name ?? "Grupo" }
          : { to: "/app/cobrancas", label: "Cobranças" },
  );

  async function submit(e: FormEvent) {
    e.preventDefault();
    reset();
    const amount = parseMoney(values.amount);
    const local: Record<string, string> = {};
    if (!byName && !values.client_id) local.client_id = "Escolha o cliente.";
    if (byName && values.payer_name.trim().length < 2) local.payer_name = "Informe para quem é a cobrança.";
    if (!values.description.trim()) local.description = "Descreva a cobrança.";
    if (amount === null || amount < 1) local.amount_cents = "Informe um valor maior que zero.";
    if (!values.due_date) local.due_date = "Informe o vencimento.";
    if (Object.keys(local).length > 0) {
      setErrors(local);
      setFormError("Confira os campos destacados.");
      return;
    }
    const common = {
      description: values.description.trim(),
      amount_cents: amount,
      due_date: values.due_date,
      note: values.note.trim() || null,
    };
    const standalone = { payer_name: values.payer_name.trim(), group_id: values.group_id || null };
    setBusy(true);
    try {
      const saved = editing
        ? await api.patch<Charge>(`/charges/${initial!.id}`, { ...common, ...(byName ? standalone : {}) })
        : await api.post<Charge>("/charges", { kind: "one_off", ...common, ...(byName ? standalone : { client_id: values.client_id }) });
      await invalidate();
      toast.show(editing ? "Cobrança atualizada." : "Cobrança avulsa criada.");
      if (editing) origin.goBack();
      else navigate(`/app/cobrancas/${saved.id}`, { replace: true, state: origin.carry });
    } catch (err) {
      fail(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <PageHeader
        back={origin.back}
        eyebrow={editing ? "// Editar cobrança" : "// Nova cobrança"}
        title={editing ? "Editar cobrança avulsa" : "Nova cobrança avulsa"}
        subtitle="Vale só para o mês do vencimento e não cria mensalidade. Use para um serviço único, instalação, visita técnica…"
      />

      <form onSubmit={submit} noValidate className="flex max-w-[960px] flex-col gap-6">
        <FormError message={locked ? "Só é possível editar cobranças pendentes." : formError} />

        <FormSection step={1} title="Para quem" description={byName ? "Sem cadastro: não cria cliente. Escolha um grupo para somar no total dele." : "A cobrança entra no grupo do cliente."}>
          {fixedClient ? (
            <div className="rounded-xl border-2 border-navy bg-paper-2 px-4 py-3">
              <p className="font-mono text-[11px] tracking-widest text-muted uppercase">Cliente</p>
              <p className="font-bold">{fixedClient}</p>
            </div>
          ) : (
            <div className="flex flex-col gap-5">
              {!editing && (
                <FilterChips
                  value={values.payer}
                  onChange={(v) => set("payer", v)}
                  options={[
                    { value: "client", label: "Cliente cadastrado" },
                    { value: "name", label: "Sem cadastro" },
                  ]}
                />
              )}
              {byName ? (
                <div className="grid gap-5 md:grid-cols-2">
                  <Field label="Nome" required htmlFor="payer" error={errors.payer_name}>
                    <Input id="payer" maxLength={120} placeholder="Quem vai pagar" value={values.payer_name} onChange={(e) => set("payer_name", e.target.value)} invalid={!!errors.payer_name} />
                  </Field>
                  <Field label="Grupo" htmlFor="group" hint="Opcional. Soma no total do grupo." error={errors.group_id}>
                    <Select id="group" value={values.group_id} onChange={(e) => set("group_id", e.target.value)} invalid={!!errors.group_id}>
                      <option value="">Sem grupo</option>
                      {groups?.map((g) => (
                        <option key={g.id} value={g.id}>
                          {g.name}
                        </option>
                      ))}
                    </Select>
                  </Field>
                </div>
              ) : (
                <Field label="Cliente" required htmlFor="client" error={errors.client_id}>
                  <Select id="client" value={values.client_id} onChange={(e) => set("client_id", e.target.value)} invalid={!!errors.client_id}>
                    <option value="">Escolha um cliente ativo</option>
                    {clients?.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </Select>
                </Field>
              )}
            </div>
          )}
        </FormSection>

        <FormSection step={2} stepColor="#FBAF24" title="Cobrança" description="O que foi feito, quanto e quando vence.">
          <div className="flex flex-col gap-5">
            <Field label="Descrição" required htmlFor="description" error={errors.description}>
              <Input id="description" maxLength={120} placeholder="Ex.: Instalação, visita técnica" value={values.description} onChange={(e) => set("description", e.target.value)} invalid={!!errors.description} />
            </Field>
            <div className="grid gap-5 md:grid-cols-2">
              <Field label="Valor (R$)" required htmlFor="amount" error={errors.amount_cents}>
                <Input id="amount" inputMode="decimal" placeholder="0,00" value={values.amount} onChange={(e) => set("amount", maskMoneyInput(e.target.value))} invalid={!!errors.amount_cents} />
              </Field>
              <Field label="Vencimento" required htmlFor="due" error={errors.due_date}>
                <Input id="due" type="date" value={values.due_date} onChange={(e) => set("due_date", e.target.value)} invalid={!!errors.due_date} />
              </Field>
            </div>
          </div>
        </FormSection>

        <Card className="p-5 sm:p-7">
          <h2 className="mb-4 text-xl font-bold">Nota</h2>
          <Textarea maxLength={500} className="min-h-20" placeholder="Opcional." value={values.note} onChange={(e) => set("note", e.target.value)} invalid={!!errors.note} aria-label="Nota" />
          {errors.note && <p className="mt-1.5 text-[13px] font-medium text-red">{errors.note}</p>}
        </Card>

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button onClick={origin.goBack}>Cancelar</Button>
          <Button type="submit" variant="primary" icon={Check} loading={busy} disabled={readOnly || locked}>
            {editing ? "Salvar cobrança" : "Criar cobrança"}
          </Button>
        </div>
      </form>
    </>
  );
}
