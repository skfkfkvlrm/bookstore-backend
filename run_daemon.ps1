# Book System v2 Full Daemon Orchestrator
$backendDir = "d:\samuel\java\day_by_spring_sm_v2"
$frontendDir = "d:\samuel\java\day_by_spring_sm_ui_v2"
$logsDir = Join-Path $backendDir "logs"

if (-not (Test-Path $logsDir)) {
    New-Item -ItemType Directory -Path $logsDir -Force | Out-Null
}

Write-Host "=== [0/3] Checking Docker MySQL Container ===" -ForegroundColor Cyan
$dockerStatus = docker inspect -f '{{.State.Running}}' day_by_spring_sm_v2-mysql-db-1 2>$null
if ($dockerStatus -ne "true") {
    Write-Host "Starting day_by_spring_sm_v2-mysql-db-1 container..." -ForegroundColor Yellow
    docker start day_by_spring_sm_v2-mysql-db-1 | Out-Null
    Start-Sleep -Seconds 3
}
Write-Host "MySQL v2 (Port 3308) Ready!" -ForegroundColor Green

Write-Host "`n=== [1/3] Checking and cleaning target ports (8089, 5175) ===" -ForegroundColor Cyan
$targetPorts = @(8089, 5175)
foreach ($p in $targetPorts) {
    $conns = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
    if ($conns) {
        foreach ($c in $conns) {
            Write-Host "Killing existing PID $($c.OwningProcess) on port $p..." -ForegroundColor Yellow
            Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue
        }
    }
}

$runningProcs = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()

Write-Host "`n=== [2/3] Starting Backend (Port: 8089) ===" -ForegroundColor Cyan
$jarPath = Join-Path $backendDir "target\spring-0.0.1-SNAPSHOT.jar"
$stdoutBackend = Join-Path $logsDir "backend.log"
$stderrBackend = Join-Path $logsDir "backend-err.log"

$bp = Start-Process -FilePath "java" `
    -ArgumentList "-Xms256m", "-Xmx512m", "-Dserver.port=8089", "-jar", $jarPath `
    -RedirectStandardOutput $stdoutBackend `
    -RedirectStandardError $stderrBackend `
    -WorkingDirectory $backendDir `
    -WindowStyle Hidden `
    -PassThru

$runningProcs.Add($bp)

$backendUp = $false
$attempt = 0
while ($attempt -lt 30) {
    Start-Sleep -Milliseconds 500
    $conn = Get-NetTCPConnection -LocalPort 8089 -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        Write-Host "  -> Backend is UP on port 8089!" -ForegroundColor Green
        $backendUp = $true
        break
    }
    $attempt++
}

if (-not $backendUp) {
    Write-Host "  [WARN] Backend port 8089 not detected yet, check logs at $stderrBackend" -ForegroundColor Yellow
}

Write-Host "`n=== [3/3] Starting Frontend (Port: 5175) ===" -ForegroundColor Cyan
$stdoutFrontend = Join-Path $logsDir "frontend.log"
$stderrFrontend = Join-Path $logsDir "frontend-err.log"

$fp = Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c npx vite --port 5175" `
    -RedirectStandardOutput $stdoutFrontend `
    -RedirectStandardError $stderrFrontend `
    -WorkingDirectory $frontendDir `
    -WindowStyle Hidden `
    -PassThru

$runningProcs.Add($fp)

$frontendUp = $false
$attempt = 0
while ($attempt -lt 20) {
    Start-Sleep -Milliseconds 500
    $conn = Get-NetTCPConnection -LocalPort 5175 -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        Write-Host "  -> Frontend is UP on port 5175!" -ForegroundColor Green
        $frontendUp = $true
        break
    }
    $attempt++
}

if (-not $frontendUp) {
    Write-Host "  [WARN] Frontend port 5175 not detected yet, check logs at $stderrFrontend" -ForegroundColor Yellow
}

Write-Host "`n=======================================================" -ForegroundColor Green
Write-Host " Book System v2 Running in Daemon Mode!" -ForegroundColor Green
Write-Host " - Frontend UI:    http://localhost:5175" -ForegroundColor White
Write-Host " - Backend API:    http://localhost:8089" -ForegroundColor White
Write-Host " - Swagger UI:     http://localhost:8089/swagger-ui.html" -ForegroundColor White
Write-Host " - OpenAPI Specs:  http://localhost:8089/v3/api-docs" -ForegroundColor White
Write-Host "=======================================================" -ForegroundColor Green

# Keep script alive indefinitely to prevent process tree cleanup
while ($true) {
    Start-Sleep -Seconds 30
}
