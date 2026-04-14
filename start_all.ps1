$ErrorActionPreference = "Stop"

Write-Host "Checking if Docker is running..."
docker ps > $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Docker is not running. Please start Docker Desktop and run this script again." -ForegroundColor Red
    exit 1
}

Write-Host "1. Starting infrastructure (PostgreSQL, RabbitMQ, Redis)..." -ForegroundColor Green
docker-compose down
docker-compose up -d

Write-Host "Waiting a few seconds for databases to initialize..."
Start-Sleep -Seconds 10

Write-Host "2. Starting Backend API..." -ForegroundColor Green
Start-Process -FilePath "mvn.cmd" -ArgumentList "spring-boot:run" -WorkingDirectory ".\backend-api"

Write-Host "3. Starting Worker Service..." -ForegroundColor Green
Start-Process -FilePath "mvn.cmd" -ArgumentList "spring-boot:run" -WorkingDirectory ".\worker-service"

Write-Host "4. Starting AI Service..." -ForegroundColor Green
# Ensure we run using the python installed in the venv or globally
Start-Process -FilePath "cmd.exe" -ArgumentList "/c pip install -r requirements.txt && python -m uvicorn app.main:app --host 127.0.0.1 --port 8000" -WorkingDirectory ".\ai-service"

Write-Host "5. Starting Frontend..." -ForegroundColor Green
Start-Process -FilePath "cmd.exe" -ArgumentList "/c npm install && npm run dev" -WorkingDirectory ".\devsecwatch-frontend"

Write-Host "All services have been started in background processes!" -ForegroundColor Cyan
Write-Host "Frontend: http://localhost:5173"
Write-Host "Backend API: http://localhost:8080"
Write-Host "AI Service: http://localhost:8000"
Write-Host "To stop them, you will need to close the terminal or kill the java/node/python processes."
