import { ArrowRight, MailCheck } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router";
import { Button, Eyebrow, Field, FormError, Input, LinkButton } from "../../components/ui";
import { api } from "../../lib/api";
import { useForm } from "../../lib/useForm";
import { AuthFrame, GroupShowcase } from "./AuthFrame";

export function ForgotPassword() {
  const { values, set, errors, formError, fail, reset } = useForm({ email: "" });
  const [busy, setBusy] = useState(false);
  const [sent, setSent] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    reset();
    setBusy(true);
    try {
      await api.post("/auth/forgot-password", { email: values.email.trim() });
      setSent(true);
    } catch (err) {
      fail(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthFrame eyebrow="// Suíte Parlandim TI · Gestão de clientes" headline={<>Saiba quanto vai receber este mês.</>} showcase={<GroupShowcase />}>
      <Eyebrow>// Acesso</Eyebrow>
      <h1 className="mt-1 text-[34px] font-extrabold tracking-tight">Esqueci a senha</h1>
      {sent ? (
        <div className="mt-6">
          <div className="flex items-start gap-3 rounded-xl border-2 border-navy bg-green-soft p-4">
            <MailCheck className="mt-0.5 size-5 shrink-0 text-green" />
            <p className="text-[15px]">
              Se <strong>{values.email}</strong> tiver uma conta, você vai receber um e-mail com o link para criar uma nova senha. O link vale por pouco tempo.
            </p>
          </div>
          <LinkButton to="/entrar" variant="secondary" block className="mt-6">
            Voltar para entrar
          </LinkButton>
        </div>
      ) : (
        <>
          <p className="mt-1 text-muted">Informe o e-mail da conta. Enviaremos um link para você criar uma nova senha.</p>
          <form onSubmit={submit} className="mt-7 flex flex-col gap-5" noValidate>
            <FormError message={formError} />
            <Field label="E-mail" htmlFor="email" error={errors.email}>
              <Input id="email" type="email" inputMode="email" autoComplete="email" placeholder="voce@empresa.com.br" value={values.email} onChange={(e) => set("email", e.target.value)} invalid={!!errors.email} />
            </Field>
            <Button type="submit" variant="primary" size="lg" block loading={busy} iconRight={ArrowRight}>
              Enviar link
            </Button>
          </form>
          <div className="mt-7 border-t-2 border-navy pt-6 text-[15px]">
            Lembrou?{" "}
            <Link to="/entrar" className="font-semibold text-blue underline underline-offset-2">
              Entrar
            </Link>
          </div>
        </>
      )}
    </AuthFrame>
  );
}

export function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const { values, set, errors, setErrors, formError, fail, reset } = useForm({ password: "", confirm: "" });
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    reset();
    if (values.password !== values.confirm) {
      setErrors({ confirm: "As senhas não são iguais." });
      return;
    }
    setBusy(true);
    try {
      await api.post("/auth/reset-password", { token, password: values.password });
      setDone(true);
    } catch (err) {
      fail(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthFrame eyebrow="// Suíte Parlandim TI · Gestão de clientes" headline={<>Saiba quanto vai receber este mês.</>} showcase={<GroupShowcase />}>
      <Eyebrow>// Acesso</Eyebrow>
      <h1 className="mt-1 text-[34px] font-extrabold tracking-tight">Nova senha</h1>
      {!token ? (
        <div className="mt-6">
          <FormError message="Link inválido. Peça um novo link de redefinição." />
          <LinkButton to="/esqueci-a-senha" block className="mt-6">
            Pedir novo link
          </LinkButton>
        </div>
      ) : done ? (
        <div className="mt-6">
          <p className="rounded-xl border-2 border-navy bg-green-soft p-4 text-[15px]">Senha alterada. Entre com a nova senha.</p>
          <LinkButton to="/entrar" variant="primary" block className="mt-6">
            Entrar
          </LinkButton>
        </div>
      ) : (
        <form onSubmit={submit} className="mt-7 flex flex-col gap-5" noValidate>
          <FormError message={errors.token ? `${errors.token} Peça um novo link.` : formError} />
          <Field label="Nova senha" htmlFor="password" error={errors.password} hint="Ao menos 8 caracteres, com letras e números.">
            <Input id="password" type="password" autoComplete="new-password" value={values.password} onChange={(e) => set("password", e.target.value)} invalid={!!errors.password} />
          </Field>
          <Field label="Repita a senha" htmlFor="confirm" error={errors.confirm}>
            <Input id="confirm" type="password" autoComplete="new-password" value={values.confirm} onChange={(e) => set("confirm", e.target.value)} invalid={!!errors.confirm} />
          </Field>
          <Button type="submit" variant="primary" size="lg" block loading={busy}>
            Salvar nova senha
          </Button>
          {errors.token && (
            <Link to="/esqueci-a-senha" className="text-center text-sm font-semibold text-blue underline">
              Pedir novo link
            </Link>
          )}
        </form>
      )}
    </AuthFrame>
  );
}
