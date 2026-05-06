TRUNCATE TABLE
    payments,
    restaurant_votes,
    restaurant_vote_sessions,
    user_orders,
    order_sessions,
    menu_items,
    restaurants,
    users,
    audit_logs
RESTART IDENTITY CASCADE;

INSERT INTO restaurants (
    id,
    name,
    description,
    phone,
    address,
    is_default,
    is_active,
    container_enabled,
    default_container_price,
    delivery_enabled,
    default_delivery_price,
    created_at,
    updated_at
)
VALUES
    (1, 'Osh Posh', 'Default lunch restaurant', NULL, NULL, TRUE, TRUE, TRUE, 2000, TRUE, 20000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Tarnov', 'National meals', NULL, NULL, FALSE, TRUE, TRUE, 2000, TRUE, 20000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'KFC', 'Fast food', NULL, NULL, FALSE, TRUE, TRUE, 2000, TRUE, 20000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO menu_items (
    restaurant_id,
    name,
    description,
    price,
    is_active,
    sort_order,
    container_required,
    container_price_override,
    created_at,
    updated_at
)
VALUES
    (1, 'To''y oshi', NULL, 33000, TRUE, 1, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Choyxona', NULL, 35000, TRUE, 2, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Sho''rva', NULL, 30000, TRUE, 3, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Assorti sho''rva', NULL, 30000, TRUE, 4, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Chuchvara sho''rva', NULL, 30000, TRUE, 5, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Bifshteks', NULL, 33000, TRUE, 6, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Kivskiy kotlet + garnir', NULL, 33000, TRUE, 7, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Halim', NULL, 38000, TRUE, 8, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Tushonka', NULL, 38000, TRUE, 9, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Bifstogan', NULL, 35000, TRUE, 10, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Assorti', NULL, 40000, TRUE, 11, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
