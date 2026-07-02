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