# Build
FROM node:24.21.0-alpine AS build

WORKDIR /app

RUN corepack enable

COPY package.json yarn.lock ./

RUN yarn install --immutable

COPY . .

RUN yarn build


# Runtime
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]