ALTER TABLE restaurants
    ADD COLUMN container_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN default_container_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN delivery_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN default_delivery_price NUMERIC(19, 2) NOT NULL DEFAULT 0;

ALTER TABLE menu_items
    ADD COLUMN container_required BOOLEAN,
    ADD COLUMN container_price_override NUMERIC(19, 2);

ALTER TABLE order_sessions
    DROP COLUMN container_price;

ALTER TABLE restaurants
    ADD CONSTRAINT chk_restaurants_name_not_blank CHECK (btrim(name) <> ''),
    ADD CONSTRAINT chk_restaurants_default_container_price_non_negative CHECK (default_container_price >= 0),
    ADD CONSTRAINT chk_restaurants_default_delivery_price_non_negative CHECK (default_delivery_price >= 0);

ALTER TABLE menu_items
    ADD CONSTRAINT chk_menu_items_name_not_blank CHECK (btrim(name) <> ''),
    ADD CONSTRAINT chk_menu_items_price_non_negative CHECK (price >= 0),
    ADD CONSTRAINT chk_menu_items_container_price_override_non_negative CHECK (container_price_override IS NULL OR container_price_override >= 0);

ALTER TABLE order_sessions
    ADD CONSTRAINT chk_order_sessions_delivery_price_non_negative CHECK (delivery_price >= 0);

ALTER TABLE user_orders
    ADD CONSTRAINT chk_user_orders_food_price_non_negative CHECK (food_price >= 0),
    ADD CONSTRAINT chk_user_orders_container_price_non_negative CHECK (container_price >= 0),
    ADD CONSTRAINT chk_user_orders_delivery_share_non_negative CHECK (delivery_share >= 0),
    ADD CONSTRAINT chk_user_orders_final_price_non_negative CHECK (final_price >= 0);

WITH existing_default AS (
    SELECT id
    FROM restaurants
    WHERE is_default = TRUE
    ORDER BY id
    LIMIT 1
)
INSERT INTO restaurants (
    name,
    phone,
    address,
    is_default,
    is_active,
    container_enabled,
    default_container_price,
    delivery_enabled,
    default_delivery_price
)
SELECT 'Default Restaurant', NULL, NULL, TRUE, TRUE, TRUE, 2000, TRUE, 20000
WHERE NOT EXISTS (SELECT 1 FROM existing_default);

WITH default_restaurant AS (
    SELECT id
    FROM restaurants
    WHERE is_default = TRUE
    ORDER BY id
    LIMIT 1
)
UPDATE restaurants restaurant
SET name = 'Default Restaurant',
    is_default = TRUE,
    is_active = TRUE,
    container_enabled = TRUE,
    default_container_price = 2000,
    delivery_enabled = TRUE,
    default_delivery_price = 20000
FROM default_restaurant
WHERE restaurant.id = default_restaurant.id;

WITH default_restaurant AS (
    SELECT id
    FROM restaurants
    WHERE is_default = TRUE
    ORDER BY id
    LIMIT 1
)
UPDATE menu_items menu_item
SET name = 'Bifshteks',
    price = 33000,
    is_active = TRUE
FROM default_restaurant
WHERE menu_item.restaurant_id = default_restaurant.id
  AND menu_item.name = 'Bifteks'
  AND NOT EXISTS (
      SELECT 1
      FROM menu_items existing
      WHERE existing.restaurant_id = default_restaurant.id
        AND existing.name = 'Bifshteks'
  );

WITH default_restaurant AS (
    SELECT id
    FROM restaurants
    WHERE is_default = TRUE
    ORDER BY id
    LIMIT 1
)
UPDATE menu_items menu_item
SET price = 33000,
    is_active = TRUE
FROM default_restaurant
WHERE menu_item.restaurant_id = default_restaurant.id
  AND menu_item.name = 'Bifshteks';

WITH default_restaurant AS (
    SELECT id
    FROM restaurants
    WHERE is_default = TRUE
    ORDER BY id
    LIMIT 1
)
UPDATE menu_items menu_item
SET price = 33000,
    is_active = TRUE
FROM default_restaurant
WHERE menu_item.restaurant_id = default_restaurant.id
  AND menu_item.name = 'Toy Oshi';

WITH default_restaurant AS (
    SELECT id
    FROM restaurants
    WHERE is_default = TRUE
    ORDER BY id
    LIMIT 1
)
UPDATE menu_items menu_item
SET price = 35000,
    is_active = TRUE
FROM default_restaurant
WHERE menu_item.restaurant_id = default_restaurant.id
  AND menu_item.name = 'Choyxona';

WITH default_restaurant AS (
    SELECT id
    FROM restaurants
    WHERE is_default = TRUE
    ORDER BY id
    LIMIT 1
)
UPDATE menu_items menu_item
SET price = 35000,
    is_active = TRUE
FROM default_restaurant
WHERE menu_item.restaurant_id = default_restaurant.id
  AND menu_item.name = 'Bifstogan';

WITH default_restaurant AS (
    SELECT id
    FROM restaurants
    WHERE is_default = TRUE
    ORDER BY id
    LIMIT 1
)
UPDATE menu_items menu_item
SET price = 40000,
    is_active = TRUE
FROM default_restaurant
WHERE menu_item.restaurant_id = default_restaurant.id
  AND menu_item.name = 'Assorti';

WITH default_restaurant AS (
    SELECT id
    FROM restaurants
    WHERE is_default = TRUE
    ORDER BY id
    LIMIT 1
)
INSERT INTO menu_items (restaurant_id, name, price, is_active, container_required, container_price_override)
SELECT default_restaurant.id, menu_seed.name, menu_seed.price, TRUE, NULL, NULL
FROM default_restaurant
JOIN (
    VALUES
        ('Toy Oshi', 33000),
        ('Choyxona', 35000),
        ('Shorva', 30000),
        ('Assorti Shorva', 30000),
        ('Chuchvara Shorva', 30000),
        ('Bifshteks', 33000),
        ('Kivskiy Kotlet + Garnir', 33000),
        ('Halim', 38000),
        ('Tushonka', 38000),
        ('Bifstogan', 35000),
        ('Assorti', 40000)
) AS menu_seed(name, price) ON TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM menu_items existing
    WHERE existing.restaurant_id = default_restaurant.id
      AND existing.name = menu_seed.name
);

CREATE INDEX idx_restaurants_active_default ON restaurants(is_active, is_default);
CREATE INDEX idx_menu_items_restaurant_active ON menu_items(restaurant_id, is_active);
