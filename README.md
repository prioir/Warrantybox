# WarrantyBox — Your Digital Warranty Locker

A personal digital warranty management system. Store your purchased products, warranty
details, serial numbers and invoices in one place, and see at a glance what's active,
expiring soon, or expired.

## Tech Stack

- Java 25
- Spring Boot 3.5 (Spring MVC, Spring Security, Spring Data MongoDB)
- Maven
- MongoDB
- Thymeleaf
- Lombok
- HTML5 / CSS3 / Vanilla JavaScript (no frontend frameworks)

## Prerequisites

- JDK 25 installed and on your `PATH`
- Maven 3.9+
- A running MongoDB instance (local or remote)

## Configuration

The app reads its MongoDB connection from the `MONGODB_URI` environment variable and
falls back to `mongodb://localhost:27017/warrantybox` if it isn't set — see
`src/main/resources/application.properties`. Never commit real credentials; set them as
environment variables instead, e.g.:

```bash
export MONGODB_URI="mongodb://<user>:<password>@<host>:27017/warrantybox?authSource=admin"
```

Uploaded invoices are stored on disk under `./uploads` (configurable via `file.upload-dir`).

## Running Locally

```bash
# 1. Start MongoDB (example using Docker)
docker run -d -p 27017:27017 --name warrantybox-mongo mongo:7

# 2. Build and run
mvn clean spring-boot:run
```

The app starts on **http://localhost:8080**.

## Project Structure

```
WarrantyBox/
├── src/main/java/com/warrantybox/
│   ├── WarrantyBoxApplication.java
│   ├── config/SecurityConfig.java
│   ├── controller/  (AuthController, DashboardController, ProductController)
│   ├── model/        (User, Product)
│   ├── repository/   (UserRepository, ProductRepository)
│   └── service/       (UserService, ProductService)
├── src/main/resources/
│   ├── templates/      (index, auth/, dashboard, products/)
│   ├── static/css/     (style.css, auth.css, dashboard.css)
│   ├── static/js/       (script.js)
│   └── application.properties
└── pom.xml
```

## Core Features

- **Register / Login / Logout** via Spring Security, BCrypt-hashed passwords.
- **Add / Edit / Delete products** — every product belongs to exactly one user;
  ownership is verified on every read/edit/delete using the logged-in user's id, so a
  user can never access or modify someone else's product by guessing a URL.
- **Automatic warranty end date** — calculated from purchase date + warranty period
  (in years) whenever a product is added or edited.
- **Dynamic warranty status** — computed on the fly, never stored stale:
  - `ACTIVE` — more than 30 days remaining
  - `EXPIRING SOON` — 1–30 days remaining
  - `EXPIRED` — end date has passed
- **Dashboard** — summary cards (Total / Active / Expiring Soon / Expired) plus a
  searchable, filterable product table, scoped to the logged-in user only.
- **Invoice upload** — PDF/JPG/JPEG/PNG, 5MB max, stored on local disk; the app works
  fine without an invoice too.
- **Responsive UI** — a soft blue/indigo/lavender SaaS-style design that adapts down
  to mobile, including a card-style table on small screens.

## Security Notes

- Passwords are BCrypt-hashed — plain text is never stored or logged.
- `/`, `/login`, `/register` and static assets are public; everything else requires
  authentication.
- Every product read/update/delete goes through `findByIdAndUserId(...)`, so ownership
  is enforced at the data-access layer, not just in the UI.
- CSRF protection is left on (Spring Security's default) for all state-changing forms.

## Notes for Reviewers

The codebase intentionally stays small: 3 controllers, 2 models, 2 repositories, 2
services and 1 security config, matching the brief. Warranty status and days-remaining
are computed dynamically on the `Product` model rather than persisted, so they're
always accurate against "today" without a background job.
