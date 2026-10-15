# Multi-stage Dockerfile for HRGenius Angular Frontend
FROM node:20-alpine AS builder
WORKDIR /app

# Copy dependency manifests
COPY package*.json ./
RUN npm ci

# Copy source code and build production bundle
COPY . .
RUN npm run build -- --configuration production

# Production stage using Nginx
FROM nginx:alpine
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
