# Amrutam Frontend

React 18 + TypeScript + Vite single-page app for the Amrutam telemedicine
platform. Talks to the `amrutam-api` Spring Boot backend over REST and
GraphQL, both of which are proxied so the browser never crosses a CORS
boundary in development or in the Docker build.

## Quick start (dev)

```bash
# 1. Install dependencies
npm install

# 2. (Optional) copy the env template — the defaults work out of the box
#    when the backend runs on localhost:8080.
cp .env.example .env

# 3. Start the backend first (see amrutam-api/README):
#    the Vite dev server proxies /api, /graphql and /swagger-ui to it.
#    Start the dev server:
npm run dev
```

Open http://localhost:5173.

## Environment variables

Set in `.env` (Vite reads `VITE_*` variables at build time):

| Variable             | Default                 | Purpose                                             |
| -------------------- | ----------------------- | --------------------------------------------------- |
| `VITE_API_BASE_URL`  | `http://localhost:8080` | Backend origin. Leave unset for same-origin dev (the Vite proxy fronts the backend). |

## Scripts

| Script               | What it does                                   |
| -------------------- | ---------------------------------------------- |
| `npm run dev`        | Start the Vite dev server (with API proxy)     |
| `npm run build`      | Type-check (`tsc -b`) then bundle into `dist/`|
| `npm test`           | Run the Vitest unit tests once                 |
| `npm run typecheck`  | Type-check without emitting                    |
| `npm run preview`    | Serve the built `dist/` locally                |

## Tests

Unit tests live colocated under `__tests__` folders next to the pages
they cover (e.g. `src/pages/__tests__/LoginPage.test.tsx`,
`src/pages/__tests__/BookingFlow.test.tsx`). The network layer
(`src/lib/api.ts`) is mocked with `vi.mock`, so no test touches the
backend.

```bash
npm test
```

Tests run in a `jsdom` environment via the `test` block in
`vite.config.ts` (with `@testing-library/jest-dom` matchers loaded from
`src/test/setup.ts`).

## Project layout

```
frontend/
├── .env.example          env template (VITE_API_BASE_URL)
├── Dockerfile            multi-stage: node build -> nginx serve
├── nginx.conf            nginx site config (proxy to AMRUTAM_API_HOST)
├── 20-render-amrutam.conf  entrypoint hook that renders nginx.conf
├── index.html            Vite SPA entry
├── package.json
├── tsconfig*.json
├── vite.config.ts        dev proxy + vitest config
└── src/
    ├── main.tsx          app root: router + providers
    ├── App.tsx           (unused placeholder; routes are in main.tsx)
    ├── styles.css        single hand-rolled stylesheet
    ├── types.ts          TypeScript types mirroring the backend DTOs
    ├── lib/
    │   ├── api.ts        REST client (JWT + idempotency + envelope unwrap)
    │   ├── api-internal.ts  auth/URL helpers shared with the GraphQL client
    │   └── graphql.ts    minimal GraphQL client
    ├── context/
    │   └── AuthContext.tsx    login/register/logout + role helpers
    ├── components/
    │   ├── Layout.tsx            app shell (header/nav)
    │   ├── ProtectedRoute.tsx    role-gated route wrapper
    │   ├── BookingFlow.tsx        3-step patient booking
    │   ├── SlotsManager.tsx       doctor availability CRUD
    │   ├── DailyConsultationsChart.tsx  inline-SVG admin chart
    │   └── ui.tsx                small shared primitives
    └── pages/
        ├── LoginPage.tsx
        ├── RegisterPage.tsx
        ├── PatientDashboard.tsx
        ├── DoctorDashboard.tsx
        ├── AdminDashboard.tsx
        └── DoctorsPage.tsx
```

## Docker

```bash
# Build the image (from the frontend/ directory)
docker build -t amrutam-frontend .

# Run it against the backend service (compose or a local backend):
docker run --rm -p 8080:80 -e AMRUTAM_API_HOST=backend:8080 amrutam-frontend
# ... or, for a local backend on the host:
docker run --rm -p 8080:80 -e AMRUTAM_API_HOST=localhost:8080 amrutam-frontend
```

`AMRUTAM_API_HOST` defaults to `backend:8080` — the service name the
repo's docker-compose (or an equivalent orchestrator) assigns to
`amrutam-api`. Override it at run time to point anywhere else.

## Role-based access

- **Anyone** can browse `/doctors` (search + doctor cards).
- **PATIENT** accounts land on `/patient` after sign-in: book a
  consultation (pick doctor → pick an open slot → confirm), view their
  own consultations, open prescriptions, and cancel.
- **DOCTOR** accounts land on `/doctor`: add availability slots, list
  upcoming consultations, mark one complete, and read its prescription.
- **ADMIN** accounts land on `/admin`: daily-consultations bar chart,
  30-day revenue summary, doctor utilization, and system health.

The login flow optionally steps through MFA when the account has TOTP
enabled (`/login` → code entry → token issued).
