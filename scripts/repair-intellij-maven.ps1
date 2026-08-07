$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$env:JAVA_HOME = 'D:\work\jdk-21.0.11+10'
$env:MAVEN_HOME = 'D:\work\apache-maven-3.9.16'
$env:M2_HOME = $env:MAVEN_HOME
$env:Path = "D:\work\PowerShell\7.6.4;$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

mvn -q clean install -DskipTests
mvn -q -pl customer-service/biz -am compile

Write-Host ''
Write-Host 'Maven modules are installed and customer-service/biz compiles successfully.'
Write-Host 'In IntelliJ: Maven panel -> Reload All Maven Projects, then Build -> Rebuild Project.'
