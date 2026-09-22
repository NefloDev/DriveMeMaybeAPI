# Configuration

All settings are read from environment variables via `src/main/resources/application.properties`. Never commit real secrets — use placeholders and local shell exports or an untracked `.env` file (note: `*.env` is gitignored).

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `DATABASE_URL` | Yes | Full JDBC URL, e.g. `jdbc:postgresql://localhost:5432/driveme_maybe` |
| `DATABASE_USER` | Yes | Database user, e.g. `postgres` |
| `DATABASE_PASSWORD` | Yes | Database password (never commit) |
| `JWT_SECRET_KEY` | Yes | Base64-encoded HS256 signing key, 256-bit minimum |
| `OAUTH2_GOOGLE_CLIENT_ID` | For Google login | Google OAuth client ID, e.g. `xxx.apps.googleusercontent.com`; verified as token audience |

Template: [`src/main/resources/.env.example`](src/main/resources/.env.example).

## Application Properties

File: [`src/main/resources/application.properties`](src/main/resources/application.properties).

| Property | Source | What it controls |
|---|---|---|
| `spring.datasource.url` | `${DATABASE_URL}` | JDBC connection string |
| `spring.datasource.username` | `${DATABASE_USER}` | DB user |
| `spring.datasource.password` | `${DATABASE_PASSWORD}` | DB password |
| `spring.datasource.driver-class-name` | `org.postgresql.Driver` | Fixed driver |
| `security.jwt.secret-key` | `${JWT_SECRET_KEY}` | HS256 signing key (must be Base64) |
| `security.jwt.expiration-time` | `3600000` (fixed, 1 hour) | Token lifetime in ms; `expiresOn` = issued + this |
| `spring.security.oauth2.client.registration.google.client-id` | `${OAUTH2_GOOGLE_CLIENT_ID}` | Expected Google token audience |
| `logging.level.root` | `INFO` | Log verbosity |

## Profiles

No Spring profiles (`spring.profiles.active`) are defined in this repo. Use environment values per stage:

### Development

```bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/driveme_maybe"
export DATABASE_USER="postgres"
export DATABASE_PASSWORD="secret"
export JWT_SECRET_KEY="<base64-256-bit-key>"
export OAUTH2_GOOGLE_CLIENT_ID="xxx.apps.googleusercontent.com"
mvn spring-boot:run
# API on http://localhost:8080
```

### Testing

```bash
mvn test
mvn clean verify
```

Point `DATABASE_URL` at a scratch database if tests require one; keep secrets out of CI logs via masked variables.

### Production

- Set all 5 variables in the host/secret manager (WAR runs with provided Tomcat via `ServletInitializer`).
- Use a long random Base64 `JWT_SECRET_KEY` unique per environment.
- Restrict DB user privileges and use TLS (`?sslmode=require` on `DATABASE_URL` when supported).

```bash
mvn clean package
# -> target/DriveMeMaybeAPI.war
java -jar target/DriveMeMaybeAPI.war
```

## External Services

### PostgreSQL

1. Install PostgreSQL 14+ and create the database:
   ```bash
   createdb driveme_maybe
   ```
2. Set `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`.
3. Verify connectivity:
   ```bash
   psql "$DATABASE_URL" -U "$DATABASE_USER" -c "SELECT 1;"
   ```

### Google OAuth (for `POST /auth/google/login`)

1. Create an OAuth client ID in Google Cloud Console.
2. Set `OAUTH2_GOOGLE_CLIENT_ID` to that client ID.
3. Verify: log in from the mobile app and call:
   ```bash
   curl -X POST http://localhost:8080/auth/google/login \
     -H "Content-Type: application/json" \
     -d '{"idToken":"<google-id-token>"}'
   # -> {"token":"<jwt>","expiresOn":...}
   ```
