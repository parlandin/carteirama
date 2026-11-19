import { Check, Layers, Plus, Trash2 } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router";
import { emptyExtra, ExtraFields, toExtraBody, type ExtraDraft, type ExtraErrors } from "../../components/ClientExtras";
import { Button, Card, EmptyState, ErrorState, Field, FormError, FormSection, Input, LinkButton, Loading, PageHeader, Select, Textarea } from "../../components/ui";
import { api, errorMessage } from "../../lib/api";
import { isReadOnly, useMe } from "../../lib/auth";
import { cx } from "../../lib/cx";
import { centsToInput, currentMonth, digits, formatPhone, maskDocument, maskMoneyInput, maskPhone, parseMoney, formatDocument } from "../../lib/format";
import { resetAutoGenerate, useClient, useGroups, useInvalidateData } from "../../lib/queries";
import { useToast } from "../../lib/toast";
import type { Client, ClientStatus } from "../../lib/types";
import { useForm } from "../../lib/useForm";

export default function ClientFormPage() {
  const { id } = useParams();
  if (id) return <EditClient id={id} />;
  return <ClientForm />;
}

function EditClient({ id }: { id: string }) {
  const { data, isLoading, error, refetch } = useClient(id);
  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;
  return <ClientForm initial={data.client} />;
}

