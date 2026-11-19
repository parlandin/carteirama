import {
  ArrowRight,
  Check,
  CircleCheck,
  Layers,
  Menu,
  ReceiptText,
  Users,
  X,
} from "lucide-react";
import { useState, type ReactNode } from "react";
import { Link } from "react-router";
import { Eyebrow, GithubIcon, LinkButton, Logo } from "../components/ui";
import { useMe } from "../lib/auth";
import { cx } from "../lib/cx";

const REPO_URL = "https://github.com/parlandin/carteirama";

const SAMPLE = [
  {
    s: "AS",
    name: "Assinatura SaaS",
    value: "R$ 2.082,00",
    color: "#2553E6",
    text: "#fff",
  },
  {
    s: "PX",
    name: "Projeto X",
    value: "R$ 3.650,00",
    color: "#FBAF24",
    text: "#111b35",
  },
  {
    s: "HV",
    name: "Hospedagem e VPS",
    value: "R$ 540,00",
    color: "#167A3C",
    text: "#fff",
  },
  {
    s: "WA",
    name: "WhatsApp com IA",
    value: "R$ 2.270,00",
    color: "#111B35",
    text: "#fff",
  },
  {
    s: "MS",
    name: "Manutenção de sites",
    value: "R$ 180,00",
    color: "#6D3CE0",
    text: "#fff",
  },
];

const NAV = [
  ["#recursos", "Recursos"],
  ["#telas", "Telas"],
  ["#como-usar", "Como usar"],
  ["#planos", "Planos"],
];

