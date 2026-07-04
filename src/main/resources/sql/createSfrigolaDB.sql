-- ============================================================
--  CORE SCHEMA - Cooking Recipes App
--  Database:  PostgreSQL 15+
--  Version:   V1 - Core (AI layer excluded)
--  Naming:    snake_case, semantic keys in English
--  Audit:     managed by Hibernate (@EnableJpaAuditing)
--             columns exist as safety net for seeds/migrations
--  public_id: UUID exposed in REST APIs, BIGSERIAL for internal use
--  Tags:      controlled vocabulary via tags table
--             no free TEXT[] - everything referenced by FK
-- ============================================================


-- ============================================================
--  ENUM TYPES
--  Closed sets of values - DB rejects anything not listed here.
--  Renaming requires ALTER TYPE - use only for truly stable sets.
-- ============================================================

-- Recipe difficulty levels
CREATE TYPE difficulty_level AS ENUM ('easy', 'medium', 'hard');

-- Meal types a recipe is suitable for
CREATE TYPE meal_type AS ENUM ('breakfast', 'lunch', 'dinner', 'snack', 'dessert', 'appetizer');

-- Season a recipe or ingredient is associated with
CREATE TYPE season_type AS ENUM ('spring', 'summer', 'autumn', 'winter', 'all_year');

-- What kind of semantic information a tag carries
CREATE TYPE tag_type AS ENUM ('recipe', 'flavor', 'texture', 'season', 'dietary');

-- Which entity a tag can be applied to
CREATE TYPE tag_scope AS ENUM ('recipe', 'ingredient', 'both');

-- Approval lifecycle for user-proposed tags
CREATE TYPE tag_status AS ENUM ('approved', 'pending', 'rejected');


-- ============================================================
--  LANGUAGES
--  Registry of supported app languages.
--  Adding a new language = one INSERT, zero DDL changes.
--  All translation tables reference this table via locale FK.
-- ============================================================

CREATE TABLE languages (
   id          BIGSERIAL    PRIMARY KEY,
   public_id   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
   code        VARCHAR(10)  NOT NULL UNIQUE,   -- BCP-47: 'en', 'it', 'fr', 'de'
   name        VARCHAR(50)  NOT NULL,           -- native name: 'English', 'Italiano'
   is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
   is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
   created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
   updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
   created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
   updated_by  VARCHAR(50)  NOT NULL DEFAULT 'system'
);

-- Only one language can be the default fallback
CREATE UNIQUE INDEX uq_languages_default
    ON languages (is_default)
    WHERE is_default = TRUE;


-- ============================================================
--  ROLES
--  User roles for Spring Security.
--  name is the technical code read directly by Spring Security.
--  No translation needed - it is a technical identifier, not UI text.
-- ============================================================

CREATE TABLE roles (
   id          BIGSERIAL    PRIMARY KEY,
   public_id   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
   name        VARCHAR(50)  NOT NULL UNIQUE,   -- e.g. ROLE_USER, ROLE_ADMIN, ROLE_CHEF
   description VARCHAR(200),
   created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
   updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
   created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
   updated_by  VARCHAR(50)  NOT NULL DEFAULT 'system'
);


-- ============================================================
--  USERS
--  role_id: direct FK - one user has exactly one role (1:1)
--  preferred_lang: user's preferred language for translations
-- ============================================================

CREATE TABLE users (
   id              BIGSERIAL       PRIMARY KEY,
   public_id       UUID            NOT NULL UNIQUE DEFAULT gen_random_uuid(),
   role_id         BIGINT          NOT NULL REFERENCES roles(id),
   username        VARCHAR(50)     NOT NULL UNIQUE,
   email           VARCHAR(150)    NOT NULL UNIQUE,
   password_hash   VARCHAR(255)    NOT NULL,
   preferred_lang  VARCHAR(10)     NOT NULL DEFAULT 'en' REFERENCES languages(code),
   is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
   first_name      VARCHAR(100),
   last_name       VARCHAR(100),
   avatar_url      VARCHAR(500),
   bio             TEXT,
   created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
   updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
   created_by      VARCHAR(50)     NOT NULL DEFAULT 'system',
   updated_by      VARCHAR(50)     NOT NULL DEFAULT 'system'
);


-- ============================================================
--  TAGS
--  Controlled vocabulary - no free text strings anywhere.
--  slug: universal English key e.g. 'fresh', 'quick', 'creamy'
--  type: what kind of semantic information the tag carries
--  scope: which entity the tag can be applied to
--  status: approved = usable, pending = awaiting admin review
--
--  Human-readable labels live in tag_translations, not here.
-- ============================================================

CREATE TABLE tags (
  id          BIGSERIAL    PRIMARY KEY,
  public_id   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
  slug        VARCHAR(100) NOT NULL UNIQUE,
  type        tag_type     NOT NULL,
  scope       tag_scope    NOT NULL DEFAULT 'both',
  status      tag_status   NOT NULL DEFAULT 'approved',
  created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
  created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
  updated_by  VARCHAR(50)  NOT NULL DEFAULT 'system'
);

