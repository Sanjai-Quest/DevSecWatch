# DevSecWatch Deployment Guide

This guide describes how to deploy the DevSecWatch services to a cloud environment (e.g., Railway, Vercel).

## Prerequisites
- **PostgreSQL Database**
- **RabbitMQ Service**
- **Redis Service**
- **Groq API Key** for AI explanations
- **JDK 17+** and **Maven** (for building locally if needed)

## 1. Backend Service (Railway/Docker)
The backend is a Spring Boot application.

### Environment Variables
| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | JDBC URL for PostgreSQL | `jdbc:postgresql://host:port/db` |
| `DATABASE_USERNAME` | DB Username | `postgres` |
| `DATABASE_PASSWORD` | DB Password | `secret` |
| `RABBITMQ_HOST` | RabbitMQ Host | `rabbitmq.railway.internal` |
| `RABBITMQ_PORT` | RabbitMQ Port | `5672` |
| `RABBITMQ_USERNAME` | RabbitMQ User | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ Pass | `guest` |
| `RABBITMQ_VHOST` | Virtual Host | `/` |
| `REDIS_URL` | Redis Connection URL | `redis://:pass@host:port` |
| `JWT_SECRET` | Secure 64-char string | `openssl rand -base64 48` |
| `FRONTEND_URL` | URL of deployed frontend | `https://devsecwatch.vercel.app` |

### Deployment Steps
1.  Push code to GitHub.
2.  Connect repository to Railway.
3.  Set the Root Directory to `backend-api`.
4.  Add the environment variables above.
5.  Deploy.

## 2. Worker Service (Railway/Docker)
The worker consumes messages and runs scans using Semgrep.

### Environment Variables
| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | Same as Backend |
| `DATABASE_USERNAME` | Same as Backend |
| `DATABASE_PASSWORD` | Same as Backend |
| `RABBITMQ_HOST` | Same as Backend |
| `RABBITMQ_PORT` | Same as Backend |
| `RABBITMQ_USERNAME`| Same as Backend |
| `RABBITMQ_PASSWORD`| Same as Backend |
| `RABBITMQ_VHOST` | Same as Backend |
| `REDIS_URL` | Same as Backend |
| `AI_SERVICE_URL` | URL of AI Service |

### Deployment Steps
1.  Connect repository to Railway (create new service).
2.  Set Root Directory to `worker-service`.
3.  Add environment variables.
4.  Deploy.

## 3. AI Service (Railway/Python)
The AI service proxies LLM requests.

### Environment Variables
| Variable | Description |
|----------|-------------|
| `GROQ_API_KEY` | Your Groq API Key |
| `BACKEND_URL` | URL of Backend |

### Deployment Steps
1.  Connect repository to Railway.
2.  Set Root Directory to `ai-service`.
3.  Add environment variables.
4.  Deploy.

## 4. Frontend (Vercel)
React SPA using Vite.

### Environment Variables
| Variable | Description |
|----------|-------------|
| `VITE_API_URL` | URL of deployed Backend |

### Deployment Steps
1.  Connect repository to Vercel.
2.  Set Root Directory to `devsecwatch-frontend`.
3.  Add `VITE_API_URL` environment variable.
4.  Deploy.

## Verification
1.  Visit Frontend URL.
2.  Register a new user.
3.  Create a scan (e.g. `https://github.com/spring-projects/spring-petclinic`).
4.  Wait for completion and verify results.
