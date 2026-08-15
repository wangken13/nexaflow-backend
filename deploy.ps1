param(
    [string]$EnvFile = (Join-Path $PSScriptRoot 'infra\.env.prod'),
    [switch]$SkipPackage,
    [switch]$RunTests
)

& (Join-Path $PSScriptRoot 'infra\scripts\deploy.ps1') `
    -EnvFile $EnvFile `
    -SkipPackage:$SkipPackage `
    -RunTests:$RunTests