-- Translated human-readable label for each tag per language
CREATE TABLE tag_translations (
  id          BIGSERIAL    PRIMARY KEY,
  public_id   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
  tag_id      BIGINT       NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  locale      VARCHAR(10)  NOT NULL REFERENCES languages(code) ON DELETE CASCADE,
  label       VARCHAR(100) NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
  created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
  updated_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
  UNIQUE (tag_id, locale)
);


-- ============================================================
--  CATEGORIES
--  Self-referential hierarchy: a category can have a parent.
--  slug: universal English URL key e.g. 'first-courses', 'pasta'
--  Human-readable names live in category_translations.
-- ============================================================

CREATE TABLE categories (
    id          BIGSERIAL    PRIMARY KEY,
    public_id   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    slug        VARCHAR(100) NOT NULL UNIQUE,
    parent_id   BIGINT       REFERENCES categories(id) ON DELETE SET NULL,
    sort_order  SMALLINT     NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(50)  NOT NULL DEFAULT 'system'
);

-- Translated name and description for each category per language
CREATE TABLE category_translations (
   id          BIGSERIAL    PRIMARY KEY,
   public_id   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
   category_id BIGINT       NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
   locale      VARCHAR(10)  NOT NULL REFERENCES languages(code) ON DELETE CASCADE,
   name        VARCHAR(100) NOT NULL,
   description TEXT,
   created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
   updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
   created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
   updated_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
   UNIQUE (category_id, locale)
);


-- ============================================================
--  INGREDIENTS
--  Global ingredient catalogue. Universal and reusable data only.
--  slug: universal English key e.g. 'cherry-tomato', 'mozzarella'
--  allergens: kept as TEXT[] - standard international codes
--             (gluten, milk, eggs, nuts…) managed as static map
--             in frontend, no DB translation needed.
--  flavor, texture and season info managed via ingredient_tags.
--  Human-readable names live in ingredient_translations.
-- ============================================================

CREATE TABLE ingredients (
     id                  BIGSERIAL    PRIMARY KEY,
     public_id           UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
     slug                VARCHAR(150) NOT NULL UNIQUE,
     category            VARCHAR(100),              -- 'vegetable', 'dairy', 'protein', 'grain'
     calories_per_100g   NUMERIC(7,2),
     allergens           TEXT[],                    -- ARRAY['gluten','milk','eggs','nuts']
     is_vegetarian       BOOLEAN      NOT NULL DEFAULT FALSE,
     is_vegan            BOOLEAN      NOT NULL DEFAULT FALSE,
     is_gluten_free      BOOLEAN      NOT NULL DEFAULT FALSE,
     created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
     updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
     created_by          VARCHAR(50)  NOT NULL DEFAULT 'system',
     updated_by          VARCHAR(50)  NOT NULL DEFAULT 'system'
);

-- Translated name for each ingredient per language
CREATE TABLE ingredient_translations (
     id              BIGSERIAL    PRIMARY KEY,
     public_id       UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
     ingredient_id   BIGINT       NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
     locale          VARCHAR(10)  NOT NULL REFERENCES languages(code) ON DELETE CASCADE,
     name            VARCHAR(150) NOT NULL,
     created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
     updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
     created_by      VARCHAR(50)  NOT NULL DEFAULT 'system',
     updated_by      VARCHAR(50)  NOT NULL DEFAULT 'system',
     UNIQUE (ingredient_id, locale)
);

-- Bridge table: ingredient <-> tag (flavor, texture, season tags)
-- Only tags with scope = 'ingredient' or 'both' should be used here
CREATE TABLE ingredient_tags (
     ingredient_id   BIGINT NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
     tag_id          BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
     created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
     created_by      VARCHAR(50) NOT NULL DEFAULT 'system',
     PRIMARY KEY (ingredient_id, tag_id)
);


-- ============================================================
--  RECIPES
--  Core recipe data - universal and language-independent fields only.
--  Human-readable text (title, description, instructions)
--  lives in recipe_translations.
--  Tags are managed via recipe_tags bridge table.
-- ============================================================

CREATE TABLE recipes (
     id              BIGSERIAL        PRIMARY KEY,
     public_id       UUID             NOT NULL UNIQUE DEFAULT gen_random_uuid(),
     author_id       BIGINT           NOT NULL REFERENCES users(id),
     category_id     BIGINT           REFERENCES categories(id) ON DELETE SET NULL,
     difficulty      difficulty_level NOT NULL DEFAULT 'medium',
     meal_type       meal_type,
     season          season_type      NOT NULL DEFAULT 'all_year',
     prep_time_min   INT              CHECK (prep_time_min >= 0),
     cook_time_min   INT              CHECK (cook_time_min >= 0),
     servings        SMALLINT         CHECK (servings > 0),
     is_vegetarian   BOOLEAN          NOT NULL DEFAULT FALSE,
     is_vegan        BOOLEAN          NOT NULL DEFAULT FALSE,
     is_gluten_free  BOOLEAN          NOT NULL DEFAULT FALSE,
     is_published    BOOLEAN          NOT NULL DEFAULT FALSE,
     created_at      TIMESTAMP        NOT NULL DEFAULT NOW(),
     updated_at      TIMESTAMP        NOT NULL DEFAULT NOW(),
     created_by      VARCHAR(50)      NOT NULL DEFAULT 'system',
     updated_by      VARCHAR(50)      NOT NULL DEFAULT 'system'
);