function Header() {
  const [open, setOpen] = useState(false);
  const { data: me } = useMe();
  return (
    <header className="sticky top-0 z-30 border-b-2 border-navy/10 bg-paper/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-[1240px] items-center justify-between px-4 sm:h-20 sm:px-8">
        <Link to="/" aria-label="Carteirama, início">
          <Logo dark={false} />
        </Link>
        <nav className="hidden items-center gap-7 font-mono text-xs tracking-wider uppercase md:flex">
          {NAV.map(([href, label]) => (
            <a key={href} href={href} className="hover:text-blue">
              [{label}]
            </a>
          ))}
        </nav>
        <div className="hidden items-center gap-3 md:flex">
          {me ? (
            <LinkButton
              to={me.account ? "/app" : "/admin"}
              variant="primary"
              size="sm"
              iconRight={ArrowRight}
            >
              Abrir o app
            </LinkButton>
          ) : (
            <>
              <Link
                to="/entrar"
                className="px-2 text-sm font-bold hover:text-blue"
              >
                Entrar
              </Link>
              <a
                href={REPO_URL}
                target="_blank"
                rel="noreferrer"
                className="inline-flex h-9 items-center gap-2 rounded-lg border-2 border-navy bg-green px-3 text-sm font-bold text-white shadow-hard-sm"
              >
                <GithubIcon className="size-4" /> Ver no GitHub
              </a>
            </>
          )}
        </div>
        <button
          type="button"
          className="grid size-11 place-items-center rounded-lg md:hidden"
          onClick={() => setOpen((v) => !v)}
          aria-label="Menu"
          aria-expanded={open}
        >
          {open ? <X className="size-6" /> : <Menu className="size-6" />}
        </button>
      </div>
      {open && (
        <div className="border-t-2 border-navy bg-paper px-4 pt-3 pb-5 md:hidden">
          <nav className="flex flex-col">
            {NAV.map(([href, label]) => (
              <a
                key={href}
                href={href}
                onClick={() => setOpen(false)}
                className="border-b border-line py-3 font-mono text-sm tracking-wider uppercase"
              >
                [{label}]
              </a>
            ))}
          </nav>
          <div className="mt-4 grid grid-cols-2 gap-3">
            {me ? (
              <LinkButton
                to={me.account ? "/app" : "/admin"}
                variant="primary"
                className="col-span-2"
              >
                Abrir o app
              </LinkButton>
            ) : (
              <>
                <LinkButton to="/entrar">Entrar</LinkButton>
                <LinkButton to="/criar-conta" variant="primary">
                  Criar conta
                </LinkButton>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
}

function HeroCard() {
  return (
    <div className="relative mx-auto w-full max-w-[540px] pt-6">
      <span className="absolute -top-1 left-2 z-10 -rotate-6 rounded-md border-2 border-navy bg-amber px-3 py-1 font-mono text-[11px] font-bold tracking-widest text-navy uppercase sm:left-0">
        Da suíte Parlandim TI
      </span>
      <div className="rotate-[-2deg] overflow-hidden rounded-2xl border-2 border-navy bg-white shadow-hard">
        <div className="flex items-center gap-2 border-b-2 border-navy bg-navy px-4 py-2.5">
          <span className="size-2.5 rounded-full bg-white/40" />
          <span className="size-2.5 rounded-full bg-white/40" />
          <span className="mx-auto rounded bg-white/10 px-3 py-0.5 font-mono text-[10px] text-white/80">
            app.carteirama.com.br
          </span>
        </div>
        <p className="border-b-2 border-navy px-4 py-2 font-mono text-[10px] tracking-widest text-muted uppercase">
          Grupos de clientes · out 2026
        </p>
        <ul>
          {SAMPLE.map((g) => (
            <li
              key={g.s}
              className="flex items-center gap-3 border-b-2 border-navy px-4 py-2.5"
            >
              <span
                className="grid size-8 place-items-center rounded-md border-2 border-navy font-mono text-[11px] font-bold"
                style={{ background: g.color, color: g.text }}
              >
                {g.s}
              </span>
              <span className="flex-1 truncate text-sm font-semibold">
                {g.name}
              </span>
              <span className="text-sm font-extrabold">{g.value}</span>
            </li>
          ))}
        </ul>
        <div className="flex items-center justify-between bg-navy px-4 py-3.5">
          <span className="font-mono text-[10px] font-bold tracking-widest text-amber uppercase">
            Total a receber
          </span>
          <span className="text-xl font-extrabold text-amber">R$ 8.722,00</span>
        </div>
      </div>
      <p className="mt-5 mr-2 text-right font-hand text-2xl text-muted">
        todo mês, na sua tela
      </p>
    </div>
  );
}

function Section({
  id,
  className,
  children,
}: {
  id?: string;
  className?: string;
  children: ReactNode;
}) {
  return (
    <section
      id={id}
      className={cx("scroll-mt-20 px-4 py-16 sm:px-8 sm:py-24", className)}
    >
      <div className="mx-auto max-w-[1240px]">{children}</div>
    </section>
  );
}

export default function Landing() {
  return (
    <div className="min-h-dvh bg-paper">
      <Header />

      {/* Hero */}
      <section className="px-4 pt-10 pb-16 sm:px-8 sm:pt-16 sm:pb-24">
        <div className="mx-auto grid max-w-[1240px] items-center gap-12 lg:grid-cols-2">
          <div>
            <Eyebrow className="mb-4">
              // Suíte Parlandim TI · Gestão de clientes
            </Eyebrow>
            <h1 className="text-[44px] leading-[0.98] font-extrabold tracking-tight sm:text-[64px] lg:text-[76px]">
              Saiba quanto vai receber este mês.
            </h1>
            <p className="mt-6 max-w-xl text-lg text-muted">
              Carteirama organiza seus clientes em grupos, soma a assinatura de
              cada um e mostra o total do mês em uma tela só. Um sistema da
              suíte Parlandim TI: use na nossa nuvem ou instale no seu servidor.
            </p>
            <svg
              viewBox="0 0 160 12"
              className="mt-5 h-3 w-40 text-amber"
              aria-hidden="true"
            >
              <path
                d="M2 8 Q 22 0 42 8 T 82 8 T 122 8 T 158 6"
                fill="none"
                stroke="currentColor"
                strokeWidth="3"
                strokeLinecap="round"
              />
            </svg>
            <div className="mt-7 flex flex-wrap gap-3">
              <LinkButton
                to="/criar-conta"
                variant="primary"
                size="lg"
                iconRight={ArrowRight}
              >
                Testar 7 dias grátis
              </LinkButton>
              <a
                href="#planos"
                className="inline-flex h-13 items-center rounded-xl border-2 border-navy bg-white px-5 font-mono text-sm font-bold tracking-wider uppercase shadow-hard-sm"
              >
                [Ver planos]
              </a>
            </div>
            <div className="mt-7 flex flex-wrap gap-2">
              {[
                "Suíte Parlandim TI",
                "Na nuvem ou no seu servidor",
                "Plano comunitário grátis",
              ].map((t) => (
                <span
                  key={t}
                  className="rounded-full border-2 border-navy px-3 py-1 font-mono text-[11px]"
                >
                  {t}
                </span>
              ))}
            </div>
          </div>
          <HeroCard />
        </div>
      </section>

      {/* Recursos */}
      <Section id="recursos">
        <div className="mb-8 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <Eyebrow tone="muted">[Em foco]</Eyebrow>
          <p className="font-mono text-xs text-muted">
            Três ideias simples. Juntas, cobrem o mês inteiro.
          </p>
        </div>
        <div className="grid gap-6 md:grid-cols-3">
          {[
            {
              icon: Layers,
              color: "#2553E6",
              title: "Grupos de clientes",
              text: "Crie categorias como Assinatura SaaS ou Projeto X. Cada grupo mostra quanto rende por mês.",
            },
            {
              icon: Users,
              color: "#FBAF24",
              title: "Cadastro completo de clientes",
              text: "CPF ou CNPJ, e-mail, telefone, grupo, valor da assinatura e dia de vencimento em um só formulário.",
            },
            {
              icon: ReceiptText,
              color: "#167A3C",
              title: "Total do mês",
              text: "A tela inicial soma todos os grupos e mostra o valor que você vai receber.",
            },
          ].map((f) => (
            <div
              key={f.title}
              className="rounded-2xl border-2 border-navy bg-white p-6 shadow-hard"
            >
              <span
                className="grid size-11 place-items-center rounded-xl border-2 border-navy"
                style={{ background: f.color }}
              >
                <f.icon
                  className="size-5"
                  color={f.color === "#FBAF24" ? "#111b35" : "#fff"}
                />
              </span>
              <h3 className="mt-5 text-xl font-bold">{f.title}</h3>
              <p className="mt-2 text-muted">{f.text}</p>
            </div>
          ))}
        </div>

        <div className="mt-20 grid gap-12 lg:grid-cols-2">
          <div>
            <Eyebrow tone="muted" className="mb-3">
              [Para quem cobra todo mês]
            </Eyebrow>
            <h2 className="text-4xl leading-tight font-extrabold tracking-tight sm:text-5xl">
              Chega de planilha espalhada.
            </h2>
            <p className="mt-4 max-w-md text-muted">
              Cada cliente em um grupo, cada grupo com seu total. Você abre o
              sistema e já sabe o que entra no mês.
            </p>
            <LinkButton to="/criar-conta" variant="primary" className="mt-7">
              Começar agora
            </LinkButton>
          </div>
          <ol className="border-t-2 border-navy">
            {[
              [
                "Total mensal por grupo",
                "Veja quanto cada grupo rende e a soma de todos eles, mês a mês.",
              ],
              [
                "Cobranças do mês",
                "Pago, a vencer ou em atraso, com um clique para marcar cada pagamento.",
              ],
              [
                "Feito para o Brasil",
                "CPF, CNPJ, telefones e valores em reais.",
              ],
              [
                "Na nossa nuvem ou na sua",
                "Use o plano Cloud, com infraestrutura e suporte da Parlandim TI, ou instale por conta própria.",
              ],
            ].map(([t, d], i) => (
              <li key={t} className="flex gap-5 border-b-2 border-navy py-5">
                <span className="font-mono text-xs text-blue">0{i + 1}</span>
                <div>
                  <p className="font-bold">{t}</p>
                  <p className="mt-1 text-sm text-muted">{d}</p>
                </div>
              </li>
            ))}
          </ol>
        </div>
      </Section>

      {/* Por dentro */}
      <Section id="telas" className="bg-navy text-white">
        <div className="grid items-center gap-12 lg:grid-cols-2">
          <div>
            <Eyebrow tone="amber" className="mb-3">
              [Por dentro]
            </Eyebrow>
            <h2 className="text-4xl leading-tight font-extrabold tracking-tight sm:text-5xl">
              Cada grupo com seu total. No final, a soma.
            </h2>
            <p className="mt-4 max-w-md text-white/80">
              Clique em um grupo e veja os clientes dele, a assinatura que cada
              um paga e o total do grupo.
            </p>
            <div className="mt-6 flex flex-wrap items-center gap-2 font-mono text-[11px]">
              {["Grupos", "Clientes", "Cobranças"].map((t) => (
                <span key={t} className="flex items-center gap-2">
                  <span className="rounded-full border-2 border-white px-3 py-1">
                    {t}
                  </span>
                  <span className="text-white/50">›</span>
                </span>
              ))}
              <span className="rounded-full border-2 border-amber px-3 py-1 text-amber">
                Total do mês
              </span>
            </div>
            <ul className="mt-8 flex flex-col gap-5">
              {[
                [
                  "Tela inicial com todos os grupos",
                  "Valor mensal de cada grupo e o total a receber no fim da lista.",
                ],
                [
                  "Lista de clientes por grupo",
                  "Quem paga, contato, vencimento, situação e o valor da assinatura.",
                ],
                [
                  "Cadastro de cliente e de grupo",
                  "Formulários com todos os campos, do CNPJ à cor do grupo.",
                ],
                [
                  "Assinatura e faturas da sua conta",
                  "Plano, uso e histórico de faturas da Carteirama.",
                ],
              ].map(([t, d]) => (
                <li key={t} className="flex gap-3">
                  <CircleCheck className="mt-0.5 size-5 shrink-0 fill-amber text-navy" />
                  <div>
                    <p className="font-bold">{t}</p>
                    <p className="text-sm text-white/75">{d}</p>
                  </div>
                </li>
              ))}
            </ul>
          </div>
          <div className="overflow-hidden rounded-2xl border-2 border-white bg-white text-ink shadow-hard-amber">
            <div className="flex items-center gap-3 border-b-2 border-navy p-4">
              <span className="grid size-10 place-items-center rounded-lg border-2 border-navy bg-blue font-mono text-sm font-bold text-white">
                AS
              </span>
              <div className="flex-1">
                <p className="font-bold">Assinatura SaaS</p>
                <p className="font-mono text-[10px] text-muted">6 CLIENTES</p>
              </div>
              <p className="text-lg font-extrabold">R$ 2.082,00</p>
            </div>
            <div className="flex justify-between border-b-2 border-navy bg-paper-2 px-4 py-1.5 font-mono text-[10px] tracking-widest text-muted uppercase">
              <span>Cliente</span>
              <span>Assinatura</span>
            </div>
            {[
              ["Studio Aurora Design", "R$ 297,00"],
              ["Clínica Vita Odonto", "R$ 497,00"],
              ["Rocha & Lima Advocacia", "R$ 297,00"],
              ["Mercado Bom Preço", "R$ 197,00"],
              ["Imobiliária Casa Viva", "R$ 497,00"],
              ["Auto Center Paulista", "R$ 297,00"],
            ].map(([n, v]) => (
              <div
                key={n}
                className="flex justify-between border-b-2 border-navy px-4 py-2.5 text-sm"
              >
                <span className="font-semibold">{n}</span>
                <span className="font-bold">{v}</span>
              </div>
            ))}
            <div className="flex items-center justify-between bg-navy px-4 py-4">
              <span className="font-mono text-[10px] font-bold tracking-widest text-amber uppercase">
                Total do grupo
              </span>
              <span className="text-xl font-extrabold text-amber">
                R$ 2.082,00
              </span>
            </div>
          </div>
        </div>
      </Section>

      {/* Como usar */}
      <Section id="como-usar">
        <Eyebrow tone="muted" className="mb-3">
          [Como usar]
        </Eyebrow>
        <h2 className="text-4xl font-extrabold tracking-tight sm:text-5xl">
          Da conta ao total do mês.
        </h2>
        <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {[
            [
              "Crie sua conta",
              "Comece no plano Cloud em minutos, ou instale o código no seu servidor.",
            ],
            [
              "Crie os grupos",
              "Separe seus clientes por assinatura, projeto ou serviço.",
            ],
            [
              "Cadastre os clientes",
              "Preencha os dados e o valor mensal de cada assinatura.",
            ],
            [
              "Acompanhe o mês",
              "Veja o total a receber e marque o que já foi pago.",
            ],
          ].map(([t, d], i) => (
            <div
              key={t}
              className={cx(
                "rounded-2xl border-2 border-navy bg-white p-6 shadow-hard",
                i % 2 === 0 ? "sm:-rotate-1" : "sm:rotate-1",
              )}
            >
              <p className="text-4xl font-extrabold text-blue">{i + 1}</p>
              <p className="mt-3 text-lg font-bold">{t}</p>
              <p className="mt-1 text-sm text-muted">{d}</p>
            </div>
          ))}
        </div>
      </Section>

      {/* Planos */}
      <Section id="planos" className="pt-0 sm:pt-0">
        <Eyebrow tone="muted" className="mb-3">
          [Planos]
        </Eyebrow>
        <h2 className="text-4xl font-extrabold tracking-tight sm:text-5xl">
          Escolha como usar.
        </h2>
        <p className="mt-3 max-w-xl text-muted">
          Instale por conta própria, de graça, ou deixe a infraestrutura e o
          suporte com a Parlandim TI. O sistema é o mesmo nos dois planos.
        </p>
        <div className="mt-10 grid gap-8 lg:grid-cols-2">
          <div className="flex flex-col rounded-2xl border-2 border-navy bg-white p-6 shadow-hard sm:p-8">
            <Eyebrow>[Comunitário]</Eyebrow>
            <p className="mt-3 text-5xl font-extrabold">
              R$ 0{" "}
              <span className="font-mono text-xs font-medium text-muted">
                para sempre
              </span>
            </p>
            <p className="mt-4 text-muted">
              Você instala, hospeda e mantém. Ideal para quem já tem servidor e
              quer controle total do código e dos dados.
            </p>
            <ul className="mt-6 flex flex-col gap-3 border-t-2 border-navy pt-6 text-sm">
              {[
                "Código aberto no GitHub",
                "Instalação por sua conta, em servidor próprio",
                "Atualizações pelo GitHub, quando você decidir aplicar",
                "Backups, segurança e domínio por sua conta",
                "Suporte apenas pelo GitHub: issues e discussões da comunidade",
              ].map((t) => (
                <li key={t} className="flex gap-2.5">
                  <CircleCheck className="size-5 shrink-0" /> {t}
                </li>
              ))}
            </ul>
            <a
              href={REPO_URL}
              target="_blank"
              rel="noreferrer"
              className="mt-8 inline-flex h-12 items-center justify-center gap-2 rounded-xl border-2 border-navy bg-white font-bold shadow-hard-sm hover:bg-paper-2"
            >
              <GithubIcon className="size-4" /> Ver no GitHub
            </a>
          </div>
          <div className="relative flex flex-col rounded-2xl border-2 border-navy bg-navy p-6 text-white shadow-hard-amber sm:p-8">
            <span className="absolute -top-3.5 right-6 rotate-3 rounded-md border-2 border-navy bg-amber px-3 py-1 font-mono text-[10px] font-bold tracking-widest text-navy uppercase">
              Recomendado
            </span>
            <Eyebrow tone="amber">[Cloud]</Eyebrow>
            <p className="mt-3 text-5xl font-extrabold">
              R$ 24,99{" "}
              <span className="font-mono text-xs font-medium text-white/70">
                por mês
              </span>
            </p>
            <p className="mt-4 text-white/80">
              Rodamos tudo na infraestrutura da Parlandim TI e cuidamos do
              suporte. Você só cadastra clientes e acompanha o mês.
            </p>
            <ul className="mt-6 flex flex-col gap-3 border-t-2 border-white/20 pt-6 text-sm">
              {[
                "Pronto em minutos, sem instalar nada",
                "Hospedagem na infraestrutura da Parlandim TI, com backups diários",
                "Atualizações e segurança por nossa conta",
                "Suporte humano por WhatsApp e e-mail",
                "7 dias de teste para conhecer o sistema",
              ].map((t) => (
                <li key={t} className="flex gap-2.5">
                  <Check
                    className="size-5 shrink-0 rounded-full bg-amber p-0.5 text-navy"
                    strokeWidth={3}
                  />{" "}
                  {t}
                </li>
              ))}
            </ul>
            <LinkButton
              to="/criar-conta"
              variant="primary"
              size="lg"
              iconRight={ArrowRight}
              className="mt-8"
            >
              Começar teste de 7 dias
            </LinkButton>
          </div>
        </div>
      </Section>

      {/* Suíte */}
      <Section className="pt-0 sm:pt-0">
        <div className="grid gap-10 rounded-2xl border-2 border-navy bg-white p-6 shadow-hard sm:p-10 lg:grid-cols-2 lg:items-center">
          <div>
            <Eyebrow className="mb-3">[Feito por]</Eyebrow>
            <h2 className="text-4xl leading-tight font-extrabold tracking-tight">
              Um sistema da suíte Parlandim TI.
            </h2>
            <p className="mt-4 text-muted">
              A Parlandim TI cria sites, sistemas, automações, servidores e
              atendimento com IA para empresas brasileiras. A Carteirama é um
              dos sistemas da suíte, e o plano Cloud roda na mesma
              infraestrutura que mantém os nossos projetos.
            </p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {[
              ["Sites", "Páginas e lojas que apresentam sua empresa."],
              ["Sistemas", "Sistemas sob medida, como a Carteirama."],
              ["Automações", "Tarefas repetitivas rodando sozinhas."],
              ["Servidores e VPS", "Hospedagem e infraestrutura gerenciada."],
              ["E-mail corporativo", "Caixas com o domínio da sua empresa."],
              ["WhatsApp com IA", "Atendimento e vendas automatizados."],
            ].map(([t, d], i) => (
              <div
                key={t}
                className={cx(
                  "rounded-xl border-2 border-navy p-3 sm:p-4",
                  i === 1 ? "bg-amber" : "bg-paper-2",
                )}
              >
                <p className="font-mono text-[10px] text-muted">0{i + 1}</p>
                <p className="font-bold">{t}</p>
                <p className="mt-1 text-xs text-muted">{d}</p>
                {i === 1 && (
                  <span className="mt-2 inline-block rounded-full border-2 border-navy bg-white px-2 py-0.5 font-mono text-[9px] font-bold uppercase">
                    Você está aqui
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      </Section>

      {/* CTA */}
      <section className="bg-blue px-4 py-16 text-center text-white sm:py-24">
        <h2 className="mx-auto max-w-2xl text-4xl leading-tight font-extrabold tracking-tight sm:text-5xl">
          Pronto para ver o total do mês?
        </h2>
        <p className="mt-4 text-white/90">
          Crie sua conta e comece a cadastrar seus clientes hoje.
        </p>
        <div className="mt-8 flex flex-wrap justify-center gap-3">
          <LinkButton
            to="/criar-conta"
            variant="primary"
            iconRight={ArrowRight}
          >
            Criar conta
          </LinkButton>
          <a
            href="#planos"
            className="inline-flex h-11 items-center rounded-[10px] border-2 border-navy bg-white px-4 font-mono text-sm font-bold tracking-wider text-navy uppercase shadow-hard-sm"
          >
            [Ver planos]
          </a>
        </div>
      </section>

      <footer className="overflow-hidden px-4 pt-12 pb-8 sm:px-8">
        <div className="mx-auto max-w-[1240px]">
          <div className="flex flex-col gap-4 font-mono text-[11px] tracking-wider text-muted uppercase sm:flex-row sm:justify-between">
            <span>Carteirama · um sistema da suíte Parlandim TI</span>
            <nav className="flex flex-wrap gap-4">
              {NAV.map(([href, label]) => (
                <a key={href} href={href} className="hover:text-ink">
                  [{label}]
                </a>
              ))}
              <a
                href={REPO_URL}
                target="_blank"
                rel="noreferrer"
                className="hover:text-ink"
              >
                [GitHub]
              </a>
            </nav>
          </div>
          <p className="mt-8 text-[22vw] leading-[0.85] font-extrabold tracking-tighter lg:text-[200px]">
            Carteirama
          </p>
          <div className="mt-6 flex flex-col gap-2 text-xs text-muted sm:flex-row sm:justify-between">
            <p>
              © {new Date().getFullYear()} Parlandim TI. Carteirama é um sistema
              da suíte Parlandim TI.
            </p>
            <p className="flex gap-4">
              <Link to="/termos" className="underline hover:text-ink">
                Termos de uso
              </Link>
              <Link to="/privacidade" className="underline hover:text-ink">
                Política de privacidade
              </Link>
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
