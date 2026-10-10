@echo off
REM Compile and run the ATM Interface (requires JDK 17+)
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\com\oasis\atm\*.java || exit /b 1
java -cp out com.oasis.atm.Main