-- Translated title, description and instructions per language
CREATE TABLE recipe_translations (
     id          BIGSERIAL    PRIMARY KEY,
     public_id   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
     recipe_id   BIGINT       NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
     locale      VARCHAR(10)  NOT NULL REFERENCES languages(code) ON DELETE CASCADE,
     title       VARCHAR(200) NOT NULL,
     description TEXT,
     instructions TEXT        NOT NULL,
     created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
     updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
     created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
     updated_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
     UNIQUE (recipe_id, locale)
);

-- Bridge table: recipe <-> tag (recipe and dietary tags)
-- Only tags with scope = 'recipe' or 'both' should be used here
CREATE TABLE recipe_tags (
     recipe_id   BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
     tag_id      BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
     created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
     created_by  VARCHAR(50) NOT NULL DEFAULT 'system',
     PRIMARY KEY (recipe_id, tag_id)
);


-- ============================================================
--  RECIPE_INGREDIENTS
--  Bridge table between recipes and ingredients.
--  Carries the context-specific usage data:
--  how much, in what unit, and how to prepare for THIS recipe.
--  unit and preparation_note are in English (universal keys).
--  e.g. unit: 'g', 'ml', 'tbsp', 'cup', 'to taste'
--  e.g. preparation_note: 'finely chopped', 'at room temperature'
-- ============================================================

CREATE TABLE recipe_ingredients (
    id                  BIGSERIAL    PRIMARY KEY,
    public_id           UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    recipe_id           BIGINT       NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    ingredient_id       BIGINT       NOT NULL REFERENCES ingredients(id),
    quantity            NUMERIC(8,2),
    unit                VARCHAR(50),
    preparation_note    VARCHAR(200),
    sort_order          SMALLINT     NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_by          VARCHAR(50)  NOT NULL DEFAULT 'system',
    UNIQUE (recipe_id, ingredient_id)
);


-- ============================================================
--  FAVORITES
--  Tracks which recipes a user has saved.
--  Many-to-many between users and recipes.
--  UNIQUE constraint prevents duplicate saves.
-- ============================================================

CREATE TABLE favorites (
   id          BIGSERIAL    PRIMARY KEY,
   public_id   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
   user_id     BIGINT       NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
   recipe_id   BIGINT       NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
   created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
   updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
   created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
   updated_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
   UNIQUE (user_id, recipe_id)
);


-- ============================================================
--  RATINGS
--  One rating per user per recipe - enforced by UNIQUE constraint.
--  score: 1 to 5 - enforced by CHECK constraint.
--  comment: optional free text review.
-- ============================================================

CREATE TABLE ratings (
     id          BIGSERIAL    PRIMARY KEY,
     public_id   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
     user_id     BIGINT       NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
     recipe_id   BIGINT       NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
     score       SMALLINT     NOT NULL CHECK (score BETWEEN 1 AND 5),
     comment     TEXT,
     created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
     updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
     created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
     updated_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
     UNIQUE (user_id, recipe_id)
);


-- ============================================================
--  RECIPE_STATS
--  Pre-computed aggregates - avoids expensive GROUP BY at runtime.
--  Updated by Spring service layer after every rating or favorite
--  operation. No triggers - Hibernate owns the write logic.
--  1:1 with recipes - recipe_id is both PK and FK.
-- ============================================================

CREATE TABLE recipe_stats (
      recipe_id       BIGINT       PRIMARY KEY REFERENCES recipes(id) ON DELETE CASCADE,
      avg_rating      NUMERIC(3,2) NOT NULL DEFAULT 0.00,
      ratings_count   INT          NOT NULL DEFAULT 0,
      favorites_count INT          NOT NULL DEFAULT 0,
      views_count     INT          NOT NULL DEFAULT 0,
      last_computed   TIMESTAMP    NOT NULL DEFAULT NOW()
);


-- ============================================================
--  INDEXES
-- ============================================================

-- Users
CREATE INDEX idx_users_role              ON users (role_id);

-- Recipes - primary filters
CREATE INDEX idx_recipes_author          ON recipes (author_id);
CREATE INDEX idx_recipes_category        ON recipes (category_id);
CREATE INDEX idx_recipes_published       ON recipes (id) WHERE is_published = TRUE;
CREATE INDEX idx_recipes_season          ON recipes (season);
CREATE INDEX idx_recipes_meal_type       ON recipes (meal_type);
CREATE INDEX idx_recipes_dietary         ON recipes (is_vegetarian, is_vegan, is_gluten_free);

-- Tags - composite covers type-only and combined queries; slug covered by UNIQUE constraint
CREATE INDEX idx_tags_type_scope_status  ON tags (type, scope, status);

-- Bridge tables - reverse lookups (PK covers forward direction)
CREATE INDEX idx_recipe_tags_tag         ON recipe_tags (tag_id);
CREATE INDEX idx_ingredient_tags_tag     ON ingredient_tags (tag_id);

-- Allergens - GIN index required for ANY/&& on TEXT[]
CREATE INDEX idx_ingredients_allergens   ON ingredients USING GIN (allergens);

-- Stats - sorting by popularity
CREATE INDEX idx_recipe_stats_rating     ON recipe_stats (avg_rating DESC);
CREATE INDEX idx_recipe_stats_favs       ON recipe_stats (favorites_count DESC);

