# 🗄️ Database Architecture

<!-- docs-nav -->
<p align="center"><sub><a href="README.md">Hub</a> · <a href="Architecture-overview.md">Overview</a> · <a href="ARCHITECTURE.md">Deep dive</a> · <a href="backend-architecture.md">Backend</a> · <a href="frontend-architecture.md">Frontend</a> · <b>Database</b> · <a href="authentication-architecture.md">Auth</a> · <a href="api-endpoints.md">API</a> · <a href="SECURITY.md">Security</a> · <a href="tech-stack.md">Stack</a> · <a href="configuration-and-environment.md">Config</a> · <a href="setup.md">Setup</a> · <a href="observability.md">Observability</a> · <a href="testing.md">Testing</a> · <a href="troubleshooting.md">Troubleshooting</a></sub></p>


> **PostgreSQL** is the system of record. **Redis** supports caching and streams. Redis complements Postgres; it never replaces it.

## 🧭 1. Where data lives

```mermaid
flowchart LR
    API["⚙️ amrutam-api<br/>JPA, owns schema"] --> PG[("🐘 PostgreSQL<br/>transactional truth")]
    WK["🔄 amrutam-workers<br/>raw SQL"] --> PG
    API --> RD[("⚡ Redis<br/>cache · streams")]
    WK --> RD
```

## 🧱 2. Schema ownership

> [!IMPORTANT]
> The API module owns the schema and persistence model and is the single source of truth for schema evolution. Workers may use raw SQL for operational flows but own no JPA entities or repositories.

## 🗂️ 3. Entity groups

```mermaid
graph LR
    subgraph Identity["🔑 Identity and access"]
        U[users]; UP[user_profiles]
    end
    subgraph Care["🩺 Care delivery"]
        D[doctors]; S[availability_slots]; C[consultations]; R[prescriptions]
    end
    subgraph Ops["💳 Financial and operational"]
        P[payments]; I[idempotency_keys]; J[async_jobs]; SS[saga_steps]; AL[audit_logs]
    end
    U --- UP
    U --- D
    D --- S
    S --- C
    C --- R
    C --- P
    C --- SS
    U --- I
    U --- J
```

## 🧬 4. Full ER diagram

```mermaid
erDiagram
    USERS {
        uuid id PK
        string username UK
        string password_hash
        string role "PATIENT|DOCTOR|ADMIN"
        boolean mfa_enabled
        boolean active
        timestamp created_at
        timestamp updated_at
    }
    USER_PROFILES {
        uuid id PK
        uuid user_id FK
    }
    DOCTORS {
        uuid id PK
        uuid user_id FK "unique"
        string specialization
        decimal consultation_fee
        int years_experience
        decimal ratings
        int total_ratings
        boolean active
    }
    AVAILABILITY_SLOTS {
        uuid id PK
        uuid doctor_id FK
        timestamp start_at
        timestamp end_at
        string status "AVAILABLE|LOCKED|BOOKED|CANCELLED"
        uuid locked_by_id
        timestamp lock_expires_at
    }
    CONSULTATIONS {
        uuid id PK
        uuid patient_id FK
        uuid doctor_id FK
        uuid slot_id FK
        string status "PENDING_PAYMENT|PAID|COMPLETED|CANCELLED|NO_SHOW"
        timestamp scheduled_at
        timestamp completed_at
        string notes
    }
    PRESCRIPTIONS {
        uuid id PK
        uuid consultation_id FK "unique"
        uuid patient_id FK
        uuid doctor_id FK
        text medications "JSON text"
        string dosage_notes
    }
    PAYMENTS {
        uuid id PK
        uuid consultation_id FK
        decimal amount
        string currency "default INR"
        string status "CREATED|PAYING|CAPTURED|FAILED|REFUNDED"
        string gateway_ref
        string idempotency_key UK
    }
    IDEMPOTENCY_KEYS {
        uuid id PK
        uuid user_id FK
        string method
        string path
        string body_hash
        string state "PENDING|COMPLETED|FAILED"
        text result_json
    }
    ASYNC_JOBS {
        uuid id PK
        uuid user_id FK
        string type
        string status "PENDING|IN_PROGRESS|DONE|FAILED|DEAD"
        text payload
        int attempt
        int max_attempt
        timestamp next_run_at
        string lease_owner
        timestamp lease_expiry
    }
    SAGA_STEPS {
        uuid id PK
        uuid saga_id
        int step_index
        string step_name
        string status
        string detail
    }
    AUDIT_LOGS {
        uuid id PK
        uuid actor_id
        string action
        string target
        string ip
        text details
        timestamp occurred_at
    }
    USERS ||--o| USER_PROFILES : has
    USERS ||--o| DOCTORS : backs
    DOCTORS ||--o{ AVAILABILITY_SLOTS : owns
    USERS ||--o{ CONSULTATIONS : patient
    DOCTORS ||--o{ CONSULTATIONS : doctor
    AVAILABILITY_SLOTS ||--o| CONSULTATIONS : slot-for
    CONSULTATIONS ||--o| PAYMENTS : paid-by
    CONSULTATIONS ||--o| PRESCRIPTIONS : prescribed
    USERS ||--o{ IDEMPOTENCY_KEYS : issued
    USERS ||--o{ ASYNC_JOBS : owns
    CONSULTATIONS ||--o{ SAGA_STEPS : traced-by
```

> Constraints worth knowing: `availability_slots` is unique on `(doctor_id, start_at, end_at)`; `idempotency_keys` is unique on `(user_id, method, path, body_hash)`.

## 🔁 5. Slot lifecycle

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE
    AVAILABLE --> LOCKED: booking lock (atomic UPDATE)
    LOCKED --> BOOKED: payment captured
    LOCKED --> AVAILABLE: compensation / reconciliation
    AVAILABLE --> CANCELLED: doctor cancels
```

## ⚡ 6. Redis usage

| Use | Description |
| --- | --- |
| 🧠 Cache | Per-cache TTLs (users, doctors, availability, consultations, analytics) |
| 📡 Streams | Event-based processing, e.g. audit stream |
| 🔔 Notifications | Notification handling |
| 🧰 Worker state | Supporting background worker state |

## 🏭 7. Operational considerations

The local environment uses a simplified data model. Production is designed for backup, retention and scaling (partitioning plan and RPO/RTO targets in [ARCHITECTURE.md](ARCHITECTURE.md#8--partitioning-future-ops-step)).

<!-- docs-footer -->
---

<p align="center">⬅️ <a href="frontend-architecture.md">Frontend</a> · 🏠 <a href="README.md">Docs hub</a> · <a href="authentication-architecture.md">Auth</a> ➡️</p>