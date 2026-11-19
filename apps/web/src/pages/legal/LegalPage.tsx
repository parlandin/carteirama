import { ArrowLeft } from "lucide-react";
import { useEffect, type ReactNode } from "react";
import { Link } from "react-router";
import { Eyebrow, Logo } from "../../components/ui";

export interface LegalSection {
  id: string;
  title: string;
  body: ReactNode;
}

/** Layout das páginas legais: cabeçalho, índice (desktop) e seções numeradas. */
export function LegalPage({ eyebrow, title, updatedAt, intro, sections, other }: {
  eyebrow: string;
  title: string;
  updatedAt: string;
  intro: ReactNode;
  sections: LegalSection[];
  other: { to: string; label: string };
}) {
  useEffect(() => {
    document.title = `${title} · Carteirama`;
    window.scrollTo(0, 0);
    return () => {
      document.title = "Carteirama";
    };
  }, [title]);

  return (
    <div className="min-h-dvh bg-paper">
      <header className="bg-navy px-4 py-5 sm:px-8">
        <div className="mx-auto flex max-w-[1100px] items-center justify-between gap-4">
          <Link to="/" aria-label="Carteirama, início">
            <Logo />
          </Link>
          <Link to={other.to} className="hidden font-mono text-xs tracking-wider text-white/80 uppercase hover:text-amber sm:block">
            [{other.label}]
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-[1100px] px-4 pt-8 pb-20 sm:px-8 sm:pt-12">
        <Link to="/" className="mb-8 inline-flex items-center gap-2 font-mono text-xs tracking-widest text-muted uppercase hover:text-ink">
          <ArrowLeft className="size-4" />[Início]
        </Link>
        <Eyebrow className="mb-2">{eyebrow}</Eyebrow>
        <h1 className="text-[36px] leading-[1.05] font-extrabold tracking-tight sm:text-[52px]">{title}</h1>
        <p className="mt-3 font-mono text-xs text-muted">Última atualização: {updatedAt}</p>

        <div className="mt-10 grid gap-10 lg:grid-cols-[240px_minmax(0,1fr)]">
          <nav aria-label="Índice" className="hidden lg:block">
            <div className="sticky top-6 rounded-2xl border-2 border-navy bg-white p-5 shadow-hard-sm">
              <p className="mb-3 font-mono text-[11px] tracking-[0.14em] text-muted uppercase">[Índice]</p>
              <ol className="flex flex-col gap-2 text-sm">
                {sections.map((s, i) => (
                  <li key={s.id}>
                    <a href={`#${s.id}`} className="flex gap-2 hover:text-blue">
                      <span className="font-mono text-xs text-blue">{String(i + 1).padStart(2, "0")}</span>
                      {s.title}
                    </a>
                  </li>
                ))}
              </ol>
            </div>
          </nav>

          <article className="rounded-2xl border-2 border-navy bg-white p-5 shadow-hard sm:p-10">
            <div className="text-[16px] leading-relaxed text-ink/90">{intro}</div>
            {sections.map((s, i) => (
              <section key={s.id} id={s.id} className="mt-10 scroll-mt-6 border-t-2 border-navy pt-8">
                <h2 className="flex items-baseline gap-3 text-2xl font-bold">
                  <span className="font-mono text-sm text-blue">{String(i + 1).padStart(2, "0")}</span>
                  {s.title}
                </h2>
                <div className="legal-body mt-4 flex flex-col gap-3 text-[16px] leading-relaxed text-ink/90 [&_li]:ml-5 [&_li]:list-disc [&_li]:pl-1 [&_ul]:flex [&_ul]:flex-col [&_ul]:gap-1.5">
                  {s.body}
                </div>
              </section>
            ))}
          </article>
        </div>

        <footer className="mt-12 flex flex-col gap-3 font-mono text-[11px] tracking-wider text-muted uppercase sm:flex-row sm:justify-between">
          <span>Carteirama · um sistema da suíte Parlandim TI</span>
          <span className="flex gap-4">
            <Link to="/termos" className="hover:text-ink">[Termos de uso]</Link>
            <Link to="/privacidade" className="hover:text-ink">[Privacidade]</Link>
          </span>
        </footer>
      </main>
    </div>
  );
}
