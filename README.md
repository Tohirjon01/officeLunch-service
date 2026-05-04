# office-lunch-bot

Office Lunch Order Management System for automating daily lunch ordering in an office Telegram group.

## Project Overview

This service combines:

- a Spring Boot backend
- a universal Telegram bot
- PostgreSQL persistence
- scheduled daily lunch workflow automation
- internal REST endpoints for admin and future web panel integration

The MVP supports one default restaurant in the user flow, but the domain model and database are already multi-restaurant ready.

## Architecture Overview

The project uses a clean layered backend structure:

- `controller`: internal REST API endpoints under `/api/v1`
- `service`: core business use cases and transactional boundaries
- `service.calculation`: delivery/container/final price calculation logic
- `service.notification`: Telegram outbound messaging
- `service.report`: summary and restaurant-text generation
- `repository`: Spring Data JPA repositories
- `entity`: JPA entities for the core domain
- `bot`, `bot.handler`, `bot.keyboard`, `bot.message`: Telegram update handling and UI composition
- `scheduler`: cron-driven order automation
- `exception`: consistent error handling
- `mapper`: entity-to-response conversion
- `security`: future-ready security boundary with current internal-access configuration

## Main Features

- private Telegram registration with admin approval
- default restaurant and seeded default menu
- automatic daily session open/remind/close
- private order placement and update
- skip-today flow
- duplicate order prevention per user/session
- delivery and container price calculation using configurable rounding
- internal summary generation for office admins
- restaurant-ready order text generation
- audit logging for critical business actions
- internal REST API with unified response wrapper
- Docker-based local startup

## Technology Stack

- Java 21
- Spring Boot 3.x
- Maven
- PostgreSQL
- Spring Data JPA
- Flyway
- Telegram Bot API
- Lombok
- OpenAPI / Swagger UI
- Bean Validation
- Docker / Docker Compose

## Project Structure

```text
uz.company.lunchbot
├── config
├── bot
├── bot.handler
├── bot.keyboard
├── bot.message
├── scheduler
├── controller
├── service
├── service.calculation
├── service.notification
├── service.report
├── repository
├── entity
├── dto
├── dto.request
├── dto.response
├── enums
├── mapper
├── exception
├── util
└── security
```

## Database Schema Summary

Core tables:

- `users`
- `restaurants`
- `menu_items`
- `order_sessions`
- `user_orders`
- `audit_logs`

Key constraints:

- `users.telegram_user_id` unique
- `order_sessions(order_date, restaurant_id)` unique
- `user_orders(order_session_id, user_id)` unique
- foreign keys from menu, sessions, and orders back to restaurant/user/session

Seeded data:

- one default restaurant
- default lunch menu

## How to Run Locally

### 1. Prepare environment

Copy `.env.example` to `.env` and fill in real values.

### 2. Start PostgreSQL

Use a local PostgreSQL instance or Docker Compose.

### 3. Run the app

If Maven is available locally:

```bash
mvn spring-boot:run
```

The app will start on `http://localhost:8080`.

Swagger UI:

- `http://localhost:8080/swagger-ui.html`

## How to Run with Docker Compose

```bash
docker compose --env-file .env up --build
```

This starts:

- `postgres`
- `app`

## .env.example Explanation

- `BOT_TOKEN`: Telegram bot token from BotFather
- `BOT_USERNAME`: Telegram bot username without `@`
- `DB_URL`: JDBC URL for PostgreSQL
- `DB_USERNAME`: application DB user
- `DB_PASSWORD`: application DB password
- `POSTGRES_DB`: database name for the postgres container
- `POSTGRES_USER`: postgres container user
- `POSTGRES_PASSWORD`: postgres container password
- `LUNCH_GROUP_CHAT_ID`: Telegram lunch group chat ID
- `SUPER_ADMIN_*`: optional bootstrap super-admin metadata

## How to Create Telegram Bot with BotFather

