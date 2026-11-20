import { Archive, Check } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router";
import { Button, Card, ErrorState, Eyebrow, Field, FormError, GroupAvatar, Input, Loading, Modal, PageHeader, Textarea } from "../../components/ui";
import { api, errorMessage } from "../../lib/api";
import { isReadOnly, useMe } from "../../lib/auth";
import { DEFAULT_GROUP_COLOR, GROUP_COLORS } from "../../lib/colors";
import { cx } from "../../lib/cx";
import { money } from "../../lib/format";
import { useGroup, useInvalidateData } from "../../lib/queries";
import { useToast } from "../../lib/toast";
import type { GroupResponse } from "../../lib/types";
import { useOrigin } from "../../lib/origin";
import { useForm } from "../../lib/useForm";

export default function GroupFormPage() {
  const { id } = useParams();
  if (id) return <EditGroup id={id} />;
  return <GroupForm />;
}

function EditGroup({ id }: { id: string }) {
  const { data, isLoading, error, refetch } = useGroup(id);
  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;
  return <GroupForm initial={data} activeClients={data.clients.filter((c) => c.status !== "archived").length} />;
}

function GroupForm({ initial, activeClients = 0 }: { initial?: GroupResponse; activeClients?: number }) {
  const navigate = useNavigate();
  const toast = useToast();
  const invalidate = useInvalidateData();
  const { data: me } = useMe();
  const readOnly = isReadOnly(me);
  const editing = !!initial;
  const origin = useOrigin(editing ? { to: `/app/grupos/${initial!.id}`, label: initial!.name } : { to: "/app", label: "Grupos" });
  const [busy, setBusy] = useState(false);
  const [confirmArchive, setConfirmArchive] = useState(false);
  const [archiving, setArchiving] = useState(false);
  const { values, set, errors, formError, setFormError, fail, reset } = useForm({
    name: initial?.name ?? "",
    notes: initial?.notes ?? "",
    color: (initial?.color ?? DEFAULT_GROUP_COLOR).toUpperCase(),
  });

  useEffect(() => {
    if (readOnly) setFormError("Sua conta está suspensa. Regularize a assinatura para editar grupos.");
  }, [readOnly, setFormError]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    reset();
    setBusy(true);
    const body = { name: values.name.trim(), notes: values.notes.trim() || null, color: values.color };
    try {
      const saved = editing ? await api.patch<GroupResponse>(`/groups/${initial!.id}`, body) : await api.post<GroupResponse>("/groups", body);
      await invalidate();
      toast.show(editing ? "Grupo atualizado." : "Grupo criado.");
      if (editing) origin.goBack();
      else navigate(`/app/grupos/${saved.id}`, { replace: true, state: origin.carry });
    } catch (err) {
      fail(err);
    } finally {
      setBusy(false);
    }
  }

  async function archive() {
    setArchiving(true);
    try {
      await api.delete(`/groups/${initial!.id}`);
      await invalidate();
      toast.show("Grupo arquivado.");
      navigate("/app", { replace: true });
    } catch (err) {
      toast.show(errorMessage(err), "error");
      setConfirmArchive(false);
    } finally {
      setArchiving(false);
    }
  }

  return (
    <>
      <PageHeader
        back={origin.back}
        eyebrow={editing ? "// Editar cadastro" : "// Novo cadastro"}
        title={editing ? "Editar grupo" : "Cadastrar grupo"}
        subtitle="Um grupo é uma categoria de clientes, como uma assinatura ou um projeto. O total mensal de cada grupo aparece na tela inicial."
      />

      <form onSubmit={submit} noValidate className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="flex flex-col gap-6">
          <FormError message={formError} />
          <Card className="p-5 sm:p-7">
            <h2 className="mb-5 text-xl font-bold">Identificação</h2>
            <div className="flex flex-col gap-5">
              <Field label="Nome do grupo" required htmlFor="name" error={errors.name}>
                <Input id="name" maxLength={60} placeholder="Ex.: Assinatura SaaS" value={values.name} onChange={(e) => set("name", e.target.value)} invalid={!!errors.name} />
              </Field>
              <Field label="Descrição" htmlFor="notes" error={errors.notes}>
                <Textarea id="notes" maxLength={2000} placeholder="Para que serve este grupo e quais clientes entram nele." value={values.notes} onChange={(e) => set("notes", e.target.value)} invalid={!!errors.notes} />
              </Field>
              <fieldset>
                <legend className="mb-1.5 text-sm font-semibold">Cor</legend>
                <div className="flex flex-wrap gap-3">
                  {GROUP_COLORS.map((c) => (
                    <label
                      key={c.value}
                      className={cx(
                        "flex h-12 cursor-pointer items-center gap-2.5 rounded-[10px] border-2 border-navy px-3.5 text-[15px] font-semibold transition",
                        values.color === c.value ? "bg-blue-soft" : "bg-white hover:bg-paper-2",
                      )}
                    >
                      <input type="radio" name="color" className="size-4 accent-blue" checked={values.color === c.value} onChange={() => set("color", c.value)} />
                      <span className="size-6 rounded-md border-2 border-navy" style={{ background: c.value }} />
                      {c.name}
                    </label>
                  ))}
                </div>
                {errors.color && <p className="mt-1.5 text-[13px] font-medium text-red">{errors.color}</p>}
              </fieldset>
            </div>
          </Card>

          {editing && (
            <Card className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between sm:p-7">
              <div>
                <h2 className="text-lg font-bold">Arquivar grupo</h2>
                <p className="text-sm text-muted">
                  {activeClients > 0
                    ? `O grupo tem ${activeClients} cliente(s). Mova ou arquive os clientes antes de arquivar o grupo.`
                    : "Grupos arquivados saem da tela inicial e do total do mês."}
                </p>
              </div>
              <Button variant="danger" icon={Archive} disabled={readOnly || activeClients > 0} onClick={() => setConfirmArchive(true)}>
                Arquivar
              </Button>
            </Card>
          )}

          <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <Button onClick={origin.goBack}>Cancelar</Button>
            <Button type="submit" variant="primary" icon={Check} loading={busy} disabled={readOnly}>
              Salvar grupo
            </Button>
          </div>
        </div>

        <aside className="order-first lg:order-none">
          <Eyebrow tone="muted" className="mb-3">
            [Prévia na lista]
          </Eyebrow>
          <div className="rotate-[-1deg] rounded-2xl border-2 border-navy bg-white p-5 shadow-hard">
            <div className="flex items-center gap-3 border-b-2 border-navy pb-4">
              <GroupAvatar name={values.name || "Nome do grupo"} color={values.color} />
              <div className="min-w-0">
                <p className="truncate text-lg font-bold">{values.name || "Nome do grupo"}</p>
                <p className="truncate text-sm text-muted">{values.notes || "A descrição aparece aqui"}</p>
              </div>
            </div>
            <div className="flex items-center justify-between pt-4">
              <span className="font-mono text-sm text-muted">{activeClients} clientes</span>
              <span className="text-xl font-extrabold">{money(0)}</span>
            </div>
          </div>
          <p className="mt-4 ml-2 hidden -rotate-2 font-hand text-2xl text-muted lg:block">é assim que ele aparece na tela inicial</p>
        </aside>
      </form>

      <Modal
        open={confirmArchive}
        onClose={() => setConfirmArchive(false)}
        title="Arquivar grupo?"
        footer={
          <>
            <Button onClick={() => setConfirmArchive(false)}>Cancelar</Button>
            <Button variant="danger" loading={archiving} onClick={archive}>
              Arquivar grupo
            </Button>
          </>
        }
      >
        <p>
          O grupo <strong>{initial?.name}</strong> sai da tela inicial e do total do mês. Os dados continuam guardados.
        </p>
      </Modal>
    </>
  );
}
