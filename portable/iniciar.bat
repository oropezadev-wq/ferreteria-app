@echo off
rem Inicia la ferreteria con un doble clic: base de datos, API y pagina en http://localhost:8081
rem Si Docker esta abierto usa PostgreSQL 16; si no, usa H2 (archivo en la carpeta data).
if "%~1"=="esperar" goto esperar_web
title Ferreteria - servidor (no cerrar mientras se usa)
cd /d "%~dp0"

set PERFIL=portable,h2
set BASE=H2 (archivo local, no requiere instalar nada)

docker info >nul 2>&1
if errorlevel 1 goto arrancar

echo Docker encontrado: iniciando PostgreSQL 16...
docker compose -p ferreteria-portable up -d
if errorlevel 1 goto arrancar

set /a INTENTOS=0
:esperar_bd
docker compose -p ferreteria-portable exec -T postgres pg_isready -U ferreteria -d ferreteria >nul 2>&1
if not errorlevel 1 goto bd_lista
set /a INTENTOS+=1
if %INTENTOS% geq 30 goto arrancar
ping -n 3 127.0.0.1 >nul
goto esperar_bd

:bd_lista
set PERFIL=portable
set BASE=PostgreSQL 16 (Docker)

:arrancar
echo.
echo  Base de datos: %BASE%
echo  La pagina se abrira sola en http://localhost:8081 en unos segundos.
echo  Usuarios: admin / admin123   y   vendedor / vendedor123
echo  Para detener la aplicacion, cierre esta ventana.
echo.

rem Abre el navegador aparte, cuando la API ya responde.
start "" /b cmd /c ""%~f0" esperar"

"%~dp0runtime\bin\java.exe" -jar "%~dp0app\ferreteria.jar" --spring.profiles.active=%PERFIL%

echo.
echo La aplicacion se detuvo. Si fue por un error, revise el mensaje de arriba
echo (por ejemplo, el puerto 8081 ocupado por otro programa).
pause
exit /b

:esperar_web
rem Revisa cada segundo, hasta 2 minutos, si la API responde; entonces abre la pagina.
for /l %%i in (1,1,120) do (
  curl.exe -s -m 2 -o nul http://localhost:8081/api/catalogo && (start "" http://localhost:8081/ & exit)
  ping -n 2 127.0.0.1 >nul
)
exit
