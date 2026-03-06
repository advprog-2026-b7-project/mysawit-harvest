# Security Configuration - Database Per Service Pattern
# 
# This file documents security best practices for managing
# database credentials in a microservice architecture.

## ✅ DO

- Use environment variables for credentials in production
- Use dedicated database users per service
- Store secrets in secure vault (AWS Secrets Manager, Azure Key Vault, HashiCorp Vault)
- Use `.env.example` as template and `.env` in .gitignore
- Rotate credentials periodically
- Use strong passwords (at least 16 characters, mixed case, symbols)
- Enable SSL/TLS for database connections in production
- Use principle of least privilege for database users

## ❌ DON'T

- Hardcode credentials in code or configuration files
- Commit `.env` files to version control
- Use same credentials across environments
- Share database between multiple services
- Use default passwords (sa, admin, etc.)
- Expose sensitive logs in production
- Allow root/admin user access from service accounts

## .gitignore Configuration

Ensure your `.gitignore` includes:

```
# Environment variables
.env
.env.local
.env.*.local

# IDE secrets
.idea/
.vscode/
*.key
*.pem

# Build artifacts with secrets
build/
out/
target/

# OS specific
.DS_Store
Thumbs.db

# Application-specific
uploads/
logs/
*.log

# Production config files
application-prod.properties
```

## Example: Secure Credentials Setup

### Development (Local Machine)

Create `.env` file (never commit):
```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/mysawit_harvest_db
SPRING_DATASOURCE_USERNAME=mysawit_harvest_user
SPRING_DATASOURCE_PASSWORD=dev_password_only
```

Load in shell:
```bash
# Load environment variables
set -o allexport
source .env
set +o allexport

# Run application
./gradlew bootRun
```

### Production (Docker/Kubernetes)

Use environment variables from orchestration platform:
```bash
# Docker
docker run -e DB_URL=jdbc:postgresql://... \
           -e DB_USERNAME=prod_user \
           -e DB_PASSWORD="$(cat /run/secrets/db_password)" \
           -e STORAGE_DIR=/opt/harvests \
           mysawit-harvest:latest

# Kubernetes
kubectl create secret generic harvest-db-secret \
  --from-literal=url=jdbc:postgresql://... \
  --from-literal=username=prod_user \
  --from-literal=password=$(openssl rand -base64 32)
```

## Database User Permissions (Production)

Create service-specific database user with minimal permissions:

```sql
-- Create service database user with limited privileges
CREATE USER mysawit_harvest_prod WITH PASSWORD 'strong_password_here';

-- Grant only necessary permissions
GRANT CONNECT ON DATABASE mysawit_harvest_db TO mysawit_harvest_prod;
GRANT USAGE ON SCHEMA harvest TO mysawit_harvest_prod;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA harvest TO mysawit_harvest_prod;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA harvest TO mysawit_harvest_prod;

-- Prevent direct schema modifications
REVOKE CREATE ON SCHEMA harvest FROM mysawit_harvest_prod;
REVOKE ALL ON DATABASE mysawit_harvest_db FROM public;
```

## Secrets Management Tools

### Local Development
- 1Password, LastPass (password managers)
- dotenv files
- AWS Secrets Manager local testing

### CI/CD Pipeline
- GitHub Secrets (for GitHub Actions)
- GitLab CI/CD Variables
- Jenkins Credentials Plugin

### Cloud Deployment
- AWS Secrets Manager
- Azure Key Vault
- Google Cloud Secret Manager
- HashiCorp Vault
- Kubernetes Secrets

## Audit & Monitoring

Enable database audit logging:

```sql
-- PostgreSQL audit logging
ALTER DATABASE mysawit_harvest_db SET log_connections = on;
ALTER DATABASE mysawit_harvest_db SET log_statement = 'ddl';
ALTER DATABASE mysawit_harvest_db SET log_min_duration_statement = 5000;
```

Monitor in application logs:
```properties
# application-prod.properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping=TRACE
```
