import type { ReactNode } from "react";
import { Link } from "react-router";
import { Eyebrow, Logo } from "../../components/ui";

export function AuthFrame({
  eyebrow,
  headline,
  showcase,
  children,
}: {
  eyebrow: string;
  headline: ReactNode;
  showcase?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="flex min-h-dvh flex-col lg:flex-row">
      <aside className="relative flex flex-col bg-navy px-5 py-6 text-white sm:px-10 lg:min-h-dvh lg:w-[48%] lg:max-w-[740px] lg:px-[72px] lg:py-12">
        <Link to="/" className="self-start">
          <Logo />
        </Link>
        <div className="hidden flex-1 flex-col justify-center py-12 lg:flex">
          <Eyebrow tone="amber" className="mb-4 text-[13px]">
            {eyebrow}
          </Eyebrow>
          <h1 className="text-5xl leading-[1.02] font-extrabold tracking-tight xl:text-[60px]">{headline}</h1>
          {showcase && <div className="mt-9">{showcase}</div>}
        </div>
        <p className="hidden font-mono text-xs text-white/80 lg:block">
          <span className="mr-2 text-amber">*</span>um sistema da suíte Parlandim TI
        </p>
      </aside>
      <main className="flex flex-1 items-start justify-center px-4 py-8 sm:px-8 sm:py-12 lg:items-center">
        <div className="w-full max-w-[560px] rounded-2xl border-2 border-navy bg-white p-6 shadow-hard sm:p-9">{children}</div>
      </main>
    </div>
  );
}

export function GroupShowcase() {
  return (
    <div className="max-w-[420px]">
      <div className="-rotate-2 rounded-2xl border-2 border-navy bg-white px-5 py-2 text-ink shadow-hard-amber">
        <div className="flex items-center gap-3 border-b-2 border-navy py-3">
          <span className="grid size-10 place-items-center rounded-lg border-2 border-navy bg-blue font-mono text-sm font-bold text-white">AS</span>
          <span className="flex-1 text-lg font-bold">Assinatura SaaS</span>
          <span className="text-lg font-extrabold">R$ 2.082,00</span>
        </div>
        <div className="flex items-center gap-3 py-3">
          <span className="grid size-10 place-items-center rounded-lg border-2 border-navy bg-amber font-mono text-sm font-bold text-navy">PX</span>
          <span className="flex-1 text-lg font-bold">Projeto X</span>
          <span className="text-lg font-extrabold">R$ 3.650,00</span>
        </div>
      </div>
      <p className="mt-4 ml-6 -rotate-3 font-hand text-2xl text-white/85">grupo por grupo, mês a mês</p>
    </div>
  );
}