-- Categories - tree navigation
CREATE INDEX idx_categories_parent       ON categories (parent_id);

-- Ratings and favorites
CREATE INDEX idx_ratings_recipe          ON ratings (recipe_id);
CREATE INDEX idx_favorites_user          ON favorites (user_id);


-- ============================================================
--  SEED DATA
-- ============================================================

-- Languages
INSERT INTO languages (code, name, is_default, is_active) VALUES
  ('en', 'English',  TRUE,  TRUE),
  ('it', 'Italiano', FALSE, TRUE);

-- Roles
INSERT INTO roles (name, description) VALUES
  ('ROLE_ADMIN', 'Full access administrator'),
  ('ROLE_USER',  'Standard registered user'),
  ('ROLE_CONTRIBUTOR',  'User that can publish without moderation');

-- Categories (universal English slugs)
INSERT INTO categories (id, slug, parent_id, sort_order) VALUES
 (1,  'appetizers',    NULL, 0),
 (2,  'first-courses', NULL, 1),
 (3,  'main-courses',  NULL, 2),
 (4,  'side-dishes',   NULL, 3),
 (5,  'desserts',      NULL, 4),
 (6,  'beverages',     NULL, 5),
 (7,  'pasta',         2,    0),
 (8,  'risotto',       2,    1),
 (9,  'soups',         2,    2),
 (10, 'fish',          3,    0),
 (11, 'meat',          3,    1);

SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));

-- Category translations - Italian
INSERT INTO category_translations (category_id, locale, name, description) VALUES
   (1,  'it', 'Antipasti',     'Stuzzichini e antipasti'),
   (2,  'it', 'Primi',         'Pasta, risotti, minestre'),
   (3,  'it', 'Secondi',       'Carne, pesce, piatti proteici'),
   (4,  'it', 'Contorni',      'Verdure e contorni'),
   (5,  'it', 'Dolci',         'Dessert e dolci'),
   (6,  'it', 'Bevande',       'Succhi, frullati, drink'),
   (7,  'it', 'Pasta',         'Ricette di pasta'),
   (8,  'it', 'Risotti',       'Risotti e cereali'),
   (9,  'it', 'Zuppe',         'Minestre e zuppe'),
   (10, 'it', 'Secondi pesce', 'Ricette a base di pesce'),
   (11, 'it', 'Secondi carne', 'Ricette a base di carne');

-- Category translations - English
INSERT INTO category_translations (category_id, locale, name, description) VALUES
   (1,  'en', 'Appetizers',    'Starters and appetizers'),
   (2,  'en', 'First courses', 'Pasta, risotto, soups'),
   (3,  'en', 'Main courses',  'Meat, fish, protein dishes'),
   (4,  'en', 'Side dishes',   'Vegetables and sides'),
   (5,  'en', 'Desserts',      'Sweets and desserts'),
   (6,  'en', 'Beverages',     'Juices, smoothies, drinks'),
   (7,  'en', 'Pasta',         'Pasta recipes'),
   (8,  'en', 'Risotto',       'Risotto and grain dishes'),
   (9,  'en', 'Soups',         'Soups and broths'),
   (10, 'en', 'Fish',          'Fish and seafood recipes'),
   (11, 'en', 'Meat',          'Meat recipes');

-- ============================================================
--  TAG SEED - approved base vocabulary
--
--  type  = what kind of semantic information the tag carries
--  scope = which entity the tag applies to:
--          'recipe'     -> recipe_tags only
--          'ingredient' -> ingredient_tags only
--          'both'       -> can be used on either
-- ============================================================

-- Recipe tags (scope: recipe) - describe the recipe as a whole
INSERT INTO tags (id, slug, type, scope, status) VALUES
 (1,  'quick',        'recipe', 'recipe', 'approved'),
 (2,  'easy',         'recipe', 'recipe', 'approved'),
 (3,  'budget',       'recipe', 'recipe', 'approved'),
 (4,  'light',        'recipe', 'recipe', 'approved'),
 (5,  'fresh',        'recipe', 'recipe', 'approved'),
 (6,  'comfort-food', 'recipe', 'recipe', 'approved'),
 (7,  'one-pot',      'recipe', 'recipe', 'approved'),
 (8,  'meal-prep',    'recipe', 'recipe', 'approved'),
 (9,  'gourmet',      'recipe', 'recipe', 'approved'),
 (10, 'traditional',  'recipe', 'recipe', 'approved');

-- Flavor tags (scope: ingredient) - taste profile
INSERT INTO tags (id, slug, type, scope, status) VALUES
 (11, 'sweet',        'flavor', 'ingredient', 'approved'),
 (12, 'sour',         'flavor', 'ingredient', 'approved'),
 (13, 'salty',        'flavor', 'ingredient', 'approved'),
 (14, 'bitter',       'flavor', 'ingredient', 'approved'),
 (15, 'umami',        'flavor', 'ingredient', 'approved'),
 (16, 'spicy',        'flavor', 'ingredient', 'approved'),
 (17, 'smoky',        'flavor', 'ingredient', 'approved'),
 (18, 'aromatic',     'flavor', 'ingredient', 'approved'),
 (19, 'acidic',       'flavor', 'ingredient', 'approved'),
 (20, 'mild',         'flavor', 'ingredient', 'approved');

