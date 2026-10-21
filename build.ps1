param(
    [switch]$SkipKeystore
)

$ErrorActionPreference = "Stop"

$Src = Split-Path -Parent $MyInvocation.MyCommand.Path
# Les outils natifs (aapt2, javac, d8) ne gèrent pas les chemins non-ASCII (utilisateur "Louaï") :
# on travaille dans un dossier ASCII et on pointe le SDK via une jonction.
$Root = "C:\chapitres-build"
$SdkReal = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$Sdk = "C:\chapitres-sdk"
if (-not (Test-Path $Sdk)) {
    cmd /c "mklink /J C:\chapitres-sdk `"$SdkReal`"" | Out-Null
}
if (-not (Test-Path $Sdk)) { throw "jonction SDK impossible" }
$Bt = Join-Path $Sdk "build-tools\34.0.0"
$AndroidJar = Join-Path $Sdk "platforms\android-34\android.jar"

# JDK 8 pour javac/jar (compatibilite source 1.8), JDK 11+ pour d8/apksigner.
$Jdk = "C:\Program Files\AdoptOpenJDK\jdk-8.0.265.01-hotspot\bin"
$JdkNew = "C:\Program Files\Microsoft\jdk-25.0.0.36-hotspot"
if (-not (Test-Path $Jdk)) { throw "JDK 8 introuvable ($Jdk)" }
if (-not (Test-Path $JdkNew)) { throw "JDK 11+ introuvable ($JdkNew)" }

# Wrapper : execute un outil natif, capture stderr dans un log et ne leve
# d'exception que si le code de sortie est non nul. Sans ca, les simples
# "note:" de javac (emises sur stderr) font echouer le build.
function Invoke-Native {
    param(
        [string]$Tool,
        [string[]]$Arguments,
        [string]$LogName
    )
    $log = Join-Path $Root "$LogName.log"
    # Les arguments contenant des espaces doivent etre quotes, sinon
    # Start-Process les decoupe (ex: -dname "CN=..., OU=...").
    $quoted = @($Arguments | ForEach-Object {
        if ($_ -match "\s") { '"' + $_ + '"' } else { $_ }
    })
    $p = Start-Process -FilePath $Tool -ArgumentList $quoted -NoNewWindow -Wait -PassThru `
        -RedirectStandardOutput "$log.out" -RedirectStandardError $log
    if (Test-Path "$log.out") { Get-Content "$log.out" | ForEach-Object { Write-Host $_ } }
    if ($p.ExitCode -ne 0) {
        if (Test-Path $log) { Get-Content $log | ForEach-Object { Write-Host $_ -ForegroundColor Red } }
        throw "$Tool a echoue (code $($p.ExitCode))"
    }
    if ((Test-Path $log) -and (Get-Item $log).Length -gt 0) {
        Get-Content $log | Where-Object { $_ -notmatch "^\s*Note:" } | ForEach-Object { Write-Host $_ }
    }
}

if (Test-Path $Root) { Remove-Item -Recurse -Force $Root }
New-Item -ItemType Directory -Force -Path $Root | Out-Null
Copy-Item "$Src\res" "$Root\res" -Recurse -Force
Copy-Item "$Src\src" "$Root\src" -Recurse -Force
Copy-Item "$Src\AndroidManifest.xml" "$Root\AndroidManifest.xml" -Force

$Build = Join-Path $Root "build"
New-Item -ItemType Directory -Force -Path $Build | Out-Null
New-Item -ItemType Directory -Force -Path "$Build\gen" | Out-Null
New-Item -ItemType Directory -Force -Path "$Build\classes" | Out-Null
New-Item -ItemType Directory -Force -Path "$Build\dex" | Out-Null

