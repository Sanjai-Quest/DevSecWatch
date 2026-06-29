#Requires -Version 5.1
<#
.SYNOPSIS
    DevSecWatch - Full Stack Startup Script
.DESCRIPTION
    Starts all DevSecWatch services: infrastructure (Docker) + Backend + Worker + AI + Frontend
    Includes health checks to verify each service is ready before proceeding.
#>

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path

function Write-Banner {
    Write-Host ""
    Write-Host "  ██████╗ ███████╗██╗   ██╗███████╗███████╗ ██████╗██╗    ██╗ █████╗ ████████╗ ██████╗██╗  ██╗" -ForegroundColor Cyan
    Write-Host "  ██╔══██╗██╔════╝██║   ██║██╔════╝██╔════╝██╔════╝██║    ██║██╔══██╗╚══██╔══╝██╔════╝██║  ██║" -ForegroundColor Cyan
    Write-Host "  ██║  ██║█████╗  ██║   ██║███████╗█████╗  ██║     ██║ █╗ ██║███████║   ██║   ██║     ███████║" -ForegroundColor Blue
    Write-Host "  ██║  ██║██╔══╝  ╚██╗ ██╔╝╚════██║██╔══╝  ██║     ██║███╗██║██╔══██║   ██║   ██║     ██╔══██║" -ForegroundColor Blue
    Write-Host "  ██████╔╝███████╗ ╚████╔╝ ███████║███████╗╚██████╗╚███╔███╔╝██║  ██║   ██║   ╚██████╗██║  ██║" -ForegroundColor Magenta
    Write-Host "  ╚═════╝ ╚══════╝  ╚═══╝  ╚══════╝╚══════╝ ╚═════╝ ╚══╝╚══╝ ╚═╝  ╚═╝   ╚═╝    ╚═════╝╚═╝  ╚═╝" -ForegroundColor Magenta
    Write-Host "                       🛡️  AI-Powered Security Vulnerability Scanner" -ForegroundColor Yellow
    Write-Host ""
}

function Write-Step { param($n, $msg) Write-Host "[$n] $msg" -ForegroundColor Green }
function Write-OK   { param($msg) Write-Host "  ✅ $msg" -ForegroundColor Green }
function Write-Warn { param($msg) Write-Host "  ⚠️  $msg" -ForegroundColor Yellow }
function Write-Fail { param($msg) Write-Host "  ❌ $msg" -ForegroundColor Red }
function Write-Info { param($msg) Write-Host "  ℹ️  $msg" -ForegroundColor Cyan }

function Wait-ForHttp {
    param($Url, $ServiceName, $MaxSeconds = 90, $Interval = 3)
    Write-Info "Waiting for $ServiceName at $Url (up to ${MaxSeconds}s)..."
    $elapsed = 0
    while ($elapsed -lt $MaxSeconds) {
        try {
            $r = Invoke-WebRequest -Uri $Url -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
            if ($r.StatusCode -lt 400) {
                Write-OK "$ServiceName is UP! ($elapsed s)"
                return $true
            }
        } catch { }
        Start-Sleep -Seconds $Interval
        $elapsed += $Interval
        Write-Host "    ..." -ForegroundColor DarkGray
    }
    Write-Fail "$ServiceName did not respond after ${MaxSeconds}s"
    return $false
}

function Test-DockerRunning {
    try {
        docker ps > $null 2>&1
        return $LASTEXITCODE -eq 0
    } catch { return $false }
}

# ─── Main ─────────────────────────────────────────────────────────────────────

Write-Banner

# ─── Step 1: Docker Infrastructure ────────────────────────────────────────────
Write-Step 1 "Starting infrastructure (PostgreSQL, RabbitMQ, Redis) via Docker..."

if (-not (Test-DockerRunning)) {
    Write-Fail "Docker is not running! Please start Docker Desktop first."
    Write-Info "Download Docker Desktop: https://www.docker.com/products/docker-desktop/"
    exit 1
}

Set-Location $Root
docker-compose down --remove-orphans 2>&1 | Out-Null
docker-compose up -d 2>&1 | Out-Null

Write-Info "Waiting for infrastructure to initialize (15s)..."
Start-Sleep -Seconds 15

# Quick health checks for infra
$rabbitOk = Wait-ForHttp "http://localhost:15672" "RabbitMQ Management" 60
$pgOk = $true  # Postgres doesn't have HTTP, assume OK if container is up

if (-not $rabbitOk) {
    Write-Warn "RabbitMQ not responding — continuing anyway (services will retry)"
}

# ─── Step 2: AI Service ────────────────────────────────────────────────────────
Write-Step 2 "Starting AI Service (FastAPI + Groq)..."

$aiVenv = Join-Path $Root "ai-service\venv\Scripts\python.exe"
$aiDir  = Join-Path $Root "ai-service"