-- Texture tags (scope: ingredient) - mouthfeel profile
INSERT INTO tags (id, slug, type, scope, status) VALUES
 (21, 'crunchy',      'texture', 'ingredient', 'approved'),
 (22, 'creamy',       'texture', 'ingredient', 'approved'),
 (23, 'soft',         'texture', 'ingredient', 'approved'),
 (24, 'chewy',        'texture', 'ingredient', 'approved'),
 (25, 'crispy',       'texture', 'ingredient', 'approved'),
 (26, 'velvety',      'texture', 'ingredient', 'approved'),
 (27, 'firm',         'texture', 'ingredient', 'approved'),
 (28, 'tender',       'texture', 'ingredient', 'approved');

-- Season tags (scope: both) - apply to ingredients and recipes
INSERT INTO tags (id, slug, type, scope, status) VALUES
 (29, 'spring',       'season', 'both', 'approved'),
 (30, 'summer',       'season', 'both', 'approved'),
 (31, 'autumn',       'season', 'both', 'approved'),
 (32, 'winter',       'season', 'both', 'approved'),
 (33, 'all-year',     'season', 'both', 'approved');

-- Dietary tags (scope: both) - apply to ingredients and recipes
INSERT INTO tags (id, slug, type, scope, status) VALUES
 (34, 'high-protein', 'dietary', 'both', 'approved'),
 (35, 'low-carb',     'dietary', 'both', 'approved'),
 (36, 'low-fat',      'dietary', 'both', 'approved'),
 (37, 'dairy-free',   'dietary', 'both', 'approved'),
 (38, 'nut-free',     'dietary', 'both', 'approved');

SELECT setval('tags_id_seq', (SELECT MAX(id) FROM tags));

-- Tag translations - Italian
INSERT INTO tag_translations (tag_id, locale, label) VALUES
    (1,  'it', 'Veloce'),             (2,  'it', 'Facile'),
    (3,  'it', 'Economica'),          (4,  'it', 'Leggera'),
    (5,  'it', 'Fresca'),             (6,  'it', 'Comfort food'),
    (7,  'it', 'Piatto unico'),       (8,  'it', 'Meal prep'),
    (9,  'it', 'Gourmet'),            (10, 'it', 'Tradizionale'),
    (11, 'it', 'Dolce'),              (12, 'it', 'Acido'),
    (13, 'it', 'Salato'),             (14, 'it', 'Amaro'),
    (15, 'it', 'Umami'),              (16, 'it', 'Piccante'),
    (17, 'it', 'Affumicato'),         (18, 'it', 'Aromatico'),
    (19, 'it', 'Acidulo'),            (20, 'it', 'Delicato'),
    (21, 'it', 'Croccante'),          (22, 'it', 'Cremoso'),
    (23, 'it', 'Morbido'),            (24, 'it', 'Gommoso'),
    (25, 'it', 'Croccante'),          (26, 'it', 'Vellutato'),
    (27, 'it', 'Sodo'),               (28, 'it', 'Tenero'),
    (29, 'it', 'Primavera'),          (30, 'it', 'Estate'),
    (31, 'it', 'Autunno'),            (32, 'it', 'Inverno'),
    (33, 'it', 'Tutto l''anno'),      (34, 'it', 'Alto contenuto proteico'),
    (35, 'it', 'Pochi carboidrati'),  (36, 'it', 'Pochi grassi'),
    (37, 'it', 'Senza lattosio'),     (38, 'it', 'Senza frutta secca');

-- Tag translations - English
INSERT INTO tag_translations (tag_id, locale, label) VALUES
    (1,  'en', 'Quick'),        (2,  'en', 'Easy'),
    (3,  'en', 'Budget'),       (4,  'en', 'Light'),
    (5,  'en', 'Fresh'),        (6,  'en', 'Comfort food'),
    (7,  'en', 'One pot'),      (8,  'en', 'Meal prep'),
    (9,  'en', 'Gourmet'),      (10, 'en', 'Traditional'),
    (11, 'en', 'Sweet'),        (12, 'en', 'Sour'),
    (13, 'en', 'Salty'),        (14, 'en', 'Bitter'),
    (15, 'en', 'Umami'),        (16, 'en', 'Spicy'),
    (17, 'en', 'Smoky'),        (18, 'en', 'Aromatic'),
    (19, 'en', 'Acidic'),       (20, 'en', 'Mild'),
    (21, 'en', 'Crunchy'),      (22, 'en', 'Creamy'),
    (23, 'en', 'Soft'),         (24, 'en', 'Chewy'),
    (25, 'en', 'Crispy'),       (26, 'en', 'Velvety'),
    (27, 'en', 'Firm'),         (28, 'en', 'Tender'),
    (29, 'en', 'Spring'),       (30, 'en', 'Summer'),
    (31, 'en', 'Autumn'),       (32, 'en', 'Winter'),
    (33, 'en', 'All year'),     (34, 'en', 'High protein'),
    (35, 'en', 'Low carb'),     (36, 'en', 'Low fat'),
    (37, 'en', 'Dairy free'),   (38, 'en', 'Nut free');

