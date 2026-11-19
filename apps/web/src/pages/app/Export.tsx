import { Download } from "lucide-react";
import { useState } from "react";
import { Button, Card, Eyebrow, FormError, MonthPicker, OptionCard, PageHeader } from "../../components/ui";
import { download, errorMessage } from "../../lib/api";
import { addMonths, currentMonth, monthLabel, plural } from "../../lib/format";
import { useClients, useGroups } from "../../lib/queries";
import { useToast } from "../../lib/toast";

type Type = "clients" | "groups" | "charges";
type Format = "xlsx" | "csv";

export default function Export() {
  const toast = useToast();
  const [types, setTypes] = useState<Set<Type>>(new Set(["clients", "groups"]));
  const [format, setFormat] = useState<Format>("xlsx");
  const [month, setMonth] = useState(currentMonth());
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { data: clients } = useClients();
  const { data: groups } = useGroups(currentMonth());

  const toggle = (t: Type) =>
    setTypes((prev) => {
      const next = new Set(prev);
      if (next.has(t)) next.delete(t);
      else next.add(t);
      return next;
    });

  // O total dos grupos e as cobranças dependem do mês escolhido.
  const usesMonth = types.has("charges") || types.has("groups");

  async function run() {
    setBusy(true);
    setError(null);
    try {
      const selected = (["clients", "groups", "charges"] as Type[]).filter((t) => types.has(t));
      // Uma requisição só: a API junta os tipos marcados numa única tabela.
      await download("/export", { type: selected, format, month: usesMonth ? month : undefined }, `carteirama.${format}`);
      toast.show("Planilha baixada.");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <PageHeader eyebrow="// Dados · exportar" title="Exportar para planilha" subtitle="Baixe seus clientes, grupos e cobranças para abrir no Excel ou no Google Planilhas." />

      <Card className="max-w-[930px] p-5 sm:p-8">
        <Eyebrow className="mb-3">[O que exportar]</Eyebrow>
        <div className="flex flex-col gap-3">
          <OptionCard
            type="checkbox"
            checked={types.has("clients")}
            onChange={() => toggle("clients")}
            title="Clientes"
            description={`${clients ? plural(clients.length, "cliente", "clientes") + " com" : "Todos os clientes com"} documento, contato, grupo e valor da assinatura.`}
          />
          <OptionCard
            type="checkbox"
            checked={types.has("groups")}
            onChange={() => toggle("groups")}
            title="Grupos"
            description={`${groups ? plural(groups.length, "grupo", "grupos") + " com" : "Grupos com"} descrição e total de ${monthLabel(month)}.`}
          />
          <OptionCard type="checkbox" checked={types.has("charges")} onChange={() => toggle("charges")} title="Cobranças do mês" description={`Situação, valor, bônus, custos e líquido de cada cobrança em ${monthLabel(month)}.`} />
          {usesMonth && (
            <div className="flex flex-wrap items-center gap-3 pl-1">
              <span className="text-sm font-semibold">Mês de referência:</span>
              <MonthPicker month={month} onChange={(d) => setMonth((m) => addMonths(m, d))} />
            </div>
          )}
        </div>

        <Eyebrow className="mt-7 mb-3">[Formato]</Eyebrow>
        <div className="grid gap-3 sm:grid-cols-2">
          <OptionCard type="radio" name="format" checked={format === "xlsx"} onChange={() => setFormat("xlsx")} title="Excel (.xlsx)" description="Abre no Excel e no Google Planilhas." />
          <OptionCard type="radio" name="format" checked={format === "csv"} onChange={() => setFormat("csv")} title="CSV (.csv)" description="Texto separado por ponto e vírgula, para importar em outros sistemas." />
        </div>

        <div className="mt-7 border-t-2 border-navy pt-6">
          <FormError message={error} />
          <div className="mt-1 flex flex-col gap-4 sm:flex-row sm:items-center">
            <Button variant="primary" size="lg" icon={Download} loading={busy} disabled={types.size === 0} onClick={run}>
              Baixar planilha
            </Button>
            <p className="text-sm text-muted">{types.size > 1 ? "Tudo numa tabela só: uma linha por cliente (ou por cobrança, quando ele tem mais de uma no mês), com as colunas de cada item marcado." : "Uma tabela com os dados marcados."} Os valores saem em reais, sem símbolo.</p>
          </div>
        </div>
      </Card>
    </>
  );
}
