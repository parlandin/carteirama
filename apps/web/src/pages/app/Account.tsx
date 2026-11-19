import { useQueryClient } from "@tanstack/react-query";
import { Check, KeyRound } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Button, Field, FormError, FormSection, Input, PageHeader, Select } from "../../components/ui";
import { api } from "../../lib/api";
import { ME_KEY, useMe } from "../../lib/auth";
import { digits, formatDocument, formatPhone, maskPhone, maskZip } from "../../lib/format";
import { useToast } from "../../lib/toast";
import type { Me } from "../../lib/types";
import { useForm } from "../../lib/useForm";

const UFS = "AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO".split(" ");

function ProfileForm({ me }: { me: Me }) {
  const qc = useQueryClient();
  const toast = useToast();
  const a = me.account!;
  const pj = a.person_type === "pj";
  const [busy, setBusy] = useState(false);
  const { values, set, errors, formError, fail, reset } = useForm({
    name: me.user.name,
    phone: formatPhone(a.phone),
    legal_name: a.legal_name,
    company_name: a.company_name ?? "",
    zip: a.zip ? maskZip(a.zip) : "",
    address: a.address ?? "",
    city: a.city ?? "",
    state: a.state ?? "",
  });

  async function submit(e: FormEvent) {
    e.preventDefault();
    reset();
    setBusy(true);
    try {
      const updated = await api.patch<Me>("/me", {
        name: values.name.trim(),
        phone: digits(values.phone),
        legal_name: values.legal_name.trim(),
        ...(pj ? { company_name: values.company_name.trim() || null } : {}),
        zip: digits(values.zip) || null,
        address: values.address.trim() || null,
        city: values.city.trim() || null,
        state: values.state || null,
      });
      qc.setQueryData(ME_KEY, updated);
      toast.show("Dados atualizados.");
    } catch (err) {
      fail(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-6">
      <FormError message={formError} />
      <FormSection step={1} title="Você" description="Quem acessa a conta.">
        <div className="grid gap-5 md:grid-cols-2">
          <Field label="Seu nome" required htmlFor="name" error={errors.name}>
            <Input id="name" value={values.name} onChange={(e) => set("name", e.target.value)} invalid={!!errors.name} />
          </Field>
          <Field label="E-mail" htmlFor="email" hint="Para trocar o e-mail, fale com o suporte.">
            <Input id="email" value={me.user.email} disabled />
          </Field>
          <Field label="Telefone / WhatsApp" required htmlFor="phone" error={errors.phone}>
            <Input id="phone" type="tel" inputMode="tel" value={values.phone} onChange={(e) => set("phone", maskPhone(e.target.value))} invalid={!!errors.phone} />
          </Field>
        </div>
      </FormSection>

      <FormSection step={2} stepColor="#FBAF24" title="Dados de cobrança" description="Aparecem nas faturas e recibos da Carteirama.">
        <div className="grid gap-5 md:grid-cols-2">
          <Field label={pj ? "Razão social" : "Nome completo"} required htmlFor="legal" error={errors.legal_name}>
            <Input id="legal" value={values.legal_name} onChange={(e) => set("legal_name", e.target.value)} invalid={!!errors.legal_name} />
          </Field>
          {pj && (
            <Field label="Nome fantasia" required htmlFor="company" error={errors.company_name}>
              <Input id="company" value={values.company_name} onChange={(e) => set("company_name", e.target.value)} invalid={!!errors.company_name} />
            </Field>
          )}
          <Field label={pj ? "CNPJ" : "CPF"} htmlFor="doc" hint="Não pode ser alterado.">
            <Input id="doc" mono value={formatDocument(a.document)} disabled />
          </Field>
        </div>
      </FormSection>

      <FormSection step={3} stepColor="#167A3C" title="Endereço" description="Opcional.">
        <div className="grid gap-5 md:grid-cols-[180px_1fr]">
          <Field label="CEP" htmlFor="zip" error={errors.zip}>
            <Input id="zip" mono inputMode="numeric" placeholder="00000-000" value={values.zip} onChange={(e) => set("zip", maskZip(e.target.value))} invalid={!!errors.zip} />
          </Field>
          <Field label="Endereço" htmlFor="address" error={errors.address}>
            <Input id="address" placeholder="Rua, número, complemento" value={values.address} onChange={(e) => set("address", e.target.value)} invalid={!!errors.address} />
          </Field>
        </div>
        <div className="mt-5 grid gap-5 md:grid-cols-[1fr_180px]">
          <Field label="Cidade" htmlFor="city" error={errors.city}>
            <Input id="city" value={values.city} onChange={(e) => set("city", e.target.value)} invalid={!!errors.city} />
          </Field>
          <Field label="Estado" htmlFor="state" error={errors.state}>
            <Select id="state" value={values.state} onChange={(e) => set("state", e.target.value)} invalid={!!errors.state}>
              <option value="">Selecione</option>
              {UFS.map((u) => (
                <option key={u}>{u}</option>
              ))}
            </Select>
          </Field>
        </div>
      </FormSection>

      <div className="flex justify-end">
        <Button type="submit" variant="primary" icon={Check} loading={busy}>
          Salvar dados
        </Button>
      </div>
    </form>
  );
}

function PasswordForm() {
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const { values, set, setValues, errors, setErrors, formError, fail, reset } = useForm({ current_password: "", new_password: "", confirm: "" });

  async function submit(e: FormEvent) {
    e.preventDefault();
    reset();
    if (values.new_password !== values.confirm) {
      setErrors({ confirm: "As senhas não são iguais." });
      return;
    }
    setBusy(true);
    try {
      await api.patch("/me/password", { current_password: values.current_password, new_password: values.new_password });
      setValues({ current_password: "", new_password: "", confirm: "" });
      toast.show("Senha alterada. As outras sessões foram encerradas.");
    } catch (err) {
      fail(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate>
      <FormSection title="Trocar senha" description="Ao trocar, as sessões em outros aparelhos são encerradas.">
        <div className="flex flex-col gap-5">
          <FormError message={formError} />
          <div className="grid gap-5 md:grid-cols-3">
            <Field label="Senha atual" htmlFor="cur" error={errors.current_password}>
              <Input id="cur" type="password" autoComplete="current-password" value={values.current_password} onChange={(e) => set("current_password", e.target.value)} invalid={!!errors.current_password} />
            </Field>
            <Field label="Nova senha" htmlFor="new" error={errors.new_password}>
              <Input id="new" type="password" autoComplete="new-password" value={values.new_password} onChange={(e) => set("new_password", e.target.value)} invalid={!!errors.new_password} />
            </Field>
            <Field label="Repita a nova senha" htmlFor="conf" error={errors.confirm}>
              <Input id="conf" type="password" autoComplete="new-password" value={values.confirm} onChange={(e) => set("confirm", e.target.value)} invalid={!!errors.confirm} />
            </Field>
          </div>
          <div className="flex justify-end">
            <Button type="submit" icon={KeyRound} loading={busy}>
              Trocar senha
            </Button>
          </div>
        </div>
      </FormSection>
    </form>
  );
}

export default function Account() {
  const { data: me } = useMe();
  if (!me?.account) return null;
  return (
    <>
      <PageHeader eyebrow="// Conta · meus dados" title="Meus dados" subtitle="Seu nome, contato e os dados que aparecem nas faturas." />
      <div className="flex max-w-[960px] flex-col gap-10">
        <ProfileForm me={me} />
        <PasswordForm />
      </div>
    </>
  );
}
