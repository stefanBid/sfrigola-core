# CLAUDE.md — Project Context

> This file is automatically loaded by Claude Code at startup.
> Update it as the project evolves.

---

## Project Identity

- **GroupId:** `com.sb`
- **ArtifactId:** `sfrigola-core`
- **Version:** `1.0.0`
- **Base package:** `com.sb.sfrigola_core`
- **Context path:** `/sfrigola-core`
- **Port:** configured via env `SERVER_PORT`

---

## Internal Structure of Each Domain

```
domains/<feature>/
├── annotations/        # (only if the feature has custom annotations/validators)
├── constants/          # domain-specific validation constants
├── controller/         # thin controller, HTTP wiring only
├── dto/                # Java records for API input/output
├── entity/             # @Entity JPA
├── enums/              # ErrorCode enum (implements ISCErrorCode)
├── exception/          # custom exceptions extending SCGeneralException
│   └── ex/
├── repository/         # JPA Repository interfaces
└── service/
    ├── IXService.java                   # controller-facing contract (reads security context)
    ├── IXDomainBridgeService.java       # internal bridge contract (does NOT read security context)
    └── impl/
        ├── XServiceImpl.java
        └── XDomainBridgeServiceImpl.java
```

---

## Database Schema

Database schema lives in `src/main/resources/sql/createSfrigolaDB.sql` — read it directly, it's the source of truth (`ddl-auto=none`, Hibernate never generates or alters tables). One non-obvious point not visible from a quick skim: `users.avatar_url` is nullable and unset at signup; once image storage exists it will default to a placeholder URL, user can override with a personal photo.

---

## Implementation Status (v1.0.0 — Sprints 1-5 complete)

| Domain      | Entity | Repository | Service | Controller | Notes |
|-------------|--------|------------|---------|------------|-------|
| `auth`      | SCUser/SCRole (in users) | — | done | done | login, register, change-email, change-password |
| `languages` | done | done | done | done | GET paginated |
| `users`     | done | done | done | done | update-profile, change-lang, become-contributor, admin CRUD |
| `categories`| done | done | done | done | Category + CategoryTranslation; self-referential parent; has domain bridge |
| `tags`      | done | done | done | done | Tag + TagTranslation + enums + converters; has domain bridge |
| `ingredients` | done | done | done | done | Ingredient + IngredientTranslation + IngredientTag bridge table; has domain bridge |
| `recipes`   | done | done | done | done | Recipe + RecipeTranslation + RecipeTag/RecipeIngredient bridge tables; has domain bridge (getRecipeEntityByPublicIdOrThrow, getRecipesByIdsWithLocale) |
| `favorites` | done | done | done | done | authenticated-only: list/add/remove; has domain bridge (isFavoritedByUser, getFavoritedRecipeIds) for future recipes consumption |
| `ratings`   | done | done | done | done | authenticated-only: stats/add/edit; no bridge — nothing outside this domain consumes ratings directly, only via the stats bridge |
| `stats`     | done | done | n/a (bridge only) | no controller by design | RecipeStats entity, 1:1 with recipes via `@MapsId`; single bridge (`IRecipeStatsDomainBridgeService`) — written by favorites/ratings, read by recipes |

---

## Implemented API Endpoints

Base path: `/sfrigola-core`. Full method/path/access table for every endpoint, plus Postman collection sync rules: see the `api-endpoints-reference` skill.

`isPublished` is never part of `AddRecipeDto`/`UpdateRecipeDto` — it is only ever set by the service, never trusted from the client:
- **Create**: a contributor's translation requirement is exactly one, in any active language of their choosing — the recipe is created with `isPublished = false`. An admin's translations must cover every active language, no more no less, and the recipe is created with `isPublished = true` immediately.
- **Update**: if the actor is not an admin, the recipe is unconditionally reverted to `isPublished = false` — any content change needs re-approval. An admin's edit never touches `isPublished`.
- **Publish/unpublish**: `PATCH /recipes/admin/{publicId}/publish|unpublish`, admin-only — the only way to set `isPublished` back to `true`.