Write-Host "[1/7] aapt2 compile" -ForegroundColor Cyan
Invoke-Native -Tool "$Bt\aapt2.exe" -LogName "aapt2-compile" `
    -Arguments @("compile", "--dir", "$Root\res", "-o", "$Build\res.zip")

Write-Host "[2/7] aapt2 link" -ForegroundColor Cyan
Invoke-Native -Tool "$Bt\aapt2.exe" -LogName "aapt2-link" -Arguments @(
    "link",
    "-o", "$Build\base.apk",
    "-I", $AndroidJar,
    "--manifest", "$Root\AndroidManifest.xml",
    "--min-sdk-version", "24",
    "--target-sdk-version", "28",
    "--version-code", "1",
    "--version-name", "1.0",
    "--java", "$Build\gen",
    "$Build\res.zip")

Write-Host "[3/7] javac" -ForegroundColor Cyan
$sources = @(Get-ChildItem -Path "$Root\src" -Filter *.java -Recurse | ForEach-Object { $_.FullName })
$genSources = @(Get-ChildItem -Path "$Build\gen" -Filter *.java -Recurse | ForEach-Object { $_.FullName })
Invoke-Native -Tool "$Jdk\javac.exe" -LogName "javac" -Arguments (@(
    "-source", "1.8", "-target", "1.8", "-encoding", "UTF-8",
    "-bootclasspath", $AndroidJar,
    "-classpath", $AndroidJar,
    "-d", "$Build\classes"
) + $sources + $genSources)

Write-Host "[4/7] jar + d8" -ForegroundColor Cyan
Push-Location "$Build\classes"
Invoke-Native -Tool "$Jdk\jar.exe" -LogName "jar" -Arguments @("cf", "$Build\app.jar", ".")
Pop-Location
# d8 et apksigner (build-tools 34) exigent Java 11+
$env:JAVA_HOME = $JdkNew
$env:PATH = "$JdkNew\bin;$env:PATH"
Invoke-Native -Tool "$Bt\d8.bat" -LogName "d8" -Arguments @(
    "--lib", $AndroidJar, "--min-api", "24", "--output", "$Build\dex", "$Build\app.jar")

Write-Host "[5/7] assemblage de l'APK" -ForegroundColor Cyan
Copy-Item "$Build\base.apk" "$Build\unsigned.apk" -Force
Push-Location "$Build\dex"
Invoke-Native -Tool "$Bt\aapt.exe" -LogName "aapt-add" -Arguments @("add", "-f", "$Build\unsigned.apk", "classes.dex")
Pop-Location

Write-Host "[6/7] zipalign" -ForegroundColor Cyan
Invoke-Native -Tool "$Bt\zipalign.exe" -LogName "zipalign" -Arguments @(
    "-f", "-p", "4", "$Build\unsigned.apk", "$Build\aligned.apk")

Write-Host "[7/7] signature" -ForegroundColor Cyan
$Keystore = Join-Path $Src "debug.keystore"
if (-not (Test-Path $Keystore)) {
    Invoke-Native -Tool "$Jdk\keytool.exe" -LogName "keytool" -Arguments @(
        "-genkeypair", "-v", "-keystore", $Keystore, "-storepass", "android",
        "-alias", "androiddebugkey", "-keypass", "android",
        "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
        "-dname", "CN=Chapitres, OU=Dev, O=Chapitres, L=Paris, S=IDF, C=FR")
}
# MIUI 12 refuse d'installer les APK signés uniquement en v2/v3 : la signature
# v1 (JAR, META-INF/*.SF) est obligatoire. On force donc les trois schémas.
Invoke-Native -Tool "$Bt\apksigner.bat" -LogName "apksigner" -Arguments @(
    "sign", "--ks", $Keystore, "--ks-pass", "pass:android", "--key-pass", "pass:android",
    "--ks-key-alias", "androiddebugkey", "--min-sdk-version", "24",
    "--v1-signing-enabled", "true", "--v2-signing-enabled", "true", "--v3-signing-enabled", "true",
    "--out", "$Root\Chapitres.apk", "$Build\aligned.apk")

$verifyOut = Join-Path $Root "verify.log"
$vp = Start-Process -FilePath "$Bt\apksigner.bat" `
    -ArgumentList @("verify", "--verbose", "--print-certs", "--min-sdk-version", "21", "$Root\Chapitres.apk") `
    -NoNewWindow -Wait -PassThru -RedirectStandardOutput $verifyOut -RedirectStandardError "$verifyOut.err"
if (Test-Path $verifyOut) {
    Get-Content $verifyOut |
        Select-String -Pattern "Verifies|v1 scheme|v2 scheme|v3 scheme|Number of signers" |
        ForEach-Object { Write-Host $_ }
}

$size = [math]::Round((Get-Item "$Root\Chapitres.apk").Length / 1KB, 1)
Copy-Item "$Root\Chapitres.apk" "$Src\Chapitres.apk" -Force
Write-Host "APK : $Src\Chapitres.apk ($size Ko)" -ForegroundColor Green
