# DevSecWatch — Deployment Guide

A complete guide for deploying DevSecWatch locally and to the cloud (Railway + Vercel).

---

## 🏠 Local Development (Docker Compose)

This is the fastest way to run the full stack locally.

### Prerequisites
- Docker Desktop installed and running
- Java 21+ and Maven 3.9+
- Node.js 18+ and npm
- Python 3.11+ (optional, if not using Docker for AI service)
- Groq API Key from https://console.groq.com

### Step 1 — Configure AI Service
```bash
# Copy the example env file
cp ai-service/.env.example ai-service/.env

# Edit and add your Groq API key
# GROQ_API_KEY=gsk_your_key_here
```

### Step 2 — Start Infrastructure + All Services
```powershell
# Option A: Infrastructure only (then run services manually)
docker-compose up -d

# Option B: Full automated startup (PowerShell)
.\start_all.ps1
```

The `start_all.ps1` script will:
1. Start PostgreSQL, RabbitMQ, Redis via Docker
2. Start Backend API (Spring Boot, port 8080)
3. Start Worker Service (Spring Boot, port 8081)
4. Start AI Service (FastAPI, port 8000)
5. Start Frontend dev server (Vite, port 5173)

### Step 3 — Verify Everything is Running
| Service | URL | Expected Response |
|---------|-----|-------------------|
| Frontend | http://localhost:5173 | React app loads |
| Backend API | http://localhost:8080/actuator/health | `{"status":"UP"}` |
| AI Service | http://localhost:8000/health | `{"status":"healthy","llm_service":"groq"}` |
| Worker | http://localhost:8081/actuator/health | `{"status":"UP"}` |
| RabbitMQ UI | http://localhost:15672 | Login: guest / guest |
| PostgreSQL | localhost:5433 | DB: devsecwatch, User: postgres |
| Redis | localhost:6379 | — |

### Step 4 — Run Integration Tests
```bash
python test_apis.py
```

---

## ☁️ Cloud Deployment — Railway + Vercel

### Architecture
```
Vercel (Frontend)  →  Railway (backend-api)  →  Railway (worker-service)
                   ↓                         ↓
              Railway (ai-service)      Railway (PostgreSQL + Redis + RabbitMQ)
```

---

## 1. Managed Infrastructure on Railway

Login at https://railway.app and create a new project.

### Add PostgreSQL Plugin
1. Click **"+ New"** → **"Database"** → **"PostgreSQL"**
2. Note the connection variables from the **"Connect"** tab:
   - `DATABASE_URL` (JDBC format): `jdbc:postgresql://<host>:<port>/<db>`
   - `DATABASE_USERNAME`
   - `DATABASE_PASSWORD`

### Add Redis Plugin
1. Click **"+ New"** → **"Database"** → **"Redis"**
2. Note:
   - `REDIS_URL` (e.g., `redis://:password@host:port`)
   - Extract `SPRING_DATA_REDIS_HOST` and `SPRING_DATA_REDIS_PORT`

### Add RabbitMQ (via CloudAMQP or Template)
Option A — CloudAMQP (recommended free tier):
1. Sign up at https://www.cloudamqp.com
2. Create a free "Little Lemur" instance
3. Get connection details:
   - `RABBITMQ_HOST`
   - `RABBITMQ_PORT` (usually 5672)
   - `RABBITMQ_USERNAME`
   - `RABBITMQ_PASSWORD`
   - `RABBITMQ_VHOST` (usually `/`)

Option B — Railway RabbitMQ template:
1. Click **"+ New"** → **"Template"** → search "RabbitMQ"

---

## 2. Backend API Service (Railway)

### Deployment Steps
1. In your Railway project, click **"+ New"** → **"GitHub Repo"**
2. Connect your repository
3. Set **Root Directory** to `backend-api`
4. Railway will auto-detect the Dockerfile

### Environment Variables
| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | JDBC PostgreSQL URL | `jdbc:postgresql://host:5432/railway` |
| `DATABASE_USERNAME` | DB username | `postgres` |
| `DATABASE_PASSWORD` | DB password | `your-db-password` |
| `RABBITMQ_HOST` | RabbitMQ hostname | `rabbit.cloudamqp.com` |
| `RABBITMQ_PORT` | RabbitMQ port | `5672` |
| `RABBITMQ_USERNAME` | RabbitMQ user | `abc123` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | `your-rabbit-pass` |
| `RABBITMQ_VHOST` | Virtual host | `/` |
| `SPRING_DATA_REDIS_HOST` | Redis hostname | `redis.railway.internal` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `JWT_SECRET` | 64-char secret key | `openssl rand -base64 48` |
| `FRONTEND_URL` | Deployed frontend URL | `https://devsecwatch.vercel.app` |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `prod` |
| `GITHUB_CLIENT_ID` | GitHub OAuth App ID | (from GitHub Developer Settings) |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth App Secret | (from GitHub Developer Settings) |

