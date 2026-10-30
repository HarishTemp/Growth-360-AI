@echo off
set JAVA_HOME=C:\Users\ASUS\.jdks\temurin-21\jdk-21.0.12.1+1
set PATH=%JAVA_HOME%\bin;%PATH%
echo =======================================================
echo   Starting Growth 360 AI Localhost Web Server...
echo   Open in your browser: http://localhost:8080
echo =======================================================
call gradlew.bat runServer
pause
