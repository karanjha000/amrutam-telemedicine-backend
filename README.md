# Amrutam

Amrutam is a Docker-first telemedicine platform built with Java 21, Spring Boot 3, PostgreSQL, Redis, and a React + Vite frontend. The backend is organized as a multi-module Maven reactor, while the frontend remains isolated and interacts with the API through HTTP and GraphQL.

## Project overview

This repository brings together:

- a REST + GraphQL API for patient, doctor, and admin workflows
- a worker module for background job processing and reconciliation
- a shared common library for cross-cutting concerns such as security, JWT handling, idempotency, and rate limiting
- a React frontend for the patient, doctor, and admin experience
- Docker-based local deployment using PostgreSQL and Redis containers

## Repository layout

- [backend](backend) — backend reactor with the shared, API, and worker modules
- [backend/amrutam-common](backend/amrutam-common) — shared code and platform services
- [backend/amrutam-api](backend/amrutam-api) — application API and persistence layer
- [backend/amrutam-workers](backend/amrutam-workers) — background workers and async job processing
- [frontend](frontend) — Vite + React app
- [docs](docs) — canonical project documentation
- [docker-compose.yml](docker-compose.yml) — local full-stack startup
- [Dockerfile](Dockerfile) — backend runtime image build
- [start.sh](start.sh) and [start.ps1](start.ps1) — one-command startup scripts

## Quick start

From the repository root:

```bash
./start.sh
```

On Windows PowerShell:

```powershell
./start.ps1
```

This starts PostgreSQL, Redis, the backend API, the background workers, and the frontend in one command.

## Default local URLs

- Frontend: http://localhost
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator health: http://localhost:8080/actuator/health

## Documentation

The canonical documentation lives in [docs/README.md](docs/README.md).

### Recommended reading order

1. [docs/README.md](docs/README.md)
2. [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
3. [docs/backend-architecture.md](docs/backend-architecture.md)
4. [docs/frontend-architecture.md](docs/frontend-architecture.md)
5. [docs/database-architecture.md](docs/database-architecture.md)
6. [docs/authentication-architecture.md](docs/authentication-architecture.md)
7. [docs/api-endpoints.md](docs/api-endpoints.md)
8. [docs/tech-stack.md](docs/tech-stack.md)
9. [docs/SECURITY.md](docs/SECURITY.md)
10. [docs/setup.md](docs/setup.md)

## Tech stack

- Java 21
- Spring Boot 3.3.x
- Spring Data JPA
- Spring Security
- Spring GraphQL
- PostgreSQL 16
- Redis 7
- React 18 + TypeScript + Vite
- Maven multi-module build
- Docker + Docker Compose

## Important notes

- The backend is the source of truth for the domain model and persistence layer.
- The frontend is intentionally separated from the backend runtime flow.
- Local startup is Docker-first; the project should not depend on a local PostgreSQL or Redis installation.

For implementation details, architecture, auth flow, API references, and setup guidance, see the documentation in [docs](docs).
