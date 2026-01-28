# DevSecWatch Worker Service

Background worker service for DevSecWatch, handling git operations and scanning tasks.

## Setup

1. Configure environment variables.
2. Build: `mvn clean install`
3. Run: `mvn spring-boot:run`

## Environment Variables

- `DATABASE_URL`
- `RABBITMQ_HOST`
- `REDIS_URL`