-- Admin user
INSERT INTO users (
    role_id,
    username,
    email,
    password_hash,
    preferred_lang,
    is_active,
    first_name,
    last_name,
    created_by,
    updated_by
)
VALUES (
    (SELECT id FROM roles WHERE name = 'ROLE_ADMIN'),
    'admin',
    'admin@sfrigola.com',
    '$2a$12$CzGNXHYb3JW.uaXImbUDZeq/9O0M/5V.JYibrrZ87OwcYB/YmpFRe',
    'en',
    TRUE,
    'Admin',
    'Sfrigola',
    'system',
    'system'
);

-- Contributor user
INSERT INTO users (
    role_id,
    username,
    email,
    password_hash,
    preferred_lang,
    is_active,
    first_name,
    last_name,
    created_by,
    updated_by
)
VALUES (
    (SELECT id FROM roles WHERE name = 'ROLE_CONTRIBUTOR'),
    'contributor',
    'contributor@sfrigola.com',
    '$2a$12$0wTEADC4iyGDfOjgmcd0KOPLCCabcrEMNPZtFJJs5f7DoPWJexH0q',
    'it',
    TRUE,
    'Contributor',
    'Sfrigola',
    'system',
    'system'
);

-- Normal user
INSERT INTO users (
    role_id,
    username,
    email,
    password_hash,
    preferred_lang,
    is_active,
    first_name,
    last_name,
    created_by,
    updated_by
)
VALUES (
    (SELECT id FROM roles WHERE name = 'ROLE_USER'),
    'user',
    'user@sfrigola.com',
    '$2a$12$0wTEADC4iyGDfOjgmcd0KOPLCCabcrEMNPZtFJJs5f7DoPWJexH0q',
    'en',
    TRUE,
    'User',
    'Sfrigola',
    'system',
    'system'
);

-- ============================================================
--  SAMPLE INGREDIENTS + RECIPE
--  Minimal end-to-end dataset so every table has at least one
--  row from a fresh init - no manual seeding needed to start.
-- ============================================================

-- Ingredients
INSERT INTO ingredients (id, slug, category, calories_per_100g, allergens, is_vegetarian, is_vegan, is_gluten_free) VALUES
 (1,  'spaghetti',        'grain',     158.00, ARRAY['gluten'],        TRUE,  TRUE,  FALSE),
 (2,  'tomato',           'vegetable',  18.00, NULL,                   TRUE,  TRUE,  TRUE),
 (3,  'garlic',           'vegetable', 149.00, NULL,                   TRUE,  TRUE,  TRUE),
 (4,  'olive-oil',        'fat',       884.00, NULL,                   TRUE,  TRUE,  TRUE),
 (5,  'parmesan',         'dairy',     431.00, ARRAY['milk'],          TRUE,  FALSE, TRUE),
 (6,  'basil',            'herb',       22.00, NULL,                   TRUE,  TRUE,  TRUE),
 (7,  'onion',            'vegetable',  40.00, NULL,                   TRUE,  TRUE,  TRUE),
 (8,  'carrot',           'vegetable',  41.00, NULL,                   TRUE,  TRUE,  TRUE),
 (9,  'arborio-rice',     'grain',     130.00, NULL,                   TRUE,  TRUE,  TRUE),
 (10, 'butter',           'dairy',     717.00, ARRAY['milk'],          TRUE,  FALSE, TRUE),
 (11, 'porcini-mushroom', 'vegetable',  22.00, NULL,                   TRUE,  TRUE,  TRUE),
 (12, 'chicken-breast',   'protein',   165.00, NULL,                   FALSE, FALSE, TRUE),
 (13, 'lemon',            'fruit',      29.00, NULL,                   TRUE,  TRUE,  TRUE),
 (14, 'egg',              'protein',   155.00, ARRAY['eggs'],          TRUE,  FALSE, TRUE),
 (15, 'mascarpone',       'dairy',     429.00, ARRAY['milk'],          TRUE,  FALSE, TRUE),
 (16, 'ladyfingers',      'grain',     384.00, ARRAY['gluten','eggs'], TRUE,  FALSE, FALSE),
 (17, 'coffee',           'beverage',    2.00, NULL,                   TRUE,  TRUE,  TRUE),
 (18, 'cocoa-powder',     'other',     228.00, NULL,                   TRUE,  TRUE,  TRUE),
 (19, 'sugar',            'other',     387.00, NULL,                   TRUE,  TRUE,  TRUE);

SELECT setval('ingredients_id_seq', (SELECT MAX(id) FROM ingredients));

-- Ingredient translations - English
INSERT INTO ingredient_translations (ingredient_id, locale, name) VALUES
 (1,  'en', 'Spaghetti'),
 (2,  'en', 'Tomato'),
 (3,  'en', 'Garlic'),
 (4,  'en', 'Olive oil'),
 (5,  'en', 'Parmesan'),
 (6,  'en', 'Basil'),
 (7,  'en', 'Onion'),
 (8,  'en', 'Carrot'),
 (9,  'en', 'Arborio rice'),
 (10, 'en', 'Butter'),
 (11, 'en', 'Porcini mushroom'),
 (12, 'en', 'Chicken breast'),
 (13, 'en', 'Lemon'),
 (14, 'en', 'Egg'),
 (15, 'en', 'Mascarpone'),
 (16, 'en', 'Ladyfingers'),
 (17, 'en', 'Coffee'),
 (18, 'en', 'Cocoa powder'),
 (19, 'en', 'Sugar');

