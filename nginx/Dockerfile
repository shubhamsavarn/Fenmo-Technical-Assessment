# Nginx Dockerfile — builds the React app and serves it
# Separate from the client Dockerfile for proper Docker Compose context

# ── Stage 1: Build React app ───────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app
COPY client/package*.json ./
RUN npm ci
COPY client/ .
RUN npm run build

# ── Stage 2: Serve with Nginx ─────────────────────────────
FROM nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
