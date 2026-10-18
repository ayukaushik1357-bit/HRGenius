# Multi-stage Dockerfile for HRGenius Angular Frontend
FROM node:22-alpine AS builder
WORKDIR /app

# Copy dependency manifests
COPY package*.json ./
# Same npm as package.json "packageManager" (npm 10 in node:22 rejects the lock file)
RUN npm install -g npm@11.6.0 && npm ci

# Copy source code and build production bundle
COPY . .
RUN npm run build -- --configuration production

# Production stage using Nginx
FROM nginx:alpine
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
