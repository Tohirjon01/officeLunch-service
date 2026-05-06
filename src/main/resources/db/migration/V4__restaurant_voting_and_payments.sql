CREATE TABLE restaurant_vote_sessions (
    id BIGSERIAL PRIMARY KEY,
    vote_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    deadline_at TIMESTAMP NOT NULL,
    winner_restaurant_id BIGINT REFERENCES restaurants(id),
    group_message_id BIGINT,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE restaurant_votes (
    id BIGSERIAL PRIMARY KEY,
    vote_session_id BIGINT NOT NULL REFERENCES restaurant_vote_sessions(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_restaurant_votes_session_user UNIQUE (vote_session_id, user_id)
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_order_id BIGINT NOT NULL REFERENCES user_orders(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    order_session_id BIGINT NOT NULL REFERENCES order_sessions(id),
    amount NUMERIC(19, 2) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    receipt_file_id VARCHAR(255),
    receipt_message_id BIGINT,
    admin_comment TEXT,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payments_user_order UNIQUE (user_order_id),
    CONSTRAINT chk_payments_amount_non_negative CHECK (amount >= 0)
);

CREATE INDEX idx_restaurant_vote_sessions_vote_date_status ON restaurant_vote_sessions(vote_date, status);
CREATE INDEX idx_restaurant_votes_session ON restaurant_votes(vote_session_id);
CREATE INDEX idx_payments_order_session_status ON payments(order_session_id, status);
CREATE INDEX idx_payments_user_order_date_status ON payments(user_id, status, order_session_id);

INSERT INTO restaurants (
    name,
    description,
    phone,
    address,
    is_default,
    is_active,
    container_enabled,
    default_container_price,
    delivery_enabled,
    default_delivery_price
)
SELECT 'Tarnov', 'National meals', NULL, NULL, FALSE, TRUE, TRUE, 2000, TRUE, 20000
WHERE NOT EXISTS (SELECT 1 FROM restaurants WHERE name = 'Tarnov');

INSERT INTO restaurants (
    name,
    description,
    phone,
    address,
    is_default,
    is_active,
    container_enabled,
    default_container_price,
    delivery_enabled,
    default_delivery_price
)
SELECT 'KFC', 'Fast food', NULL, NULL, FALSE, TRUE, TRUE, 2000, TRUE, 20000
WHERE NOT EXISTS (SELECT 1 FROM restaurants WHERE name = 'KFC');

INSERT INTO menu_items (restaurant_id, name, description, price, is_active, sort_order, container_required, container_price_override)
SELECT restaurant.id, seed.name, NULL, seed.price, TRUE, seed.sort_order, NULL, NULL
FROM restaurants restaurant
JOIN (
    VALUES
        ('Tarnov', 'Qozon kabob', 42000, 1),
        ('Tarnov', 'Manti', 36000, 2),
        ('Tarnov', 'Lag''mon', 34000, 3),
        ('KFC', 'Twister', 39000, 1),
        ('KFC', 'Burger', 45000, 2),
        ('KFC', 'Boxmaster', 52000, 3)
) AS seed(restaurant_name, name, price, sort_order)
    ON restaurant.name = seed.restaurant_name
WHERE NOT EXISTS (
    SELECT 1
    FROM menu_items existing
    WHERE existing.restaurant_id = restaurant.id
      AND existing.name = seed.name
);
