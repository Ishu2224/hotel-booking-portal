# Padav: Hotel Booking Portal

A full-stack hotel booking application built for the Thomas Cook technical interview assignment.
Guests browse hotels, check availability for their dates, see a night-by-night price, book a room, then look up,
cancel or download a voucher for their booking. There are no accounts and no payments (both out of scope).

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA (Hibernate), Bean Validation, Lombok |
| Database | MySQL 8 with Flyway migrations (schema and seed data are created automatically) |
| Frontend | Angular 22 (standalone components, signals, reactive forms), plain responsive CSS |
| Tests | JUnit 5, Mockito, Spring Boot Test + MockMvc, H2 in MySQL mode (no Docker needed) |

For the architecture, schema, API contracts and the reasoning behind the key decisions, read **[DESIGN.md](DESIGN.md)**.

## Features

- **Browse and search** 8 seeded hotels across Mumbai, Goa, Jaipur, Bengaluru, Manali, Udaipur, Kochi and Rishikesh. Filter by location and dates; with dates, every hotel shows how many rooms are left.
- **Availability** computed on the server for the half-open stay `[checkIn, checkOut)`: `totalRooms - CONFIRMED bookings that overlap`.
- **Booking flow** with a server-calculated price. The client never sends a price. A reference such as `TC-2026-K7XM3P` is issued on confirmation.
- **Lookup and cancel** with both the reference and the guest email. Cancelling releases the room immediately.
- **Concurrency safe**: simultaneous bookings for the last room are serialized with a pessimistic lock on the hotel row; the loser gets `409 Conflict`.
- **Surprise feature**: 15% weekend surcharge on Friday and Saturday nights, shown night by night, plus a downloadable, printable HTML booking voucher.

## Prerequisites

| Tool | Version | Used for |
|---|---|---|
| JDK | 17 or newer | Backend |
| Maven | 3.9 or newer | Backend build and tests |
| MySQL | 8.0 or newer, running locally | Application database |
| Node.js | 22.22.3 or newer (24.15+ also works) | Frontend |
| npm | 10 or newer (ships with Node) | Frontend dependencies |

The MySQL user needs permission to create a database (the default connection URL uses `createDatabaseIfNotExist=true`).
If you prefer to create it yourself: `CREATE DATABASE hotel_booking;`

## Screenshots

| Search | Price breakdown |
|---|---|
| ![Search](docs/screenshots/01-search.png) | ![Price](docs/screenshots/02-hotel-price.png) |

| Confirmation | Voucher | Cancellation |
|---|---|---|
| ![Confirmation](docs/screenshots/03-confirmation.png) | ![Voucher](docs/screenshots/04-voucher.png) | ![Cancel](docs/screenshots/05-cancel.png) |


## Quick start

Run all commands from the repository root unless stated otherwise.

**1. Configure the database credentials** (nothing secret is stored in the repo):

```bash
cp .env.example .env          # Windows: copy .env.example .env
```

Open `.env` and set `DB_USERNAME` and `DB_PASSWORD` for your local MySQL.

**2. Start the backend** (creates the tables and loads the 8 hotels on first start):

```bash
cd backend
mvn spring-boot:run
```

The API is now on http://localhost:8080. Quick check: http://localhost:8080/api/hotels

**3. Start the frontend** in a second terminal:

```bash
cd frontend
npm install
npm start
```

Open http://localhost:4200. The dev server proxies `/api` to the backend on port 8080, so no CORS setup is needed.

## Configuration