-- Ingredient translations - Italian
INSERT INTO ingredient_translations (ingredient_id, locale, name) VALUES
 (1,  'it', 'Spaghetti'),
 (2,  'it', 'Pomodoro'),
 (3,  'it', 'Aglio'),
 (4,  'it', 'Olio d''oliva'),
 (5,  'it', 'Parmigiano'),
 (6,  'it', 'Basilico'),
 (7,  'it', 'Cipolla'),
 (8,  'it', 'Carota'),
 (9,  'it', 'Riso Arborio'),
 (10, 'it', 'Burro'),
 (11, 'it', 'Fungo porcino'),
 (12, 'it', 'Petto di pollo'),
 (13, 'it', 'Limone'),
 (14, 'it', 'Uovo'),
 (15, 'it', 'Mascarpone'),
 (16, 'it', 'Savoiardi'),
 (17, 'it', 'Caffe'''),
 (18, 'it', 'Cacao in polvere'),
 (19, 'it', 'Zucchero');

-- Ingredient tags (flavor/texture/season)
INSERT INTO ingredient_tags (ingredient_id, tag_id) VALUES
 (2, 19),  -- tomato: acidic
 (2, 30),  -- tomato: summer
 (3, 18),  -- garlic: aromatic
 (5, 15),  -- parmesan: umami
 (5, 13),  -- parmesan: salty
 (6, 18),  -- basil: aromatic
 (7, 18),  -- onion: aromatic
 (10, 22), -- butter: creamy
 (11, 15), -- porcini: umami
 (11, 31), -- porcini: autumn
 (13, 12), -- lemon: sour
 (15, 22), -- mascarpone: creamy
 (17, 14), -- coffee: bitter
 (18, 14), -- cocoa: bitter
 (19, 11); -- sugar: sweet

-- Recipes
INSERT INTO recipes (
    id, author_id, category_id, difficulty, meal_type, season,
    prep_time_min, cook_time_min, servings,
    is_vegetarian, is_vegan, is_gluten_free, is_published
) VALUES
 (1, (SELECT id FROM users WHERE username = 'admin'),
     (SELECT id FROM categories WHERE slug = 'pasta'),
     'easy', 'dinner', 'all_year', 10, 20, 4, TRUE, FALSE, FALSE, TRUE),
 (2, (SELECT id FROM users WHERE username = 'contributor'),
     (SELECT id FROM categories WHERE slug = 'risotto'),
     'medium', 'dinner', 'autumn', 10, 35, 4, TRUE, FALSE, TRUE, TRUE),
 (3, (SELECT id FROM users WHERE username = 'admin'),
     (SELECT id FROM categories WHERE slug = 'meat'),
     'easy', 'lunch', 'all_year', 15, 25, 4, FALSE, FALSE, TRUE, TRUE),
 (4, (SELECT id FROM users WHERE username = 'contributor'),
     (SELECT id FROM categories WHERE slug = 'desserts'),
     'medium', 'dessert', 'all_year', 30, 0, 6, TRUE, FALSE, FALSE, TRUE);

SELECT setval('recipes_id_seq', (SELECT MAX(id) FROM recipes));

-- Recipe translations - English
INSERT INTO recipe_translations (recipe_id, locale, title, description, instructions) VALUES
 (1, 'en', 'Spaghetti al Pomodoro', 'Classic Italian pasta with fresh tomato sauce.',
  'Boil salted water and cook spaghetti until al dente. Meanwhile, saute garlic in olive oil, add chopped tomatoes and simmer. Toss pasta with the sauce, top with grated parmesan and fresh basil.'),
 (2, 'en', 'Porcini Mushroom Risotto', 'Creamy autumn risotto with porcini mushrooms and parmesan.',
  'Saute chopped onion in butter and olive oil, add rice and toast briefly. Add porcini mushrooms, then ladle in warm stock gradually, stirring until creamy. Finish with butter and grated parmesan.'),
 (3, 'en', 'Lemon Chicken', 'Quick pan-seared chicken breast with garlic and lemon.',
  'Season chicken breast and sear in olive oil until golden. Add minced garlic and lemon juice, simmer briefly to make a light pan sauce, then finish with a knob of butter.'),
 (4, 'en', 'Tiramisu', 'Classic Italian no-bake dessert with coffee and mascarpone.',
  'Whisk egg yolks with sugar until pale, fold in mascarpone. Dip ladyfingers in coffee and layer with the mascarpone cream. Repeat layers, chill, then dust with cocoa powder before serving.');

