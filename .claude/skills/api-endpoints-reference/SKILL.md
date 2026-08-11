---
name: api-endpoints-reference
description: Full reference of every sfrigola-core REST endpoint (method, path, access level, notes) plus Postman collection sync conventions. Load when adding, changing, removing, or documenting an API endpoint, or when updating postman/Sfrigola-Core.postman_collection.json.
---

# API Endpoints Reference

Base path: `/sfrigola-core`

## Auth — `/auth`

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| POST | `/auth/login` | public | returns JWT |
| POST | `/auth/register` | public | |
| PATCH | `/auth/change-email` | authenticated | |
| PATCH | `/auth/change-password` | authenticated | |

## Languages — `/languages`

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| GET | `/languages` | authenticated | paginated, isActive filter |

## Users — `/users`

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| PATCH | `/users/settings/change-preferred-lang/{code}` | authenticated | |
| PATCH | `/users/profile/update` | authenticated | |
| POST | `/users/profile/update-avatar` | authenticated | `multipart/form-data`, body is `SCImageBodyDto` (`imageFile`); returns the new avatar as an absolute, client-loadable URL (not the raw storage reference persisted on the entity) — see File Storage in root `CLAUDE.md` |
| PATCH | `/users/profile/became-contributor` | authenticated | promotes to ROLE_CONTRIBUTOR |
| GET | `/users/admin` | ROLE_ADMIN | paginated, sort/search/isActive filters |
| PATCH | `/users/admin/{publicId}/status` | ROLE_ADMIN | activate/deactivate user |

## Recipes — `/recipes`

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| GET | `/recipes/home/category/{categoryId}` | public | fixed short rows per `FeedType` (QUICK, LIKE_A_CHEF, ECONOMICAL; VIRAL not yet implemented) |
| GET | `/recipes/feed/{feedType}` | public | paginated recipe feed-group; `feedType` is any `FeedType` (QUICK, LIKE_A_CHEF, ECONOMICAL, VIRAL — VIRAL routed through `IRecipeStatsDomainBridgeService` instead of the filter repository) |
| GET | `/recipes/search`, `/recipes/search/category/{categoryId}` | public | paginated, `searchKey` matches title/description |
| GET | `/recipes/favorites` | authenticated | the authenticated user's favorited recipes — see Favorites section below for why this lives here |
| GET | `/recipes/details/{publicId}` | public | draft recipes 404 exactly like non-existent ones |
| GET | `/recipes/admin`, `/recipes/admin/category/{categoryId}` | ROLE_ADMIN | paginated, includes draft recipes, dietary/status filters |
| GET | `/recipes/admin/details/{publicId}` | ROLE_ADMIN | includes draft recipes, ingredient/tag lists |
| POST | `/recipes` | ROLE_CONTRIBUTOR, ROLE_ADMIN | author resolved from security context, never trusted from payload; see draft/publish flow below |
| PUT | `/recipes/{publicId}` | author or ROLE_ADMIN | one translation upserted per call; no `isPublished` in the payload — see draft/publish flow below |
| PATCH | `/recipes/admin/{publicId}/publish` | ROLE_ADMIN only | sets `isPublished = true`; no author fallback, unlike `PUT` above |
| PATCH | `/recipes/admin/{publicId}/unpublish` | ROLE_ADMIN only | sets `isPublished = false`; no author fallback |
| DELETE | `/recipes/{publicId}` | author or ROLE_ADMIN | cascades translations/ingredients/tags |
| POST | `/recipes/{publicId}/cover` | author or ROLE_ADMIN | `multipart/form-data`, body is `UpsetRecipeCoverDto` (`recipeCoverImageFile`); returns the new cover as an absolute, client-loadable URL (not the raw storage reference persisted on `Recipe.imageUrl`) — same pattern as avatar upload, see File Storage in root `CLAUDE.md` |
| DELETE | `/recipes/{publicId}/cover` | author or ROLE_ADMIN | unlike avatar delete, no default replacement is assigned — the recipe is simply left without a cover |

`isPublished` is never part of `AddRecipeDto`/`UpdateRecipeDto` — it is only ever set by the service, never trusted from the client (this rule also lives in the root `CLAUDE.md` since it's safety-critical):
- **Create**: a contributor's translation requirement is exactly one, in any active language of their choosing (`locale` only picks the preview, no other role in the choice) — the recipe is created with `isPublished = false`. An admin's translations must instead cover every active language, no more no less, and the recipe is created with `isPublished = true` immediately.
- **Update**: if the actor is not an admin (i.e. the recipe's own contributor-author, since `assertAuthorOrAdmin` already excludes everyone else), the recipe is unconditionally reverted to `isPublished = false` — any content change needs re-approval. An admin's edit never touches `isPublished`.
- **Publish/unpublish**: the only way to set `isPublished` back to `true` — admin-only, separate from `PUT`.

Intended draft/publish flow: a contributor creates a recipe in one language, `isPublished = false`; an admin adds the missing active-language translations via `PUT` (one locale per call — each such admin edit leaves `isPublished` alone) and only then calls `PATCH .../publish`. If the contributor later edits their own (by-then-published) recipe, it silently reverts to draft and needs `PATCH .../publish` again.

`RecipeDto` (public list/search/favorites/home-feed results), `RecipeDetailsDto` (public single-recipe details), `RecipePreviewAdminDto` and `RecipeDetailsAdminDto` (admin) all carry `avgRating`/`ratingsCount`/`favoritesCount` (public DTOs also carry `isFavourite`), sourced from `IRecipeStatsDomainBridgeService` — batched per page (`getStatsBatch`), single lookup for single-recipe endpoints (`getStats`). See Favorites/Ratings/Stats section below for the bridge.

## Favorites — `/favorites`

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| POST | `/favorites/{recipePublicId}` | authenticated | |
| DELETE | `/favorites/{recipePublicId}` | authenticated | |

Listing the authenticated user's favorited recipes is `GET /recipes/favorites` (see Recipes section above) — it is fundamentally a recipe query filtered by favorites, so it lives in the recipes domain and returns `RecipeDto`; the favorites domain only owns add/remove and the cross-domain bridge checks.

## Ratings — `/ratings`

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| GET | `/ratings/recipe/{recipePublicId}/stats` | authenticated | average rating + total rating count |
| POST | `/ratings` | authenticated | one rating per user per recipe |
| PUT | `/ratings/recipe/{recipePublicId}` | authenticated | edits the authenticated user's own rating |

## Postman Collection

`postman/Sfrigola-Core.postman_collection.json` must stay in sync with every new/changed endpoint.

- **Every path variable in a request's URL must be a collection variable** (`{{name}}`), both in `url.raw` and in the `url.path` array — never a hardcoded literal segment (e.g. `"QUICK"`, `"en"` typed straight into the path). Follow the existing pattern: `{{publicId}}`, `{{categoryId}}`, `{{it}}`, `{{approved}}`.
- Declare new path variables in the collection-level `variable` array (top of the file) with a sensible default value, the same way `feedType`, `approved`, `it` are declared — don't invent a one-off local variable scoped to a single request.
- Query params always go in the `query` array (mirroring `url.raw`), `disabled: true` for optional ones with no default worth pre-filling — same pattern as `searchKey`, `isActive`, etc.
- Public (`noauth`) endpoints get `"auth": { "type": "noauth" }` on the request, matching the controller's `[Public]` marker.
- New requests go in the folder matching their domain, in the same order as the controller's methods.