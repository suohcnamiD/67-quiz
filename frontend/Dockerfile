# ---- Build stage ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

RUN npm run build && \
    OUT=$(ls -d dist build out 2>/dev/null | head -1) && \
    mv "$OUT" /output

# ---- Runtime stage ----
FROM joseluisq/static-web-server:2-alpine

COPY --from=build /output /var/public

ENV SERVER_ROOT=/var/public
ENV SERVER_FALLBACK_PAGE=/var/public/index.html