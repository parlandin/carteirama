import {
  ArrowLeft,
  CreditCard,
  Download,
  LayoutGrid,
  Layers,
  Lock,
  Plus,
  ReceiptText,
  Settings,
  Tag,
  Users,
  Building2,
} from "lucide-react";
import { Navigate, Outlet, useLocation } from "react-router";
import { Shell, type NavSection } from "../components/Shell";
import { LinkButton, Loading } from "../components/ui";
import { displayAccountName, isReadOnly, useMe } from "../lib/auth";
import { money } from "../lib/format";
import { useInvoices } from "../lib/queries";
import type { Me } from "../lib/types";

function useRequireMe() {
  const { data: me, isLoading, isError } = useMe();
  const location = useLocation();
  if (isLoading) return { state: "loading" as const };
  if (isError) return { state: "error" as const };
  if (!me) {
    const next = encodeURIComponent(location.pathname + location.search);
    return { state: "redirect" as const, to: `/entrar?next=${next}` };
  }
  return { state: "ok" as const, me };
}

function FullLoading() {
  return (
    <div className="grid min-h-dvh place-items-center">
      <Loading />
    </div>
  );
}

function ServerDown() {
  return (
    <div className="grid min-h-dvh place-items-center px-6 text-center">
      <div>
        <p className="text-xl font-bold">Não foi possível falar com o servidor.</p>
        <p className="mt-2 text-muted">Verifique sua conexão e recarregue a página.</p>
      </div>
    </div>
  );
}

function SuspendedBanner({ me }: { me: Me }) {
  const { data: invoices } = useInvoices(true);
  const open = invoices?.find((i) => i.status === "pending");
  const cancelled = me.account?.status === "cancelled";
  return (
    <div className="border-b-2 border-navy bg-amber px-4 py-3 sm:px-8">
      <div className="mx-auto flex max-w-[1400px] flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <p className="text-sm font-semibold text-navy">
          <span className="mr-3 inline-flex items-center gap-1.5 font-mono text-xs font-bold tracking-[0.16em] uppercase">
            <Lock className="size-4" /> {cancelled ? "Conta cancelada" : "Conta suspensa"}
          </span>
          {cancelled
            ? "Sua assinatura foi cancelada. Você ainda pode consultar e exportar seus dados."
            : open
              ? `A fatura ${open.number} de ${money(open.amount_cents)} não foi paga. Regularize para voltar a cadastrar clientes. Seus dados estão guardados.`
              : "Regularize a assinatura para voltar a cadastrar clientes. Seus dados estão guardados."}
        </p>
        {!cancelled && (
          <LinkButton to="/app/assinatura" variant="dark" size="sm" icon={CreditCard} className="self-start md:self-auto">
            Regularizar fatura
          </LinkButton>
        )}
      </div>
    </div>
  );
}

export function AppLayout() {
  const r = useRequireMe();
  if (r.state === "loading") return <FullLoading />;
  if (r.state === "error") return <ServerDown />;
  if (r.state === "redirect") return <Navigate to={r.to} replace />;
  const me = r.me;
  if (!me.account) return <Navigate to={me.user.is_admin ? "/admin" : "/entrar"} replace />;

  const sections: NavSection[] = [
    {
      items: [
        { to: "/app", label: "Grupos", icon: Layers, end: true },
        { to: "/app/clientes", label: "Clientes", icon: Users, end: true },
        { to: "/app/cobrancas", label: "Cobranças", icon: ReceiptText },
      ],
    },
    {
      title: "Cadastrar",
      items: [
        { to: "/app/clientes/novo", label: "Novo cliente", icon: Plus },
        { to: "/app/grupos/novo", label: "Novo grupo", icon: Plus },
      ],
    },
    { title: "Dados", items: [{ to: "/app/exportar", label: "Exportar planilha", icon: Download }] },
    {
      title: "Conta",
      items: [
        { to: "/app/assinatura", label: "Assinatura", icon: CreditCard },
        { to: "/app/conta", label: "Meus dados", icon: Settings },
      ],
    },
  ];
  if (me.user.is_admin) {
    sections.push({ title: "Admin", items: [{ to: "/admin", label: "Ir para painel admin", icon: LayoutGrid, highlight: true }] });
  }

  return (
    <Shell
      sections={sections}
      user={{ name: me.user.name, subtitle: displayAccountName(me) }}
      banner={isReadOnly(me) ? <SuspendedBanner me={me} /> : undefined}
    >
      <Outlet />
    </Shell>
  );
}

export function AdminLayout() {
  const r = useRequireMe();
  if (r.state === "loading") return <FullLoading />;
  if (r.state === "error") return <ServerDown />;
  if (r.state === "redirect") return <Navigate to={r.to} replace />;
  const me = r.me;
  if (!me.user.is_admin) return <Navigate to="/app" replace />;

  const sections: NavSection[] = [
    {
      items: [
        { to: "/admin", label: "Visão geral", icon: LayoutGrid, end: true },
        { to: "/admin/contas", label: "Contas", icon: Building2 },
        { to: "/admin/pagamentos", label: "Pagamentos", icon: ReceiptText },
        { to: "/admin/planos", label: "Planos", icon: Tag },
      ],
    },
  ];
  if (me.account) sections.push({ title: "Atalhos", items: [{ to: "/app", label: "Abrir o app", icon: ArrowLeft }] });

  return (
    <Shell
      admin
      sections={sections}
      user={{ name: me.user.name, subtitle: me.account ? displayAccountName(me) : me.user.email }}
      footerNote="// Suíte Parlandim TI"
    >
      <Outlet />
    </Shell>
  );
}

export function GuestOnly() {
  const { data: me, isLoading } = useMe();
  const location = useLocation();
  if (isLoading) return <FullLoading />;
  if (me) {
    const next = new URLSearchParams(location.search).get("next");
    const safe = next && next.startsWith("/") && !next.startsWith("//") ? next : null;
    return <Navigate to={safe ?? (me.account ? "/app" : "/admin")} replace />;
  }
  return <Outlet />;
}
