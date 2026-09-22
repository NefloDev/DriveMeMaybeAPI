# DriveMeMaybeAPI

> REST API for tracking shared trips and keeping driving balances fair.

[![Java 21](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/) [![Spring Boot 4.1.0](https://img.shields.io/badge/Spring_Boot-4.1.0-brightgreen)](https://spring.io/projects/spring-boot) [![License: GPL-3.0-only](https://img.shields.io/badge/License-GPL--3.0--only-blue)](LICENSE)

DriveMeMaybeAPI powers the DriveMeMaybe mobile app. It manages users, trip groups joined by invite code, trips with driver plus passengers, and per-member time/km balances with driving-time insights. It is consumed by the mobile app and any HTTP client speaking JWT-authenticated REST.

## Features

- Email/password signup and login with JWT issuance
- Google Sign-In via server-side ID-token verification
- Token refresh for active sessions (`GET /auth/refresh`)
- Trip groups with invite-by-code (`groupCode`) creation, update, and leave flow
- Group responses with derived member and trip counts (`groupMembers`, `groupTrips`)
- User preferences: dark-mode flag and language, plus preference-filter lookup (`GET /users/prefs`)
- Group member listing and per-member time/km balances
- Trip recording with driver, passengers, date, distance and/or duration, origin/destination, notes
- Group trip listing and driving-time insights by month or year
- Stateless Spring Security with JWT filter and CORS
- Centralized error envelope via `CustomExceptionHandler`

## Screenshots / Demo

No UI — HTTP API. Base URL (development): `http://localhost:8080`. Full contract: [openapi.yaml](openapi.yaml).

Smoke test — signup and call a protected endpoint:

```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"secret123","name":"Alice","nickname":"ali"}'
# -> {"token":"<jwt>","expiresOn":1789990800000}

curl http://localhost:8080/users/profile \
  -H "Authorization: Bearer <token>"
# -> {"id":"11111111-1111-1111-1111-111111111111","email":"alice@example.com","name":"Alice","nickname":"ali","pfp":null,"preferences":{"isDarkMode":false,"preferredLang":null}}
```

## Requirements

- Java 21+ (`java --version`)
- Maven 3.9+ (`mvn --version`)
- PostgreSQL 14+ (`psql --version`), reachable database
- Google OAuth client ID (only for `POST /auth/google/login`)

```bash
java --version
mvn --version
psql --version
```

## Installation

```bash
git clone https://github.com/NefloDev/DriveMeMaybeAPI.git
cd DriveMeMaybeAPI

# 1. Create database
createdb driveme_maybe
# or: CREATE DATABASE driveme_maybe;

# 2. Configure environment (see Configuration)
cp src/main/resources/.env.example src/main/resources/.env
# then fill values, or export them in your shell

# 3. Build
mvn clean install

# 4. Run
mvn spring-boot:run
# API on http://localhost:8080
```

## Configuration

Brief summary — 5 variables, all read from the environment via `src/main/resources/application.properties`:

| Var | Required | Example |
|-----|----------|---------|
| `DATABASE_URL` | Yes | `jdbc:postgresql://localhost:5432/driveme_maybe` |
| `DATABASE_USER` | Yes | `postgres` |
| `DATABASE_PASSWORD` | Yes | `secret` |
| `JWT_SECRET_KEY` | Yes | Base64-encoded 256-bit+ key |
| `OAUTH2_GOOGLE_CLIENT_ID` | For Google login | `xxx.apps.googleusercontent.com` |

See [CONFIGURATION.md](CONFIGURATION.md) for full details.

## Usage

### How it works

1. Sign up or log in via `/auth/*` to obtain a JWT (`token`, `expiresOn`).
2. Create a trip group via `POST /groups/group` and share its `groupCode`.
3. Second user joins via `GET /users/groups/join/{groupCode}`.
4. Members record trips via `POST /trips/{groupId}/trip` with driver, passengers, and distance and/or duration.
5. Check fairness via `GET /groups/{groupId}/members/balance` and insights via `POST /groups/{groupId}/insights`.

```text
POST /auth/signup -> get JWT
POST /groups/group -> create group -> share groupCode
GET  /users/groups/join/{groupCode} -> second user joins
POST /trips/{groupId}/trip -> record a trip
GET  /groups/{groupId}/members/balance -> see who is ahead / behind
```

Example balance change — Alice drives Bob for 50 km / 45 min:

```text
Alice   +50 km / +45 min
Bob     -50 km / -45 min
```

### Concepts

#### Users

People using the app. A user belongs to many groups and takes part in many trips. Key fields (`UserDTO`): `email`, `password` (optional on update), `name`, `nickname`, optional `pfp`, plus `preferences` (`UserPreferences`: `isDarkMode`, `preferredLang`).

```json
{
  "email": "alice@example.com",
  "password": "secret123",
  "name": "Alice",
  "nickname": "ali",
  "pfp": "https://example.com/pfp/alice.png",
  "preferences": {
    "isDarkMode": true,
    "preferredLang": "EN"
  }
}
```

On update, `preferredLang` is resolved case-insensitively against the languages table (defaults to `EN`); the profile response returns the language formal name (e.g. `"English"`) and the stored dark-mode flag. `GET /users/prefs` returns the available options (`UserPreferencesFilters`):

```json
{
  "languageSelection": [
    { "code": "DE", "formalName": "Deutsch" },
    { "code": "EN", "formalName": "English" },
    { "code": "FR", "formalName": "Français" }
  ]
}
```

#### Trip groups

Users tracking trips together, joined via a generated `groupCode`. Create payload (`GroupRequestDTO`): `name` (required), `pfp` (optional). Stored shape (`GroupDTO`): `id`, `name`, `groupCode`, derived counts `groupMembers` / `groupTrips`, optional `pfp`.

```json
{
  "name": "Weekend road crew",
  "pfp": "https://example.com/pfp/group.png"
}
```

#### Trips

One journey inside a group: driver (member UUID), passengers (1+ member UUIDs), date, distance and/or duration, origin/destination, notes. Response resolves the driver nickname.

```json
{
  "driver": "11111111-1111-1111-1111-111111111111",
  "date": "2026-09-17",
  "durationMinutes": 45,
  "distanceKm": 50,
  "origin": "Berlin",
  "destination": "Potsdam",
  "notes": "Evening return",
  "passengers": ["22222222-2222-2222-2222-222222222222"]
}
```

#### Balances and insights

Balance (`GroupMemberBalanceDTO`): `nickname`, `timeBalance` (net minutes), `kmBalance` (net km). Driving adds, riding subtracts. Insights (`GroupInsights`): `year`, optional `month`, plus `driverDrivingTime` map of nickname to driven minutes.

```json
{
  "nickname": "ali",
  "timeBalance": 45,
  "kmBalance": 50
}
```

### API reference

Base URL: `http://localhost:8080`. All endpoints except `POST /auth/signup`, `POST /auth/login`, `POST /auth/google/login` require `Authorization: Bearer <token>`. For exact paths, see controllers in `src/main/java/neflo/dev/*/api/` (`auth/`, `user/`, `group/`, `trip/`).

#### Auth — `auth/api/AuthenticationController.java`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/signup` | Public | Register with `UserDTO`, returns `LoginResponse(token, expiresOn)` |
| POST | `/auth/login` | Public | Login with `{email, password}`, returns `LoginResponse` |
| GET | `/auth/refresh` | Bearer | Re-issue token for current user |
| POST | `/auth/google/login` | Public | Login with `{idToken}`, verifies with Google, returns `LoginResponse` |

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"secret123"}'

curl http://localhost:8080/users/profile \
  -H "Authorization: Bearer <token>"
```

#### Users — `user/api/UserController.java`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/users/profile` | Bearer | Current user details (incl. `preferences`) |
| GET | `/users/prefs` | Bearer | Available preference filters (`languageSelection` of `{code, formalName}`, sorted by code) |
| GET | `/users/groups` | Bearer | Groups the current user belongs to |
| GET | `/users/groups/join/{groupCode}` | Bearer | Join a group by invite code |
| PUT | `/users/update` | Bearer | Update profile + preferences (`UserDTO`) |
| DELETE | `/users/delete` | Bearer | Delete current user |

```bash
curl http://localhost:8080/users/groups/join/XK7Q2M \
  -H "Authorization: Bearer <token>"

curl http://localhost:8080/users/prefs \
  -H "Authorization: Bearer <token>"
# -> {"languageSelection":[{"code":"DE","formalName":"Deutsch"},{"code":"EN","formalName":"English"}]}
```

#### Groups — `group/api/GroupController.java`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/groups/group` | Bearer | Create group (`GroupRequestDTO`) |
| GET | `/groups/{groupId}` | Bearer | Group details (members only) |
| GET | `/groups/{groupId}/members` | Bearer | Member list |
| GET | `/groups/{groupId}/members/balance` | Bearer | Per-member balances |
| GET | `/groups/{groupId}/trips` | Bearer | Trips in group |
| POST | `/groups/{groupId}/insights` | Bearer | Insights for `{periodType, year, month}` |
| PUT | `/groups/{groupId}/update` | Bearer | Update group |
| PUT | `/groups/{groupId}/leave` | Bearer | Leave group |

```bash
curl -X POST http://localhost:8080/groups/group \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Weekend road crew"}'

curl -X POST http://localhost:8080/groups/<groupId>/insights \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"periodType":"MONTHLY","year":2026,"month":9}'
```

#### Trips — `trip/api/TripController.java`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/trips/{groupId}/trip` | Bearer | Create trip (`TripCreateDTO`) |
| GET | `/trips/{groupId}/{tripId}` | Bearer | Trip details |
| PUT | `/trips/{groupId}/{tripId}` | Bearer | Update trip |
| DELETE | `/trips/{groupId}/{tripId}` | Bearer | Delete trip |

```bash
curl -X POST http://localhost:8080/trips/<groupId>/trip \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"driver":"<uuid>","date":"2026-09-17","distanceKm":50,"durationMinutes":45,"passengers":["<uuid>"]}'
```

### Authentication

- Public: `POST /auth/signup`, `POST /auth/login`, `POST /auth/google/login` are `permitAll`. `GET /auth/refresh` and everything else require authentication (`SecurityConfig.java`).
- Stateless sessions (`SessionCreationPolicy.STATELESS`) with `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.
- Exact header format: `Authorization: Bearer <token>`.
- Token lifetime: 1 hour (`security.jwt.expiration-time=3600000` ms). Response carries absolute expiry `expiresOn` (epoch millis). No refresh tokens — re-issue via `GET /auth/refresh` with a valid JWT.
- Google login verifies `idToken` server-side against `OAUTH2_GOOGLE_CLIENT_ID` audience.
- Invalid, malformed, or expired JWTs are rejected with HTTP 403 and `authorizationException`.

### Error handling

Handled by `shared/exception/CustomExceptionHandler.java`. Envelope (`CustomErrorResponse`):

```json
{
  "errorCode": "validation-exception",
  "detail": "Group name must not be blank",
  "statusCode": 400,
  "statusName": "BAD_REQUEST",
  "timestamp": "2026-09-17T12:00:00"
}
```

Note: the current handler always constructs the body with `HttpStatus.NOT_FOUND` while the HTTP status line carries the real code — the table below shows the intended contract also documented in [openapi.yaml](openapi.yaml).

| HTTP | When |
|------|------|
| 400 | `ValidationException` (`validation-exception`) |
| 401 | `AuthenticationException` (`authentication-exception`) |
| 403 | JWT / filter failures (`JwtException`, `ExpiredJwtException`, `BadCredentialsException` → `authorizationException`) |
| 404 | `NoEntitiesFoundException` (`no-entities-found`) |
| 500 | `DatabaseException`, `UnexpectedException`, generic fallback (`generic-exception`) |

## Development

Controllers stay thin and HTTP-only; business rules live in `*/application/` (plus `auth/application/`); persistence lives in `*/infrastructure/persistence/` (plus `shared/persistence/`); the API never exposes entities — it uses DTO records mapped via MapStruct. Validate at the API boundary and extend `CustomExceptionHandler` instead of leaking stack traces.

See [ARCHITECTURE.md](ARCHITECTURE.md) for full architecture, modules, dependency rules, and data flow.

### Tech stack

| Tech | Version | Purpose |
|------|---------|---------|
| Java | 21 | Programming language |
| Spring Boot | 4.1.0 | Application framework |
| Spring Web | via Boot | REST API |
| Spring Data JPA + JDBC | via Boot | Persistence |
| Spring Security + OAuth2 Client | via Boot | Auth, JWT filter, Google verification |
| PostgreSQL driver | via Boot | Relational database |
| JJWT (`jjwt-api` / `jjwt-impl` / `jjwt-jackson`) | 0.13.0 | JWT creation / validation |
| Google API Client | 2.8.1 | Google ID-token verification |
| MapStruct | 1.6.3 | DTO ↔ entity mapping |
| Lombok | via Boot | Boilerplate reduction |
| Commons Lang3 | 3.20.0 | Utilities |
| Maven | 3.9+ | Build, WAR packaging (`DriveMeMaybeAPI.war`) |

### Project structure

```text
src/main/java/neflo/dev/     # MainApplication.java, ServletInitializer.java
src/main/java/neflo/dev/shared/ # exception/, security/, config/, persistence/, dto/
src/main/java/neflo/dev/auth/ # api/ (controllers + DTOs), application/ (services)
src/main/java/neflo/dev/user/ # api/, application/, domain/, infrastructure/persistence/, infrastructure/mapper/
src/main/java/neflo/dev/group/ # api/, application/, domain/, infrastructure/persistence/, infrastructure/mapper/
src/main/java/neflo/dev/trip/ # api/, application/, domain/, infrastructure/persistence/, infrastructure/mapper/
src/main/resources/          # application.properties, .env.example
```

### Running

```bash
# Development
mvn spring-boot:run

# Build WAR (Tomcat is provided-scope via ServletInitializer)
mvn clean package
# -> target/DriveMeMaybeAPI.war

# Run packaged artifact
java -jar target/DriveMeMaybeAPI.war
```

Logs default to `INFO` (`logging.level.root=INFO`).

## Testing

```bash
mvn test
```

Ensure a clean build plus tests before opening a PR:

```bash
mvn clean verify
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Please open an issue first for large changes and include curl smoke tests for any endpoint change.

## License

GPL-3.0-only — see [LICENSE](LICENSE).