function ClientForm({ initial }: { initial?: Client }) {
  const navigate = useNavigate();
  const toast = useToast();
  const invalidate = useInvalidateData();
  const [params] = useSearchParams();
  const { data: me } = useMe();
  const readOnly = isReadOnly(me);
  const { data: groups, isLoading: loadingGroups } = useGroups(currentMonth());
  const editing = !!initial;
  const [busy, setBusy] = useState(false);
  const [extras, setExtras] = useState<ExtraDraft[]>([]);
  const [extraErrors, setExtraErrors] = useState<ExtraErrors[]>([]);

  const { values, set, errors, setErrors, formError, setFormError, fail, reset } = useForm({
    name: initial?.name ?? "",
    document: initial?.document ? formatDocument(initial.document) : "",
    email: initial?.email ?? "",
    phone: initial?.phone ? formatPhone(initial.phone) : "",
    group_id: initial?.group_id ?? params.get("grupo") ?? "",
    amount: initial ? centsToInput(initial.amount_cents) : "",
    due_day: initial ? String(initial.due_day) : "",
    status: (initial?.status ?? "active") as ClientStatus,
    notes: initial?.notes ?? "",
  });

  useEffect(() => {
    if (readOnly) setFormError("Sua conta está suspensa. Regularize a assinatura para cadastrar ou editar clientes.");
  }, [readOnly, setFormError]);

  const archived = initial?.status === "archived";

  async function submit(e: FormEvent) {
    e.preventDefault();
    reset();
    const local: Record<string, string> = {};
    const amount = parseMoney(values.amount);
    const day = Number(values.due_day);
    if (!values.name.trim()) local.name = "Campo obrigatório.";
    if (!values.group_id) local.group_id = "Escolha um grupo.";
    if (amount === null || amount < 1) local.amount_cents = "Informe um valor maior que zero.";
    if (!Number.isInteger(day) || day < 1 || day > 28) local.due_day = "Escolha um dia de 1 a 28.";
    const extraResults = editing ? [] : extras.map(toExtraBody);
    const extraErrs = extraResults.map((r) => ("errors" in r ? r.errors : {}));
    setExtraErrors(extraErrs);
    const extrasInvalid = extraErrs.some((x) => Object.keys(x).length > 0);
    if (Object.keys(local).length || extrasInvalid) {
      setErrors(local);
      setFormError("Confira os campos destacados.");
      return;
    }
    const body = {
      name: values.name.trim(),
      group_id: values.group_id,
      document: digits(values.document) || null,
      email: values.email.trim() || null,
      phone: digits(values.phone) || null,
      amount_cents: amount,
      due_day: day,
      notes: values.notes.trim() || null,
      ...(editing ? { status: values.status } : {}),
    };
    setBusy(true);
    try {
      const saved = editing ? await api.patch<Client>(`/clients/${initial!.id}`, body) : await api.post<Client>("/clients", body);
      let extrasFailed = 0;
      for (const r of extraResults) {
        if (!("body" in r)) continue;
        try {
          await api.post(`/clients/${saved.id}/extras`, r.body);
        } catch {
          extrasFailed++;
        }
      }
      resetAutoGenerate();
      await invalidate();
      if (extrasFailed > 0) {
        toast.show(`Cliente cadastrado, mas ${extrasFailed === 1 ? "um bônus ou custo não foi salvo" : `${extrasFailed} bônus ou custos não foram salvos`}. Adicione de novo no detalhe do cliente.`, "error");
      } else {
        toast.show(editing ? "Cliente atualizado." : "Cliente cadastrado.");
      }
      navigate(`/app/clientes/${saved.id}`, { replace: true });
    } catch (err) {
      fail(err);
    } finally {
      setBusy(false);
    }
  }

  if (loadingGroups) return <Loading />;

  if (!editing && groups && groups.length === 0) {
    return (
      <>
        <PageHeader back={{ to: "/app/clientes", label: "Clientes" }} eyebrow="// Novo cadastro" title="Cadastrar cliente" />
        <Card>
          <EmptyState
            icon={Layers}
            title="Crie um grupo primeiro"
            description="Todo cliente fica dentro de um grupo, como “Assinatura SaaS” ou “Hospedagem”. Assim você vê quanto cada grupo rende por mês."
            action={
              <LinkButton to="/app/grupos/novo" variant="primary" icon={Plus} disabled={readOnly}>
                Criar grupo
              </LinkButton>
            }
          />
        </Card>
      </>
    );
  }

  const isPj = digits(values.document).length > 11;

  return (
    <>
      <PageHeader
        back={editing ? { to: `/app/clientes/${initial!.id}`, label: initial!.name } : { to: "/app/clientes", label: "Clientes" }}
        eyebrow={editing ? "// Editar cadastro" : "// Novo cadastro"}
        title={editing ? "Editar cliente" : "Cadastrar cliente"}
        subtitle={
          <>
            Preencha os dados do cliente, o contato e a assinatura. Campos com <span className="text-blue">*</span> são obrigatórios.
          </>
        }
      />

      <form onSubmit={submit} noValidate className="flex max-w-[960px] flex-col gap-6">
        <FormError message={archived ? "Este cliente está arquivado e não pode ser editado." : formError} />

        <FormSection step={1} title="Cliente" description="Dados que aparecem na cobrança e no cadastro.">
          <div className="grid gap-5 md:grid-cols-[1.4fr_1fr]">
            <Field label="Nome do cliente" required htmlFor="name" error={errors.name}>
              <Input id="name" maxLength={120} placeholder="Ex.: Clínica Vita Odonto" value={values.name} onChange={(e) => set("name", e.target.value)} invalid={!!errors.name} />
            </Field>
            <Field label={isPj ? "CNPJ" : "CPF ou CNPJ"} htmlFor="document" error={errors.document}>
              <Input id="document" mono inputMode="numeric" placeholder="00.000.000/0000-00" value={values.document} onChange={(e) => set("document", maskDocument(e.target.value))} invalid={!!errors.document} />
            </Field>
          </div>
        </FormSection>

        <FormSection step={2} stepColor="#FBAF24" title="Contato" description="Como falar com quem paga.">
          <div className="grid gap-5 md:grid-cols-2">
            <Field label="E-mail" htmlFor="email" error={errors.email}>
              <Input id="email" type="email" inputMode="email" maxLength={254} placeholder="financeiro@empresa.com.br" value={values.email} onChange={(e) => set("email", e.target.value)} invalid={!!errors.email} />
            </Field>
            <Field label="Telefone / WhatsApp" htmlFor="phone" error={errors.phone}>
              <Input id="phone" type="tel" inputMode="tel" placeholder="(00) 00000-0000" value={values.phone} onChange={(e) => set("phone", maskPhone(e.target.value))} invalid={!!errors.phone} />
            </Field>
          </div>
        </FormSection>

        <FormSection step={3} stepColor="#111B35" title="Assinatura" description="Grupo, valor e vencimento. O valor entra no total mensal do grupo.">
          <div className="grid gap-5 md:grid-cols-3">
            <Field
              label="Grupo"
              required
              htmlFor="group"
              error={errors.group_id}
              hint={
                <Link to="/app/grupos/novo" className="font-semibold text-blue underline">
                  Criar novo grupo
                </Link>
              }
            >
              <Select id="group" value={values.group_id} onChange={(e) => set("group_id", e.target.value)} invalid={!!errors.group_id}>
                <option value="">Selecione um grupo</option>
                {groups?.map((g) => (
                  <option key={g.id} value={g.id}>
                    {g.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Valor mensal (R$)" required htmlFor="amount" error={errors.amount_cents}>
              <Input id="amount" inputMode="decimal" placeholder="0,00" value={values.amount} onChange={(e) => set("amount", maskMoneyInput(e.target.value))} invalid={!!errors.amount_cents} />
            </Field>
            <Field label="Dia de vencimento" required htmlFor="due" error={errors.due_day} hint="De 1 a 28, para valer em todos os meses.">
              <Input id="due" type="number" inputMode="numeric" min={1} max={28} placeholder="1 a 28" value={values.due_day} onChange={(e) => set("due_day", e.target.value.slice(0, 2))} invalid={!!errors.due_day} />
            </Field>
          </div>
          {editing && !archived && (
            <fieldset className="mt-5">
              <legend className="mb-1.5 text-sm font-semibold">Situação</legend>
              <div className="flex flex-wrap gap-3">
                {(
                  [
                    ["active", "Ativo"],
                    ["paused", "Pausado"],
                  ] as const
                ).map(([v, label]) => (
                  <label key={v} className={cx("flex h-12 cursor-pointer items-center gap-2.5 rounded-[10px] border-2 border-navy px-4 font-semibold", values.status === v ? "bg-blue-soft" : "bg-white")}>
                    <input type="radio" name="status" className="size-4 accent-blue" checked={values.status === v} onChange={() => set("status", v)} />
                    {label}
                  </label>
                ))}
              </div>
              <p className="mt-1.5 text-[13px] text-muted">Clientes pausados não geram cobrança nos próximos meses.</p>
            </fieldset>
          )}
        </FormSection>

        {!editing && (
          <FormSection step={4} stepColor="#167A3C" title="Bônus e custos" description="Opcional. Bônus somam no valor cobrado; custos saem do seu ganho. Podem valer todo mês ou só em um mês.">
            {extras.length > 0 && (
              <ul className="mb-5 flex flex-col gap-4">
                {extras.map((d, i) => (
                  <li key={i} className="rounded-xl border-2 border-navy bg-paper-2 p-4 sm:p-5">
                    <div className="mb-4 flex items-center justify-between">
                      <p className="font-mono text-[11px] font-bold tracking-[0.14em] text-muted uppercase">[Item {i + 1}]</p>
                      <button
                        type="button"
                        className="inline-flex items-center gap-1 text-sm font-semibold text-muted hover:text-red"
                        onClick={() => {
                          setExtras((xs) => xs.filter((_, j) => j !== i));
                          setExtraErrors((xs) => xs.filter((_, j) => j !== i));
                        }}
                      >
                        <Trash2 className="size-4" /> Remover
                      </button>
                    </div>
                    <ExtraFields
                      id={`extra-${i}`}
                      value={d}
                      errors={extraErrors[i]}
                      onChange={(v) => {
                        setExtras((xs) => xs.map((x, j) => (j === i ? v : x)));
                        setExtraErrors((xs) => xs.map((x, j) => (j === i ? {} : x)));
                      }}
                    />
                  </li>
                ))}
              </ul>
            )}
            <div className="flex flex-wrap gap-3">
              <Button size="sm" icon={Plus} onClick={() => setExtras((xs) => [...xs, emptyExtra("bonus")])}>
                Adicionar bônus
              </Button>
              <Button size="sm" icon={Plus} onClick={() => setExtras((xs) => [...xs, { ...emptyExtra("cost"), when: "recurring" }])}>
                Adicionar custo
              </Button>
            </div>
          </FormSection>
        )}

        <Card className="p-5 sm:p-7">
          <h2 className="mb-4 text-xl font-bold">Observações</h2>
          <Textarea maxLength={2000} placeholder="Combinados, preferências de contato, qualquer detalhe útil." value={values.notes} onChange={(e) => set("notes", e.target.value)} invalid={!!errors.notes} aria-label="Observações" />
          {errors.notes && <p className="mt-1.5 text-[13px] font-medium text-red">{errors.notes}</p>}
        </Card>

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button onClick={() => navigate(-1)}>Cancelar</Button>
          <Button type="submit" variant="primary" icon={Check} loading={busy} disabled={readOnly || archived}>
            Salvar cliente
          </Button>
        </div>
      </form>
    </>
  );
}