-- Recipe translations - Italian
INSERT INTO recipe_translations (recipe_id, locale, title, description, instructions) VALUES
 (1, 'it', 'Spaghetti al Pomodoro', 'Pasta italiana classica con salsa di pomodoro fresco.',
  'Cuocere gli spaghetti in acqua salata fino a cottura al dente. Nel frattempo, soffriggere l''aglio nell''olio d''oliva, aggiungere i pomodori a pezzi e far sobbollire. Mantecare la pasta con il sugo, guarnire con parmigiano grattugiato e basilico fresco.'),
 (2, 'it', 'Risotto ai Funghi Porcini', 'Risotto autunnale cremoso con funghi porcini e parmigiano.',
  'Soffriggere la cipolla tritata nel burro e nell''olio, aggiungere il riso e tostarlo brevemente. Unire i funghi porcini, poi versare il brodo caldo poco alla volta mescolando fino a cremosita. Mantecare con burro e parmigiano grattugiato.'),
 (3, 'it', 'Pollo al Limone', 'Petto di pollo in padella veloce con aglio e limone.',
  'Insaporire il petto di pollo e rosolarlo nell''olio d''oliva fino a doratura. Aggiungere l''aglio tritato e il succo di limone, far sobbollire brevemente per creare un sughetto leggero, quindi mantecare con una noce di burro.'),
 (4, 'it', 'Tiramisu', 'Classico dolce italiano al cucchiaio con caffe e mascarpone.',
  'Sbattere i tuorli con lo zucchero fino a renderli chiari, incorporare il mascarpone. Inzuppare i savoiardi nel caffe e alternare a strati con la crema al mascarpone. Ripetere gli strati, far raffreddare e spolverare con cacao prima di servire.');

-- Recipe tags
INSERT INTO recipe_tags (recipe_id, tag_id) VALUES
 (1, 2),  -- spaghetti: easy
 (1, 5),  -- spaghetti: fresh
 (1, 10), -- spaghetti: traditional
 (2, 6),  -- risotto: comfort-food
 (2, 9),  -- risotto: gourmet
 (2, 31), -- risotto: autumn
 (3, 1),  -- chicken: quick
 (3, 2),  -- chicken: easy
 (3, 5),  -- chicken: fresh
 (4, 6),  -- tiramisu: comfort-food
 (4, 9),  -- tiramisu: gourmet
 (4, 10); -- tiramisu: traditional

-- Recipe ingredients
INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, preparation_note, sort_order) VALUES
 (1, 1, 400.00, 'g',     NULL,             0),
 (1, 2, 500.00, 'g',     'chopped',        1),
 (1, 3, 2.00,   'clove', 'minced',         2),
 (1, 4, 3.00,   'tbsp',  NULL,             3),
 (1, 5, 50.00,  'g',     'grated',         4),
 (1, 6, 10.00,  'g',     'fresh leaves',   5),
 (2, 9,  320.00, 'g',   NULL,              0),
 (2, 11, 300.00, 'g',   'soaked',          1),
 (2, 7,  1.00,   'unit','chopped',         2),
 (2, 10, 50.00,  'g',   NULL,              3),
 (2, 4,  2.00,   'tbsp',NULL,              4),
 (2, 5,  60.00,  'g',   'grated',          5),
 (3, 12, 500.00, 'g',   NULL,              0),
 (3, 13, 2.00,   'unit','juiced',          1),
 (3, 3,  1.00,   'clove','minced',         2),
 (3, 4,  2.00,   'tbsp', NULL,             3),
 (3, 10, 20.00,  'g',   NULL,              4),
 (4, 15, 500.00, 'g',   NULL,              0),
 (4, 14, 4.00,   'unit','yolks',           1),
 (4, 16, 200.00, 'g',   NULL,              2),
 (4, 17, 300.00, 'ml',  'brewed, cooled',  3),
 (4, 18, 20.00,  'g',   'for dusting',     4),
 (4, 19, 100.00, 'g',   NULL,              5);

-- Favorites
INSERT INTO favorites (user_id, recipe_id) VALUES
 ((SELECT id FROM users WHERE username = 'user'),        1),
 ((SELECT id FROM users WHERE username = 'contributor'), 1),
 ((SELECT id FROM users WHERE username = 'user'),        2),
 ((SELECT id FROM users WHERE username = 'admin'),       2),
 ((SELECT id FROM users WHERE username = 'user'),        3),
 ((SELECT id FROM users WHERE username = 'user'),        4),
 ((SELECT id FROM users WHERE username = 'admin'),       4);

-- Ratings
INSERT INTO ratings (user_id, recipe_id, score, comment) VALUES
 ((SELECT id FROM users WHERE username = 'user'),        1, 5, 'Delicious and easy to make!'),
 ((SELECT id FROM users WHERE username = 'contributor'), 1, 4, 'Great weeknight classic.'),
 ((SELECT id FROM users WHERE username = 'user'),        2, 4, 'Rich and creamy, worth the stirring.'),
 ((SELECT id FROM users WHERE username = 'admin'),       2, 5, 'Restaurant quality at home.'),
 ((SELECT id FROM users WHERE username = 'user'),        3, 5, 'Fast, fresh, perfect for lunch.'),
 ((SELECT id FROM users WHERE username = 'contributor'), 3, 4, 'Simple and tasty.'),
 ((SELECT id FROM users WHERE username = 'user'),        4, 5, 'Best tiramisu recipe I''ve tried.'),
 ((SELECT id FROM users WHERE username = 'admin'),       4, 4, 'Classic and reliable.');

-- Recipe stats (mirrors the seeded ratings/favorites above)
INSERT INTO recipe_stats (recipe_id, avg_rating, ratings_count, favorites_count, views_count) VALUES
 (1, 4.50, 2, 2, 42),
 (2, 4.50, 2, 2, 30),
 (3, 4.50, 2, 1, 18),
 (4, 4.50, 2, 2, 55);