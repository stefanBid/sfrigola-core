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
 (1,  'appetizers',    NULL, 1),
 (2,  'first-courses', NULL, 2),
 (3,  'main-courses',  NULL, 3),
 (4,  'side-dishes',   NULL, 4),
 (5,  'desserts',      NULL, 5),
 (6,  'beverages',     NULL, 6),
 (7,  'pasta',         2,    1),
 (8,  'risotto',       2,    2),
 (9,  'soups',         2,    3),
 (10, 'fish',          3,    1),
 (11, 'meat',          3,    2);

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