if (Test-Path $aiVenv) {
    $aiProcess = Start-Process -FilePath $aiVenv `
        -ArgumentList "-m", "uvicorn", "app.main:app", "--host", "127.0.0.1", "--port", "8000" `
        -WorkingDirectory $aiDir `
        -WindowStyle Minimized `
        -PassThru
    Write-Info "AI Service started (PID: $($aiProcess.Id))"
} else {
    # Fallback: use system python
    $aiProcess = Start-Process -FilePath "python" `
        -ArgumentList "-m", "uvicorn", "app.main:app", "--host", "127.0.0.1", "--port", "8000" `
        -WorkingDirectory $aiDir `
        -WindowStyle Minimized `
        -PassThru
    Write-Info "AI Service started with system Python (PID: $($aiProcess.Id))"
}

$aiOk = Wait-ForHttp "http://localhost:8000/health" "AI Service" 30
if (-not $aiOk) { Write-Warn "AI Service not ready — Backend will still start" }

# ─── Step 3: Backend API ───────────────────────────────────────────────────────
Write-Step 3 "Starting Backend API (Spring Boot, port 8080)..."

$backendJar = Join-Path $Root "backend-api\target\backend-api-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $backendJar)) {
    Write-Info "JAR not found — building Backend API (this may take ~60s)..."
    & mvn.cmd clean package -DskipTests -f (Join-Path $Root "backend-api\pom.xml") | Out-Null
}

$backendProcess = Start-Process -FilePath "java" `
    -ArgumentList "-jar", $backendJar `
    -WorkingDirectory (Join-Path $Root "backend-api") `
    -WindowStyle Minimized `
    -PassThru
Write-Info "Backend API started (PID: $($backendProcess.Id))"

$backendOk = Wait-ForHttp "http://localhost:8080/actuator/health" "Backend API" 90
if (-not $backendOk) {
    Write-Fail "Backend API failed to start! Check logs in backend-api\backend_startup.log"
    exit 1
}

# ─── Step 4: Worker Service ────────────────────────────────────────────────────
Write-Step 4 "Starting Worker Service (Spring Boot, port 8081)..."

$workerJar = Join-Path $Root "worker-service\target\worker-service-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $workerJar)) {
    Write-Info "JAR not found — building Worker Service (this may take ~60s)..."
    & mvn.cmd clean package -DskipTests -f (Join-Path $Root "worker-service\pom.xml") | Out-Null
}

$workerProcess = Start-Process -FilePath "java" `
    -ArgumentList "-jar", $workerJar `
    -WorkingDirectory (Join-Path $Root "worker-service") `
    -WindowStyle Minimized `
    -PassThru
Write-Info "Worker Service started (PID: $($workerProcess.Id))"

# Worker doesn't need to be fully up before frontend
Start-Sleep -Seconds 5

# ─── Step 5: Frontend ──────────────────────────────────────────────────────────
Write-Step 5 "Starting Frontend dev server (Vite, port 5173)..."

$frontendDir = Join-Path $Root "devsecwatch-frontend"
$frontendProcess = Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c npm run dev" `
    -WorkingDirectory $frontendDir `
    -WindowStyle Minimized `
    -PassThru
Write-Info "Frontend started (PID: $($frontendProcess.Id))"

$frontendOk = Wait-ForHttp "http://localhost:5173" "Frontend" 30

# ─── Done ──────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  🛡️  DevSecWatch is running!" -ForegroundColor Yellow
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "  🌐 Frontend:        http://localhost:5173" -ForegroundColor White
Write-Host "  🔧 Backend API:     http://localhost:8080" -ForegroundColor White
Write-Host "  🤖 AI Service:      http://localhost:8000" -ForegroundColor White
Write-Host "  🔄 Worker:          http://localhost:8081" -ForegroundColor White
Write-Host "  🐇 RabbitMQ UI:     http://localhost:15672  (guest/guest)" -ForegroundColor White
Write-Host "  🐘 PostgreSQL:      localhost:5433          (postgres/postgres)" -ForegroundColor White
Write-Host "  📦 Redis:           localhost:6379" -ForegroundColor White
Write-Host ""
Write-Host "  Process IDs:" -ForegroundColor DarkGray
Write-Host "    Backend:  $($backendProcess.Id)  Worker: $($workerProcess.Id)  AI: $($aiProcess.Id)  Frontend: $($frontendProcess.Id)" -ForegroundColor DarkGray
Write-Host ""
Write-Host "  To stop all services:" -ForegroundColor DarkGray
Write-Host "    Stop-Process -Id $($backendProcess.Id),$($workerProcess.Id),$($aiProcess.Id),$($frontendProcess.Id) -Force" -ForegroundColor DarkGray
Write-Host "    docker-compose down" -ForegroundColor DarkGray
Write-Host ""
