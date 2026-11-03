# Carteirama: backend do MVP

Nov 1, 2026 · @Parlandim

## Visão geral

O MVP é uma API REST em JSON com PostgreSQL, cobrindo as telas já desenhadas: login e cadastro, grupos, clientes, cobranças, exportação, assinatura e painel admin. Uma conta tem exatamente 1 usuário e clientes ilimitados; existe um único plano pago (Cloud) com 14 dias de trial.

**Stack sugerida** (troque à vontade, as regras abaixo não dependem dela)

- Node + TypeScript (Fastify ou Express), validação com Zod
- PostgreSQL 15+, migrations versionadas (Prisma ou Drizzle)
- Gateway de pagamento com assinatura recorrente e webhook (ex.: Asaas ou Stripe)
- E-mail transacional para recuperar senha e avisar cobrança (ex.: Resend ou SES)

**Convenções da API**

- Base `/api/v1`, JSON em UTF-8, nomes de campo em `snake_case`
- IDs em UUID; datas em ISO 8601 UTC (`2026-11-01T21:30:00Z`); datas sem hora (vencimento) em `YYYY-MM-DD`
- Valores em **centavos, inteiro** (R$ 59,00 = `5900`), nunca decimal
- Listas paginadas: `?page=1&limit=20` (máx. 100), resposta `{ data: [], meta: { page, limit, total } }`
- Autenticação por cookie `httpOnly`, `Secure`, `SameSite=Lax` com sessão guardada no banco (expira em 30 dias; 12 horas sem "manter conectado")
- Senha com argon2id; limite de tentativas de login por IP e por e-mail
- **Isolamento entre contas:** toda consulta de rotas de usuário filtra por `account_id` da sessão. Recurso de outra conta responde `404`, não `403`, para não revelar que existe.
- Papéis: `user` (padrão) e `admin` (`users.is_admin = true`). Rotas `/admin/*` exigem admin.

## Banco de dados

12 tabelas. Toda tabela tem `id uuid` (PK), `created_at` e `updated_at` (timestamptz), omitidos abaixo. Tabelas das contas clientes carregam `account_id` e têm índice nele.

**Acesso e conta**

| Tabela | Campos principais | Regras |
| --- | --- | --- |
| `users` | `email` (citext), `password_hash`, `name`, `is_admin` bool padrão false | `email` único |
| `sessions` | `user_id`, `token_hash`, `expires_at`, `ip`, `user_agent` | apaga ao sair; job limpa expiradas |
| `password_resets` | `user_id`, `token_hash`, `expires_at`, `used_at` | válido por 1 hora, uso único |
| `accounts` | `user_id` (FK, único), `person_type` (`pf`\|`pj`), `document` (só dígitos), `legal_name`, `company_name` (só PJ), `phone`, `zip`, `address`, `city`, `state`, `status`, `trial_ends_at`, `suspended_at`, `cancelled_at`, `gateway_customer_id` | `document` único; `status` em `trial`, `active`, `suspended`, `cancelled` |

**Assinatura e pagamentos (SaaS)**

| Tabela | Campos principais | Regras |
| --- | --- | --- |
| `plans` | `code` (`cloud`), `name`, `price_cents`, `trial_days`, `active` | 1 plano pago; o admin edita o preço |
| `subscriptions` | `account_id` (único), `plan_code`, `price_cents`, `status`, `gateway_subscription_id`, `next_due_date`, `cancel_at_period_end` | guarda o preço contratado; mudar o plano não altera quem já assina |
| `invoices` | `account_id`, `number` (`FAT-1041`), `amount_cents`, `due_date`, `status` (`pending`\|`paid`\|`cancelled`), `paid_at`, `method` (`pix`\|`boleto`\|`card`), `gateway_charge_id` | `number` e `gateway_charge_id` únicos |
| `webhook_events` | `gateway_event_id`, `type`, `payload` jsonb, `processed_at` | `gateway_event_id` único, garante idempotência |
| `audit_log` | `actor_user_id`, `account_id`, `action`, `meta` jsonb | registra suspender, reativar, marcar como pago e mudança de preço |

**Dados do usuário (o produto)**