The backend reads these variables from the environment, or from a `.env` file in the repo root or in `backend/`.
See [.env.example](.env.example).

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/hotel_booking?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true` | JDBC URL |
| `DB_USERNAME` | none, required | Database user |
| `DB_PASSWORD` | none, required | Database password |
| `SERVER_PORT` | `8080` | Backend port (if you change it, also update the target in `frontend/proxy.conf.json`) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Origins allowed to call `/api` directly (comma separated) |

Business settings live in `backend/src/main/resources/application.yml` under `app.pricing`
(`weekend-surcharge-percent: 15`, `currency: INR`).

## Running the tests

```bash
cd backend
mvn test
```

The tests run on in-memory H2 (MySQL compatibility mode) using the **same Flyway migrations and seed data** as production,
so neither MySQL nor Docker is required. There are 54 test methods (60 test cases, counting the parameterized overlap table):

| Test class | Covers |
|---|---|
| `PricingServiceTest` | Multi-night pricing, Friday/Saturday surcharge, mixed stays, checkout day not charged, rounding |
| `StayDatesValidationTest` | Past check-in, checkout before or equal to check-in, 30-night cap, optional search dates, guest fields |
| `BookingServiceTest` | Booking creation, full hotel, unknown hotel, lookup normalization, cancel, double cancel (Mockito) |
| `AvailabilityIntegrationTest` | Half-open overlap table, cancelled bookings ignored, back-to-back stays, cancellation releases inventory |
| `BookingConcurrencyIntegrationTest` | 8 simultaneous requests for 3 rooms sell exactly 3; two guests racing for the last room, one wins |
| `BookingApiIntegrationTest` | HTTP contract end to end: search, quotes, validation errors, error JSON shape, full lifecycle, 409, voucher |

The frontend has no unit tests; `npm run build` in `frontend/` type-checks every template in strict mode.

## API quick reference

Full request and response examples are in [DESIGN.md](DESIGN.md#3-rest-api-contracts).

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/hotels?location=&checkInDate=&checkOutDate=` | Search hotels (dates optional, but only as a pair) |
| GET | `/api/hotels/{id}` | Hotel details |
| GET | `/api/hotels/{id}/availability?checkInDate=&checkOutDate=` | Hotel details plus rooms left for the stay |
| GET | `/api/hotels/{id}/quote?checkInDate=&checkOutDate=` | Night-by-night price preview |
| POST | `/api/bookings` | Create a booking (`201`) |
| POST | `/api/bookings/lookup` | Retrieve a booking by reference and email |
| POST | `/api/bookings/cancel` | Cancel a `CONFIRMED` booking |
| POST | `/api/bookings/voucher` | Download the HTML voucher |

Every error returns the same JSON: `{ timestamp, status, error, message, fieldErrors[] }`.

## Try the concurrency rule yourself

Lake Pichola Residency (hotel `6`) has only 3 rooms. Fire 6 simultaneous requests for the same dates
(use any future dates):

```bash
for i in 1 2 3 4 5 6; do
  curl -s -o /dev/null -w "request $i -> HTTP %{http_code}\n" -X POST http://localhost:8080/api/bookings \
    -H "Content-Type: application/json" \
    -d "{\"hotelId\":6,\"checkInDate\":\"2026-12-14\",\"checkOutDate\":\"2026-12-16\",\"guestName\":\"Racer $i\",\"guestEmail\":\"racer$i@example.com\"}" &
done; wait
```

Expected: exactly three `201` and three `409`, in any order.
Then open the app, search "Udaipur" for those dates, and the hotel shows as sold out.

## Assumptions and scope

