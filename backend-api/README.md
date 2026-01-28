# DevSecWatch Backend API

Spring Boot 3.2 application for the DevSecWatch security scanning system.

## Setup

1. Configure environment variables in `.env` or run with defaults.
2. Build: `mvn clean install`
3. Run: `mvn spring-boot:run`

## Environment Variables

- `DATABASE_URL`
- `RABBITMQ_HOST`
- `REDIS_URL`
- `JWT_SECRET`
