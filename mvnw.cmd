@echo off
setlocal
set "MAVEN_VERSION=3.9.11"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd"
set "ARCHIVE=apache-maven-%MAVEN_VERSION%-bin.zip"
set "BASE_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries"

if exist "%MAVEN_BIN%" goto run_maven

where powershell.exe >nul 2>nul
if errorlevel 1 (
  echo PowerShell is required to download Maven.
  exit /b 1
)

echo Downloading Apache Maven %MAVEN_VERSION%...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$dir='%MAVEN_HOME%'; $archive=Join-Path $env:TEMP '%ARCHIVE%';" ^
  "$checksumFile=$archive+'.sha512';" ^
  "$base='%BASE_URL%';" ^
  "New-Item -ItemType Directory -Force -Path $dir | Out-Null;" ^
  "Invoke-WebRequest -Uri ($base+'/%ARCHIVE%') -OutFile $archive;" ^
  "Invoke-WebRequest -Uri ($base+'/%ARCHIVE%.sha512') -OutFile $checksumFile;" ^
  "$expected=((Get-Content $checksumFile -Raw).Trim() -split '\s+')[0].ToLower();" ^
  "$actual=(Get-FileHash $archive -Algorithm SHA512).Hash.ToLower();" ^
  "if ($expected -ne $actual) { throw 'Maven SHA-512 verification failed.' };" ^
  "Expand-Archive -Path $archive -DestinationPath $env:TEMP -Force;" ^
  "$extracted=Join-Path $env:TEMP 'apache-maven-%MAVEN_VERSION%';" ^
  "Get-ChildItem $extracted -Force | Move-Item -Destination $dir -Force;" ^
  "Remove-Item $extracted -Recurse -Force; Remove-Item $archive,$checksumFile -Force;"
if errorlevel 1 exit /b 1

:run_maven
call "%MAVEN_BIN%" %*
exit /b %ERRORLEVEL%
