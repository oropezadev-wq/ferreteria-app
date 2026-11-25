# Arma la versión lista para usar: Ferreteria-portable.zip (página + API + Java incluido).
# Requisitos solo en la PC que la arma: JDK 21 y Node.js 20 o superior.
# Uso, desde la raíz del repositorio:  powershell -ExecutionPolicy Bypass -File scripts\armar-version-portable.ps1
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$salida = Join-Path $raiz 'portable-dist'
$carpeta = Join-Path $salida 'Ferreteria'

# jlink viene con el JDK: se toma de JAVA_HOME o del java que esté en el PATH.
if ($env:JAVA_HOME) { $jdk = $env:JAVA_HOME }
else { $jdk = Split-Path -Parent (Split-Path -Parent (Get-Command java).Source) }
$jlink = Join-Path $jdk 'bin\jlink.exe'
if (-not (Test-Path $jlink)) { throw "No se encontró jlink en $jdk (se necesita un JDK 21)." }
$env:JAVA_HOME = $jdk

Write-Host '1/4 Compilando la vista (Angular)...'
Push-Location (Join-Path $raiz 'frontend')
if (-not (Test-Path 'node_modules')) { npm ci; if ($LASTEXITCODE) { throw 'npm ci falló' } }
npx ng build; if ($LASTEXITCODE) { throw 'ng build falló' }
Pop-Location

Write-Host '2/4 Empaquetando la API (Spring Boot)...'
Push-Location (Join-Path $raiz 'backend')
.\mvnw.cmd -q -DskipTests package; if ($LASTEXITCODE) { throw 'mvnw package falló' }
Pop-Location

Write-Host '3/4 Armando la carpeta...'
if (Test-Path $salida) { Remove-Item -Recurse -Force $salida }
New-Item -ItemType Directory -Force (Join-Path $carpeta 'app') | Out-Null
$jar = Get-ChildItem (Join-Path $raiz 'backend\target') -Filter 'ferreteria-api-*.jar' |
    Where-Object { $_.Name -notlike '*plain*' } | Select-Object -First 1
Copy-Item $jar.FullName (Join-Path $carpeta 'app\ferreteria.jar')
Copy-Item -Recurse (Join-Path $raiz 'frontend\dist\frontend\browser') (Join-Path $carpeta 'web')
Copy-Item (Join-Path $raiz 'backend\compose.yaml') $carpeta
Copy-Item (Join-Path $raiz 'portable\iniciar.bat'), (Join-Path $raiz 'portable\LEEME.txt') $carpeta

# Java reducido a los módulos que usa Spring Boot (unos 60 MB en lugar de un JDK completo).
$modulos = 'java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,' +
    'java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,' +
    'java.security.sasl,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.management,' +
    'jdk.naming.dns,jdk.net,jdk.unsupported,jdk.zipfs,jdk.localedata'
& $jlink --add-modules $modulos --strip-debug --no-header-files --no-man-pages --compress zip-6 `
    --output (Join-Path $carpeta 'runtime')
if ($LASTEXITCODE) { throw 'jlink falló' }

Write-Host '4/4 Comprimiendo...'
$zip = Join-Path $salida 'Ferreteria-portable.zip'
Compress-Archive -Path $carpeta -DestinationPath $zip
Write-Host "Listo: $zip"
