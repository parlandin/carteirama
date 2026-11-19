import { LogOut, Menu, X, type LucideIcon } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router";
import { useLogout } from "../lib/auth";
import { cx } from "../lib/cx";
import { Logo } from "./ui";

export interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
  highlight?: boolean;
}

export interface NavSection {
  title?: string;
  items: NavItem[];
}

function NavLinks({ sections, onNavigate }: { sections: NavSection[]; onNavigate?: () => void }) {
  return (
    <nav className="flex flex-col gap-1" aria-label="Principal">
      {sections.map((section, i) => (
        <div key={i} className={cx(section.title && "mt-5")}>
          {section.title && <p className="mb-2 px-3 font-mono text-[10px] tracking-[0.16em] text-white/60 uppercase">[{section.title}]</p>}
          <div className="flex flex-col gap-1.5">
            {section.items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                onClick={onNavigate}
                className={({ isActive }) =>
                  cx(
                    "flex h-11 items-center gap-3 rounded-[10px] px-3.5 text-[15px] font-semibold transition",
                    item.highlight
                      ? "border-2 border-amber text-amber hover:bg-amber/10"
                      : isActive
                        ? "bg-blue text-white"
                        : "text-white/90 hover:bg-white/8",
                  )
                }
              >
                <item.icon className="size-[18px]" strokeWidth={2} />
                {item.label}
              </NavLink>
            ))}
          </div>
        </div>
      ))}
    </nav>
  );
}

function UserBox({ name, subtitle }: { name: string; subtitle: string }) {
  const logout = useLogout();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  return (
    <div className="flex items-center gap-3 border-t border-white/15 pt-4">
      <span className="grid size-9 shrink-0 place-items-center rounded-full border-2 border-navy bg-amber text-sm font-bold text-navy">
        {name.trim().charAt(0).toUpperCase() || "?"}
      </span>
      <div className="min-w-0 flex-1 leading-tight">
        <p className="truncate text-sm font-bold text-white">{name}</p>
        <p className="truncate text-xs text-white/70">{subtitle}</p>
      </div>
      <button
        type="button"
        disabled={busy}
        onClick={async () => {
          setBusy(true);
          await logout().catch(() => {});
          navigate("/entrar", { replace: true });
        }}
        className="grid size-9 place-items-center rounded-lg text-white/80 hover:bg-white/10 hover:text-white"
        aria-label="Sair"
        title="Sair"
      >
        <LogOut className="size-[18px]" />
      </button>
    </div>
  );
}

export function Shell({
  sections,
  user,
  admin,
  banner,
  footerNote,
  children,
}: {
  sections: NavSection[];
  user: { name: string; subtitle: string };
  admin?: boolean;
  banner?: ReactNode;
  footerNote?: string;
  children: ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const home = admin ? "/admin" : "/app";

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  useEffect(() => {
    document.body.style.overflow = open ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [open]);

  const sidebar = (onNavigate?: () => void) => (
    <>
      <div className="flex-1">
        <NavLinks sections={sections} onNavigate={onNavigate} />
      </div>
      {footerNote && <p className="mt-6 mb-3 px-3 font-mono text-[10px] tracking-[0.16em] text-white/50 uppercase">{footerNote}</p>}
      <div className="mt-6">
        <UserBox name={user.name} subtitle={user.subtitle} />
      </div>
    </>
  );

  return (
    <div className="flex min-h-dvh flex-col">
      {banner}
      <div className="flex flex-1">
        <aside className="sticky top-0 hidden h-dvh w-[250px] shrink-0 flex-col overflow-y-auto bg-navy px-4 pt-6 pb-5 lg:flex">
          <Link to={home} className="mb-7 px-2">
            <Logo admin={admin} />
          </Link>
          {sidebar()}
        </aside>

        <div className="flex min-w-0 flex-1 flex-col">
          <header className="sticky top-0 z-30 flex h-16 items-center justify-between bg-navy px-4 lg:hidden">
            <Link to={home}>
              <Logo admin={admin} />
            </Link>
            <button
              type="button"
              onClick={() => setOpen(true)}
              className="grid size-11 place-items-center rounded-lg text-white hover:bg-white/10"
              aria-label="Abrir menu"
              aria-expanded={open}
            >
              <Menu className="size-6" />
            </button>
          </header>

          <main className="mx-auto w-full max-w-[1180px] flex-1 px-4 pt-6 pb-16 sm:px-8 lg:px-14 lg:pt-11">{children}</main>
        </div>
      </div>

      {open && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-navy/60" onClick={() => setOpen(false)} />
          <aside className="absolute inset-y-0 left-0 flex w-[min(300px,86vw)] flex-col overflow-y-auto bg-navy px-4 pt-4 pb-5 shadow-2xl">
            <div className="mb-6 flex items-center justify-between px-2">
              <Logo admin={admin} />
              <button type="button" onClick={() => setOpen(false)} className="grid size-10 place-items-center rounded-lg text-white hover:bg-white/10" aria-label="Fechar menu">
                <X className="size-6" />
              </button>
            </div>
            {sidebar(() => setOpen(false))}
          </aside>
        </div>
      )}
    </div>
  );
}