### Generate a secure JWT secret:
```bash
# PowerShell
[Convert]::ToBase64String((1..48 | ForEach-Object { [byte](Get-Random -Max 256) }))

# Linux/Mac
openssl rand -base64 48
```

---

## 3. Worker Service (Railway)

### Deployment Steps
1. Click **"+ New"** → **"GitHub Repo"** (same repo)
2. Set **Root Directory** to `worker-service`
3. Uses the Dockerfile which installs Python + Semgrep inside the container

### Environment Variables
| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | Same as Backend |
| `DATABASE_USERNAME` | Same as Backend |
| `DATABASE_PASSWORD` | Same as Backend |
| `RABBITMQ_HOST` | Same as Backend |
| `RABBITMQ_PORT` | Same as Backend |
| `RABBITMQ_USERNAME` | Same as Backend |
| `RABBITMQ_PASSWORD` | Same as Backend |
| `RABBITMQ_VHOST` | Same as Backend |
| `SPRING_DATA_REDIS_HOST` | Same as Backend |
| `SPRING_DATA_REDIS_PORT` | Same as Backend |
| `AI_SERVICE_URL` | URL of deployed AI service (e.g. `https://ai-service.up.railway.app`) |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `APP_TEMP_DIRECTORY` | `/tmp/devsecwatch` |

> **Note:** The Worker Dockerfile installs Semgrep directly (no Docker-in-Docker needed). This is because Railway containers cannot run Docker inside them.

---

## 4. AI Service (Railway)

### Deployment Steps
1. Click **"+ New"** → **"GitHub Repo"** (same repo)
2. Set **Root Directory** to `ai-service`
3. Uses `Dockerfile` (Python 3.11-slim + FastAPI + Uvicorn)

### Environment Variables
| Variable | Description | Example |
|----------|-------------|---------|
| `GROQ_API_KEY` | Your Groq API Key | `gsk_...` |
| `BACKEND_URL` | URL of Backend service | `https://backend-api.up.railway.app` |

> ⚠️ **Security:** Never commit `ai-service/.env` — the `.gitignore` already excludes `.env` files.

---

## 5. Frontend (Vercel)

### Deployment Steps
1. Go to https://vercel.com → **"Add New Project"**
2. Import your GitHub repository
3. Set **Root Directory** to `devsecwatch-frontend`
4. Framework preset: **Vite**
5. Build command: `npm run build`
6. Output directory: `dist`

### Environment Variables
| Variable | Description | Example |
|----------|-------------|---------|
| `VITE_API_URL` | Public URL of Backend API | `https://backend-api.up.railway.app` |

### GitHub OAuth Callback
After deploying, update your GitHub OAuth App's callback URL:
- Go to GitHub → Settings → Developer Settings → OAuth Apps
- Set Authorization callback URL to: `https://backend-api.up.railway.app/login/oauth2/code/github`

---

## 🔒 Security Checklist Before Going Live

- [ ] `JWT_SECRET` is a strong random 64-character string (not the default)
- [ ] `ai-service/.env` is NOT committed to Git
- [ ] GitHub OAuth client secret is stored as Railway env var (not in code)
- [ ] PostgreSQL password is strong and unique
- [ ] RabbitMQ credentials are changed from defaults (`guest/guest`)
- [ ] CORS is configured with production frontend URL only (not `*`)
- [ ] `SPRING_PROFILES_ACTIVE=prod` is set (enables validation-mode DB, structured logging)
- [ ] Backend API is behind HTTPS (Railway provides this automatically)

---

## ✅ Post-Deployment Verification

1. **Visit** your Vercel frontend URL
2. **Register** a new user account
3. **Create a scan** with: `https://github.com/spring-projects/spring-petclinic`
4. **Watch** real-time WebSocket progress updates (Queued → Cloning → Scanning → AI Enrichment → Completed)
5. **Verify** vulnerability list with AI-powered descriptions and fix suggestions
6. **Test chat** with the AI Security Copilot
7. **Check** RabbitMQ management UI (if accessible) for queue activity

---

## 🔧 Troubleshooting

### Backend fails to connect to RabbitMQ
- Verify `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_VHOST` are all set correctly
- CloudAMQP vhost is typically your account name (not `/`)

### Flyway migration fails
- Ensure `DATABASE_URL` uses JDBC format: `jdbc:postgresql://host:port/db`
- Check that `SPRING_PROFILES_ACTIVE=prod` is set (uses `validate` DDL, not `create`)

### Worker can't find Semgrep
- The Worker Dockerfile installs Semgrep via `pip3 install semgrep`
- Verify the Docker build completes without errors in Railway logs

### Frontend shows CORS error
- Set `FRONTEND_URL` env var on Backend to match your exact Vercel URL
- Include the `https://` prefix (no trailing slash)

### AI Service returns template explanations only
- Check `GROQ_API_KEY` is set correctly on the AI Service deployment
- Visit `https://your-ai-service.up.railway.app/health` — `llm_service` should be `"groq"` not `"template_only"`

