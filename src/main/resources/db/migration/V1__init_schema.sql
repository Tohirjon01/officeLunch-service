CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    private_chat_id BIGINT,
    status VARCHAR(32) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE restaurants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    address VARCHAR(255),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE menu_items (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id),
    name VARCHAR(255) NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_sessions (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id),
    order_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    delivery_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    container_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    opened_at TIMESTAMP NOT NULL,
    deadline_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_sessions_order_date_restaurant UNIQUE (order_date, restaurant_id)
);

CREATE TABLE user_orders (
    id BIGSERIAL PRIMARY KEY,
    order_session_id BIGINT NOT NULL REFERENCES order_sessions(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    menu_item_id BIGINT REFERENCES menu_items(id),
    status VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    food_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    container_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    delivery_share NUMERIC(19, 2) NOT NULL DEFAULT 0,
    final_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    payment_status VARCHAR(32) NOT NULL,
    ordered_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_orders_session_user UNIQUE (order_session_id, user_id)
);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    order_session_id BIGINT REFERENCES order_sessions(id),
    user_id BIGINT REFERENCES users(id),
    action VARCHAR(64) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_menu_items_restaurant ON menu_items(restaurant_id);
CREATE INDEX idx_order_sessions_status ON order_sessions(status);
CREATE INDEX idx_user_orders_status ON user_orders(status);

INSERT INTO restaurants (name, phone, address, is_default, is_active)
VALUES ('Default Restaurant', NULL, NULL, TRUE, TRUE);

INSERT INTO menu_items (restaurant_id, name, price, is_active)
VALUES
    (1, 'Bifteks', 33000, TRUE),
    (1, 'Toy Oshi', 33000, TRUE),
    (1, 'Choyxona', 35000, TRUE),
    (1, 'Bifstogan', 35000, TRUE),
    (1, 'Assorti', 40000, TRUE);
