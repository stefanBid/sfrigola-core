# sfrigola-core

Backend REST API for the Sfrigola recipe platform. Work in progress.

---

## Tech Stack

| Layer | Choice |
|---|---|
| Framework | Spring Boot 4.1.0 |
| Language | Java 25 |
| Build | Maven |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate (`ddl-auto=none`) |
| Security | Spring Security + JWT stateless (jjwt 0.13.0) |
| Validation | Jakarta Validation |
| Utilities | Lombok |
| Dev | Spring Boot DevTools, Docker Compose integration |

---

## Running Locally

```bash
# Start (Docker Compose spins up PostgreSQL automatically)
./mvnw spring-boot:run

# Build
./mvnw clean package -DskipTests

# Compile check
./mvnw compile

# Tests
./mvnw test
```

Required environment variables:

```
SERVER_PORT=...
DB_URL=...
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...
```

---

## Base URL

```
http://localhost:{SERVER_PORT}/sfrigola-core
```

---

## API Docs (Swagger)

Interactive OpenAPI docs, generated automatically from the controllers.

| What | URL |
|---|---|
| Swagger UI | `http://localhost:{SERVER_PORT}/sfrigola-core/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:{SERVER_PORT}/sfrigola-core/api/v3/api-docs` |

Both are public (no JWT needed to view). To try authenticated endpoints from the UI, click **Authorize** and paste a JWT obtained from `POST /auth/login` (no `Bearer ` prefix needed, Swagger adds it).

> Note: `/swagger-ui/index.html` has **no** `/api` prefix (it's a static resource, not a controller); `/api/v3/api-docs` **does** (it's served by a real `@RestController`, prefixed like everything else via `WebConfig`).

---

## Postman

A ready-to-import collection covering every endpoint in the project lives at:

```
postman/Sfrigola-Core.postman_collection.json
```

**Setup:**
1. Postman → **Import** → select the file above.
2. Open the collection → tab **Variables** → set `baseUrl` (defaults to `http://localhost:8080/sfrigola-core/api`) to match your `SERVER_PORT`.
3. Run **Auth → Login** once with valid credentials. Its test script auto-saves the JWT into the collection variable `token`.
4. Every other request inherits `Authorization: Bearer {{token}}` from the collection-level auth — no manual header editing, no re-pasting tokens.

To switch user (e.g. test as admin vs. regular user), just re-run **Login** with different credentials — `token` is overwritten automatically and every request picks up the new one immediately.

Requests are organized one folder per domain: **Auth, Languages, Users, Categories, Tags, Ingredients**. Admin-only requests are prefixed `[Admin]`. `{{publicId}}` / `{{parentPublicId}}` are empty placeholder variables — fill them in manually with real UUIDs returned by a prior create/list call.

---

## API Overview

### Auth — `/auth`

| Method | Path | Access |
|---|---|---|
| POST | `/auth/login` | public |
| POST | `/auth/register` | public |
| PATCH | `/auth/change-email` | authenticated |
| PATCH | `/auth/change-password` | authenticated |

### Languages — `/languages`

| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/languages` | authenticated | paginated, `isActive` filter |

### Users — `/users`

| Method | Path | Access | Notes |
|---|---|---|---|
| PATCH | `/users/settings/change-preferred-lang/{code}` | authenticated | |
| PATCH | `/users/profile/update` | authenticated | |
| PATCH | `/users/profile/became-contributor` | authenticated | promotes to ROLE_CONTRIBUTOR |
| GET | `/users/admin` | ROLE_ADMIN | paginated, sort/search/isActive filters |
| PATCH | `/users/admin/{publicId}/status` | ROLE_ADMIN | activate/deactivate user |

### Categories — `/categories`

| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/categories` | authenticated | tree or flat list |
| GET | `/categories/{publicId}` | authenticated | single with translations |
| POST | `/categories` | ROLE_ADMIN | create with translations |
| PUT | `/categories/{publicId}` | ROLE_ADMIN | update |
| DELETE | `/categories/{publicId}` | ROLE_ADMIN | blocked if has children |
| PATCH | `/categories/reorder` | ROLE_ADMIN | bulk sort_order update |

---

## Domain Status

| Domain | Entity | Repo | Service | Controller |
|---|---|---|---|---|
| `auth` | via users | — | done | done |
| `languages` | done | done | done | done |
| `users` | done | done | done | done |
| `categories` | done | done | done | done |
| `tags` | — | — | — | — |
| `ingredients` | — | — | — | — |
| `recipes` | — | — | — | — |
| `favorites` | — | — | — | — |
| `ratings` | — | — | — | — |
| `recipe_stats` | — | — | — | no controller (internal) |

---

## Architecture Notes

**Response envelope** — every endpoint returns `SCGeneralResponseDto<T, K>`:
```json
{
  "data": { ... },
  "option": { "currentPage": 1, "pageSize": 10, "totalElements": 42, "totalPages": 5, "hasMore": true },
  "errorData": null
}
```

**Pagination** — `SCFilterQuery` → service returns `SCPagedResult<T>` → controller maps to envelope.

**Roles** — `ROLE_ADMIN`, `ROLE_USER`, `ROLE_CONTRIBUTOR`. Path authorization configured via `SecurityBeansConfig` qualifier beans.

**Dual service contract**:
- `IXService` — controller-facing, reads security context internally
- `IXDomainBridgeService` — internal bridge, receives data explicitly

**IDs** — `id` (Long) is internal DB only; `publicId` (UUID) is what APIs expose.

**Schema** — managed manually via `src/main/resources/sql/createSfrigolaDB.sql`. Hibernate never alters tables.

**Endpoint versioning** — Spring Boot 4.x `version = "1.0"` on mapping annotations.

---

## Roles

| Role | Access |
|---|---|
| `ROLE_ADMIN` | full platform management |
| `ROLE_CONTRIBUTOR` | create/manage own recipes |
| `ROLE_USER` | browse, rate, favorite |