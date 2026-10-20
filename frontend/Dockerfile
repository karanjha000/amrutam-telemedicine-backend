# ------------------------------------------------------------------
# Stage 1: build the frontend with Node
# ------------------------------------------------------------------
FROM node:24-alpine AS build
WORKDIR /app

# Install dependencies first for layer caching.
COPY package.json package-lock.json* ./
RUN npm install

# Source and config.
COPY tsconfig.json tsconfig.node.json vite.config.ts index.html ./
COPY src ./src

# Type-check + bundle.
RUN npm run build

# ------------------------------------------------------------------
# Stage 2: serve the static bundle with nginx, proxying API traffic
# to the backend service (AMRUTAM_API_HOST, default "backend:8080").
# ------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

# nginx does not read environment variables at runtime. The entrypoint
# renders the template (which uses an ${AMRUTAM_API_HOST} placeholder)
# into the real conf before starting nginx, so the same image works
# against the compose service name in production and a local backend
# during development.
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY 20-render-amrutam.conf /docker-entrypoint.d/20-render-amrutam.conf
COPY --from=build /app/dist /usr/share/nginx/html

# Default backend host; override at run time:
#   docker run -e AMRUTAM_API_HOST=backend:8080 amrutam-frontend
ENV AMRUTAM_API_HOST=backend:8080

EXPOSE 80
