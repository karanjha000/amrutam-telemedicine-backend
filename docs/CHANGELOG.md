# 📝 Changelog

<!-- docs-nav -->
<p align="center"><sub><a href="README.md">Hub</a> · <a href="Architecture-overview.md">Overview</a> · <a href="ARCHITECTURE.md">Deep dive</a> · <a href="backend-architecture.md">Backend</a> · <a href="frontend-architecture.md">Frontend</a> · <a href="database-architecture.md">Database</a> · <a href="authentication-architecture.md">Auth</a> · <a href="api-endpoints.md">API</a> · <a href="SECURITY.md">Security</a> · <a href="tech-stack.md">Stack</a> · <a href="configuration-and-environment.md">Config</a> · <a href="setup.md">Setup</a> · <a href="observability.md">Observability</a> · <a href="testing.md">Testing</a> · <a href="troubleshooting.md">Troubleshooting</a></sub></p>


> Records what changed in the documentation and the repository state it was checked against. No release version numbers have been assigned.

## 🆕 Current documentation update

This documentation set was updated to match the verified source and runtime state of the repository.

### ✨ Added

| File | Purpose |
| --- | --- |
| [README.md](README.md) | Canonical documentation hub |
| [Architecture-overview.md](Architecture-overview.md) | Architecture overview |
| [DOCS_AUDIT.md](DOCS_AUDIT.md) | Record of every correction made against the code |
| [testing.md](testing.md) | Verification and evidence for Docker, Maven and k6 runs |

### 🔧 Corrected

- Dependency graph and module boundaries
- Runtime and startup documentation
- API inventory and role/authorization details

### 🐛 Fixed in code

- Fixed the shared Spring Security bean wiring so worker tests no longer fail when no API `AuthenticationProvider` bean exists

## ✅ Verified source baseline

| Component | Verified state | Source |
| --- | --- | --- |
| Java | 21 | Maven parent `backend/pom.xml` |
| Spring Boot | 3.3.4 | `backend/pom.xml` |
| PostgreSQL | 16 | `docker-compose.yml` |
| Redis | 7 | `docker-compose.yml` |
| Frontend | React 18 + TypeScript + Vite | `frontend/package.json` |
| Docker Compose stack | Healthy | Live output |
| Backend Maven tests | Pass (`EXIT_CODE=0`) | Fresh run |

## ⏳ Open items

| Item | Status |
| --- | --- |
| Browser-captured screenshots for every frontend page | Pending |
| Production deployment runbooks and backup/disaster-recovery documentation | Future operational enhancement |

<!-- docs-footer -->
---

<p align="center">⬅️ <a href="troubleshooting.md">Troubleshooting</a> · 🏠 <a href="README.md">Docs hub</a> · <a href="DOCS_AUDIT.md">Docs audit</a> ➡️</p>