import { useQueryClient } from "@tanstack/react-query";
import { Check, X } from "lucide-react";
import { useState } from "react";
import { Button, Card, ErrorState, Eyebrow, Field, FormError, GithubIcon, Input, Loading, PageHeader, Pill } from "../../components/ui";
import { ApiError, api, errorMessage } from "../../lib/api";
import { centsToInput, maskMoneyInput, money, parseMoney } from "../../lib/format";
import { useAdminPlans, useAdminSummary } from "../../lib/queries";
import { useToast } from "../../lib/toast";
import type { AdminPlan } from "../../lib/types";

const REPO_URL = "https://github.com/parlandin/carteirama";

function PlanCard({ plan, mrr }: { plan: AdminPlan; mrr?: number }) {
  const qc = useQueryClient();
  const toast = useToast();
  const [price, setPrice] = useState(centsToInput(plan.price_cents));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const cents = parseMoney(price);
  const dirty = cents !== plan.price_cents;

  async function save() {
    setError(null);
    if (cents === null || cents < 100 || cents > 999_999) {
      setError("Informe um preço entre R$ 1,00 e R$ 9.999,99.");
      return;
    }
    setBusy(true);
    try {
      await api.patch(`/admin/plans/${plan.code}`, { price_cents: cents });
      await qc.invalidateQueries({ queryKey: ["admin"] });
      toast.show("Preço atualizado. Vale para as próximas faturas.");
    } catch (e) {
      setError(e instanceof ApiError && e.details.length ? e.details[0].message : errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card className="p-5 sm:p-7">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-2">
        <Eyebrow>[{plan.code}]</Eyebrow>
        {mrr !== undefined && <p className="font-mono text-xs text-muted">{money(mrr)} MRR</p>}
      </div>
      <div className="flex flex-col gap-5">
        <FormError message={error} />
        <Field label="Nome do plano" htmlFor={`name-${plan.code}`}>
          <Input id={`name-${plan.code}`} value={plan.name} disabled />
        </Field>
        <div className="grid gap-5 md:grid-cols-2">
          <Field label="Preço por mês (R$)" htmlFor={`price-${plan.code}`} hint="Vale para as faturas geradas a partir de agora.">
            <Input id={`price-${plan.code}`} inputMode="decimal" value={price} onChange={(e) => setPrice(maskMoneyInput(e.target.value))} />
          </Field>
          <Field label="Dias de teste" htmlFor={`trial-${plan.code}`} hint="Antes da primeira cobrança.">
            <Input id={`trial-${plan.code}`} value={plan.trial_days} disabled />
          </Field>
        </div>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <p className="flex items-center gap-2 text-[15px]">
            {plan.active ? <Check className="size-4 text-green" strokeWidth={3} /> : <X className="size-4 text-red" />}
            {plan.active ? "Disponível para novas contas" : "Indisponível para novas contas"}
          </p>
          <Button variant="primary" icon={Check} loading={busy} disabled={!dirty} onClick={save}>
            Salvar alterações
          </Button>
        </div>
      </div>
    </Card>
  );
}

export default function AdminPlans() {
  const { data: plans, isLoading, error, refetch } = useAdminPlans();
  const { data: summary } = useAdminSummary();

  return (
    <>
      <PageHeader eyebrow="// Admin · planos" title="Planos" subtitle="Preço e período de teste dos planos da Carteirama." />
      {isLoading ? (
        <Loading />
      ) : error || !plans ? (
        <ErrorState message={errorMessage(error)} onRetry={refetch} />
      ) : (
        <div className="flex flex-col gap-7">
          {plans.map((p) => (
            <PlanCard key={p.code} plan={p} mrr={plans.length === 1 ? summary?.mrr_cents : undefined} />
          ))}
          <Card className="flex flex-col gap-5 p-5 sm:p-7 md:flex-row md:items-center md:justify-between">
            <div>
              <div className="mb-2 flex items-center gap-3">
                <Eyebrow>[Comunitário]</Eyebrow>
                <Pill tone="gray" icon={X}>
                  Sem cobrança
                </Pill>
              </div>
              <p className="text-2xl font-extrabold">Instalação própria, R$ 0</p>
              <p className="mt-1 max-w-xl text-muted">Quem instala o código por conta própria não tem conta neste painel. O suporte é apenas pelo GitHub, com issues e discussões da comunidade.</p>
            </div>
            <a
              href={REPO_URL}
              target="_blank"
              rel="noreferrer"
              className="inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-[10px] border-2 border-navy bg-white px-4 font-bold shadow-hard-sm hover:bg-paper-2"
            >
              <GithubIcon className="size-4" /> Abrir repositório
            </a>
          </Card>
        </div>
      )}
    </>
  );
}