1. Open Telegram and search for `@BotFather`
2. Send `/newbot`
3. Set the bot name and username
4. Copy the token into `BOT_TOKEN`
5. Put the bot username into `BOT_USERNAME`

## How to Get Group Chat ID

One practical option:

1. Add the bot to the lunch group
2. Send a message in the group
3. Call Telegram `getUpdates` for the bot token or inspect the received update logs
4. Copy the negative chat ID and place it into `LUNCH_GROUP_CHAT_ID`

## How User Registration Works

1. Employee opens a private chat with the bot
2. Employee sends `/start`
3. Bot creates a `PENDING` user if they are new
4. Approved admins receive pending-user approval buttons in private chat
5. Admin approves or rejects
6. Approved users receive the main menu and can place lunch orders

## Daily Lunch Flow

1. Scheduler opens the lunch session at 10:00
2. Bot posts the announcement in the office group
3. Users order privately via bot buttons
4. Scheduler sends reminder at 11:15 for non-responders
5. Scheduler closes the session at 11:30
6. Service recalculates delivery/container/final totals
7. Group receives closed summary and admin action buttons
8. Admin confirms session
9. Bot generates restaurant-ready order text

## Admin Commands

Private admin menu supports:

- `Today's Summary`
- `Close Order`
- `Confirm Order`
- `Extend Deadline`
- `Not Responded Users`
- `Pending Users`

Additional operational changes are available through the REST API in this MVP:

- menu management
- delivery price updates
- container price updates
- manual order edits

## API Endpoints Overview

### Users

- `GET /api/v1/users`
- `GET /api/v1/users/pending`
- `POST /api/v1/users/{id}/approve`
- `POST /api/v1/users/{id}/reject`
- `POST /api/v1/users/{id}/block`
- `POST /api/v1/users/{id}/role`

### Restaurants

- `GET /api/v1/restaurants`
- `POST /api/v1/restaurants`
- `PUT /api/v1/restaurants/{id}`
- `PATCH /api/v1/restaurants/{id}/status`

### Menu

- `GET /api/v1/menu-items`
- `POST /api/v1/menu-items`
- `PUT /api/v1/menu-items/{id}`
- `PATCH /api/v1/menu-items/{id}/status`

### Order Sessions

- `POST /api/v1/order-sessions/open`
- `POST /api/v1/order-sessions/{id}/close`
- `POST /api/v1/order-sessions/{id}/confirm`
- `POST /api/v1/order-sessions/{id}/cancel`
- `POST /api/v1/order-sessions/{id}/extend`
- `GET /api/v1/order-sessions/today`
- `GET /api/v1/order-sessions/{id}`
- `GET /api/v1/order-sessions/{id}/summary`
- `GET /api/v1/order-sessions/{id}/restaurant-text`
- `POST /api/v1/order-sessions/{id}/delivery`
- `POST /api/v1/order-sessions/{id}/container`

### Orders

- `GET /api/v1/order-sessions/{id}/orders`
- `POST /api/v1/order-sessions/{id}/orders/manual`
- `PUT /api/v1/orders/{id}`
- `POST /api/v1/orders/{id}/skip`
- `POST /api/v1/orders/{id}/cancel`
- `POST /api/v1/orders/{id}/paid`
- `POST /api/v1/orders/{id}/unpaid`

## Security Notes

JWT is intentionally excluded from the MVP.

The codebase is structured so stronger security can be added later. For the current internal API:

- mutating endpoints accept optional `X-Actor-User-Id`
- Telegram admin actions are checked against stored roles

## Testing

Included unit tests cover:

- `OrderCalculationService`
- `UserOrderService` duplicate prevention
- `OrderSessionService` lifecycle rules
- not-responded user detection

## Future Roadmap

- multi-restaurant selection UI
- payment tracking
- Excel export
- web admin panel
- JWT security
- monthly debt report

## MVP Exclusions

Not implemented in this version:

- payment gateway integration
- multi-restaurant user selection UI
- web admin panel
- JWT authentication
- complex quantity selector UI
- restaurant API integration
