DEV  = docker compose -f docker-compose.yml -f docker-compose.dev.yml
PROD = docker compose -f docker-compose.yml -f docker-compose.prod.yml

.PHONY: dev dev-build dev-down prod prod-down logs ps create-admin

dev:
	$(DEV) up

dev-build:
	$(DEV) up --build --renew-anon-volumes

dev-down:
	$(DEV) down

prod:
	$(PROD) up -d --build

prod-down:
	$(PROD) down

logs:
	$(PROD) logs -f

ps:
	$(PROD) ps

# Cria o primeiro administrador (pede a senha no terminal): make create-admin EMAIL=voce@exemplo.com
create-admin:
	$(PROD) run --rm api --create-admin=$(EMAIL)