---

## Architectural Patterns

### Universal Response Envelope

Every endpoint returns `SCGeneralResponseDto<T, K>`:
```java
SCGeneralResponseDto.success(data)                    // single item
SCGeneralResponseDto.success(list, pagedOptionDto)    // paginated list
SCGeneralResponseDto.successMutation("Done")          // mutation (string message)
SCGeneralResponseDto.error(errorData)                 // error (handled by SCExceptionHandler)
```

### Pagination

Always use `SCFilterQuery` + `SCPagedResult` + `SCPagedOptionDto`:
```java
// Controller
SCFilterQuery.essential(sortBy, SortDirection.fromString(sort), take, page);
SCFilterQuery.essentialWithSearch(searchKey, sortBy, sort, take, page);
SCFilterQuery.powerful(searchKey, sortBy, sort, take, page, customFilter);

// Service returns
SCPagedResult<XDto>   // { content: List<XDto>, pagedOptionDto: SCPagedOptionDto }
```

### Custom Exceptions

All extend `SCGeneralException`:
```java
protected SCGeneralException(HttpStatus status, ISCErrorCode errorCode, String errorMessage)
// → toErrorMap() = Map.of(errorCode.code(), errorMessage)
```

Each domain has its own `XErrorCode enum implements ISCErrorCode`.

### Endpoint Versioning

Spring Boot 4.x feature — `version = "1.0"` on mapping annotations:
```java
@GetMapping(version = "1.0")
@PostMapping(value = "/login", version = "1.0")
```

### Security: path-based via @Qualifier beans

Paths configured as `List<String>` beans in `SecurityBeansConfig`:
- `publicPath` — no auth required
- `authPath` — any authenticated role
- `adminPath` — minimum role ROLE_ADMIN
- `userPath` — minimum role ROLE_USER
- `contributorPath` — minimum role ROLE_CONTRIBUTOR
- `allowedOriginsPaths` — CORS allowed origins

A `RoleHierarchy` bean (`scHierarchy`) declares `ROLE_ADMIN > ROLE_CONTRIBUTOR > ROLE_USER`, applied via `AuthorityAuthorizationManager` in `SecurityConfig`. Higher roles automatically satisfy lower-role path checks (e.g. ROLE_ADMIN passes `contributorPath` and `userPath` too) — path beans express the *minimum* role required, not an exclusive match.

Every new API endpoint must be registered in the correct bean in `SecurityBeansConfig`.

### Dual Service Contract

1. **`IXService`** — controller-facing: reads security context internally
2. **`IXDomainBridgeService`** — internal bridge: receives data explicitly, does NOT read security context

Example: `ISCUserService` (controller-facing) vs `ISCUserDomainBridgeService` (bridge used by auth).

**Bridge return types: entities, never DTOs.** `IXDomainBridgeService` methods return JPA entities (`Category`, `Tag`, `Recipe`, `RecipeStats`, ...), `Optional<Entity>`/`List<Entity>`/`Map<Long, Entity>`, or plain primitives/ids (`boolean`, `Long`, `Set<Long>`, `Page<Long>`) — never a `dto/` class. Bridges are service-to-service, not API-facing; a DTO built for one caller's JSON shape has no business dictating another domain's internal contract, and reusing a `dto/view` class (built for a controller response) as a bridge return type couples the two so a frontend-only field change forces a bridge signature change too. If the caller only needs a couple of fields, let it read them off the entity itself.
- Exception: value/model types that are not DTOs are fine (e.g. `SCAuthUser`, the security principal used by `ISCUserDomainBridgeService.findByEmailWithRole` — required detached/serializable for the security context and JWT, never a JPA entity there). The rule is "never DTO", not "always entity" — models, primitives, and ids are all fine; only `dto/` (view/input) classes are excluded.
- When a bridge has no consumer left (e.g. a single-purpose passthrough bridge whose only caller was removed), delete it — don't keep a bridge alive "just in case". Prefer one consolidated bridge per concept over one per consumer (e.g. `IRecipeStatsDomainBridgeService` is the single point both `favorites` and `ratings` use to keep `recipe_stats` in sync, and the single point `recipes` uses to read it — there is no separate `ratings`-owned stats bridge).