| Tabela | Campos principais | Regras |
| --- | --- | --- |
| `groups` | `account_id`, `name`, `color`, `notes`, `archived_at` | nome único por conta entre grupos ativos |
| `clients` | `account_id`, `group_id`, `name`, `document`, `email`, `phone`, `amount_cents`, `due_day` (1–28), `status` (`active`\|`paused`\|`archived`), `notes` | pertence a 1 grupo; sem limite de quantidade |
| `charges` | `account_id`, `client_id`, `reference_month` (date, dia 1), `amount_cents`, `due_date`, `status` (`pending`\|`paid`\|`cancelled`), `paid_at`, `note` | único por (`client_id`, `reference_month`) |

**Atraso é calculado, não gravado.** Uma cobrança ou fatura é "atrasada" quando `status = pending` e `due_date < hoje` (fuso America/Sao\_Paulo). A API devolve `late` no campo `display_status`. Assim nenhum job precisa atualizar milhares de linhas por dia.

**Índices além das PKs e únicos**

- `clients (account_id, group_id)` e `clients (account_id, status)`
- `charges (account_id, reference_month)` e `charges (account_id, status, due_date)`
- `invoices (account_id, due_date desc)` e `invoices (status, due_date)` para o painel admin
- `accounts (status)`

**Remoção.** Cliente e grupo são arquivados (`archived`), nunca apagados, para preservar o histórico de cobranças. Cancelar a conta mantém os dados por 90 dias e depois um job apaga tudo (definir com o jurídico).

## Rotas

Todas sob `/api/v1`. **Acesso:** P = público, U = usuário logado, A = admin. **Suspensa:** "ok" funciona com a conta suspensa; "bloqueia" responde `403 account_suspended` (regra na próxima seção).

**Autenticação e perfil**

| Método | Rota | O que faz | Acesso | Suspensa |
| --- | --- | --- | --- | --- |
| POST | `/auth/register` | Cria usuário e conta (PF ou PJ), inicia trial de 14 dias, abre sessão | P | n/a |
| POST | `/auth/login` | Valida e-mail e senha, abre sessão | P | ok |
| POST | `/auth/logout` | Encerra a sessão | U | ok |
| POST | `/auth/forgot-password` | Envia link de redefinição; resposta igual exista ou não o e-mail | P | n/a |
| POST | `/auth/reset-password` | Troca a senha com o token do e-mail | P | n/a |
| GET | `/me` | Usuário, conta, `is_admin`, `account.status` (alimenta a faixa de conta suspensa e o item "Ir para painel admin") | U | ok |
| PATCH | `/me` | Edita nome, telefone e dados de cobrança | U | ok |
| PATCH | `/me/password` | Troca a senha (exige a atual) | U | ok |

**Grupos, clientes e cobranças**

| Método | Rota | O que faz | Acesso | Suspensa |
| --- | --- | --- | --- | --- |
| GET | `/groups?month=2026-11` | Lista grupos com nº de clientes e total do mês | U | ok |
| POST | `/groups` | Cria grupo | U | bloqueia |
| GET | `/groups/:id` | Detalhe do grupo com seus clientes | U | ok |
| PATCH | `/groups/:id` | Edita nome, cor, notas | U | bloqueia |
| DELETE | `/groups/:id` | Arquiva o grupo (recusa se tiver clientes ativos) | U | bloqueia |
| GET | `/clients?group_id&status&q&page` | Lista e busca clientes | U | ok |
| POST | `/clients` | Cria cliente | U | bloqueia |
| GET | `/clients/:id` | Detalhe com histórico de cobranças | U | ok |
| PATCH | `/clients/:id` | Edita cliente, muda de grupo, pausa | U | bloqueia |
| DELETE | `/clients/:id` | Arquiva o cliente (cobranças pendentes viram `cancelled`) | U | bloqueia |
| GET | `/charges?month&status&group_id&client_id&page` | Lista cobranças; `status=late` filtra pelo atraso calculado | U | ok |
| POST | `/charges/generate` | Gera as cobranças do mês `{ month }` para todos os clientes ativos; repetir não duplica | U | bloqueia |
| POST | `/charges` | Cria cobrança avulsa | U | bloqueia |
| PATCH | `/charges/:id` | Edita valor, vencimento, nota (só se `pending`) | U | bloqueia |
| POST | `/charges/:id/pay` | Marca como paga `{ paid_at? }` (padrão: hoje) | U | bloqueia |
| POST | `/charges/:id/unpay` | Desfaz o pagamento | U | bloqueia |
| DELETE | `/charges/:id` | Cancela a cobrança | U | bloqueia |
| GET | `/summary?month=2026-11` | Total do mês: a receber, recebido, pendente, atrasado | U | ok |
| GET | `/export?type&format&month` | Baixa a planilha com os dados da conta | U | ok |

