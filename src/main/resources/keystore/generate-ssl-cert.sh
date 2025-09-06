#!/bin/bash
# SSL Certificate Generation Script for TradeMaster Trading Service
# This generates a self-signed certificate for development/testing purposes.
# For production, use certificates from a trusted CA.

echo "Generating SSL certificate for TradeMaster Trading Service..."

# Configuration
KEYSTORE_PASSWORD="trademaster"
CERTIFICATE_ALIAS="trading-service"
KEYSTORE_FILE="trading-service.p12"
VALIDITY_DAYS="365"
KEY_SIZE="2048"

# Certificate information
COUNTRY="IN"
STATE="Maharashtra"
CITY="Mumbai" 
ORGANIZATION="TradeMaster"
ORGANIZATIONAL_UNIT="Trading Service"
COMMON_NAME="localhost"

# Generate the keystore with certificate
keytool -genkeypair \
  -alias "$CERTIFICATE_ALIAS" \
  -keyalg RSA \
  -keysize "$KEY_SIZE" \
  -storetype PKCS12 \
  -keystore "$KEYSTORE_FILE" \
  -storepass "$KEYSTORE_PASSWORD" \
  -keypass "$KEYSTORE_PASSWORD" \
  -validity "$VALIDITY_DAYS" \
  -dname "CN=$COMMON_NAME,OU=$ORGANIZATIONAL_UNIT,O=$ORGANIZATION,L=$CITY,S=$STATE,C=$COUNTRY" \
  -ext "SAN=dns:localhost,dns:trading-service,ip:127.0.0.1"

echo "SSL certificate generated successfully!"
echo "Keystore file: $KEYSTORE_FILE"
echo "Keystore password: $KEYSTORE_PASSWORD"
echo "Certificate alias: $CERTIFICATE_ALIAS"
echo ""
echo "To use this certificate:"
echo "1. Set SSL_ENABLED=true in your environment"
echo "2. Set SSL_KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD"
echo "3. The application will automatically use the keystore from classpath:keystore/$KEYSTORE_FILE"
echo ""
echo "⚠️  This is a self-signed certificate for development only!"
echo "⚠️  For production, obtain a certificate from a trusted Certificate Authority."

# List the certificate details
echo ""
echo "Certificate details:"
keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD" -alias "$CERTIFICATE_ALIAS"