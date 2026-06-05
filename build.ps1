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

# Cari Java local JDK dulu untuk diprioritaskan
$localJdkPattern = "$PSScriptRoot\.jdk\jdk-*"
$foundLocalJava = Get-ChildItem $localJdkPattern -ErrorAction SilentlyContinue | Select-Object -First 1
if ($foundLocalJava) {
    $env:JAVA_HOME = $foundLocalJava.FullName
    $env:PATH = "$($env:JAVA_HOME)\bin;$($env:PATH)"
    Write-Host "Menggunakan Local JDK: $($env:JAVA_HOME)" -ForegroundColor Green
} elseif (!(Get-Command "java" -ErrorAction SilentlyContinue) -and !($env:JAVA_HOME)) {
    $possibleJavaPaths = @(
        "C:\Program Files\Java\jdk-*",
        "C:\Program Files\Java\latest",
        "C:\Program Files\Java\jdk-21*"
    )
    foreach ($p in $possibleJavaPaths) {
        $foundJava = Get-ChildItem $p -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($foundJava) {
            $env:JAVA_HOME = $foundJava.FullName
            $env:PATH = "$($env:JAVA_HOME)\bin;$($env:PATH)"
            Write-Host "Java ditemukan di: $($env:JAVA_HOME)" -ForegroundColor Gray
            break
        }
    }
}

# Konfigurasi Maven Portabel
$mavenVersion = "3.9.6"
$mavenHome = Join-Path $PSScriptRoot ".maven"
$mvnExe = "mvn"

# Cari executable Maven
if (!(Get-Command $mvnExe -ErrorAction SilentlyContinue)) {
    Write-Host "Maven tidak ditemukan di PATH sistem." -ForegroundColor Yellow
    
    $localMvnDir = Join-Path $mavenHome "apache-maven-$mavenVersion"
    $mvnExe = Join-Path $localMvnDir "bin\mvn.cmd"
    
    if (!(Test-Path $mvnExe)) {
        Write-Host "Mengunduh Maven portabel (v$mavenVersion)..." -ForegroundColor Cyan
        
        if (!(Test-Path $mavenHome)) { 
            New-Item -ItemType Directory -Path $mavenHome -Force | Out-Null 
        }
        
        $zipFile = Join-Path $mavenHome "maven.zip"
        $url = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
        
        try {
            Write-Host "Menghubungi: $url" -ForegroundColor Gray
            Invoke-WebRequest -Uri $url -OutFile $zipFile
            Write-Host "Mengekstrak file..." -ForegroundColor Gray
            Expand-Archive -Path $zipFile -DestinationPath $mavenHome -Force
            Remove-Item $zipFile
            Write-Host "Maven portabel siap digunakan." -ForegroundColor Green
        } catch {
            Write-Host "Gagal mengunduh Maven otomatis. Pastikan Anda memiliki koneksi internet." -ForegroundColor Red
            Write-Error "Gagal mengunduh Maven: $($_.Exception.Message)"
            exit 1
        }
    } else {
        Write-Host "Menggunakan Maven portabel: $localMvnDir" -ForegroundColor Gray
    }
}

if ($MavenArgs.Count -eq 0) {
    Write-Host "--- Memulai Build (Default: clean package) ---" -ForegroundColor Cyan
    & $mvnExe clean package
} else {
    # Pre-process argumen untuk mempermudah (contoh: -psnapshot -> -Psnapshot)
    for ($i = 0; $i -lt $MavenArgs.Count; $i++) {
        if ($MavenArgs[$i] -eq "-psnapshot") {
            $MavenArgs[$i] = "-Psnapshot"
        }
    }

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