---

## 💸 Completely Free Deployment Strategy

If you want to deploy the entire DevSecWatch stack (Frontend, Backend, Worker, AI, Postgres, Redis, RabbitMQ) completely for free without trial credits, use the **Oracle Cloud Infrastructure (OCI) Always Free Tier**.

OCI offers up to **4 ARM Ampere A1 Compute instances with 24GB of RAM** completely free forever. This is more than enough to run the entire `docker-compose.full.yml` stack on a single server.

### 1. Provision an Oracle Cloud VM
1. Sign up for [Oracle Cloud Free Tier](https://www.oracle.com/cloud/free/).
2. Create a Compute Instance:
   - **Shape**: `VM.Standard.A1.Flex` (ARM-based)
   - **OCPUs**: 2 to 4
   - **Memory**: 12GB to 24GB
   - **Image**: Ubuntu 22.04 or 24.04
   - **Boot Volume**: up to 200GB (Always Free)
3. Download the SSH keys.
4. Update Security Lists (Firewall) in Oracle Cloud Dashboard to allow ingress on ports `80`, `443`, `8080`, `8081`, `8000`, `15672` (or whichever ones you wish to expose publicly).

### 2. Connect and Install Docker
SSH into your instance:
```bash
ssh -i <your-key.pem> ubuntu@<instance-public-ip>
```

Install Docker and Docker Compose:
```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
newgrp docker
```

### 3. Deploy DevSecWatch
Clone your repository onto the VM:
```bash
git clone <your-repo-url> devsecwatch
cd devsecwatch
```

Configure environment variables:
```bash
# Add your GROQ API key
cp ai-service/.env.example ai-service/.env
nano ai-service/.env
```

Start the entire stack using Docker Compose:
```bash
# Start all infrastructure and services in the background
docker compose -f docker-compose.full.yml up -d --build
```

### 4. Deploy Frontend (Vercel)
For the frontend, it's best to continue using **Vercel** (which is 100% free for frontend hosting):
1. Import your repo to Vercel.
2. Set **Root Directory** to `devsecwatch-frontend`.
3. Set the Environment Variable `VITE_API_URL` to your Oracle VM's public IP: `http://<your-vm-ip>:8080`.
4. Deploy!

### 5. Final Setup
- In your Oracle VM's `docker-compose.full.yml`, ensure the `backend-api` has `FRONTEND_URL` set to your Vercel URL.
- Update GitHub OAuth settings with `http://<your-vm-ip>:8080/login/oauth2/code/github`.
- **Note:** For production use, you should set up a Reverse Proxy (like Caddy or Nginx Proxy Manager) on the Oracle VM to enable HTTPS (SSL) for your APIs, as GitHub OAuth generally requires HTTPS callbacks. Caddy is excellent for automatic free SSL via Let's Encrypt.

---

## ☁️ Alternative: Managed PaaS Free Tier (No Virtual Machine)

If you strictly do not want to manage a Virtual Machine and want to deploy everything using purely managed Platform-as-a-Service (PaaS) providers for free, you will need to piece together multiple services.

> ⚠️ **Warning:** Free tiers on PaaS providers (like Koyeb or Render) typically limit you to **512MB of RAM**. Java Spring Boot applications and Python AI models consume a lot of memory and may crash frequently if they exceed these limits.

### 1. Database (PostgreSQL)
Use **Supabase** or **Neon**:
1. Create a free account and start a new project.
2. Get the JDBC connection string (`DATABASE_URL`).
3. Note the DB username and password.

### 2. Cache (Redis)
Use **Upstash**:
1. Create a free Upstash Redis database.
2. Get the `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, and password.

### 3. Message Broker (RabbitMQ)
Use **CloudAMQP**:
1. Create a free "Little Lemur" instance.
2. Extract the `RABBITMQ_HOST`, port, username, password, and vhost.

### 4. Backend Services (Koyeb or Render)
You can deploy your 3 backend services (API, Worker, AI) to **Koyeb** (offers 1 free service) and **Render** (offers free web services, though they spin down after inactivity). You may need to spread the 3 services across different providers or multiple accounts to stay on the free tiers.
1. Connect your GitHub repository.
2. Set the root directory to `backend-api` (or `worker-service` / `ai-service`).
3. Input all the environment variables collected from steps 1-3.
4. For the Worker, be aware that Railway/Render free tiers might struggle with installing `semgrep` on low RAM.

### 5. Frontend (Vercel)
Use **Vercel** as usual:
1. Import the repository and set the root to `devsecwatch-frontend`.
2. Set `VITE_API_URL` to your deployed Backend API URL (e.g., `https://my-backend.koyeb.app`).

### Summary of this approach:
- **Pros:** No server to manage, automatic SSL certificates (HTTPS), auto-deploy on git push.
- **Cons:** Very fragmented, low memory limits (512MB) causing potential out-of-memory crashes, backend services sleep after inactivity, more complex to set up.
