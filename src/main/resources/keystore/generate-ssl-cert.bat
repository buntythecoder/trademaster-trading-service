@echo off
REM SSL Certificate Generation Script for TradeMaster Trading Service
REM This generates a self-signed certificate for development/testing purposes.
REM For production, use certificates from a trusted CA.

echo Generating SSL certificate for TradeMaster Trading Service...

REM Configuration
set KEYSTORE_PASSWORD=trademaster
set CERTIFICATE_ALIAS=trading-service
set KEYSTORE_FILE=trading-service.p12
set VALIDITY_DAYS=365
set KEY_SIZE=2048

REM Certificate information
set COUNTRY=IN
set STATE=Maharashtra
set CITY=Mumbai
set ORGANIZATION=TradeMaster
set ORGANIZATIONAL_UNIT=Trading Service
set COMMON_NAME=localhost

REM Generate the keystore with certificate
keytool -genkeypair ^
  -alias %CERTIFICATE_ALIAS% ^
  -keyalg RSA ^
  -keysize %KEY_SIZE% ^
  -storetype PKCS12 ^
  -keystore %KEYSTORE_FILE% ^
  -storepass %KEYSTORE_PASSWORD% ^
  -keypass %KEYSTORE_PASSWORD% ^
  -validity %VALIDITY_DAYS% ^
  -dname "CN=%COMMON_NAME%,OU=%ORGANIZATIONAL_UNIT%,O=%ORGANIZATION%,L=%CITY%,S=%STATE%,C=%COUNTRY%" ^
  -ext "SAN=dns:localhost,dns:trading-service,ip:127.0.0.1"

echo SSL certificate generated successfully!
echo Keystore file: %KEYSTORE_FILE%
echo Keystore password: %KEYSTORE_PASSWORD%
echo Certificate alias: %CERTIFICATE_ALIAS%
echo.
echo To use this certificate:
echo 1. Set SSL_ENABLED=true in your environment
echo 2. Set SSL_KEYSTORE_PASSWORD=%KEYSTORE_PASSWORD%
echo 3. The application will automatically use the keystore from classpath:keystore/%KEYSTORE_FILE%
echo.
echo WARNING: This is a self-signed certificate for development only!
echo WARNING: For production, obtain a certificate from a trusted Certificate Authority.

REM List the certificate details
echo.
echo Certificate details:
keytool -list -v -keystore %KEYSTORE_FILE% -storepass %KEYSTORE_PASSWORD% -alias %CERTIFICATE_ALIAS%

pause