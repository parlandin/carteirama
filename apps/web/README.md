# Carteirama · web

Front-end em React 19 + Vite + Tailwind 4, com React Router, TanStack Query e ícones Lucide.

## Rodando

No Docker (recomendado), pela raiz do repositório: `make dev` e acesse http://localhost:8080. Depois de mudar dependências, rode `make dev-build`.

Fora do Docker, com a API já rodando atrás do Nginx em `localhost:8080`:

```bash
yarn install
VITE_API_PROXY=http://localhost:8080 yarn dev
```

## Estrutura

```
src/
├── components/   # ui.tsx (botões, cards, campos, status...), Shell (sidebar/menu mobile), modais
├── layouts/      # AppLayout, AdminLayout e GuestOnly (guardas de rota)
├── lib/          # api (fetch + erros), auth, queries (TanStack Query), format, tipos
└── pages/
    ├── Landing.tsx
    ├── auth/     # entrar, criar conta, esqueci/redefinir senha
    ├── app/      # grupos, clientes, cobranças, exportar, assinatura, meus dados
    └── admin/    # visão geral, contas, pagamentos, planos (carregadas sob demanda)
```

## Rotas

| Rota | Tela |
| --- | --- |
| `/` | Landing page |
| `/entrar`, `/criar-conta`, `/esqueci-a-senha` | Acesso |
| `/reset-password?token=` | Nova senha (link enviado por e-mail pela API) |
| `/app` | Grupos de clientes (início) |
| `/app/grupos/novo`, `/app/grupos/:id`, `/app/grupos/:id/editar` | Grupos |
| `/app/clientes`, `/app/clientes/novo`, `/app/clientes/:id`, `/app/clientes/:id/editar` | Clientes |
| `/app/cobrancas` | Cobranças do mês |
| `/app/exportar` | Exportar planilha |
| `/app/assinatura`, `/app/conta` | Assinatura e dados da conta |
| `/admin`, `/admin/contas`, `/admin/contas/:id`, `/admin/pagamentos`, `/admin/planos` | Painel admin |

O mês selecionado fica na URL (`?mes=2026-10`). As cobranças do mês corrente são geradas automaticamente ao abrir Grupos ou Cobranças (a geração na API é idempotente).
