# Wallet System: Idempotency and Concurrency

Spring Boot 3.5 / Java 21 / H2 backend for a digital wallet (add money, transfer money) that stays
consistent under duplicate and concurrent requests.

Secured with Spring Security + JWT (stateless). Roles: `USER` (wallet APIs) and `ADMIN` (admin APIs).

## Run locally

Requirements: JDK 21 (Maven wrapper included).

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

| What            | URL                                   |
|-----------------|---------------------------------------|
| API             | http://localhost:8080                 |
| Swagger UI      | http://localhost:8080/swagger-ui.html |
| H2 console      | http://localhost:8080/h2-console      |
| Health          | http://localhost:8080/actuator/health |

H2 console: JDBC URL `jdbc:h2:file:./data/walletdb`, user `sa`, empty password.
Data is stored in `./data/` (file-based H2), so it survives restarts.

A default admin is created on first start: `admin@wallet.com` / `Admin@12345`. Override with the `ADMIN_EMAIL` / `ADMIN_PASSWORD` environment variables for any real deployment.

The H2 console is a development convenience: set `H2_CONSOLE_ENABLED=false` to turn it off (the Dockerfile does this).

## Tests, coverage, SonarQube

```bash
./mvnw clean verify             # runs tests + JaCoCo, FAILS if line coverage < 80%
# report: target/site/jacoco/index.html
```

SonarQube (local):
```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
# open http://localhost:9000 (admin/admin), create a project token, then:
./mvnw clean verify sonar:sonar -Dsonar.token=<your-token>
```
Coverage is passed to Sonar through the JaCoCo XML report (configured in `pom.xml`).

## API

Everything except `/auth/**` needs `Authorization: Bearer <token>` (token from `/auth/login`).
Money calls also need `Idempotency-Key: <unique string, 1-100 chars>`.

| Method | Path                  | Who   | Notes |
|--------|-----------------------|-------|-------|
| POST   | `/auth/register`      | public | creates USER + wallet (balance 0) in one transaction |
| POST   | `/auth/login`         | public | returns a JWT (`accessToken`, `role`, ...) |
| POST   | `/wallet/add`         | USER  | body `{"amount": 100.50}`, needs `Idempotency-Key`. Response includes `availableBalance` (your balance after the add) |
| POST   | `/wallet/transfer`    | USER  | body `{"receiverEmail": "b@x.com", "amount": 40}`, needs `Idempotency-Key`. Returns 200 on success with `availableBalance` (the sender's balance after the transfer); 422 `INSUFFICIENT_FUNDS` ("Insufficient balance") if the balance is too low. The failed attempt is still saved as a FAILED transaction |
| GET    | `/wallet`             | USER  | own balance |
| GET    | `/wallet/transactions?page=0&size=10` | USER | own history, paginated, newest first |
| GET    | `/admin/wallets`      | ADMIN | paginated |
| GET    | `/admin/transactions` | ADMIN | paginated |

```bash
curl -X POST localhost:8080/auth/register -H 'Content-Type: application/json' \
     -d '{"email":"a@x.com","password":"Passw0rd!"}'
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"a@x.com","password":"Passw0rd!"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')
curl -X POST localhost:8080/wallet/add -H "Authorization: Bearer $TOKEN" -H 'Idempotency-Key: k-1' \
     -H 'Content-Type: application/json' -d '{"amount":100}'
curl -X POST localhost:8080/wallet/transfer -H "Authorization: Bearer $TOKEN" -H 'Idempotency-Key: k-2' \
     -H 'Content-Type: application/json' -d '{"receiverEmail":"b@x.com","amount":40}'
```

### Request flow (transfer)

```
Security    -> CustomJwtVerificationFilter verifies the token, role rule checked (401 / 403)
Controller  -> caller id from the token, validate body (@Valid), read Idempotency-Key
Service     -> BEGIN TRANSACTION
              1. resolve sender / receiver wallet ids (ids only, nothing cached)
              2. SELECT ... FOR UPDATE on both wallets, in ascending id order
              3. idempotency lookup (sender wallet, TRANSFER, key):
                   found     -> same amount + receiver: return stored result
                                different request:      422 IDEMPOTENCY_KEY_REUSED
                   not found -> continue
              4. enough balance?  no -> save FAILED transaction (balances untouched)
                                  yes -> debit sender, credit receiver, save SUCCESS transaction
              COMMIT  (any exception = full rollback)
Controller  -> 200 (SUCCESS, also for a replayed duplicate) / 422 INSUFFICIENT_FUNDS error (FAILED result, already saved)
```

### Error format
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request",
  "code": "VALIDATION_FAILED", "message": "Request validation failed", "path": "/wallet/add",
  "details": ["amount: Amount must be at least 0.01"] }
