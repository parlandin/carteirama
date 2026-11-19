import { LinkButton, Logo } from "../components/ui";

export default function NotFound() {
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-6 bg-navy px-6 text-center text-white">
      <Logo />
      <p className="font-mono text-sm tracking-widest text-amber">[404]</p>
      <h1 className="text-4xl font-extrabold">Página não encontrada</h1>
      <p className="max-w-md text-white/80">O endereço pode ter mudado ou o item foi arquivado.</p>
      <LinkButton to="/" variant="amber">
        Voltar ao início
      </LinkButton>
    </div>
  );
}