**Assinatura (o usuário pagante)**

| Método | Rota | O que faz | Acesso | Suspensa |
| --- | --- | --- | --- | --- |
| GET | `/subscription` | Plano, status, próximo vencimento, uso (clientes e usuários) | U | ok |
| GET | `/subscription/invoices` | Faturas da conta, mais recentes primeiro | U | ok |
| POST | `/subscription/checkout` | Cria o checkout no gateway e devolve a URL de pagamento | U | ok |
| POST | `/subscription/invoices/:id/payment-link` | Devolve Pix, boleto ou link de cartão da fatura em aberto ("Regularizar fatura") | U | ok |
| POST | `/subscription/cancel` | Cancela ao fim do período pago | U | ok |

**Painel admin e gateway**

| Método | Rota | O que faz | Acesso |
| --- | --- | --- | --- |
| GET | `/admin/summary` | MRR, contas por status, faturas do mês (recebido, pendente, atrasado), MRR por mês | A |
| GET | `/admin/accounts?status&q&page` | Lista e busca contas | A |
| GET | `/admin/accounts/:id` | Conta, assinatura, faturas e histórico do `audit_log` | A |
| POST | `/admin/accounts/:id/suspend` | Suspende `{ reason }` | A |
| POST | `/admin/accounts/:id/reactivate` | Reativa a conta | A |
| GET | `/admin/invoices?status&month&page` | Lista faturas de todas as contas | A |
| POST | `/admin/invoices/:id/mark-paid` | Baixa manual `{ paid_at, method }` | A |
| GET | `/admin/plans` | Lista planos | A |
| PATCH | `/admin/plans/cloud` | Muda o preço; vale só para novas assinaturas | A |
| POST | `/webhooks/gateway` | Recebe eventos do gateway; valida a assinatura do cabeçalho; idempotente por `gateway_event_id` | P (assinado) |

Eventos do webhook tratados no MVP: **pagamento confirmado** (fatura `paid`, conta `active`, fim da suspensão), **pagamento vencido** (conta passa a "atrasada" no painel), **assinatura cancelada** (conta `cancelled`).

## Regras de negócio

O estado da conta decide o que o usuário pode fazer. Os prazos abaixo são valores iniciais, ajuste no `plans` ou em variáveis de ambiente.

| Estado | Como entra | Pode ler e exportar | Pode criar e editar |
| --- | --- | --- | --- |
| `trial` | Cadastro concluído; dura 14 dias, sem cartão | sim | sim |
| `active` | Primeiro pagamento confirmado | sim | sim |
| `suspended` | Trial vencido sem pagamento, 5 dias de atraso numa fatura, ou ação do admin | sim | **não** |
| `cancelled` | Fim do período pago após cancelamento | sim, por 90 dias | não |

- **"Atrasada" não é estado gravado.** É uma conta `active` com fatura vencida e não paga; `/admin/accounts` devolve `display_status: late` para ela. Ela ainda usa o sistema normalmente até virar `suspended`.
- **Conta suspensa:** o front mostra a faixa "Conta suspensa" com o botão "Regularizar fatura" (`/subscription/invoices/:id/payment-link`). Os dados ficam intactos e podem ser exportados.
- **Reativação:** automática quando o webhook confirma o pagamento da fatura em aberto, ou manual pelo admin. Suspensão manual só é desfeita pelo admin.
- **Faturas:** o gateway gera uma por mês; o sistema cria o registro em `invoices` ao receber o evento e numera em sequência (`FAT-` + número).
- **Preço:** o valor da assinatura fica gravado em `subscriptions.price_cents` no momento da contratação.
- **Cobranças do usuário:** `/charges/generate` cria uma por cliente ativo no mês, com `amount_cents` copiado do cliente e `due_date` = mês + `due_day`. O valor da cobrança não muda se o cliente for editado depois.
- **Admin:** não suspende a própria conta, e toda ação de admin grava uma linha em `audit_log`.

**Jobs diários** (cron às 03:00, America/Sao\_Paulo)