### Translatable Entities Pattern

Applies to every entity with a `*_translations` child table (`categories`, `tags`, `ingredients`, `recipes`). Modeled first on `categories`/`tags` in sprint 4/5 — follow this exactly for new translatable domains instead of re-deriving it.

**Locale validation/resolution always goes through `ILanguageDomainBridgeService`** — never inline a `Language`/locale null-check that duplicates it:
- `validateLocaleIsActiveOrThrow(locale)` — single locale, no map loaded yet.
- `validateLocaleIsActiveByActiveLanguagesMapKeysOrThrow(activeLanguagesKeys, locale)` — guard a locale when the active-languages map is already in memory (avoids a second query).
- `getLangFromEntitiesMapFromKeyOrThrow(activeLanguagesMap, locale)` — resolve **and** validate a `Language` entity in one call, when the map of entities is already loaded.
- All three throw `common.exception.ex.SCLocaleNotActiveException` — domains must not keep their own `LocaleNotActiveException`/`InvalidXLocaleException`.

**The `translatedLanguages: Map<String, String>` field on admin preview DTOs (`XPreviewAdminDto`) is also built via `ILanguageDomainBridgeService`** — never inline the stream that computes it:
- Every JPA translation entity (`CategoryTranslation`, `TagTranslation`, `IngredientTranslation`, `RecipeTranslation`) implements `common.interfaces.ISCTranslationEntity` (exposes `getLanguage()`). New translatable domains must do the same — it's a one-line `implements` addition, Lombok's `@Getter` already provides the method.
- `buildTranslatedLanguagesMap(translations, activeLanguagesMap)` — generic over any `List<? extends ISCTranslationEntity>`; filters the entity's translations down to currently-active locales and maps each to its display name. This is the localization-coverage map shown in admin CMS previews.
- `toSimpleLanguagesMap(languageEntitiesMap)` — flattens a `Map<String, Language>` (e.g. from `getActiveLanguageEntitiesMap()`) into `Map<String, String>`, for the common case where a `create`/`update` method already loaded the entities map for translation resolution and also needs the simple map to feed `buildTranslatedLanguagesMap` or the response DTO.
- Domains must not keep their own private `toSimpleLanguagesMap`/translated-languages-stream copies — this was previously duplicated across `categories`/`tags`/`ingredients`/`recipes` and consolidated into the bridge for exactly this reason.

**Create (`createNewX`)** — payload carries a `List<XTranslationInputDto>` that must cover **every** active language, no more no less:
1. Reject duplicate `langCode` in the list → `DuplicateXLocaleException`.
2. Reject if the set of `langCode`s doesn't exactly match the active-languages set → `MissingXLocalesException`.
3. Resolve each `Language` via `getLangFromEntitiesMapFromKeyOrThrow`, build translation entities, set the owning-entity back-reference on each before save.
4. The endpoint/service also takes a `locale` param used **only** to pick which translation to show in the response preview (filter the just-built list by `locale`) — it has no bearing on validation.

**Update (`updateX`)** — payload carries a **single** `specificTranslation` (one locale per call), never a list:
1. Resolve+validate that locale via `getLangFromEntitiesMapFromKeyOrThrow`.
2. Look up an existing translation for that locale on the entity: if absent, create it (set the back-reference, add to the collection); if present, patch only the fields that actually changed.
3. No "cover all locales" check on update (unlike create) — partial edits are expected.
4. To touch N locales, the caller makes N calls. No `locale` query param needed — the response preview is always the translation just upserted.
5. No delete-of-a-single-translation capability via update. If a domain ever needs that, it must be a separate explicit endpoint/method — don't overload update with a "blank label ⇒ delete" convention.

