$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir
& .\mvnw.cmd clean test
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
