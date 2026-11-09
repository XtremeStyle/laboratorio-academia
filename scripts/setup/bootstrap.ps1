$ErrorActionPreference = "Stop"

$ProjectDir = Join-Path $HOME "Projects\ACADEMY\Laboratorio"
$RepoName = "laboratorio-academia"

Write-Host "`n[Laboratorio] Preparación inicial para Laboratorio"
Write-Host "Carpeta definitiva: $ProjectDir"
$answer = Read-Host "¿Continuar? [s/N]"
if ($answer -notin @("s", "S", "si", "SI", "Si")) { exit 0 }

function Has-Command($name) {
    return [bool](Get-Command $name -ErrorAction SilentlyContinue)
}

if (-not (Has-Command "winget")) {
    throw "No se detectó winget. Instala App Installer desde Microsoft Store y vuelve a ejecutar."
}

$packages = @()
if (-not (Has-Command "git")) { $packages += "Git.Git" }
if (-not (Has-Command "java")) { $packages += "Microsoft.OpenJDK.21" }
if (-not (Has-Command "gh")) { $packages += "GitHub.cli" }

if ($packages.Count -gt 0) {
    Write-Host "Se instalarán con winget: $($packages -join ', ')"
    $install = Read-Host "¿Autorizar instalación? [s/N]"
    if ($install -in @("s", "S", "si", "SI", "Si")) {
        foreach ($package in $packages) {
            winget install --id $package --exact --accept-package-agreements --accept-source-agreements
        }
    } else {
        throw "Instala las herramientas faltantes y vuelve a ejecutar."
    }
}

if (-not (Has-Command "git") -or -not (Has-Command "java") -or -not (Has-Command "gh")) {
    throw "No se pudieron verificar Git, Java y GitHub CLI. Cierra y abre PowerShell y vuelve a ejecutar."
}

$javaVersion = (& java -version 2>&1 | Select-Object -First 1)
Write-Host "Java detectado: $javaVersion"
if ($javaVersion -notmatch '"21([.+-]|")') {
    throw "Se requiere Java 21. Detectado: $javaVersion"
}

if (Test-Path $ProjectDir) {
    if ((Get-ChildItem -Force $ProjectDir | Measure-Object).Count -gt 0) {
        throw "La carpeta ya existe y no está vacía: $ProjectDir. No se sobrescribirá."
    }
} else {
    New-Item -ItemType Directory -Force -Path $ProjectDir | Out-Null
}

$PackageRoot = Split-Path -Parent $PSScriptRoot
Copy-Item (Join-Path $PackageRoot "README.md") $ProjectDir
Copy-Item (Join-Path $PackageRoot ".gitignore") $ProjectDir
New-Item -ItemType Directory -Force -Path (Join-Path $ProjectDir "docs"), (Join-Path $ProjectDir "src"), (Join-Path $ProjectDir "scripts\setup") | Out-Null
Copy-Item (Join-Path $PSScriptRoot "bootstrap.sh") (Join-Path $ProjectDir "scripts\setup\")
Copy-Item (Join-Path $PSScriptRoot "bootstrap.ps1") (Join-Path $ProjectDir "scripts\setup\")

Push-Location $ProjectDir
try {
    git init -b main
    git add README.md .gitignore docs src scripts

    if (-not (git config user.name)) {
        $gitName = Read-Host "Nombre para los commits de este repositorio"
        if ([string]::IsNullOrWhiteSpace($gitName)) { throw "El nombre no puede estar vacío." }
        git config user.name $gitName
    }
    if (-not (git config user.email)) {
        $gitEmail = Read-Host "Correo para los commits (puedes usar el correo privado de GitHub)"
        if ([string]::IsNullOrWhiteSpace($gitEmail)) { throw "El correo no puede estar vacío." }
        git config user.email $gitEmail
    }

    git commit -m "chore: initialize Laboratorio repository"

    $null = gh auth status 2>$null
    if ($LASTEXITCODE -eq 0) {
        $inputRepo = Read-Host "Nombre del repositorio público de GitHub [$RepoName]"
        if (-not [string]::IsNullOrWhiteSpace($inputRepo)) { $RepoName = $inputRepo }
        Write-Host "Se creará y publicará $RepoName como repositorio PÚBLICO."
        $publicAnswer = Read-Host "¿Confirmas que el contenido inicial será público? [s/N]"
        if ($publicAnswer -in @("s", "S", "si", "SI", "Si")) {
            gh repo create $RepoName --public --source . --remote origin --push
            if ($LASTEXITCODE -ne 0) { throw "No se pudo crear/publicar el repositorio. Revisa el mensaje de GitHub CLI." }
        } else {
            Write-Host "Repositorio local creado. No se publicó en GitHub."
        }
    } else {
        Write-Host "GitHub CLI aún no está autenticado. Ejecuta: gh auth login"
        Write-Host "Después, en $ProjectDir ejecuta:"
        Write-Host "gh repo create $RepoName --public --source . --remote origin --push"
    }
}
finally {
    Pop-Location
}

Write-Host "`n[Laboratorio] Preparación inicial terminada. Carpeta: $ProjectDir"
