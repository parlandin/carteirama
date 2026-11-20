# carteirama

Monorepo com o front (React + Vite) e a API (Spring Boot), orquestrados com Docker Compose e Nginx.

## Estrutura

```
carteirama/
├── apps/
│   ├── web/                  # React (Vite)
│   └── api/                  # Spring Boot (Java 21)
├── infra/
│   └── nginx/conf.d/
│       ├── default.dev.conf  # / → Vite (HMR), /api → api
│       └── default.prod.conf # / → build estático, /api → api
├── docker-compose.yml        # base: api + db + databasus
├── docker-compose.dev.yml    # dev: nginx, web (Vite), api com volume, adminer
├── docker-compose.prod.yml   # prod: web (Nginx + build), restart, limites
├── .env.example
└── Makefile
```

## Como o tráfego flui

```
Navegador → Nginx (:8080)
              ├── /       → React (Vite em dev, build estático em prod)
              └── /api/*  → api:8080 → db (rede interna)
```

O Nginx é o único serviço exposto em prod. Front e API ficam na mesma origem, então não há CORS. O prefixo `/api` é mantido até a API, e a versão fica no código (`/api/v1/...`).

## Rodando

```bash
cp .env.example .env

make dev    # docker compose -f docker-compose.yml -f docker-compose.dev.yml up
make prod   # docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Sem `make` (Windows), rode os comandos `docker compose` acima diretamente.

Em dev:

- App: http://localhost:8080
- Adminer: http://localhost:8081 (servidor `db`)
- Databasus (backups do Postgres): http://localhost:4005
- Postgres: `localhost:5432`
- A API roda com `mvn spring-boot:run` e devtools: recompile na IDE (o `target/` é montado no container) para reiniciar.
- Depois de mudar dependências do front, rode `make dev-build`: o `node_modules` do container fica num volume anônimo, que só `--renew-anon-volumes` recria (`--build` sozinho mantém o antigo).

## Segurança das requisições

As escritas (POST, PATCH, DELETE) exigem o token CSRF: a API grava o cookie `XSRF-TOKEN` em `GET /api/v1/auth/csrf` e o front devolve o valor no cabeçalho `X-XSRF-TOKEN`. Requisições com cabeçalho `Origin` de outro site são recusadas; só passam `FRONTEND_URL`, as origens de `CORS_ALLOWED_ORIGINS` e o próprio endereço da API. Webhooks ficam fora do CSRF e são validados pela assinatura.

## Front e API em domínios separados (opcional)

O padrão é tudo na mesma origem, atrás do Nginx. Para publicar o front como site estático e a API em outro subdomínio do **mesmo domínio** (ex.: `app.seudominio.com` e `api.seudominio.com`):

- API: `FRONTEND_URL=https://app.seudominio.com`, `CORS_ALLOWED_ORIGINS=https://app.seudominio.com` (aceita várias, separadas por vírgula) e `CSRF_COOKIE_DOMAIN=seudominio.com`, para o front conseguir ler o cookie do token.
- Front (no build): `VITE_API_URL=https://api.seudominio.com/api/v1`. O host precisa redirecionar rotas desconhecidas para `index.html`.
- Use um domínio próprio. Nos domínios padrão dos hosts (`*.onrender.com`, `*.up.railway.app`…) front e API viram sites diferentes e os navegadores bloqueiam o cookie de sessão.

Sem essas variáveis, tudo continua como antes.

## Pagamentos (gateway)

A cobrança da assinatura passa pela interface `PaymentGateway`. Cada gateway é um provider isolado em `apps/api/src/main/java/com/parlandim/carteirama/billing/provider/<nome>/`, ligado só quando `PAYMENT_PROVIDER=<nome>`. Com `PAYMENT_PROVIDER=none` (padrão), nenhum gateway fica ativo e o pagamento retorna "Gateway de pagamento não configurado".

Para trocar de gateway, mude `PAYMENT_PROVIDER`. Para remover um, apague a pasta do provider. Para adicionar outro, crie uma pasta nova com uma classe `@Configuration` + `@ConditionalOnProperty(name = "carteirama.payment.provider", havingValue = "<nome>")` que registre um `PaymentGateway` `@Primary`.

## Backups (Databasus)

O [Databasus](https://github.com/databasus/databasus) faz os backups agendados do Postgres. Ele sobe junto com o resto (`make dev` ou `make prod`) e guarda a configuração no volume `databasus-data`.

1. Abra http://localhost:4005 e crie a conta: a primeira conta vira administradora.
2. Em **New Database**, conecte no Postgres com host `db`, porta `5432` e o banco, usuário e senha do `.env`.
3. Escolha o agendamento, o destino (disco local, S3, R2, SFTP…) e a retenção.

Em prod a porta só escuta em `127.0.0.1`. Acesse por um túnel SSH (`ssh -L 4005:localhost:4005 servidor`). Prefira um destino fora do servidor e guarde também a chave de criptografia do Databasus, sem ela os backups não podem ser restaurados.

## Migrations

O schema é versionado com Flyway, e as migrations rodam quando a API sobe. O Hibernate só valida (`ddl-auto: validate`), então toda mudança no banco precisa de uma migration.

Crie os arquivos em `apps/api/src/main/resources/db/migration/`, no formato `V<número>__<descricao>.sql` (ex.: `V2__create_users.sql`). Não edite uma migration que já rodou: o Flyway compara o checksum e a API deixa de subir. Para corrigir, crie uma nova.
