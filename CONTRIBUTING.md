# Contributing to DriveMeMaybeAPI

Thanks for contributing. This file explains how to set up, branch, commit, test, and open PRs.

## Getting Started

```bash
git clone https://github.com/NefloDev/DriveMeMaybeAPI.git
cd DriveMeMaybeAPI

createdb driveme_maybe

cp src/main/resources/.env.example src/main/resources/.env
# fill DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD, JWT_SECRET_KEY, OAUTH2_GOOGLE_CLIENT_ID
# or export them in your shell (see CONFIGURATION.md)

mvn clean install
mvn spring-boot:run
# API on http://localhost:8080
```

Smoke test your setup:

```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"secret123","name":"Alice","nickname":"ali"}'
```

## Development Environment

- Java 21+ (`java --version`)
- Maven 3.9+ (`mvn --version`)
- PostgreSQL 14+ (`psql --version`)
- Google OAuth client ID (only for `POST /auth/google/login`)

## Project Structure

See [ARCHITECTURE.md](ARCHITECTURE.md) for full architecture. Key folders: Client → API → Service → DB.

- `src/main/java/neflo/dev/*/api/` — REST controllers + DTO records (thin, HTTP-only)
- `src/main/java/neflo/dev/*/application/` — business logic; `auth/application/` for auth, JWT, Google
- `src/main/java/neflo/dev/*/domain/` — JPA entities (`user/`, `group/`, `trip/`)
- `src/main/java/neflo/dev/*/infrastructure/persistence/` — Spring Data JPA repositories; `shared/persistence/` for `JDBCRepository`
- `src/main/java/neflo/dev/*/infrastructure/mapper/` — MapStruct mappers
- `src/main/java/neflo/dev/shared/` — `security/` (`SecurityConfig`, `JwtAuthenticationFilter`, `JwtService`), `config/`, `exception/` (`CustomExceptionHandler`), `dto/`
- `src/main/resources/` — `application.properties`, `.env.example`

## Branching Strategy

- `main` is releasable, branch from it: `feat/...`, `fix/...`, `docs/...`.
- Keep branches short-lived, rebase/sync before PR.

## Commit Convention

- Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `chore:`.
- Small focused commits, present tense.

## Pull Requests

- Link related issue, describe API/resource changes + migrations.
- Keep thin controllers, add/adjust DTOs + validation.

## Code Style

- Java 21, Spring Boot conventions; MapStruct for entity ↔ DTO mapping.
- Keep controllers thin; put logic in `*/application/`; validate at the API boundary with DTO records.
- Keep auth (Security / JWT / Google) separate from domain logic.
- Extend `CustomExceptionHandler` for new error cases instead of leaking stack traces.
- Never commit secrets — use placeholders and local `.env` / shell exports.

## Testing Requirements

```bash
mvn test
mvn clean verify
# + smoke test curl for changed endpoints, e.g.:
curl http://localhost:8080/users/profile -H "Authorization: Bearer <token>"
```

## Issue Guidelines

- Bug: repro steps, expected vs actual, logs, versions.
- Feature: use case, proposed resources/endpoints.

## Review Process

Maintainer review, CI green, resolve threads before merge.

Before submitting a PR:

- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No secrets committed
- [ ] Code follows project conventions
- [ ] Existing tests pass
