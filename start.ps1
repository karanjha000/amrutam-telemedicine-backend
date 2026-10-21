Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker is required but was not found in PATH."
    exit 1
}

try {
    docker compose version | Out-Null
} catch {
    Write-Error "Docker Compose is required but is not available."
    exit 1
}

Write-Host "Starting Amrutam stack with Docker Compose..."
docker compose up --build --remove-orphans -d

Write-Host "Amrutam stack started."
Write-Host "Frontend: http://localhost"
Write-Host "API: http://localhost:8080"
Write-Host "Swagger: http://localhost:8080/swagger-ui.html"
