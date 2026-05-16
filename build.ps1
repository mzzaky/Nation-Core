<#
.SYNOPSIS
    Script untuk mem-build projek Nation Core.
.DESCRIPTION
    Script ini menjalankan Maven untuk melakukan packaging atau perintah Maven lainnya.
    Jika tidak ada argumen, akan menjalankan 'clean package'.
.EXAMPLE
    .\build.ps1
    Menjalankan 'mvn clean package'
.EXAMPLE
    .\build.ps1 clean install -DskipTests
    Menjalankan 'mvn clean install -DskipTests'
#>

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$ErrorActionPreference = "Stop"

# Cari executable Maven
$mvnExe = "mvn"
if (!(Get-Command $mvnExe -ErrorAction SilentlyContinue)) {
    Write-Host "PERINGATAN: 'mvn' tidak ditemukan di PATH." -ForegroundColor Yellow
    
    # Lokasi umum jika tidak ada di PATH
    $possiblePaths = @(
        "C:\Program Files\apache-maven-*\bin\mvn.cmd",
        "C:\maven\bin\mvn.cmd"
    )
    
    $foundMvn = $null
    foreach ($p in $possiblePaths) {
        $foundMvn = Get-ChildItem $p -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($foundMvn) { break }
    }
    
    if ($foundMvn) {
        $mvnExe = $foundMvn.FullName
        Write-Host "Ditemukan Maven di: $mvnExe" -ForegroundColor Gray
    } else {
        Write-Error "Maven tidak ditemukan. Silakan instal Maven atau tambahkan ke PATH sistem Anda agar command 'mvn' bisa digunakan."
        exit 1
    }
}

if ($MavenArgs.Count -eq 0) {
    Write-Host "--- Memulai Build (Default: clean package) ---" -ForegroundColor Cyan
    & $mvnExe clean package
} else {
    # Periksa apakah ada goal dalam argumen (argumen yang tidak dimulai dengan '-')
    $hasGoal = $false
    foreach ($arg in $MavenArgs) {
        if ($arg -notlike "-*") {
            $hasGoal = $true
            break
        }
    }

    if (-not $hasGoal) {
        Write-Host "--- Menambahkan goal default 'clean package' ke argumen Anda ---" -ForegroundColor Gray
        $MavenArgs = @("clean", "package") + $MavenArgs
    }

    Write-Host "--- Menjalankan Maven dengan Argumen: $MavenArgs ---" -ForegroundColor Cyan
    & $mvnExe @MavenArgs
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[SUKSES] Build selesai dengan sukses!" -ForegroundColor Green
} else {
    Write-Host "`n[ERROR] Build gagal dengan exit code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}
