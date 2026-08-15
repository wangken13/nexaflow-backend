param(
    [string]$EnvFile = (Join-Path $PSScriptRoot '..\.env.prod'),
    [switch]$SkipPackage,
    [switch]$RunTests
)

$ErrorActionPreference = 'Stop'
$infraDir = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$backendDir = (Resolve-Path (Join-Path $infraDir '..')).Path
$frontendDir = Join-Path (Split-Path $backendDir -Parent) 'tradeflow-ai-frontend'
$composeFile = Join-Path $infraDir 'docker-compose.prod.yml'
$mavenSettings = Join-Path $infraDir 'maven-settings.xml'
$mavenCacheVolume = 'nexaflow-maven-cache'
$EnvFile = (Resolve-Path $EnvFile).Path
$env:IMAGE_TAG = (& git -C $backendDir rev-parse --short HEAD).Trim()

$infrastructureServices = @('mysql', 'redis', 'rabbitmq', 'minio')
$businessServices = @(
    'auth-service', 'tenant-service', 'customer-service', 'product-service',
    'inquiry-service', 'ai-service', 'quotation-service', 'order-service',
    'task-service', 'notification-service', 'file-service', 'aigc-service'
)
$bootModules = @(
    'gateway-service', 'auth-service/biz', 'tenant-service/biz', 'customer-service/biz',
    'product-service/biz', 'inquiry-service/biz', 'ai-service/biz', 'quotation-service/biz',
    'order-service/biz', 'task-service/biz', 'notification-service/biz', 'file-service/biz',
    'aigc-service'
)

if (-not (Test-Path $frontendDir)) {
    throw "Missing frontend repository: $frontendDir"
}
if (-not (Test-Path $mavenSettings)) {
    throw "Missing Maven settings: $mavenSettings"
}

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$ComposeArguments)
    & docker compose --env-file $EnvFile -f $composeFile @ComposeArguments
    if ($LASTEXITCODE -ne 0) { throw "docker compose failed: $($ComposeArguments -join ' ')" }
}

function Get-EnvValue([string]$Name) {
    $entry = Get-Content $EnvFile | Where-Object { $_ -match "^$Name=" } | Select-Object -Last 1
    if (-not $entry) { return '' }
    return ($entry -split '=', 2)[1].Trim()
}

function Wait-Healthy([string]$Service, [int]$MaxAttempts = 180) {
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $containerId = (& docker compose --env-file $EnvFile -f $composeFile ps -q $Service).Trim()
        if ($containerId) {
            $status = (& docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $containerId).Trim()
            $containerState = (& docker inspect --format '{{.State.Status}}' $containerId).Trim()
            if ($status -eq 'healthy') {
                Write-Output "[ready] $Service"
                return
            }
            if ($containerState -in @('exited', 'dead') -or ($containerState -eq 'restarting' -and $attempt -ge 3)) {
                Invoke-Compose -ComposeArguments @('logs', '--tail=160', $Service)
                throw "$Service entered an unrecoverable state during startup: $containerState"
            }
        }
        if ($attempt % 12 -eq 0) { Write-Output "[wait] $Service ($($attempt * 5)s)" }
        Start-Sleep -Seconds 5
    }
    Invoke-Compose -ComposeArguments @('logs', '--tail=160', $Service)
    throw "$Service did not become healthy within $($MaxAttempts * 5) seconds"
}

function Initialize-NacosAdmin {
    $username = Get-EnvValue 'NACOS_USERNAME'
    $password = Get-EnvValue 'NACOS_PASSWORD'
    if ($username -notmatch '^[A-Za-z0-9_.-]+$' -or $password.Length -lt 12) {
        throw 'Configure a valid NACOS_USERNAME and a Nacos password of at least 12 characters.'
    }
    $hashLine = (& docker run --rm httpd:2.4-alpine htpasswd -bnBC 10 '' $password).Trim()
    $hash = ($hashLine -split ':', 2)[1]
    $sql = @"
INSERT INTO users (username, password, enabled) VALUES ('$username', '$hash', TRUE) ON DUPLICATE KEY UPDATE password = VALUES(password), enabled = TRUE;
INSERT INTO roles (username, role) VALUES ('$username', 'ROLE_ADMIN') ON DUPLICATE KEY UPDATE role = VALUES(role);
"@
    $sql | & docker compose --env-file $EnvFile -f $composeFile exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot nacos_config'
    if ($LASTEXITCODE -ne 0) { throw 'Failed to initialize the Nacos administrator.' }
}

function Initialize-RabbitMqUser {
    $username = Get-EnvValue 'RABBITMQ_USER'
    $password = Get-EnvValue 'RABBITMQ_PASSWORD'
    if ($username -notmatch '^[A-Za-z0-9_.-]+$' -or $password.Length -lt 12) {
        throw 'Configure a valid RABBITMQ_USER and a RabbitMQ password of at least 12 characters.'
    }
    $users = & docker compose --env-file $EnvFile -f $composeFile exec -T rabbitmq rabbitmqctl list_users -q
    if ($users -match "(?m)^$([regex]::Escape($username))\s") {
        & docker compose --env-file $EnvFile -f $composeFile exec -T rabbitmq rabbitmqctl change_password $username $password
    } else {
        & docker compose --env-file $EnvFile -f $composeFile exec -T rabbitmq rabbitmqctl add_user $username $password
    }
    if ($LASTEXITCODE -ne 0) { throw 'Failed to initialize the RabbitMQ user.' }
    & docker compose --env-file $EnvFile -f $composeFile exec -T rabbitmq rabbitmqctl set_permissions -p / $username '.*' '.*' '.*'
    if ($LASTEXITCODE -ne 0) { throw 'Failed to grant RabbitMQ permissions.' }
}

