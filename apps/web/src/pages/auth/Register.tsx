import { useQueryClient } from "@tanstack/react-query";
import { ArrowRight, Check } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router";
import { Button, Eyebrow, Field, FormError, Input } from "../../components/ui";
import { api } from "../../lib/api";
import { ME_KEY } from "../../lib/auth";
import { cx } from "../../lib/cx";
import { digits, maskDocument, maskPhone } from "../../lib/format";
import { useForm } from "../../lib/useForm";
import type { Me, PersonType } from "../../lib/types";
import { AuthFrame } from "./AuthFrame";

function Included() {
  return (
    <div className="max-w-[480px]">
      <div className="-rotate-1 rounded-2xl border-2 border-navy bg-white px-6 py-5 text-ink shadow-hard-amber">
        <Eyebrow className="mb-3">[O que está incluído]</Eyebrow>
        {["Hospedagem e backups diários pela Parlandim TI", "Suporte por WhatsApp e e-mail"].map((t) => (
          <p key={t} className="mt-2 flex items-center gap-2.5 text-[15px]">
            <Check className="size-4 text-green" strokeWidth={3} /> {t}
          </p>
        ))}
      </div>
    </div>
  );
}

export default function Register() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const { values, set, errors, setErrors, formError, fail, reset } = useForm({
    person_type: "pj" as PersonType,
    name: "",
    email: "",
    company_name: "",
    document: "",
    phone: "",
    password: "",
    terms_accepted: false,
  });
  const pj = values.person_type === "pj";

  async function submit(e: FormEvent) {
    e.preventDefault();
    reset();
    if (!values.terms_accepted) {
      setErrors({ terms_accepted: "Aceite os termos para continuar." });
      return;
    }
    setBusy(true);
    try {
      const me = await api.post<Me>("/auth/register", {
        person_type: values.person_type,
        name: values.name.trim(),
        email: values.email.trim(),
        company_name: pj ? values.company_name.trim() || null : null,
        legal_name: pj ? values.company_name.trim() || null : null,
        document: digits(values.document),
        phone: digits(values.phone),
        password: values.password,
        terms_accepted: true,
      });
      qc.clear();
      qc.setQueryData(ME_KEY, me);
      navigate("/app", { replace: true });
    } catch (err) {
      fail(err, { legal_name: "company_name" });
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthFrame eyebrow="// Plano Cloud · 7 dias de teste" headline={<>Comece a cadastrar seus clientes hoje.</>} showcase={<Included />}>
      <Eyebrow>// Nova conta</Eyebrow>
      <h1 className="mt-1 text-[34px] font-extrabold tracking-tight">Criar conta</h1>
      <p className="mt-1 text-muted">Teste por 7 dias. Depois, R$ 24,99/mês, e você cancela quando quiser.</p>

      <form onSubmit={submit} className="mt-7 flex flex-col gap-5" noValidate>
        <FormError message={formError} />

        <fieldset>
          <legend className="mb-1.5 text-sm font-semibold">Tipo de conta</legend>
          <div className="grid grid-cols-2 gap-3 sm:gap-4">
            {(
              [
                ["pf", "Pessoa física"],
                ["pj", "Pessoa jurídica"],
              ] as const
            ).map(([v, label]) => (
              <button
                key={v}
                type="button"
                aria-pressed={values.person_type === v}
                onClick={() => {
                  set("person_type", v);
                  set("document", "");
                }}
                className={cx(
                  "h-13 rounded-[10px] border-2 border-navy text-[15px] font-bold transition",
                  values.person_type === v ? "bg-navy text-white shadow-[4px_4px_0_0_var(--color-blue)]" : "bg-white hover:bg-paper-2",
                )}
              >
                {label}
              </button>
            ))}
          </div>
        </fieldset>

        <div className="grid gap-5 sm:grid-cols-2">
          <Field label="Seu nome" htmlFor="name" error={errors.name}>
            <Input id="name" autoComplete="name" placeholder="Renata Alves" value={values.name} onChange={(e) => set("name", e.target.value)} invalid={!!errors.name} />
          </Field>
          <Field label="E-mail" htmlFor="email" error={errors.email}>
            <Input id="email" type="email" inputMode="email" autoComplete="email" placeholder="voce@empresa.com.br" value={values.email} onChange={(e) => set("email", e.target.value)} invalid={!!errors.email} />
          </Field>
          {pj && (
            <Field label="Nome da empresa" htmlFor="company" error={errors.company_name}>
              <Input id="company" autoComplete="organization" placeholder="Alves Web Studio" value={values.company_name} onChange={(e) => set("company_name", e.target.value)} invalid={!!errors.company_name} />
            </Field>
          )}
          <Field label={pj ? "CNPJ" : "CPF"} htmlFor="document" error={errors.document} hint="Usado nas faturas e recibos.">
            <Input
              id="document"
              inputMode="numeric"
              placeholder={pj ? "00.000.000/0001-00" : "000.000.000-00"}
              value={values.document}
              onChange={(e) => set("document", maskDocument(e.target.value, pj ? "cnpj" : "cpf"))}
              invalid={!!errors.document}
            />
          </Field>
          <Field label="Telefone / WhatsApp" htmlFor="phone" error={errors.phone} className={pj ? "sm:col-span-2" : undefined}>
            <Input id="phone" type="tel" inputMode="tel" autoComplete="tel" placeholder="(00) 00000-0000" value={values.phone} onChange={(e) => set("phone", maskPhone(e.target.value))} invalid={!!errors.phone} />
          </Field>
        </div>

        <Field label="Senha" htmlFor="password" error={errors.password} hint="Ao menos 8 caracteres, com letras e números.">
          <Input id="password" type="password" autoComplete="new-password" placeholder="Mínimo de 8 caracteres" value={values.password} onChange={(e) => set("password", e.target.value)} invalid={!!errors.password} />
        </Field>

        <div>
          <div className="flex items-start gap-3 text-[15px]">
            <input
              id="terms"
              type="checkbox"
              checked={values.terms_accepted}
              onChange={(e) => set("terms_accepted", e.target.checked)}
              aria-invalid={!!errors.terms_accepted || undefined}
              className="mt-0.5 size-5 shrink-0 cursor-pointer accent-blue"
            />
            <p>
              <label htmlFor="terms" className="cursor-pointer">
                Li e aceito os
              </label>{" "}
              <Link to="/termos" target="_blank" rel="noopener" className="font-semibold text-blue underline underline-offset-2">
                termos de uso
              </Link>{" "}
              <label htmlFor="terms" className="cursor-pointer">
                e a
              </label>{" "}
              <Link to="/privacidade" target="_blank" rel="noopener" className="font-semibold text-blue underline underline-offset-2">
                política de privacidade
              </Link>
              .
            </p>
          </div>
          {errors.terms_accepted && <p className="mt-1.5 text-[13px] font-medium text-red">{errors.terms_accepted}</p>}
        </div>

        <Button type="submit" variant="primary" size="lg" block loading={busy} iconRight={ArrowRight} className="mt-2">
          Começar teste de 7 dias
        </Button>
      </form>

      <div className="mt-7 border-t-2 border-navy pt-6 text-[15px]">
        Já tem conta?{" "}
        <Link to="/entrar" className="font-semibold text-blue underline underline-offset-2">
          Entrar
        </Link>
      </div>
    </AuthFrame>
  );
}
