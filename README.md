# Traffic Violation & Fine Management System (TVMS)

Web Technology project · AUCA

Traffic police in Rwanda record offences on paper or in separate systems, drivers learn about fines late, and paying or contesting a fine means visiting an office. **TVMS** lets officers record a violation at the roadside in under a minute, notifies the driver instantly by SMS and email, and lets drivers check, pay (Mobile Money, simulated) or appeal their fines online. Administrators manage the registry, decide appeals and follow collections on a dashboard.

## Roles

| Role | What they can do |
| --- | --- |
| Public (no login) | Check outstanding fines by plate number |
| Driver | Register (links to an existing license record), see own and owned-vehicle violations, pay with MoMo, appeal within 14 days, track demerit points |
| Officer | Look up a vehicle by plate, record a violation with GPS, photos and offence-specific evidence, see own records |
| Admin | Dashboard, decide appeals, manage offence types, drivers, vehicles, officer accounts and user accounts |

## Business rules

- A fine is due **30 days** after it is issued; its amount is copied from the offence type at issue time.
- Unpaid fines past the due date become **OVERDUE** with a **10% penalty** (nightly job); a reminder goes out **3 days** before the due date.
- Offences must be recorded within **7 days** and never in the future.
- Drivers can appeal **once**, within **14 days**, while the fine is unpaid. A pending appeal blocks payment and stops the fine from going overdue. Accepted: violation dismissed and fine cancelled. Rejected: at least 7 more days to pay.
- Demerit points over the last 12 months; **20 points** puts the license at risk.
- Drivers must be 18+, licenses and plates are unique, plates follow the Rwandan format `RAB123A`.

## Tech stack

| Layer | Technology |
| --- | --- |
| Frontend | React 19, Vite, Tailwind CSS 4, React Router, Axios |
| Backend | Spring Boot 4 (Java 17), layered package-by-feature (controller → service → repository) |
| Relational DB | PostgreSQL 17: users, drivers, officers, vehicles, violation types, violations, fines |
| NoSQL DB | MongoDB 7: roadside evidence (flexible per offence), notification logs, audit logs (180-day TTL) |
| Cache | Redis: offence catalogue and admin statistics |
| Messaging | RabbitMQ topic exchange `tvms.events` → email + SMS queues, 3 retries, dead-letter queue |
| Security | Spring Security, JWT (15 min) + rotating refresh tokens, Google OAuth2, BCrypt, RBAC |
| DevOps | Docker multi-stage images, Docker Compose, GitHub Actions CI → GHCR |
| Testing | JUnit 5, Mockito, AssertJ (22 unit tests) |

## Run it

Requirements: Docker Desktop (and Java 17 + Maven + Node 20+ for development mode).

```bash
docker compose --profile app up -d --build
```

| Service | URL |
| --- | --- |
| Web app | http://localhost:3000 |
| API | http://localhost:8080/api |
| Mailpit (test inbox) | http://localhost:8025 |
| RabbitMQ dashboard | http://localhost:15672 (tvms / tvms123) |

Development mode:

```bash
docker compose up -d                          # PostgreSQL, MongoDB, Redis, RabbitMQ, Mailpit
cd backend && mvn spring-boot:run             # API on :8080
cd frontend && npm install && npm run dev     # UI on :5173
```

Demo accounts (seeded on first start):

| Role | Email | Password |
| --- | --- | --- |
| Admin | admin@tvms.rw | Admin@12345 |
| Officer | officer@tvms.rw | Officer@12345 |
| Driver | driver@tvms.rw | Driver@12345 |

Try the public lookup with plates `RAB123A` or `RAD456B`. Driver Jean Mugisha (license `RW-DL-200002`, born 1990-07-01) has no account yet: register with those details to see license linking.

Google login: create an OAuth client with redirect URI `http://localhost:8080/login/oauth2/code/google` and put `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in a `.env` file next to `docker-compose.yml`.

## Main API

| Method | Path | Access |
| --- | --- | --- |
| POST | /api/auth/register, /login, /refresh, /logout | Public |
| GET | /oauth2/authorization/google | Public |
| GET | /api/public/fines?plate=RAB123A | Public |
| GET | /api/violation-types | Logged in |
| POST | /api/violations | OFFICER |
| GET | /api/violations/recorded-by-me | OFFICER |
| GET | /api/vehicles/plate/{plate} | OFFICER, ADMIN |
| GET | /api/me/summary, /api/me/vehicles, /api/me/violations | DRIVER |
| POST | /api/violations/{id}/appeal | DRIVER (own) |
| POST | /api/fines/{id}/pay | DRIVER (own) |
| GET | /api/violations?plate=&appealStatus= | ADMIN |
| PATCH | /api/violations/{id}/appeal-decision | ADMIN |
| CRUD | /api/drivers, /api/vehicles, /api/officers, /api/violation-types | ADMIN (read: OFFICER) |
| ALL | /api/admin/** | ADMIN |

## Tests

```bash
cd backend && mvn test
```

## Git workflow

`main` ← `develop` ← feature work, merged through a pull request.