function Repair-KnownFailedMigrations {
    $repairFile = Join-Path $infraDir 'mysql\repair\V018__complete_operation_audit_repair.sql'
    $query = "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '018' AND success = 0"
    $failedV018 = (& docker compose --env-file $EnvFile -f $composeFile exec -T mysql `
        sh -c "MYSQL_PWD=`"`$MYSQL_PASSWORD`" mysql -N -B -u`"`$MYSQL_USER`" trade_ai -e `"$query`"" 2>$null).Trim()
    if ($failedV018 -eq '1') {
        Write-Output '[repair] complete failed Flyway migration V018'
        Get-Content -Raw $repairFile | & docker compose --env-file $EnvFile -f $composeFile exec -T mysql `
            sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" trade_ai'
        if ($LASTEXITCODE -ne 0) { throw 'Failed to repair Flyway migration V018.' }
    }
}

Write-Output '[1/9] Validate production configuration'
Invoke-Compose -ComposeArguments @('config', '--quiet')

Write-Output '[2/9] Start infrastructure'
Invoke-Compose -ComposeArguments (@('up', '-d') + $infrastructureServices)
foreach ($service in $infrastructureServices) { Wait-Healthy $service 120 }
Initialize-RabbitMqUser

Write-Output '[3/9] Initialize and start Nacos'
Initialize-NacosAdmin
Repair-KnownFailedMigrations
Invoke-Compose -ComposeArguments @('up', '-d', 'nacos')
Wait-Healthy 'nacos' 180

Write-Output '[4/9] Package backend'
if (-not $SkipPackage) {
    $mavenArgs = if ($RunTests) { @('-B', 'clean', 'verify') } else { @('-B', '-Dmaven.test.skip=true', 'clean', 'package') }
    & docker run --rm `
        -v "$mavenCacheVolume`:/root/.m2" `
        -v "$mavenSettings`:/tmp/maven-settings.xml:ro" `
        -v "$backendDir`:/workspace" `
        -w /workspace `
        maven:3.9.11-eclipse-temurin-21 `
        mvn -s /tmp/maven-settings.xml @mavenArgs
    if ($LASTEXITCODE -ne 0) { throw 'Backend Maven package failed.' }
}

Write-Output '[5/9] Validate Spring Boot artifacts'
foreach ($module in $bootModules) {
    $jars = @(Get-ChildItem (Join-Path $backendDir "$module\target\*.jar") -File)
    if ($jars.Count -ne 1) { throw "$module must contain exactly one deployable JAR" }
    $entries = & docker run --rm -v "$($jars[0].DirectoryName):/jars:ro" maven:3.9.11-eclipse-temurin-21 jar tf "/jars/$($jars[0].Name)"
    if ($LASTEXITCODE -ne 0 -or -not ($entries -match '^BOOT-INF/')) { throw "$module is not a Spring Boot executable JAR" }
    Write-Output "[jar] $($jars[0].Name)"
}

Write-Output "[6/9] Build release images: $env:IMAGE_TAG"
Invoke-Compose -ComposeArguments @('build')

Write-Output '[7/9] Start gateway'
Invoke-Compose -ComposeArguments @('up', '-d', '--force-recreate', '--no-deps', 'gateway-service')
Wait-Healthy 'gateway-service' 180

Write-Output '[8/9] Start business services'
Invoke-Compose -ComposeArguments (@('up', '-d', '--force-recreate', '--no-deps') + $businessServices)
foreach ($service in $businessServices) { Wait-Healthy $service 180 }

Write-Output '[9/9] Start frontend and verify routing'
Invoke-Compose -ComposeArguments @('up', '-d', '--force-recreate', '--no-deps', 'frontend')
Wait-Healthy 'frontend' 60
Invoke-Compose -ComposeArguments @('exec', '-T', 'frontend', 'wget', '-q', '-O', '/dev/null', '--timeout=15', 'http://gateway-service:18080/readyz')
Invoke-Compose -ComposeArguments @('exec', '-T', 'frontend', 'wget', '-q', '-O', '/dev/null', '--timeout=30', 'http://gateway-service:18080/api/auth/captcha')

$httpPort = Get-EnvValue 'HTTP_PORT'
if (-not $httpPort) { $httpPort = '80' }
Invoke-RestMethod -Uri "http://127.0.0.1:$httpPort/readyz" -TimeoutSec 15 | Out-Null
Invoke-Compose -ComposeArguments @('ps')
Write-Output "Release image tag: $env:IMAGE_TAG"
Write-Output 'Deployment completed successfully.'