**Delete (`deleteX`)** — no `locale` param, no translation preview, no admin-preview DTO returned:
- Delete the parent row; translations cascade via `CascadeType.ALL` on the JPA relation (and DB `ON DELETE CASCADE`) — never touch/query translations explicitly in the delete method.
- Return only the deleted entity's `publicId` (`UUID`), not a DTO.

### BaseEntity

`@MappedSuperclass` with automatic auditing via Spring Data JPA Auditing:
- `createdAt` / `updatedAt` — `Instant`
- `createdBy` / `updatedBy` — `String` (actor's username)

All entities must extend `BaseEntity`.

### Entity Convention

```java
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;                           // internal PK, never exposed in APIs

@Column(name = "public_id", nullable = false, unique = true, updatable = false)
private UUID publicId = UUID.randomUUID(); // exposed in APIs
```

### PostgreSQL ENUM → Java Enum (AttributeConverter)

PostgreSQL native enums (e.g. `tag_type`, `tag_scope`, `tag_status`) are mapped via `@Converter(autoApply = true)`:

```java
// enums/TagTypeConverter.java
@Converter(autoApply = true)
public class TagTypeConverter implements AttributeConverter<TagType, String> {
    @Override
    public String convertToDatabaseColumn(TagType attr) {
        return attr == null ? null : attr.getValue();
    }
    @Override
    public TagType convertToEntityAttribute(String dbData) {
        return Arrays.stream(TagType.values())
            .filter(e -> e.getValue().equals(dbData))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown: " + dbData));
    }
}

// Entity field — must declare columnDefinition to reference the PG ENUM type
// AND @ColumnTransformer to force an explicit cast on write (see rules below)
@Column(name = "type", nullable = false, columnDefinition = "tag_type")
@ColumnTransformer(write = "?::tag_type")
private TagType type;
```

Rules:
- One converter class per enum, co-located in the same `enums/` package as the enum.
- `autoApply = true` — no `@Convert` annotation needed on entity fields.
- `columnDefinition = "pg_enum_name"` is mandatory on the `@Column` — without it Hibernate maps to VARCHAR and PostgreSQL rejects the cast.
- `@ColumnTransformer(write = "?::pg_enum_name")` (`org.hibernate.annotations.ColumnTransformer`) is **also mandatory** on every converted-enum field. `columnDefinition` only affects DDL generation (irrelevant here since `ddl-auto=none`) — it does NOT change how Hibernate binds the JDBC parameter at runtime. Without `@ColumnTransformer`, Hibernate sends the converted value as a plain `VARCHAR` on every INSERT/UPDATE, and PostgreSQL rejects the implicit `varchar → pg_enum` assignment cast with: `column "x" is of type x_enum but expression is of type character varying`. `@ColumnTransformer(write = "?::pg_enum_name")` makes Hibernate emit the explicit cast in the generated SQL (`?::tag_type`), which PostgreSQL accepts.
- Enum must expose `getValue()` returning the lowercase string stored in the DB.

### Boolean fields in entities

Always use `boolean` primitive (NOT `Boolean` wrapper) for NOT NULL boolean columns:

```java
// CORRECT — column is NOT NULL
@Column(name = "is_active", nullable = false)
private boolean isActive = true;

// WRONG — wrapper implies nullable, Lombok generates getIsActive() not isActive()
@Column(name = "is_active", nullable = false)
private Boolean isActive = true;
```

Use `Boolean` wrapper only when the DB column is genuinely nullable.

---

## HTTP Status Code Policy

Every status code the API returns falls into exactly one of two client-handling buckets. This is a hard rule for new exceptions — pick the bucket first, then the code, never the other way around.

- **Global bucket** (`401`, `5xx`) — client-side interceptor handles it once, centrally. The component that fired the request never inspects `errorData`.
  - `401` → session/token is no longer valid → interceptor triggers refresh-or-logout + redirect to login. **The only acceptable use of `401` is "this JWT/session is invalid."** Never use `401` for a business-facing "you typed the wrong password" — that belongs in the case-by-case bucket (see `SCBadCredentialException` below).
  - `5xx` → unexpected server-side fault, not caused by client input → interceptor shows a generic "Something went wrong, try again" toast. Client never parses `errorCode` for these.
- **Case-by-case bucket** (`400`, `403`, `404`, `409`) — never intercepted. The calling component always reads `errorData`'s `errorCode` key and reacts specifically (inline field error, "not found" state, "already exists" message, disabled-action message, etc).

Full exception → status → error-code table (every custom exception in the codebase): see the `error-code-reference` skill.

Rule for every **new** exception: decide client-handling bucket first (does the client just need to know "retry" / "log in again", or does it need the specific `errorCode` to react?), then pick `401`/`5xx` for the former, `400`/`403`/`404`/`409` for the latter — never introduce a new global-bucket code.

---

## Best Practices

### Project Structure

Package-by-feature under `domains/`. No global horizontal layers (`/controller`, `/service`, etc.).
Cross-cutting config goes in `config/`. Reusable cross-domain code goes in `common/`.

### Dependency Injection

Always constructor injection with `@RequiredArgsConstructor` + `final`.

### REST Controller

- Always return DTOs, never JPA entities.
- `ResponseEntity<SCGeneralResponseDto<T, K>>` using the record factory methods.
- `@Validated` on the class to validate `@RequestParam`/`@PathVariable`.
- `@Valid` on `@RequestBody` to validate DTOs.
- Thin controllers: zero business logic.

### Service Layer

- `@Transactional(readOnly = true)` for reads.
- `@Transactional` for writes.
- All business logic in the service, never in the controller.

### Repository

- `JpaRepository<Entity, Long>` as base.
- Derived queries for simple cases.
- `@Query` JPQL for complex queries.
- Avoid N+1: use `JOIN FETCH` or `@EntityGraph`.

### Service Interfaces (Javadoc)

Mandatory Javadoc on every interface:
- Class: general contract, whether it reads security context or receives explicit data, "succeed or throw" contract if applicable.
- Method: `@param`, `@return`, `@throws` with fully-qualified names (no extra imports just for docs).

### Exception Handling

Global handler: `SCExceptionHandler` (`@RestControllerAdvice` in `common/exception/`).
Custom exceptions extend `SCGeneralException`. Do not add handlers at controller level.

### Input Validation

Jakarta Validation on DTOs. Message constants in `SCRequestParamValidationCodeConstants`.

### Configuration

- Config is YAML, split by profile: `application.yaml` (common — context path, datasource wiring, JPA, aspect thresholds), `application-dev.yaml`, `application-prod.yaml`. No single `application.properties` anymore. `spring.profiles.default: dev` in the base file — no `SPRING_PROFILES_ACTIVE` set at all falls back to `dev`.
- Secrets always via env vars (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`, `${SERVER_PORT}`, `${JWT_SECRET_KEY}`). No YAML file, in any profile, ever contains a literal secret value.
- **App code never hardcodes a fallback default for an externally-configured value — that's a config-layer concern only.** Every `Environment.getProperty(KEY)` call in Java either uses the resolved value or throws; it never supplies a Java-side default (no `env.getProperty(KEY, someHardcodedDefault)`). Convenience defaults for non-secret, environment-specific values (`JWT_EXPIRATION_MS`, `ALLOWED_ORIGINS`, and — dev only — `JWT_SECRET_KEY`) live exclusively in `application-dev.yaml`/`application-prod.yaml` as `${VAR:default}`; `application-prod.yaml` omits the default (and omits the key entirely if it has no default — a self-referencing `KEY: ${KEY}` with nothing backing it resolves as a circular-placeholder boot failure instead of the intended clean error) so a missing value fails the same way in every deploy method (bare `java -jar`, systemd, Docker).
  - Missing `JWT_SECRET_KEY`/`JWT_EXPIRATION_MS` at token-generation/validation time → `SCAuthSecuritySystemException` (500) from `JwtService`, or `ENV_NOT_AVAILABLE` (401) from `JwtValidationFilter` — never a silently-reused hardcoded secret.
  - Missing `ALLOWED_ORIGINS` → `SecurityBeansConfig.allowedOriginsPaths()` throws `IllegalStateException` at boot — application context fails to start, never a silent empty-CORS fallback.
  - Applies to every future externally-configured value, not just JWT/CORS — same standard project-wide.
- DB schema managed via `src/main/resources/sql/createSfrigolaDB.sql`, not by Hibernate.

### Logging

`@Slf4j` + SLF4J. Never log passwords, tokens, or personal data.

### Caching

- `CacheManager` bean lives in `config/cache/CacheConfig.java` (`@Configuration @EnableCaching`) — never on `SfrigolaCoreApplication`, keep the entry-point file clean.
- Provider: Caffeine, wired manually via `SimpleCacheManager` holding one named `CaffeineCache` per cache (not Spring Boot's Caffeine auto-config) — each cache gets its own `maximumSize` + `expireAfterWrite`, set per use case, not a single global spec.
- `@Cacheable` goes directly on the repository interface method (not the service layer), so each query is cached independently instead of lumping every repo call inside a service method under one policy. Cache key is built explicitly (e.g. `'categories_' + #locale + '_' + #pageable.pageNumber + '_' + #pageable.pageSize`) — include every parameter that changes the result set.
- Every new cache name used in a `@Cacheable` must have a matching `CaffeineCache` registered in `CacheConfig` — an unregistered cache name fails at runtime (`Cannot find cache named '...'`).
- **Eviction rule:** if the cached entity has any in-app CRUD surface (create/update/delete/reorder endpoints), every mutating service method must carry `@CacheEvict(value = "<cache>", allEntries = true)` — do not rely on `expireAfterWrite` alone to cover writes. Reference-data caches with zero in-app CRUD surface (e.g. `languages`, `roles` — writes only happen out-of-band directly on Postgres) may skip eviction entirely, since `@CacheEvict` could never fire anyway; the Caffeine TTL is then a staleness backstop, not a substitute for eviction where writes do exist.
- A harmless JSpecify `@NullMarked` override warning may appear on inherited repo methods (e.g. `findAll()`) once `@Cacheable` is added — ignore it, don't add a `package-info.java` just to silence it.

---

## Code Conventions

- Cross-domain class prefix: `SC` (e.g. `SCUser`, `SCRole`, `SCGeneralException`)
- Commits: conventional commits in English (`feat:`, `fix:`, `refactor:`, etc.)
- Every PR must have at least one associated test

---

## Notes for Claude

- Actual domain package: `com.sb.sfrigola_core.domains.<feature>` — not `shared/` as in older versions.
- Translation entities (e.g. `CategoryTranslation`, `TagTranslation`) live in the same domain as their parent entity (`domains/categories/entity/`), never in a separate `translations` domain — but they must `implements ISCTranslationEntity` (`common/interfaces`) so `ILanguageDomainBridgeService.buildTranslatedLanguagesMap`/`toSimpleLanguagesMap` work for them (see Translatable Entities Pattern).
- Security, auditing, web config: in `config/`, never inside a domain.
- Cross-cutting reusable code: in `common/`, not in a domain.
- `stats` has no controller: internal service called by `rating` and `favorite`.
- Every new API path must be registered in the correct bean in `SecurityBeansConfig`.
- `public_id` (UUID) is what gets exposed in APIs; `id` (Long) is internal DB only.
- `preferred_lang` in `SCUser` is a `String` (FK to `languages.code`), not a referenced entity.
- `ddl-auto=none` — any schema change requires updating the SQL file.
- Tag approval flow (`pending → approved/rejected`) is ROLE_ADMIN only.
- `tags/enums/` holds both the enum and its converter — keep them co-located, never split to `common/`.
- For any new PostgreSQL native ENUM: create the enum + `AttributeConverter` in the domain's `enums/` package; use `columnDefinition` on the entity `@Column`.
- Never add `env.getProperty(KEY, hardcodedDefault)` (or any Java-side fallback) for an externally-configured value — get-or-throw only, defaults belong in `application-dev.yaml`/`application-prod.yaml` (see Configuration).
- Flag any request that violates these conventions before proceeding.