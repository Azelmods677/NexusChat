@echo off
echo Limpiando cache de Gradle...

REM Eliminar directorio .gradle del proyecto
if exist ".gradle" (
    echo Eliminando .gradle del proyecto...
    rmdir /s /q ".gradle"
)

REM Eliminar directorio build del proyecto
if exist "build" (
    echo Eliminando build del proyecto...
    rmdir /s /q "build"
)

REM Eliminar directorio build de app
if exist "app\build" (
    echo Eliminando app\build...
    rmdir /s /q "app\build"
)

REM Eliminar cache de Gradle global (opcional)
echo.
echo Limpieza completada!
echo.
echo Ahora ejecuta: gradlew clean
echo Luego sincroniza el proyecto en Android Studio
pause
