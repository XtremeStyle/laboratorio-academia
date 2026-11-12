$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir
& .\mvnw.cmd spring-boot:run
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
