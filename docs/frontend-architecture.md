# 🖥️ Frontend Architecture

<!-- docs-nav -->
<p align="center"><sub><a href="README.md">Hub</a> · <a href="Architecture-overview.md">Overview</a> · <a href="ARCHITECTURE.md">Deep dive</a> · <a href="backend-architecture.md">Backend</a> · <b>Frontend</b> · <a href="database-architecture.md">Database</a> · <a href="authentication-architecture.md">Auth</a> · <a href="api-endpoints.md">API</a> · <a href="SECURITY.md">Security</a> · <a href="tech-stack.md">Stack</a> · <a href="configuration-and-environment.md">Config</a> · <a href="setup.md">Setup</a> · <a href="observability.md">Observability</a> · <a href="testing.md">Testing</a> · <a href="troubleshooting.md">Troubleshooting</a></sub></p>


> A separate **React + TypeScript** app that serves the telemedicine UI, intentionally isolated from backend runtime composition.

## 🧰 1. Stack

![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-build-646CFF?logo=vite&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-runtime-009639?logo=nginx&logoColor=white)

CSS-based styling · REST + GraphQL client layers.

## 🎯 2. Responsibilities

| Area | Features |
| --- | --- |
| 🔑 Access | Login, registration, role-based routes |
| 🔎 Discovery | Doctor search and browsing |
| 📅 Booking | Booking flow, consultation status |
| 🩺 Doctor tools | Scheduling and administration |
| 📊 Admin | Analytics dashboards |

## 🌳 3. Application structure

```text
frontend/
└── src/
    ├── pages/        📄 route-driven screens
    ├── components/   🧩 reusable UI + app shell
    ├── context/      🧠 shared state + auth context
    ├── lib/          🔌 REST/GraphQL clients, helpers
    └── types.ts      🏷️ common TypeScript models
```

```mermaid
flowchart TD
    PAGES["📄 pages"] --> COMP["🧩 components"]
    PAGES --> CTX["🧠 context (auth)"]
    PAGES --> LIB["🔌 lib (API clients)"]
    COMP --> TYPES["🏷️ types.ts"]
    LIB --> TYPES
    LIB -- "REST + GraphQL" --> BE["⚙️ Backend API"]
```

## 🚦 4. Auth and routing model

```mermaid
flowchart TD
    L["Login"] --> G{"Route guard<br/>role from auth context"}
    G -- PATIENT --> P["🧑 Patient dashboard · booking pages"]
    G -- DOCTOR --> D["🩺 Scheduling · consultation tools"]
    G -- ADMIN --> A["🛠️ Analytics · operational views"]
    G -- "no token" --> L
```

> [!NOTE]
> Route guards are a UX convenience. Real enforcement always happens on the backend (see [authentication-architecture.md](authentication-architecture.md)).

## 🔌 5. API integration

| Style | Used for |
| --- | --- |
| REST | Transactional operations |
| GraphQL | Read-focused interactions |

The dev server proxies API calls, so local development needs no direct CORS handling.

## 🐳 6. Runtime

```mermaid
flowchart LR
    SRC["React source"] -- "vite build" --> DIST["static bundle"] --> NG["Nginx container :80"] --> BR["🌐 Browser"]
```

## 💡 7. Design intent

Small, focused modules · clean split between UI and client access logic · role-based routes · simple backend integration points.

<!-- docs-footer -->
---

<p align="center">⬅️ <a href="backend-architecture.md">Backend</a> · 🏠 <a href="README.md">Docs hub</a> · <a href="database-architecture.md">Database</a> ➡️</p>