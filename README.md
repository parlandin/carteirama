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
├── docker-compose.yml        # base: api + db
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
- Postgres: `localhost:5432`
- A API roda com `mvn spring-boot:run` e devtools: recompile na IDE (o `target/` é montado no container) para reiniciar.
- Depois de mudar dependências do front, rode `make dev-build`: o `node_modules` do container fica num volume anônimo, que só `--renew-anon-volumes` recria (`--build` sozinho mantém o antigo).

## Migrations

O schema é versionado com Flyway, e as migrations rodam quando a API sobe. O Hibernate só valida (`ddl-auto: validate`), então toda mudança no banco precisa de uma migration.

Crie os arquivos em `apps/api/src/main/resources/db/migration/`, no formato `V<número>__<descricao>.sql` (ex.: `V2__create_users.sql`). Não edite uma migration que já rodou: o Flyway compara o checksum e a API deixa de subir. Para corrigir, crie uma nova.
