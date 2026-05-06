ALTER TABLE restaurants
    ADD COLUMN description VARCHAR(255);

ALTER TABLE menu_items
    ADD COLUMN description VARCHAR(255),
    ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;

ALTER TABLE menu_items
    DROP CONSTRAINT chk_menu_items_price_non_negative,
    ADD CONSTRAINT chk_menu_items_price_positive CHECK (price > 0),
    ADD CONSTRAINT chk_menu_items_sort_order_non_negative CHECK (sort_order >= 0);

WITH default_restaurant AS (
    SELECT id
    FROM restaurants
    WHERE is_default = TRUE
    ORDER BY id
    LIMIT 1
)
UPDATE restaurants restaurant
SET name = 'Osh Posh',
    description = NULL,
    is_default = TRUE,
    is_active = TRUE,
    delivery_enabled = TRUE,
    default_delivery_price = 20000,
    container_enabled = TRUE,
    default_container_price = 2000
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
SET name = CASE menu_item.name
        WHEN 'Toy Oshi' THEN 'To''y oshi'
        WHEN 'Shorva' THEN 'Sho''rva'
        WHEN 'Assorti Shorva' THEN 'Assorti sho''rva'
        WHEN 'Chuchvara Shorva' THEN 'Chuchvara sho''rva'
        WHEN 'Kivskiy Kotlet + Garnir' THEN 'Kivskiy kotlet + garnir'
        ELSE menu_item.name
    END,
    price = CASE menu_item.name
        WHEN 'Toy Oshi' THEN 33000
        WHEN 'To''y oshi' THEN 33000
        WHEN 'Choyxona' THEN 35000
        WHEN 'Shorva' THEN 30000
        WHEN 'Sho''rva' THEN 30000
        WHEN 'Assorti Shorva' THEN 30000
        WHEN 'Assorti sho''rva' THEN 30000
        WHEN 'Chuchvara Shorva' THEN 30000
        WHEN 'Chuchvara sho''rva' THEN 30000
        WHEN 'Bifshteks' THEN 33000
        WHEN 'Kivskiy Kotlet + Garnir' THEN 33000
        WHEN 'Kivskiy kotlet + garnir' THEN 33000
        WHEN 'Halim' THEN 38000
        WHEN 'Tushonka' THEN 38000
        WHEN 'Bifstogan' THEN 35000
        WHEN 'Assorti' THEN 40000
        ELSE menu_item.price
    END,
    is_active = TRUE,
    sort_order = CASE menu_item.name
        WHEN 'Toy Oshi' THEN 1
        WHEN 'To''y oshi' THEN 1
        WHEN 'Choyxona' THEN 2
        WHEN 'Shorva' THEN 3
        WHEN 'Sho''rva' THEN 3
        WHEN 'Assorti Shorva' THEN 4
        WHEN 'Assorti sho''rva' THEN 4
        WHEN 'Chuchvara Shorva' THEN 5
        WHEN 'Chuchvara sho''rva' THEN 5
        WHEN 'Bifshteks' THEN 6
        WHEN 'Kivskiy Kotlet + Garnir' THEN 7
        WHEN 'Kivskiy kotlet + garnir' THEN 7
        WHEN 'Halim' THEN 8
        WHEN 'Tushonka' THEN 9
        WHEN 'Bifstogan' THEN 10
        WHEN 'Assorti' THEN 11
        ELSE menu_item.sort_order
    END
FROM default_restaurant
WHERE menu_item.restaurant_id = default_restaurant.id;

WITH default_restaurant AS (
    SELECT id
    FROM restaurants
    WHERE is_default = TRUE
    ORDER BY id
    LIMIT 1
)
INSERT INTO menu_items (restaurant_id, name, description, price, is_active, sort_order, container_required, container_price_override)
SELECT default_restaurant.id, seed.name, NULL, seed.price, TRUE, seed.sort_order, NULL, NULL
FROM default_restaurant
JOIN (
    VALUES
        ('To''y oshi', 33000, 1),
        ('Choyxona', 35000, 2),
        ('Sho''rva', 30000, 3),
        ('Assorti sho''rva', 30000, 4),
        ('Chuchvara sho''rva', 30000, 5),
        ('Bifshteks', 33000, 6),
        ('Kivskiy kotlet + garnir', 33000, 7),
        ('Halim', 38000, 8),
        ('Tushonka', 38000, 9),
        ('Bifstogan', 35000, 10),
        ('Assorti', 40000, 11)
) AS seed(name, price, sort_order) ON TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM menu_items existing
    WHERE existing.restaurant_id = default_restaurant.id
      AND existing.name = seed.name
);

CREATE INDEX idx_menu_items_restaurant_sort_order ON menu_items(restaurant_id, sort_order, name);