1. Suspender contas `trial` com `trial_ends_at` vencido e sem pagamento
2. Suspender contas `active` com fatura vencida há 5 dias ou mais
3. Enviar e-mail 3 dias antes do vencimento e no dia em que a conta for suspensa
4. Apagar sessões e tokens de senha expirados, e dados de contas canceladas há mais de 90 dias

## Validações

Valide no servidor com um schema por rota (Zod); o front repete as mesmas regras só para dar resposta rápida. Textos são aparados (`trim`), e-mail vai para minúsculas, CPF, CNPJ, telefone e CEP são gravados só com dígitos (a máscara é aceita na entrada). Campos não previstos no corpo são rejeitados, e o corpo tem no máximo 100 KB.

**Conta e acesso**

| Campo | Regra | Erro |
| --- | --- | --- |
| `email` | formato válido, até 254 caracteres, único | `invalid_format`; `409 email_taken` |
| `password` | 8 a 72 caracteres, ao menos 1 letra e 1 número, diferente do e-mail | `too_short`, `weak_password` |
| `name` | 2 a 120 caracteres | `too_short`, `too_long` |
| `person_type` | `pf` ou `pj` | `invalid_value` |
| `document` | PF: CPF com 11 dígitos e dígitos verificadores válidos; PJ: CNPJ com 14 dígitos e verificadores válidos; único | `invalid_document`; `409 document_taken` |
| `company_name` | obrigatório se `pj` (2 a 120); ignorado se `pf` | `required` |
| `phone` | DDD + número, 10 ou 11 dígitos | `invalid_format` |
| `zip`, `state` | CEP com 8 dígitos; UF com 2 letras válidas | `invalid_format` |
| `terms_accepted` | deve ser `true` no cadastro | `required` |

**Grupos e clientes**

| Campo | Regra | Erro |
| --- | --- | --- |
| `group.name` | 1 a 60 caracteres; único entre os grupos ativos da conta | `required`; `409 group_name_taken` |
| `group.color` | hexadecimal `#RRGGBB` | `invalid_format` |
| `client.name` | 2 a 120 caracteres | `too_short`, `too_long` |
| `client.email` | opcional; formato válido | `invalid_format` |
| `client.document` | opcional; CPF ou CNPJ válido | `invalid_document` |
| `client.amount_cents` | inteiro entre 1 e 99.999.999 | `out_of_range` |
| `client.due_day` | inteiro de 1 a 28 | `out_of_range` |
| `client.group_id` | grupo ativo da mesma conta | `404 not_found` |

**Cobranças, assinatura e admin**

| Campo | Regra | Erro |
| --- | --- | --- |
| `month` | `YYYY-MM`, de 12 meses atrás a 12 meses à frente | `invalid_format`, `out_of_range` |
| `charge.client_id` | cliente ativo da mesma conta | `404 not_found`; `409 client_not_active` |
| `charge.amount_cents` | inteiro entre 1 e 99.999.999 | `out_of_range` |
| `charge.due_date` | data válida `YYYY-MM-DD` | `invalid_format` |
| `paid_at` | data válida, não pode ser futura | `out_of_range` |
| `reason` (suspender) | 3 a 200 caracteres | `too_short`, `too_long` |
| `method` (baixa manual) | `pix`, `boleto` ou `card` | `invalid_value` |
| `price_cents` (plano) | inteiro de 100 a 999.999 | `out_of_range` |
| `format` (exportação) | `xlsx` ou `csv` | `invalid_value` |
| `page`, `limit` | `page` ≥ 1; `limit` de 1 a 100 | `out_of_range` |

**Estado do recurso** (alguns erros não vêm do corpo, vêm do estado atual)

- Editar ou pagar cobrança que não está `pending` → `409 charge_not_editable`
- Pagar cobrança já paga → `409 charge_already_paid`
- Arquivar grupo com clientes ativos → `409 group_not_empty`
- Suspender conta já suspensa, ou reativar conta que não está suspensa → `409 invalid_account_state`
- Marcar como paga fatura já paga ou cancelada → `409 invoice_not_payable`

## Erros

Toda falha devolve o mesmo formato, com o status HTTP certo e um `code` estável que o front usa para decidir o que mostrar. O texto de `message` é em português e pode mudar; o `code` não.

