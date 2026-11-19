import { useQueryClient } from "@tanstack/react-query";
import { ArrowRight } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { Button, Checkbox, Eyebrow, Field, FormError, Input } from "../../components/ui";
import { api } from "../../lib/api";
import { ME_KEY } from "../../lib/auth";
import { useForm } from "../../lib/useForm";
import type { Me } from "../../lib/types";
import { AuthFrame, GroupShowcase } from "./AuthFrame";

export default function Login() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const { values, set, errors, formError, fail, reset } = useForm({ email: "", password: "", remember: false });
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    reset();
    setBusy(true);
    try {
      const me = await api.post<Me>("/auth/login", values);
      qc.clear();
      qc.setQueryData(ME_KEY, me);
      const next = params.get("next");
      const safe = next && next.startsWith("/") && !next.startsWith("//") ? next : null;
      navigate(safe ?? (me.account ? "/app" : "/admin"), { replace: true });
    } catch (err) {
      fail(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthFrame eyebrow="// Suíte Parlandim TI · Gestão de clientes" headline={<>Saiba quanto vai receber este mês.</>} showcase={<GroupShowcase />}>
      <Eyebrow>// Acesso</Eyebrow>
      <h1 className="mt-1 text-[34px] font-extrabold tracking-tight">Entrar</h1>
      <p className="mt-1 text-muted">Use o e-mail e a senha da sua conta.</p>

      <form onSubmit={submit} className="mt-7 flex flex-col gap-5" noValidate>
        <FormError message={formError} />
        <Field label="E-mail" htmlFor="email" error={errors.email}>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            inputMode="email"
            placeholder="voce@empresa.com.br"
            value={values.email}
            onChange={(e) => set("email", e.target.value)}
            invalid={!!errors.email}
            required
          />
        </Field>
        <Field
          label="Senha"
          htmlFor="password"
          error={errors.password}
          aside={
            <Link to="/esqueci-a-senha" className="text-sm font-semibold text-blue underline underline-offset-2">
              Esqueci a senha
            </Link>
          }
        >
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            placeholder="Sua senha"
            value={values.password}
            onChange={(e) => set("password", e.target.value)}
            invalid={!!errors.password}
            required
          />
        </Field>
        <Checkbox checked={values.remember} onChange={(v) => set("remember", v)} label="Manter conectado neste computador" />
        <Button type="submit" variant="primary" size="lg" block loading={busy} iconRight={ArrowRight} className="mt-2">
          Entrar
        </Button>
      </form>

      <div className="mt-7 border-t-2 border-navy pt-6 text-[15px]">
        Ainda não tem conta?{" "}
        <Link to="/criar-conta" className="font-semibold text-blue underline underline-offset-2">
          Criar conta
        </Link>
      </div>
    </AuthFrame>
  );
}
