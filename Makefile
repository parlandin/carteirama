DEV  = docker compose -f docker-compose.yml -f docker-compose.dev.yml
PROD = docker compose -f docker-compose.yml -f docker-compose.prod.yml

.PHONY: dev dev-build dev-down prod prod-down logs ps

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
