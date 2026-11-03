# Build context: raiz do repositório (veja docker-compose.*.yml)

# Dependências
FROM node:24.21.0-alpine AS deps

WORKDIR /app

COPY apps/web/package.json apps/web/yarn.lock ./

RUN yarn install --frozen-lockfile


# Dev: Vite com HMR (o código é montado via volume)
FROM deps AS dev

EXPOSE 5173

CMD ["yarn", "dev"]


# Build
FROM deps AS build

COPY apps/web/ .

RUN yarn build


# Runtime: Nginx serve o build e faz o proxy da API
FROM nginx:stable-alpine

COPY infra/nginx/conf.d/default.prod.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
