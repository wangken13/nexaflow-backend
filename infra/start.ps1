$ErrorActionPreference = 'Stop'

$infraDir = $PSScriptRoot
$envFile = if ($env:ENV_FILE) {
    $env:ENV_FILE
} elseif (Test-Path (Join-Path $infraDir '.env.prod')) {
    Join-Path $infraDir '.env.prod'
} else {
    Join-Path $infraDir '.env'
}

& (Join-Path $infraDir 'scripts\deploy.ps1') -EnvFile $envFile @args