```
| Status | When |
|--------|------|
| 400 | validation errors, malformed JSON, missing/blank/too long (>100) `Idempotency-Key`, transfer to self |
| 401 | missing / invalid / expired token, wrong login credentials (`INVALID_CREDENTIALS`) |
| 403 | valid token but wrong role (e.g. USER on `/admin/**`) |
| 404 | wallet or receiver not found |
| 409 | email already registered; lock or version conflict (safe to retry) |
| 422 | insufficient balance (recorded as a FAILED transaction); idempotency key reused for a different request |

## Design decisions

**Transaction boundaries.** One public `WalletService` method = one database transaction
(`@Transactional`). Registration creates user and wallet in a single transaction. Read methods are
`readOnly`. The balance change and the transaction record commit together or not at all.

**Concurrency strategy.** Pessimistic locking (`SELECT ... FOR UPDATE`, `PESSIMISTIC_WRITE`) on the
wallet row(s) for every money operation. Plus `@Version` on `Wallet` as an optimistic safety net.
There is no `synchronized` or in-memory locking. Transfers lock both wallets in ascending id order, so
A to B and B to A at the same time cannot deadlock. Lock waits time out (`LOCK_TIMEOUT` in the H2 URL)
and lock/version failures become a clean `409`.

**Idempotency handling.**
- Enforced by the database: unique constraint `(owner_wallet_id, type, idempotency_key)` on `transactions`.
- The key is scoped per user, so two users can use the same key value without affecting each other.
- Because the wallet row is locked *before* the key is checked, two simultaneous requests with the same key
  are serialized: the second one sees the first one's committed row and returns it instead of reprocessing.
- Same key + same request returns the stored result. Same key + different amount/receiver returns `422 IDEMPOTENCY_KEY_REUSED`.
- `ADD` and `TRANSFER` keys are independent of each other (the type is part of the constraint).
- A FAILED (insufficient balance) outcome is stored too (the balances are not touched), so replaying it returns the same `422 INSUFFICIENT_FUNDS` error (the error `details` carry the saved `transactionId` and the current `availableBalance`).
  To retry after topping up, use a new key.

**Money.** `BigDecimal` with scale 2 (`NUMERIC(19,2)`); amounts must be >= 0.01 with at most 2 decimals.

## Assumptions
- Transfers identify the receiver by e-mail (users do not know wallet ids). The sender's wallet always comes from the caller.
- Emails are stored lower-case and are unique.
- Public registration always creates role `USER`. The admin is seeded from config.
- `/wallet/**` is USER-only and `/admin/**` is ADMIN-only, as in the assignment (an admin token gets 403 on `/wallet/**`).
- `ADD` simulates money entering from outside (no payment gateway), so `from_wallet` is null.
- `transactions` stores wallet ids as plain columns (no JPA relations), which avoids lazy-loading issues.
- List endpoints are paginated (size capped at 100), including the admin ones.
- File-based H2 is used so history is persistent. On hosts with an ephemeral disk (e.g. free Render) data resets on redeploy.

## Security
- **Login:** `POST /auth/login` checks the BCrypt password hash and returns a signed JWT (HMAC, 1 hour). Wrong email and wrong password give the same `401` message.
- **Token:** subject = email, claims `user_id`, `user_email`, `user_role` (`USER` / `ADMIN`). Set your own key with the `JWT_SECRET` env variable (Base64, at least 256 bit). The default in `application.yml` is for development only.
- **Verification:** `CustomJwtVerificationFilter` validates signature and expiry on every request and stores the caller in the SecurityContext. Invalid tokens leave the request unauthenticated, which gives `401`.
- **Roles:** `SecurityConfig` requires `ROLE_ADMIN` for `/admin/**` and `ROLE_USER` for `/wallet/**`; everything else except `/auth/**` (and Swagger, the H2 console and `/actuator/health`) needs a valid token. Wrong role gives `403`. `@PreAuthorize` on `AdminController` is a second layer of the same rule.
- **Own wallet only:** controllers take the user id from the verified token (`@AuthenticationPrincipal`), never from request data.
- Stateless: no sessions, CSRF disabled (no cookies). 401/403 (from the filter chain and from `GlobalExceptionHandler`) use the same JSON error format as the rest of the API.

## Deployment (optional)
A `Dockerfile` is included. On Render/Railway: create a Web Service from this repo (Docker runtime). The app reads `PORT` automatically. Set `JWT_SECRET` (Base64, >= 256 bit) and `ADMIN_PASSWORD` as environment variables; use `/actuator/health` as the health check path.

## Project structure
```
config/      SecurityConfig (filter chain, BCrypt, AuthenticationManager), AdminInitializer (seeds admin), SwaggerConfig
security/    JwtUtils, CustomJwtVerificationFilter, JwtPrincipal, CustomUserDetailsService / Impl, CustomAuthenticationEntryPoint, CustomAccessDeniedHandler
controller/  Auth, Wallet, Admin REST controllers
dto/         request/response classes (Lombok) with validation annotations
entity/      User, Wallet (@Version), WalletTransaction (unique idempotency constraint)
exception/   ApiException, GlobalExceptionHandler, ErrorResponse
repository/  Spring Data repositories (locking queries)
service/     AuthService, WalletService, AdminService
```
