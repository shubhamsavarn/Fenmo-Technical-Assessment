# Personal Expense Tracker

A production-quality full-stack expense tracking application built as a demonstration of software engineering practices applied to financial data handling.

**Live Demo**: _[Deploying to Render via Blueprint...]_

## Quick Start

```bash
# 1. Clone and configure
git clone <repo-url> && cd expense-tracker
cp .env.example .env          # Edit DB_PASSWORD for production

# 2. Start everything
docker compose up --build     # Builds and starts all 3 services

# 3. Open in browser
http://localhost                # Nginx → React SPA + API proxy
```

That's it. One command brings up PostgreSQL, Spring Boot, and Nginx.

---

## Architecture

```
┌─────────┐     ┌───────────────┐     ┌────────────┐
│  Nginx  │────▶│  Spring Boot  │────▶│ PostgreSQL │
│  :80    │     │  :8080        │     │  :5432     │
│         │     │               │     │            │
│ React   │     │ Controller    │     │ expenses   │
│ SPA     │     │ Service       │     │ (table)    │
│ static  │     │ Repository    │     │            │
└─────────┘     └───────────────┘     └────────────┘
```

**Stack**: Java 21 · Spring Boot 3.3 · PostgreSQL 16 · React 18 · TypeScript · Vite · Docker Compose · Nginx

---

## Key Design Decisions

### 1. Money Handling — `Money` Value Object with Paise

**Problem**: JavaScript and Java `float`/`double` cannot represent `0.1 + 0.2 = 0.3` correctly. In a finance app, accumulated rounding errors are unacceptable.

**Solution**: A custom immutable `Money` value object that:
- Stores amounts as **paise** (`long`) — `₹1,250.50` → `125050` paise
- Accepts input as **String** (e.g., `"1250.50"`) to prevent float→BigDecimal precision loss
- Validates at construction: positive, ≤2 decimal places, within bounds
- **Makes invalid states unrepresentable** — you cannot construct a `Money` with a negative amount

The database uses a **`GENERATED ALWAYS AS`** column so the human-readable `amount` is always derived from `amount_paise` — a single source of truth with zero divergence risk.

**API amounts are JSON strings**, never numbers: `{"amount": "1250.50"}`, not `{"amount": 1250.50}`.

### 2. Idempotency — DB UNIQUE Constraint + Application Check

**Problem**: Users double-click submit buttons. Networks timeout mid-POST. Pages get refreshed after submitting.

**Solution**: Every `POST /api/expenses` requires an `Idempotency-Key` header (client-generated UUID):
- Server checks if the key already exists in the database
- **New key** → `INSERT`, return `201 Created`
- **Existing key, same payload** → return existing record, `200 OK`
- **Existing key, different payload** → `409 Conflict` (prevents key reuse abuse)
- **Concurrent race** → `UNIQUE` constraint catches it, returns existing record

The client stores the idempotency key in `localStorage` so it **survives page refreshes**. It's cleared only after a confirmed success.

### 3. Pagination + Total Summary

**Problem**: "Show total of currently visible expenses." Does "visible" mean the current page or all filtered results?

**Decision**: Total reflects **all matching records**, not just the current page. If you filter by "food" and have 142 expenses across 8 pages, the total shows the sum of all 142.

Implementation: Two queries per request — one paginated (`LIMIT/OFFSET`), one aggregate (`SUM/COUNT`) — both using the same filters.

### 4. Error Handling — RFC 7807 Problem Details

All error responses follow the [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) standard with `application/problem+json` content type:
```json
{
  "type": "about:blank",
  "title": "Validation Error",
  "status": 400,
  "detail": "Request body contains invalid fields",
  "violations": [{"field": "amount", "message": "Amount must be positive"}]
}
```

### 5. Frontend State — No Optimistic Updates

For a finance app, we **do not** use optimistic UI patterns. The user only sees data that the server has durably confirmed. A brief loading spinner is preferable to showing an expense that might fail to persist.