- One booking reserves **one room** for the whole stay. Guests are not counted per booking; `maxGuestsPerRoom` is informational.
- Each hotel has a single room category, so `hotel.totalRooms` is the inventory.
- Prices are in INR. "Today" for the check-in rule is evaluated in IST (`Asia/Kolkata`).
- The guest email is not verified. It acts as the second factor, together with the reference, for lookup, cancellation and the voucher.
- A stay is limited to 30 nights.
- Out of scope, as the brief says: login, payment gateways, an admin panel, cloud deployment.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Could not resolve placeholder 'DB_PASSWORD'` | `.env` is missing or does not define `DB_USERNAME` and `DB_PASSWORD`. Create it from `.env.example`. |
| `Access denied for user` | Wrong credentials in `.env`. |
| `Unknown database 'hotel_booking'` | Your MySQL user cannot create databases. Create it manually: `CREATE DATABASE hotel_booking;` |
| Frontend shows "We can't reach the booking server" | The backend is not running, or it is on a different port than `frontend/proxy.conf.json` targets. |
| Port 8080 already in use | Set `SERVER_PORT` in `.env` and change the target in `frontend/proxy.conf.json`. |
| Flyway checksum error after editing a migration | Migrations are immutable once applied. Drop the local database and restart, or add a new `V3__...sql`. |
| Fonts look different offline | Headings and body use Google Fonts at runtime and fall back to system fonts without internet. |

## Assignment requirements and where they live

| Requirement | Implementation | Verified by |
|---|---|---|
| Browse and search hotels, 6+ seeded Indian hotels | `HotelController`, `HotelService`, `V2__seed_hotels.sql`, search page | `BookingApiIntegrationTest` |
| Availability with overlap rule | `BookingRepository.countOverlappingConfirmed`, `AvailabilityService` | `AvailabilityIntegrationTest` |
| Booking flow, backend-calculated price, readable reference | `BookingService.createBooking`, `PricingService`, `BookingReferenceGenerator` | `BookingServiceTest`, `BookingApiIntegrationTest` |
| Lookup by reference + email, cancellation releases the room | `BookingService.lookup/cancel`, manage page | `AvailabilityIntegrationTest`, `BookingApiIntegrationTest` |
| Concurrency: no double booking, 409 on conflict | `HotelRepository.findByIdForUpdate` (`PESSIMISTIC_WRITE`) inside `@Transactional` | `BookingConcurrencyIntegrationTest` |
| Surprise feature: weekend pricing + voucher | `PricingService`, `VoucherService`, `PriceBreakdownView`, `BookingTicket` | `PricingServiceTest`, `BookingApiIntegrationTest` |
| Validation and consistent error JSON | `@ValidStayDates`, `GlobalExceptionHandler` | `StayDatesValidationTest`, `BookingApiIntegrationTest` |
| No hardcoded secrets | `application.yml` placeholders, `.env.example`, `.gitignore` | Code review |
| Layered backend structure | `controller`, `service`, `repository`, `entity`, `dto`, `exception`, `config` | Code review |

## Unfinished items and known limitations

All required features from the brief are implemented. Known limitations:

- The frontend has no unit or end-to-end tests (only a strict type-checking build).
- Availability counts overlapping bookings, which is slightly conservative for long stays (see DESIGN.md section 4).
- No rate limiting on booking lookup, no email notifications, no pagination.
- The voucher is a printable HTML page (print to PDF), not a server-generated PDF.
- Automated tests run on H2 in MySQL mode, not on a real MySQL instance.

## Approximate time spent

`[fill in: e.g. 14 hours]`

## AI disclosure

This project was built with the help of **Claude** (Anthropic's AI assistant), used as a pair programmer in a
four-phase conversation (backend foundation, services and tests, Angular frontend, documentation).

**What I decided and directed:** the assignment brief, the tech stack, the pessimistic-locking approach, the choice of
the surprise feature (weekend pricing and voucher), the phase plan, and the design direction of the UI.

**What the AI produced:** the code (backend, tests, frontend), the Flyway migrations and seed data, the UI design and
SVG illustrations, and the drafts of this README and DESIGN.md.

**How the AI's work was checked in its own environment:** the Angular production build compiled with strict templates,
and the UI flows (search, booking, voucher download, cancellation, wrong-email lookup, mobile layout) were exercised in a
headless browser against a mock of the API. The backend could not be compiled or run there, so its tests had not been
executed when the code was handed over.

**What I verified myself** (complete before submitting):

- [ ] `mvn test` passes locally (result and date: `[fill in]`)
- [ ] Backend starts against my MySQL and the 8 hotels appear
- [ ] Full flow works in the browser against the real backend
- [ ] I have read the code and can explain the concurrency and pricing logic
- [ ] Changes I made after generation: `[fill in, or "none"]`
