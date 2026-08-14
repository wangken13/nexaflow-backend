param(
    [string]$EnvFile = (Join-Path $PSScriptRoot '..\.env.prod'),
    [switch]$SkipPackage
)

$ErrorActionPreference = 'Stop'
$infraDir = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$backendDir = (Resolve-Path (Join-Path $infraDir '..')).Path
$frontendDir = Join-Path (Split-Path $backendDir -Parent) 'tradeflow-ai-frontend'
$composeFile = Join-Path $infraDir 'docker-compose.prod.yml'
$EnvFile = (Resolve-Path $EnvFile).Path

if (-not (Test-Path $frontendDir)) {
    throw "Missing frontend repository: $frontendDir"
}

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$ComposeArguments)
    & docker compose --env-file $EnvFile -f $composeFile @ComposeArguments
    if ($LASTEXITCODE -ne 0) { throw "docker compose failed" }
}

function Get-EnvValue([string]$Name) {
    $entry = Get-Content $EnvFile | Where-Object { $_ -match "^$Name=" } | Select-Object -Last 1
    if (-not $entry) { return '' }
    return ($entry -split '=', 2)[1]
}

function Wait-Healthy([string]$Service) {
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        $containerId = (& docker compose --env-file $EnvFile -f $composeFile ps -q $Service).Trim()
        if ($containerId) {
            $status = (& docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}' $containerId).Trim()
            if ($status -eq 'healthy') { return }
        }
        Start-Sleep -Seconds 2
    }
    throw "$Service did not become healthy"
}

$nacosUsername = Get-EnvValue 'NACOS_USERNAME'
$nacosPassword = Get-EnvValue 'NACOS_PASSWORD'
if ($nacosUsername -notmatch '^[A-Za-z0-9_.-]+$' -or $nacosPassword.Length -lt 12) {
    throw 'Configure a valid NACOS_USERNAME and a Nacos password of at least 12 characters in .env.prod.'
}

Invoke-Compose -ComposeArguments @('up', '-d', 'mysql', 'redis', 'rabbitmq', 'minio')
Wait-Healthy mysql

$hashLine = (& docker run --rm httpd:2.4-alpine htpasswd -bnBC 10 '' $nacosPassword).Trim()
$nacosHash = ($hashLine -split ':', 2)[1]
$sql = @"
INSERT INTO users (username, password, enabled) VALUES ('$nacosUsername', '$nacosHash', TRUE) ON DUPLICATE KEY UPDATE password = VALUES(password), enabled = TRUE;
INSERT INTO roles (username, role) VALUES ('$nacosUsername', 'ROLE_ADMIN') ON DUPLICATE KEY UPDATE role = VALUES(role);
"@
$sql | & docker compose --env-file $EnvFile -f $composeFile exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot nacos_config'
if ($LASTEXITCODE -ne 0) { throw 'Failed to initialize the Nacos administrator.' }

Invoke-Compose -ComposeArguments @('up', '-d', 'nacos')
Wait-Healthy nacos

if (-not $SkipPackage) {
    & docker run --rm -v "$backendDir`:/workspace" -w /workspace maven:3.9.11-eclipse-temurin-21 mvn -B -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Backend Maven package failed.' }
}

Invoke-Compose -ComposeArguments @('up', '-d', '--build', '--remove-orphans')
Wait-Healthy frontend
$httpPort = Get-EnvValue 'HTTP_PORT'
if (-not $httpPort) { $httpPort = '80' }
$ready = $false
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    try {
        Invoke-RestMethod -Uri "http://127.0.0.1:$httpPort/readyz" -TimeoutSec 5 | Out-Null
        $ready = $true
        break
    } catch {
        Start-Sleep -Seconds 2
    }
}
if (-not $ready) {
    Invoke-Compose -ComposeArguments @('logs', '--tail=120', 'gateway-service')
    throw 'The public entry is running, but the gateway readiness probe failed.'
}
Invoke-Compose -ComposeArguments @('ps')
Write-Output "Deployment completed. Readiness probe: http://127.0.0.1:$httpPort/readyz"