```json
{
  "error": {
    "code": "validation_error",
    "message": "Dados inválidos.",
    "details": [
      { "field": "document", "code": "invalid_document", "message": "CPF inválido." }
    ],
    "request_id": "9f2c1e7a"
  }
}
```

`details` só existe em `validation_error`; `request_id` vai em toda resposta de erro e nos logs, para achar o problema depois.

| Status | `code` | Quando acontece |
| --- | --- | --- |
| 400 | `bad_request` | JSON malformado ou corpo grande demais |
| 401 | `not_authenticated` | Sem sessão ou sessão expirada; o front leva ao login |
| 401 | `invalid_credentials` | E-mail ou senha errados (mensagem igual para os dois casos) |
| 401 | `invalid_webhook_signature` | Webhook sem assinatura válida do gateway |
| 403 | `forbidden` | Rota de admin acessada por usuário comum |
| 403 | `account_suspended` | Ação de escrita com a conta suspensa ou cancelada; o front mostra a faixa e o botão "Regularizar fatura" |
| 404 | `not_found` | Recurso não existe ou é de outra conta |
| 409 | `email_taken`, `document_taken`, `group_name_taken` | Valor único já usado |
| 409 | `group_not_empty`, `client_not_active`, `charge_not_editable`, `charge_already_paid`, `invoice_not_payable`, `invalid_account_state` | Estado atual do recurso não permite a ação (ver Validações) |
| 422 | `validation_error` | Algum campo não passou nas regras; lista em `details` |
| 429 | `too_many_requests` | Limite estourado; cabeçalho `Retry-After` em segundos |
| 502 | `gateway_error` | O gateway de pagamento falhou ou não respondeu |
| 500 | `internal_error` | Erro inesperado; sem detalhes internos na resposta |

**Limites de uso**

- Login: 5 tentativas erradas por e-mail em 15 minutos, depois `429`
- `/auth/forgot-password` e `/auth/register`: 5 por hora por IP
- Demais rotas: 300 requisições por minuto por usuário

**Boas práticas que valem desde o primeiro dia**

- Nunca devolver senha, hash, token ou `gateway_*` nas respostas
- Registrar em log o `request_id`, a rota, o status e o `account_id`, sem corpo de requisição
- Falha do gateway não muda o estado local: só o webhook confirmado muda fatura e conta
- Webhook responde `200` mesmo para evento já processado, senão o gateway reenvia sem parar

## Ordem de implementação

Cada etapa deixa algo testável de ponta a ponta antes da próxima.

1. **Base:** projeto, banco, migrations, formato de erro, `request_id`, logs
2. **Acesso:** cadastro PF/PJ, login, logout, recuperar senha, `/me`
3. **Núcleo do produto:** grupos, clientes, cobranças, `/summary` (as telas Grupos, Clientes e Cobranças já funcionam aqui)
4. **Exportação:** `/export` em xlsx e csv
5. **Assinatura:** `plans`, checkout no gateway, webhook, faturas, tela Assinatura
6. **Estados da conta:** trial, suspensão automática, bloqueio de escrita (`account_suspended`), jobs diários
7. **Painel admin:** resumo, contas, conta, pagamentos, planos, `audit_log`
8. **Acabamento:** limites de uso, e-mails, backup do banco, testes das regras de estado

**Testes que não podem faltar**

- Isolamento: usuário da conta A recebe `404` em recurso da conta B, em toda rota
- Conta suspensa: leitura e exportação funcionam, toda escrita responde `403 account_suspended`
- Webhook repetido não duplica fatura nem muda o estado duas vezes
- CPF e CNPJ válidos e inválidos, incluindo números repetidos como `111.111.111-11`

**Fora do MVP**

- Plano anual, limite de clientes e mais de 1 usuário por conta (decididos como não existentes)
- Endereço próprio (domínio personalizado) e importação de planilha
- Cobrança do usuário ao cliente final por Pix ou boleto dentro do sistema (hoje o sistema só registra)
- Notificações por WhatsApp, relatórios avançados e API pública

**Pontos para você decidir**

- Qual gateway usar (Asaas, Stripe ou outro) e se o trial exige cartão
- Prazo de suspensão automática (aqui, 5 dias de atraso) e de retenção após cancelar (aqui, 90 dias)
- Se a verificação de e-mail entra já no MVP (não está prevista acima)
