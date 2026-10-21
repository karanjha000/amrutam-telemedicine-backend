# Frontend README

This frontend is a Vite + React + TypeScript application for the Amrutam telemedicine platform. It is intentionally separated from the backend runtime and communicates with the API over REST and GraphQL.

## Where to find the detailed documentation

For the full frontend design and project context, see:

- [../docs/frontend-architecture.md](../docs/frontend-architecture.md)
- [../docs/architecture.md](../docs/architecture.md)
- [../docs/api-endpoints.md](../docs/api-endpoints.md)
- [../docs/setup.md](../docs/setup.md)

## Local frontend development

From this directory:

```bash
npm install
npm run dev
```

The app runs on http://localhost:5173 by default.

## Frontend scripts

- `npm run dev` — run the Vite dev server
- `npm run build` — build the production bundle
- `npm run preview` — preview the production build
- `npm test` — run the frontend test suite

## Notes

- Frontend setup is documented in the shared project docs.
- The runtime stack is Docker-first and the backend should be started through Docker Compose.
- This README is intentionally concise to avoid duplicating the project-level documentation.