---

## Trade-offs Made (Due to Timebox)

| What I did | What I'd do with more time |
|---|---|
| **Offset pagination** (page numbers) | **Cursor-based** (keyset) for consistency during concurrent inserts |
| **Free-form categories** (text input) | Category management CRUD with color-coded badges |
| **No authentication** | JWT/OAuth2 with Spring Security — expenses scoped per user |
| **No edit/delete** | Full CRUD with soft-delete (financial records should never hard-delete) |
| **Basic rate limiting** (none currently) | Spring Boot rate limiter with Bucket4j or API gateway |
| **No CI/CD** | GitHub Actions: lint → test → build → deploy |
| **Automated deployment** | Render Blueprint for 1-click stack provisioning |

## What I Intentionally Did NOT Do

- **No Redux/Zustand** — 3 pieces of state don't justify a state management library
- **No ORM magic** — Used `@Query` with native SQL where precision matters (SUM, GENERATED columns)
- **No GraphQL** — Two endpoints. REST is the right tool at this scale.
- **No microservices** — Single bounded context. A monolith is the correct architecture.
- **No event sourcing** — Append-only ledger is elegant but overkill for personal expense tracking
- **No H2 in tests** — Testcontainers with real PostgreSQL. H2 doesn't support `GENERATED ALWAYS AS` or `ON CONFLICT`.

---

## Cloud Deployment (Render.com)

This project is configured for one-click deployment using **Render Blueprints**.

### How to Deploy:
1.  **Push to GitHub**: Push this repository to your account.
2.  **Connect Render**: Go to [dashboard.render.com](https://dashboard.render.com/) → **Blueprints** → **New Blueprint Instance**.
3.  **Connect Repo**: Select this repository.
4.  **Deploy**: Render will read `render.yaml` and automatically provision:
    -   A Managed PostgreSQL instance.
    -   A Spring Boot Web Service (Backend).
    -   A Static Site (Frontend) with automated API rewrites.

---

## Project Structure

```
expense-tracker/
├── docker-compose.yml          # One command to start everything
├── .env.example                # Environment variable template
│
├── server/                     # Java 21 + Spring Boot 3.3
│   ├── Dockerfile              # Multi-stage, layered JAR, non-root user
│   ├── pom.xml
│   └── src/main/java/com/expensetracker/
│       ├── domain/             # Money value object (framework-free)
│       ├── application/        # Service layer + DTOs
│       ├── infrastructure/     # JPA entities, repositories, config
│       └── web/                # Controllers, request/response DTOs, error handler
│
├── client/                     # React 18 + TypeScript + Vite
│   ├── Dockerfile
│   └── src/
│       ├── api/                # Typed fetch wrapper with Result pattern
│       ├── components/         # Form, List, Controls, Pagination, Toast
│       ├── hooks/              # useExpenses, useToast
│       └── utils/              # Money formatting, idempotency key mgmt
│
└── nginx/                      # Reverse proxy + static file server
    ├── Dockerfile
    └── nginx.conf
```

---

## API Reference

### `POST /api/expenses`
Create an expense. Requires `Idempotency-Key` header (UUID).

### `GET /api/expenses?category=food&sort=date_desc&page=0&size=20`
Paginated expense list with optional filters. Returns page metadata + aggregate summary.

### `GET /api/expenses/categories`
Distinct categories for the filter dropdown.

### `GET /actuator/health`
Health check (DB connectivity, disk space).

---

## Testing

```bash
# Unit tests (Money value object, no Spring context)
cd server && ./mvnw test -Dtest=MoneyTest

# Integration tests (requires Docker for Testcontainers)
cd server && ./mvnw test

# Full stack smoke test
docker compose up -d
curl http://localhost/actuator/health
curl -X POST http://localhost/api/expenses \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"amount":"100.50","category":"food","description":"test","date":"2026-04-21"}'
```
