<div align="center">
  <div style="background: white; padding: 20px; border-radius: 12px; display: inline-block;">
    <img src="https://i.ibb.co/0jRSJQYY/sfrigola-core-banner.png" alt="logo" border="0" width="600" >
  </div>

  # Sfrigola Core

  ![Version](https://img.shields.io/badge/version-1.0.2-blue)
  ![Java](https://img.shields.io/badge/java-25-ED8B00?logo=openjdk&logoColor=white)
  ![Spring Boot](https://img.shields.io/badge/spring--boot-4.1.0-6DB33F?logo=springboot&logoColor=white)
  ![PostgreSQL](https://img.shields.io/badge/postgresql-336791?logo=postgresql&logoColor=white)
  ![Maven](https://img.shields.io/badge/maven-C71A36?logo=apachemaven&logoColor=white)
  ![JWT](https://img.shields.io/badge/auth-JWT%20stateless-000000?logo=jsonwebtokens&logoColor=white)
  ![License](https://img.shields.io/badge/license-MIT-green)

  **The backend REST API powering the Sfrigola digital recipe book.**

  Recipes, ingredients, tags and categories, all translatable, all searchable — with ratings, favourites and a contributor/admin publishing workflow.

</div>

---

## Table of Contents

1. [Overview](#1-overview)
2. [Getting Started](#2-getting-started)
3. [Project Structure](#3-project-structure)
4. [Database Schema](#4-database-schema)
5. [Architecture & Patterns](#5-architecture--patterns)
6. [Caching](#6-caching)
7. [Security & Roles](#7-security--roles)
8. [Error Handling & HTTP Status Codes](#8-error-handling--http-status-codes)
9. [API Reference](#9-api-reference)
10. [API Docs & Postman](#10-api-docs--postman)
11. [Testing](#11-testing)
12. [Versioning & Releases](#12-versioning--releases)
13. [Dependencies](#13-dependencies)
14. [License](#14-license)

---

## 1. Overview

Sfrigola Core is the backend for **Sfrigola**, a digital recipe book for home cooks. It serves the recipe catalogue — categories, ingredients, tags, ratings, favourites — behind a stateless JWT-secured REST API, with every user-facing entity translatable across active languages.

**v1.0.0 is the first stable release** and covers the full content pipeline end to end:

- **Auth & users** — registration, login, JWT-based sessions, profile management, contributor promotion, admin user management.
- **Catalogue** — categories (self-referential tree), tags (with an admin approval workflow: pending → approved/rejected), ingredients (allergens, dietary flags), all with per-locale translations.
- **Recipes** — full CRUD with a **draft/publish workflow**: a contributor creates a recipe in one language as a draft, an admin fills in the remaining active-language translations and publishes it; any later edit by the contributor reverts it to draft automatically.
- **Discovery** — home feed by category, curated feed groups (`QUICK`, `LIKE_A_CHEF`, `ECONOMICAL`, `VIRAL`), full-text search, paginated admin views with dietary/status filters.
- **Engagement** — favourites and 1-5 star ratings, both rolled up into `recipe_stats` (average rating, ratings count, favourites count, views count) and surfaced on every recipe DTO.
- **Cross-cutting** — a uniform response envelope, cursor-free offset pagination, Caffeine-backed caching on hot reference data, and OpenAPI/Swagger docs generated straight from the controllers.

**Target audience:** the Sfrigola mobile app (Flutter) is the primary consumer; the API is also fully explorable via Swagger UI and a ready-to-import Postman collection.

---

## 2. Getting Started

### Prerequisites

- **Java** 25 (JDK)
- **Maven** (wrapper included — no local install needed)
- **PostgreSQL** (or Docker, see below)
- **Docker** (optional — `spring-boot-docker-compose` auto-starts Postgres in dev via `docker-compose.dev.yaml`)

### Configuration — YAML + profiles

Config lives in `src/main/resources/`, split by profile — no single `application.properties` anymore:

| File | Scope |
|---|---|
| `application.yaml` | common to every profile — context path, datasource wiring, JPA, aspect thresholds. `spring.profiles.default: dev`, so no `SPRING_PROFILES_ACTIVE` set at all falls back to `dev`. |
| `application-dev.yaml` | local dev — Docker Compose auto-start, `show-sql: true`, dev-only fallback defaults for `JWT_SECRET_KEY`/`JWT_EXPIRATION_MS`/`ALLOWED_ORIGINS` (so a fresh clone runs with just DB/JWT secrets in `.env`, no CORS setup needed). |
| `application-prod.yaml` | prod — `show-sql: false`, no dev fallbacks. `JWT_SECRET_KEY`/`ALLOWED_ORIGINS` have **no default anywhere** — must come from a real env var or the app refuses to start/serve. |

**Hard rule for this codebase: application code never hardcodes a fallback for an externally-configured value.** Every `Environment.getProperty(KEY)` call gets the raw value and either uses it or throws — defaults belong exclusively in the profile YAML (dev-only convenience), never in Java. Concretely:
- Missing `JWT_SECRET_KEY`/`JWT_EXPIRATION_MS` at token-generation or validation time → `SCAuthSecuritySystemException` (500) / `ENV_NOT_AVAILABLE` (401 on the validation filter), never a silently-used hardcoded secret.
- Missing `ALLOWED_ORIGINS` → the `allowedOriginsPaths` bean throws at boot, application context fails to start — never a silent empty-CORS fallback.

### Environment variables

Actual secrets (always via env var, zero sensitive values in any YAML file):

```
SERVER_PORT=...
DB_URL=...
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET_KEY=...
```

Non-secret, environment-specific values (`JWT_EXPIRATION_MS`, `ALLOWED_ORIGINS`) have sane defaults in `application-dev.yaml`, still overridable via the same-named env var; `application-prod.yaml` requires them explicitly with no default.

### Running locally

```bash
# Start with the dev profile (Docker Compose spins up PostgreSQL automatically)
./mvnw spring-boot:run
# same, explicit:
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Compile only (check for errors)
./mvnw compile

# Run tests
./mvnw test
```

### Building & running a jar from the terminal

> **What this actually verifies:** that the project builds and the jar boots — package/compile sanity check, config/profile wiring, fail-fast behavior on missing env vars. **It is not the official way to test the `prod` deployment.** The real prod path is a Docker image built from `Dockerfile` and run via `docker-compose.prod.app.yaml`/`docker-compose.prod.db.yaml` (see [Docker](#docker) above) — that's what actually runs on the server, and it's the only flow that faithfully reproduces prod (it composes `DB_URL` for you, sets `SPRING_PROFILES_ACTIVE`, wires the internal Docker network). Running the bare jar skips all of that: you'd have to reconstruct `DB_URL` etc. by hand, and a passing bare-jar run does **not** mean the containerized prod setup works — always confirm with the actual Docker Compose flow before considering `prod` verified.
>
> `spring-boot-docker-compose` is also declared `optional` in `pom.xml`, so `spring-boot-maven-plugin` strips it out of the packaged jar on `repackage` — the auto-start-Postgres behavior described above simply isn't present in `target/sfrigola-core-<version>.jar`, by design (same treatment as `devtools`: dev-loop convenience, not meant to ship). So running the bare jar with `--spring.profiles.active=dev` will start the app but never bring up `db` for you — for the dev loop, always use `./mvnw spring-boot:run` instead (see above).

```bash
# Build the jar (skip tests)
./mvnw clean package -DskipTests
# → target/sfrigola-core-<version>.jar

# Load prod env vars from file into the current shell before running
set -a && source .env.prod && set +a

# Run it with the prod profile
java -jar target/sfrigola-core-<version>.jar --spring.profiles.active=prod
```

`set -a` exports every var `source` defines into the shell's environment (not just as a local shell variable) so the `java` child process can see them; `set +a` turns that off right after, so anything defined later isn't auto-exported too.

`SPRING_PROFILES_ACTIVE=prod` as an env var works identically to `--spring.profiles.active=prod` — pick whichever fits your process manager. No active profile at all → falls back to `dev` (see `spring.profiles.default` above). Required env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_KEY`, and — for `prod` — `ALLOWED_ORIGINS`) must already be exported in the shell, or the app fails fast per the rule above.

### Base URL

```
http://localhost:{SERVER_PORT}/sfrigola-core
```

Every endpoint lives under `/sfrigola-core/api`, versioned per-mapping via Spring Boot 4's `version = "1.0"` attribute.

### Database

Schema is managed **manually**, not by Hibernate (`ddl-auto=none`). The Postgres container runs `src/main/resources/sql/createSfrigolaDB.sql` as its init script on first boot — it creates every table, enum, index and seed row (languages, roles, the 11-category hierarchy).

### Docker

Three Compose files. Local dev and production are two fully independent lifecycles — different containers, different env source, no file shared between them:

| File | Purpose | Services | Env source |
|---|---|---|---|
| `docker-compose.dev.yaml` | local dev | `db` (`sfrigola_dev_db`) | `.env` (auto-loaded, same directory) |
| `docker-compose.prod.db.yaml` | prod — database | `db` (`sfrigola_prod_db`) | `.env.prod` (git-ignored, passed with `--env-file`) |
| `docker-compose.prod.app.yaml` | prod — backend | `app` (`sfrigola_prod_app`) | `.env.prod` |

**Local dev** — unchanged. `docker-compose.dev.yaml` is picked up automatically by Spring Boot's own docker-compose integration (`spring.docker.compose.file`, see [Dependencies](#13-dependencies)) every time you run `SfrigolaCoreApplication` (IDE Run/Debug or `./mvnw spring-boot:run`). It creates the Postgres image/volume the first time if they don't exist, reuses them after. No manual `docker compose` command needed — just hit Run.

**Production** — two separate images, two separate containers, started with two separate commands, in order. This is also how you test the production setup locally, on your own machine, before an actual deploy:

```bash
# 1. DB — creates image/volume/network if missing, container stays stopped
docker compose -f docker-compose.prod.db.yaml --env-file .env.prod create

# 2. Backend — builds the image from Dockerfile, container stays stopped
docker compose -f docker-compose.prod.app.yaml --env-file .env.prod build
docker compose -f docker-compose.prod.app.yaml --env-file .env.prod create
```

Neither command starts the containers — `create` only builds the image/volume/network and leaves the container stopped, ready to start from Docker Desktop (`db` first, wait for "healthy", then `app`).

Starting `app` before `db` exists fails immediately with a "network not found" error (see below) — that's intentional, not a bug.

**Optional — start the already-created containers from the CLI instead of Docker Desktop:**

```bash
docker compose -f docker-compose.prod.db.yaml --env-file .env.prod up -d
docker compose -f docker-compose.prod.app.yaml --env-file .env.prod up -d
```

Wait for `db` to report "(healthy)" (`docker ps --filter name=sfrigola_prod_db`) before starting `app`.

**Stopping the stack:**

```bash
docker compose -f docker-compose.prod.app.yaml down
docker compose -f docker-compose.prod.db.yaml down
```

Add `-v` to the `db` command only if you also want to wipe the Postgres volume (`postgres_data_prod`) — that deletes all data, so leave it off unless you mean it.

There's no `depends_on` between the two files — `depends_on` only works for services declared in the *same* `docker compose up` invocation, and these are two deliberately separate ones. The order is on you: run the DB command first, confirm it's healthy, then run the app command. Both files declare the same project name (`name: sfrigola-core-prod`), so:

- Docker Desktop groups them under one stack (`sfrigola-core-prod`), distinct from the local dev container (`sfrigola_db`) — you'll see both side by side without ever mixing them up.
- They share one Docker network (`sfrigola-core-prod_prod_net`), so `app` can resolve `db` by hostname. `docker-compose.prod.app.yaml` declares that network as `external: true` — if you try to start the app before the DB, Compose fails immediately with a clear "network not found" instead of silently misbehaving.

`DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_KEY` and `ALLOWED_ORIGINS` are mandatory in `docker-compose.prod.app.yaml` (`${VAR:?...}`) — Compose refuses to start instead of silently falling back to a default. `.env.prod` (git-ignored) holds them — set real values (including your actual prod domain for `ALLOWED_ORIGINS`) before an actual production deploy. `docker-compose.prod.app.yaml` also sets `SPRING_PROFILES_ACTIVE: prod` explicitly — the built jar is profile-agnostic, this is what tells it which config to load.

> Don't rely on plain `.env` for prod: Docker Compose auto-loads any file literally named `.env` sitting in the working directory, with no flag needed. Since the repo already has one at the root (dev values), running either prod command from that same directory without `--env-file .env.prod` would silently pick up the dev one instead.

### `Dockerfile`

Two-stage build for the `app` image, used **only** by `docker-compose.prod.app.yaml` (`app.build.dockerfile: Dockerfile`) — it plays no part in local dev, since `docker-compose.dev.yaml` has no `app` service to build.

1. **Build stage** (`eclipse-temurin:25-jdk`) — copies `mvnw`/`pom.xml`/`lombok.config` first and runs `dependency:go-offline` before copying `src/`, so Docker's layer cache keeps dependency downloads cached across builds where only source changed; then `./mvnw clean package -DskipTests`. `lombok.config` has to be copied explicitly here — Lombok needs it to copy `@Qualifier`/`@Lazy` onto generated constructor parameters (see [Architecture & Patterns](#5-architecture--patterns)); without it, Spring can't disambiguate the multiple `List<String>` path beans in `SecurityBeansConfig` and the container fails at startup.
2. **Runtime stage** (`eclipse-temurin:25-jre`) — copies just the built jar from the build stage and runs it. Smaller final image, no JDK/Maven left in it.

Still needed: yes, it's the only thing that turns this Spring Boot project into a runnable container image — required for the production flow, irrelevant to the "hit Run in the IDE" flow.

The `app` container also sets `SPRING_DOCKER_COMPOSE_ENABLED=false` — without it, Spring Boot would try to run its own docker-compose integration (`spring.docker.compose.file=docker-compose.dev.yaml`, the *local dev* file) from inside the already-containerized app, which has neither that file nor a Docker socket available, and would fail to start.

---

## 3. Project Structure

Package-by-feature: code is organized under `domains/<feature>`, not by technical layer. Cross-cutting infrastructure lives in `config/`; reusable cross-domain code lives in `common/`.

```
com.sb.sfrigola_core/
  SfrigolaCoreApplication.java     ← entry point only, no beans defined here

  common/                          ← cross-cutting reusable code
    constant/                        validation message constants
    dto/                             SCGeneralResponseDto envelope, paged/counter option DTOs
    entity/                          BaseEntity (createdAt/updatedAt/createdBy/updatedBy)
    enums/                           GeneralErrorCode, SCUserRole, SortDirection
    exception/                       SCExceptionHandler (global @RestControllerAdvice) + ex/
    models/                          SCAuthUser, SCFilterQuery, SCPagedResult
    util/                            auth, error-data and pagination helpers

  config/                          ← Spring configuration, not a domain
    auditor/                         JPA auditing (createdBy/updatedBy resolution)
    cache/                           CacheConfig — @EnableCaching + Caffeine CacheManager
    security/                        SecurityConfig, SecurityBeansConfig, JWT filter/service
    web/                             WebConfig (context path, CORS)

  domains/                        ← one folder per feature
    auth/                            login, register, change email/password
    languages/                       active/inactive locale registry
    users/                           profile, roles, admin user management
    categories/                      self-referential category tree + translations
    tags/                            recipe/ingredient tags + approval workflow
    ingredients/                     ingredients + allergens/dietary flags + translations
    recipes/                         recipe CRUD, draft/publish, feeds, search
    favorites/                       user ↔ recipe favourites
    ratings/                         user ↔ recipe 1-5 star ratings
    stats/                           recipe_stats — internal only, no controller
```

### Internal structure of each domain

```
domains/<feature>/
  annotations/     custom annotations/validators (only where needed)
  constants/       domain-specific validation constants
  controller/      thin controller — HTTP wiring only, zero business logic
  dto/             Java records for API input/output
  entity/          @Entity JPA classes
  enums/           XErrorCode (implements ISCErrorCode) + PG-enum converters
  exception/       exceptions extending SCGeneralException
  repository/      Spring Data JPA repositories
  service/
    IXService.java                 controller-facing contract, reads security context
    IXDomainBridgeService.java      internal cross-domain contract, explicit data only
    impl/XServiceImpl.java
    impl/XDomainBridgeServiceImpl.java
```

---

## 4. Database Schema

PostgreSQL, managed entirely via `src/main/resources/sql/createSfrigolaDB.sql` — the SQL file is the single source of truth, Hibernate never generates or alters tables.

**Standard columns** on every non-bridge table: `id BIGSERIAL` (internal PK, never exposed), `public_id UUID UNIQUE` (exposed in APIs), plus audit columns (`created_at`, `updated_at`, `created_by`, `updated_by`).

| Table | Purpose |
|---|---|
| `languages` | active locale registry, one row flagged `is_default` |
| `roles` | `ROLE_ADMIN` / `ROLE_USER` / `ROLE_CONTRIBUTOR` |
| `users` | account, role, preferred language, profile fields |
| `categories` + `category_translations` | self-referential tree (`parent_id`), 11 seeded rows |
| `tags` + `tag_translations` | `type` / `scope` / `status` PG enums, approval workflow |
| `ingredients` + `ingredient_translations` | allergens (`TEXT[]`, GIN-indexed), dietary flags |
| `recipes` + `recipe_translations` | difficulty/meal/season enums, publish flag |
| `recipe_ingredients`, `recipe_tags`, `ingredient_tags` | bridge tables (no `id`/`public_id`, cascade-deleted) |
| `favorites`, `ratings` | user ↔ recipe, one row per user per recipe |
| `recipe_stats` | 1:1 with `recipes`, rolled up by the `stats` bridge service |

PostgreSQL native enums (`difficulty_level`, `meal_type`, `season_type`, `tag_type`, `tag_scope`, `tag_status`) are mapped with `AttributeConverter` + `@ColumnTransformer(write = "?::enum_type")`, co-located with the enum in the domain's `enums/` package.

---

## 5. Architecture & Patterns

**Response envelope** — every endpoint returns `SCGeneralResponseDto<T, K>`:

```json
{
  "data": { "...": "..." },
  "option": { "currentPage": 1, "pageSize": 10, "totalElements": 42, "totalPages": 5, "hasMore": true },
  "errorData": null
}
```

**Pagination** — `SCFilterQuery` (searchKey, sortBy, sort, take, page) flows into the service, which returns `SCPagedResult<T>` (content + `SCPagedOptionDto`); the controller maps it straight into the envelope.

**Dual service contract** — every domain exposes two service interfaces:
- `IXService` — controller-facing, reads the security context internally.
- `IXDomainBridgeService` — internal, cross-domain, receives every input explicitly and never touches the security context. Bridges always return entities/primitives/ids, never `dto/` classes — e.g. `IRecipeStatsDomainBridgeService` is the single point `favorites`/`ratings` write through and `recipes` reads through to keep `recipe_stats` in sync.

**Translatable entities** — every entity with a `*_translations` child table (`categories`, `tags`, `ingredients`, `recipes`) follows one shared pattern via `ILanguageDomainBridgeService`: create requires exactly one translation per active language (no more, no less), update patches a single locale per call, delete cascades translations without touching them explicitly.

**Draft/publish workflow (recipes)** — a contributor's recipe is created as a draft in one language; an admin adds the remaining active-language translations and calls the publish endpoint; any subsequent edit by the contributor silently reverts the recipe to draft.

**Custom exceptions** — all extend `SCGeneralException` (status + error code + message), handled globally by `SCExceptionHandler` — no controller-level exception handling.

**IDs** — `id` (`Long`) is internal-only; `public_id` (`UUID`) is what every API exposes.

---

## 6. Caching

`CacheManager` is Caffeine-backed, configured once in `config/cache/CacheConfig.java` (`@Configuration @EnableCaching`) — kept out of `SfrigolaCoreApplication` on purpose, to leave the entry point clean.

| Cache | Backing query | Max size | TTL | Eviction |
|---|---|---|---|---|
| `languages` | `ILanguageRepository.findAll()` / `findAllByIsActiveTrue()` | 100 | 1 day | none — no in-app CRUD surface on this table |
| `roles` | `ISCRoleRepository.findByName(name)` | 10 | 1 day | none — 3 static seeded rows |
| `categories` | `ICategoryRepository` paginated query | 500 | 1 day | `@CacheEvict(allEntries = true)` on every category mutation |

`@Cacheable` is placed directly on the repository method (per-query granularity) rather than on the service, so each query is cached independently. Any domain with a real in-app CRUD surface must pair its mutating service methods with `@CacheEvict` — reference data with no in-app write path (languages, roles) can safely skip eviction and rely on the TTL alone as a staleness backstop.

---

## 7. Security & Roles

Stateless JWT auth (`jjwt` 0.13.0) via a custom `OncePerRequestFilter` ahead of `BasicAuthenticationFilter`. Authorization is **path-based**, configured as `List<String>` `@Qualifier` beans in `SecurityBeansConfig` (`publicPath`, `authPath`, `contributorPath`, `adminPath`, plus narrower `publicGetPath`/`authenticatedGetPath` overrides for GET-only rules on otherwise-mutating paths).

A `RoleHierarchy` bean (`scHierarchy`) makes `ROLE_ADMIN > ROLE_CONTRIBUTOR > ROLE_USER` — a higher role automatically satisfies a lower-role path rule.

| Role | Grants |
|---|---|
| `ROLE_USER` | browse, search, rate, favourite |
| `ROLE_CONTRIBUTOR` | + create/manage own recipes, suggest tags |
| `ROLE_ADMIN` | + full catalogue management (categories, tags, ingredients), publish/unpublish recipes, user administration |

---

## 8. Error Handling & HTTP Status Codes

Every error response uses the same envelope as a success response, just with `errorData` populated instead of `data`:

```json
{
  "data": null,
  "option": null,
  "errorData": { "errorMap": { "TAG_SLUG_ALREADY_EXISTS": "A tag with slug 'spicy' already exists" } }
}
```

**The status code tells the client which of two buckets the error falls into — this is the contract every client (Flutter app, web portal) is built against:**

| Bucket | Codes | Client behavior |
|---|---|---|
| **Global** | `401`, `5xx` | Handled once, centrally, by an HTTP interceptor. The calling screen never reads `errorData`. `401` → refresh-or-logout + redirect to login (session/token invalid — never used for "wrong password"). `5xx` → generic "Something went wrong, try again" toast. |
| **Case-by-case** | `400`, `403`, `404`, `409` | Never intercepted. The calling screen always reads `errorData.errorMap`'s key and reacts specifically — inline field error, "not found" empty state, "already exists" message, disabled-action message, etc. |

This split is a deliberate design choice, not incidental: it means a client only ever needs *one* interceptor rule ("is it 401 or 5xx?") to decide whether to handle an error itself — everything else is guaranteed to carry an `errorCode` worth showing to the user.

### Error code legend

**500 — server fault, global bucket** (`errorCode` not meant to be shown to the user):
`SERVER_ERROR` (generic fallback) · `NO_ROWS_AFFECTED` · `DATA_CORRUPTED` · `SECURITY_SYSTEM_ERROR` · `INVALID_ROLE_FROM_STRING`

**401 — session/token invalid, global bucket:**
`ENV_NOT_AVAILABLE` / `JWT_EXPIRED` / `JWT_VALIDATION_FAILED` (JWT filter) · `NOT_AUTHORIZED` (no auth on protected route) · `NO_USER_AUTH` (token valid, user gone)

**403 — authenticated but not allowed, case-by-case:**
`NOT_AUTHORIZED` (Spring Security role/path denial) · `USER_NOT_ACTIVE` (account deactivated) · `NOT_RECIPE_OWNER` · `CANNOT_CHANGE_ROLE_TO_ADMIN` · `CANNOT_CHANGE_OWN_ACTIVE_STATUS`

**404 — resource not found, case-by-case:**
`ENTITY_NOT_FOUND` (generic fallback) · `SELECTED_CATEGORY_NOT_FOUND` · `TAG_NOT_FOUND` · `RECIPE_NOT_FOUND` · `INGREDIENT_NOT_FOUND` · `USER_NOT_FOUND` · `FAVORITE_NOT_FOUND` · `RATING_NOT_FOUND`

**409 — resource already exists, case-by-case:**
`USER_ALREADY_EXISTS` · `CATEGORY_SLUG_ALREADY_EXISTS` · `TAG_SLUG_ALREADY_EXISTS` · `TAG_LABEL_ALREADY_EXISTS` · `INGREDIENT_SLUG_ALREADY_EXISTS` · `FAVORITE_ALREADY_EXISTS` · `RATING_ALREADY_EXISTS`

**400 — validation & business-rule violations, case-by-case:**
per-field Jakarta Validation messages · `MALFORMED_JSON` · `ILLEGAL_ARGUMENT` · `INVALID_ENUM_CODE` · `BAD_CREDENTIALS` (wrong email/password on login — deliberately not `401`) · `NEW_PASSWORD_SAME_AS_OLD_PASSWORD` · `PASSWORD_DOES_NOT_MATCH_CONFIRMATION_PASSWORD` · `COMPROMISED_PASSWORD` · `OLD_PASSWORD_NOT_MATCH` · `LOCALE_NOT_ACTIVE` · `INVALID_LANG_CODE` · `MISSING_*_LOCALES` · `DUPLICATE_*_LOCALE` · `CATEGORY_HAS_CHILDREN` · `CATEGORY_REORDER_MISMATCH` · `TAG_SCOPE_NOT_ALLOWED` · `CONTRIBUTOR_TRANSLATION_LIMIT_EXCEEDED`

---

## 9. API Reference

Base path for every route below: `/sfrigola-core/api`.

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
| GET | `/languages` | public | `isActive` filter, non-paginated |

### Users — `/users`

| Method | Path | Access | Notes |
|---|---|---|---|
| PATCH | `/users/settings/change-preferred-lang/{code}` | authenticated | |
| PATCH | `/users/profile/update` | authenticated | |
| PATCH | `/users/profile/became-contributor` | authenticated | promotes to `ROLE_CONTRIBUTOR` |
| GET | `/users/admin` | `ROLE_ADMIN` | paginated, sort/search/isActive filters |
| PATCH | `/users/admin/{publicId}/status` | `ROLE_ADMIN` | activate/deactivate user |

### Categories — `/categories`

| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/categories` | public | paginated, self-referential tree |
| GET | `/categories/admin` | `ROLE_ADMIN` | paginated, admin preview |
| GET | `/categories/admin/{publicId}` | `ROLE_ADMIN` | full details with translations |
| POST | `/categories/admin`, `/categories/admin/{parentPublicId}` | `ROLE_ADMIN` | create, optional parent |
| PUT | `/categories/admin/{publicId}` | `ROLE_ADMIN` | update one translation |
| DELETE | `/categories/admin/{publicId}` | `ROLE_ADMIN` | blocked if it has children |
| PUT | `/categories/admin/reorder` | `ROLE_ADMIN` | bulk `sort_order` update |

### Tags — `/tags`

| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/tags` | `ROLE_CONTRIBUTOR`+ | paginated |
| POST | `/tags/suggest` | `ROLE_CONTRIBUTOR`+ | creates a `pending` tag |
| GET | `/tags/admin` | `ROLE_ADMIN` | paginated |
| GET | `/tags/admin/{publicId}` | `ROLE_ADMIN` | full details |
| POST | `/tags/admin` | `ROLE_ADMIN` | create, auto-approved |
| PATCH | `/tags/admin/{publicId}/status/{status}` | `ROLE_ADMIN` | approve/reject |
| PUT | `/tags/admin/{publicId}` | `ROLE_ADMIN` | update one translation |
| DELETE | `/tags/admin/{publicId}` | `ROLE_ADMIN` | delete |

### Ingredients — `/ingredients`

| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/ingredients` | `ROLE_CONTRIBUTOR`+ | paginated |
| GET | `/ingredients/admin` | `ROLE_ADMIN` | paginated |
| GET | `/ingredients/admin/{publicId}` | `ROLE_ADMIN` | full details |
| POST | `/ingredients/admin` | `ROLE_ADMIN` | create |
| PUT | `/ingredients/admin/{publicId}` | `ROLE_ADMIN` | update one translation |
| DELETE | `/ingredients/admin/{publicId}` | `ROLE_ADMIN` | delete |

### Recipes — `/recipes`

| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/recipes/home/category/{categoryId}` | public | fixed short rows per feed type (`QUICK`, `LIKE_A_CHEF`, `ECONOMICAL`; `VIRAL` not yet on this route) |
| GET | `/recipes/feed/{feedType}` | public | paginated feed group; `VIRAL` routed through the stats bridge |
| GET | `/recipes/search`, `/recipes/search/category/{categoryId}` | public | paginated, matches title/description |
| GET | `/recipes/favorites` | authenticated | the caller's favourited recipes |
| GET | `/recipes/details/{publicId}` | public | draft recipes 404 like non-existent ones |
| GET | `/recipes/admin`, `/recipes/admin/category/{categoryId}` | `ROLE_ADMIN` | paginated, includes drafts, dietary/status filters |
| GET | `/recipes/admin/details/{publicId}` | `ROLE_ADMIN` | includes ingredient/tag lists |
| POST | `/recipes` | `ROLE_CONTRIBUTOR`+ | author from security context, never trusted from payload |
| PUT | `/recipes/{publicId}` | author or `ROLE_ADMIN` | one translation per call |
| PATCH | `/recipes/admin/{publicId}/publish` | `ROLE_ADMIN` | the only way to (re-)publish |
| PATCH | `/recipes/admin/{publicId}/unpublish` | `ROLE_ADMIN` | |
| DELETE | `/recipes/{publicId}` | author or `ROLE_ADMIN` | cascades translations/ingredients/tags |

### Favorites — `/favorites`

| Method | Path | Access |
|---|---|---|
| POST | `/favorites/{recipePublicId}` | authenticated |
| DELETE | `/favorites/{recipePublicId}` | authenticated |

### Ratings — `/ratings`

| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/ratings/recipe/{recipePublicId}/stats` | authenticated | average rating + total count |
| POST | `/ratings` | authenticated | one rating per user per recipe |
| PUT | `/ratings/recipe/{recipePublicId}` | authenticated | edits the caller's own rating |

---

## 10. API Docs & Postman

### Swagger / OpenAPI

Generated automatically from the controllers via springdoc.

| What | URL |
|---|---|
| Swagger UI | `http://localhost:{SERVER_PORT}/sfrigola-core/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:{SERVER_PORT}/sfrigola-core/api/v3/api-docs` |

Both are public. To try authenticated endpoints, click **Authorize** in the UI and paste a JWT from `POST /auth/login` (no `Bearer ` prefix — Swagger adds it).

> `/swagger-ui/index.html` has **no** `/api` prefix (static resource); `/api/v3/api-docs` **does** (served by a real controller).

### Postman

A ready-to-import collection covering every endpoint lives at `postman/Sfrigola-Core.postman_collection.json`.

1. Postman → **Import** → select the file.
2. Open the collection → **Variables** tab → set `baseUrl` to match your `SERVER_PORT`.
3. Run **Auth → Login** once — its test script auto-saves the JWT into the `token` collection variable.
4. Every other request inherits `Authorization: Bearer {{token}}` automatically.

Re-run **Login** with different credentials to switch user (e.g. admin vs. regular user) — `token` updates immediately for every request. Path variables (`{{publicId}}`, `{{categoryId}}`, etc.) are collection-level variables — fill them with real values returned by a prior call.

---

## 11. Testing

```bash
./mvnw test
```

Integration tests cover the controller and service layer for every domain, using `spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-test`, `spring-boot-starter-validation-test` and `spring-boot-starter-webmvc-test`. Every PR must ship with at least one associated test.

---

## 12. Versioning & Releases

The project follows [**Semantic Versioning**](https://semver.org) (`MAJOR.MINOR.PATCH`), tracked in `pom.xml`, and the bump is **fully automated** by [release-please](https://github.com/googleapis/release-please) (`.github/workflows/release-please.yml`, config in `release-please-config.json` / `.release-please-manifest.json`). No manual version edits, no manual tagging — `pom.xml`'s `<version>` is only ever touched by the release PR described below.

### How a bump gets triggered

release-please reads the **commit messages on `main`**, not the PR body. Every PR on this repo is **squash-merged**, so the commit that lands on `main` is exactly the **PR title** — this means the PR title must itself be a valid [Conventional Commit](https://www.conventionalcommits.org), following the same convention already required by this project (`feat:`, `fix:`, `refactor:`, ...).

| PR title prefix | Bump | Example |
|---|---|---|
| `fix: ...` | patch | `fix: correct pagination offset on categories` → `1.0.0` → `1.0.1` |
| `feat: ...` | minor | `feat: add ratings stats endpoint` → `1.0.1` → `1.1.0` |
| `feat!: ...` or a `BREAKING CHANGE:` footer in the PR description | major | `feat!: rename recipe status field` → `1.1.0` → `2.0.0` |
| `chore:`, `docs:`, `refactor:`, `test:`, `ci:`, `style:`, `perf:`, `build:` | **no bump** | still merges fine, just doesn't move the version |

If a PR mixes an unscoped title with the real change type (e.g. `"Add WebConfig class..."` with no prefix), release-please can't parse it and silently skips it for versioning purposes — the merge still works, it just contributes nothing to the next release.

### The release PR

On every push to `main` with at least one bump-worthy commit since the last release, the workflow opens/updates a standing PR titled `chore(main): release X.Y.Z` that bumps `pom.xml` and appends to `CHANGELOG.md`. **Merging that PR** is what actually creates the `vX.Y.Z` git tag and the GitHub Release — nothing is released until it's merged. Config has `skip-snapshot: true`, so versions stay plain `X.Y.Z` (no `-SNAPSHOT` in between).

`CLAUDE.md`'s `**Version:**` field is **not** touched automatically — update it by hand after a release merges.

---

## 13. Dependencies

| Package | Version | Purpose |
|---|---|---|
| `spring-boot-starter-webmvc` | 4.1.0 | REST controllers |
| `spring-boot-starter-validation` | 4.1.0 | Jakarta Bean Validation |
| `springdoc-openapi-starter-webmvc-ui` | 2.8.6 | OpenAPI/Swagger generation |
| `spring-boot-starter-security` | 4.1.0 | Auth filter chain |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | 0.13.0 | JWT issuing/parsing |
| `spring-boot-starter-data-jpa` | 4.1.0 | Hibernate/JPA |
| `postgresql` | runtime | JDBC driver |
| `spring-boot-starter-cache` | 4.1.0 | Spring caching abstraction |
| `caffeine` | latest | Cache provider (see [Caching](#6-caching)) |
| `spring-boot-starter-aop` | 4.0.0-M2 | Execution-time logging aspect |
| `lombok` | latest | Boilerplate reduction |
| `spring-boot-devtools` | 4.1.0 | Hot reload (dev only) |
| `spring-boot-docker-compose` | 4.1.0 | Auto-starts Postgres in dev |

---

## 14. License

Released under the [MIT License](LICENSE).

---

<div align="center">

Built with ❤️ by **Stefano Biddau**

[stefanobiddau.com](https://www.stefanobiddau.com) · [@stefanoBid](https://github.com/stefanBid)

</div>