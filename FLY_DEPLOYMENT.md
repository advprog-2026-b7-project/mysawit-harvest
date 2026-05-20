# Fly.io Deployment

This service deploys as a Java 21 Spring Boot app using the included multi-stage `Dockerfile`.

## One-time setup

1. Log in:

   ```bash
   fly auth login
   ```

2. Create the Fly app. If `mysawit-harvest` is taken, choose another name and update `fly.toml`.

   ```bash
   fly apps create mysawit-harvest
   ```

3. Create a volume for uploaded harvest photos:

   ```bash
   fly volumes create harvest_uploads --size 1 --region sin
   ```

4. Create or attach PostgreSQL, then set JDBC secrets.

   If you already have a Postgres database, set:

   ```bash
   fly secrets set \
     SPRING_DATASOURCE_URL='jdbc:postgresql://HOST:5432/mysawit_harvest?sslmode=require' \
     SPRING_DATASOURCE_USERNAME='USERNAME' \
     SPRING_DATASOURCE_PASSWORD='PASSWORD'
   ```

   Also set the JWT secret to the same value used by the auth service:

   ```bash
   fly secrets set JWT_SECRET='replace-with-auth-service-jwt-secret'
   ```

5. Set public URL and CORS origins:

   ```bash
   fly secrets set \
     HARVEST_PUBLIC_BASE_URL='https://mysawit-harvest.fly.dev' \
     CORS_ALLOWED_ORIGINS='https://your-frontend-domain.example,http://localhost:3000'
   ```

## Deploy

```bash
fly deploy
```

## Verify

```bash
fly status
fly logs
curl https://mysawit-harvest.fly.dev/health
```

The harvest API remains available under `/api/v1`, for example:

```bash
curl https://mysawit-harvest.fly.dev/api/v1/harvests
```

Protected harvest endpoints still require a valid `Authorization: Bearer <token>` header.
