# BabyShopHub Backend

Spring Boot REST API for the BabyShopHub shopping application. The backend provides shopper authentication, catalog browsing, account and address management, cart and checkout, orders and tracking, reviews, support tickets, and administrator operations.

## Requirements

- JDK 21
- Maven 3.9+ (or the included Maven Wrapper)
- MySQL 8.x

Create the database configured by `DB_URL` before starting the application. The default URL uses `babyshophub_db`; use the database name and schema agreed with the backend/database team.

## Configuration

Supply configuration through environment variables. Do not commit real credentials or signing keys.

| Variable | Required | Default / purpose |
| --- | --- | --- |
| `DB_URL` | No | `jdbc:mysql://localhost:3306/babyshophub_db` |
| `DB_USERNAME` | No | `root` |
| `DB_PASSWORD` | No | Empty string; set a local MySQL password as needed |
| `CORS_ALLOWED_ORIGINS` | No | Comma-separated origins; defaults to common local web development ports |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | For email | SMTP credentials for verification and reset emails |
| `APP_ADMIN_INITIAL_PASSWORD` | Optional | If set on first startup, creates the admin user |
| `APP_ADMIN_EMAIL` | No | Admin bootstrap email, defaults to `admin@example.com` |
| `APP_DEMO_USERS_INITIAL_PASSWORD` | Optional | If set, creates the John, Jane, and Mike demo accounts with this password |

The initial admin is created only when the configured email does not already exist. Demo accounts are created only when `APP_DEMO_USERS_INITIAL_PASSWORD` is set, and only when each email does not already exist. Keep bootstrap passwords out of source control and unset them after initial setup; do not enable demo-user seeding in production. Hibernate currently uses `ddl-auto=update`; review the generated schema against the agreed SQL schema before production use and back up existing data before schema changes.

## Run and verify

Start the application with the configured environment and MySQL database:

```powershell
./mvnw.cmd spring-boot:run
```

Run the automated test suite:

```powershell
./mvnw.cmd test
```

Swagger UI is available at `/swagger-ui/index.html`; the OpenAPI document is at `/v3/api-docs`.

## Authentication

Register and verify an account, then call `POST /api/auth/login`. A successful login returns `204` and establishes a Spring Security session cookie. Send that cookie with protected requests; browser clients must enable credentials. `POST /api/auth/logout` invalidates the session. Admin endpoints require the `ROLE_ADMIN` role.

## API areas

| Area | Routes |
| --- | --- |
| Authentication | `/api/auth/register`, `/login`, `/verify`, `/resend-otp`, `/forgot-password`, `/reset-password`, `/logout` |
| Shopper account | `/api/account/profile`, `/change-password`, `/addresses` |
| Catalog | `GET /api/products`, `/api/products/{id}`, `/api/categories`, `/api/brands` |
| Cart | `/api/cart`, `/api/cart/items`, `/api/cart/items/{productId}` |
| Checkout and orders | `POST /api/orders`, `/api/orders/me`, `/api/orders/{id}`, `/api/orders/{id}/tracking` |
| Product reviews | `/api/products/{productId}/reviews` and `/api/reviews/products/{productId}` |
| Support | `/api/support`, `/api/support/me` |
| Admin | `/api/admin/products`, `/categories`, `/users`, `/orders`, `/support` and their item/status operations |

Checkout uses a dummy payment record; it does not collect or process card details. Prices and stock are checked by the server when an order is placed.

## Schema and project-guide alignment notes

The implementation retains the existing user, product, category, brand, and address entity mappings so existing work and database data are not discarded. It adds order, order-item, payment, tracking, cart, password-reset OTP, and support-ticket persistence. In particular, the guide describes an example schema and mentions composite keys as a common choice; the existing cart uses its own row ID plus a unique user/product constraint. Do not rename or drop established columns/tables without coordinating a reviewed migration with the database owner.

The guide mentions seller ratings but does not define seller accounts or the rating workflow. That feature needs a product decision before implementation. Submission reports, a database backup, and the final project ZIP are delivery tasks and should be prepared with the team's submission process; credentials and live secrets must not be included in the archive.
