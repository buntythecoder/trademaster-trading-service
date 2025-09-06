# SSL/TLS Certificate Setup for TradeMaster Trading Service

This directory contains tools and certificates for SSL/TLS configuration of the Trading Service.

## Quick Setup

### Development Environment (Self-Signed Certificate)

1. **Generate Certificate** (choose your platform):
   ```bash
   # Linux/macOS
   ./generate-ssl-cert.sh
   
   # Windows
   generate-ssl-cert.bat
   ```

2. **Enable SSL in Application**:
   ```bash
   export SSL_ENABLED=true
   export SSL_KEYSTORE_PASSWORD=trademaster
   ```
   
   Or add to your environment variables:
   ```properties
   SSL_ENABLED=true
   SSL_KEYSTORE_PASSWORD=trademaster
   ```

3. **Start the Application**:
   ```bash
   ./gradlew bootRun
   ```
   
   The service will be available at: `https://localhost:8083`

### Production Environment

For production deployments:

1. **Obtain a CA-signed certificate** from a trusted Certificate Authority
2. **Convert to PKCS12 format** if necessary:
   ```bash
   openssl pkcs12 -export -in certificate.crt -inkey private.key -out trading-service.p12 -name trading-service
   ```
3. **Place the certificate** in this directory or configure the path via `SSL_KEYSTORE_PATH`
4. **Set environment variables**:
   ```bash
   export SSL_ENABLED=true
   export SSL_KEYSTORE_PASSWORD=your_secure_password
   export REQUIRE_SSL=true
   ```

## Configuration Options

All SSL configuration is externalized through environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SSL_ENABLED` | `false` | Enable/disable SSL |
| `SSL_KEYSTORE_PATH` | `classpath:keystore/trading-service.p12` | Path to keystore |
| `SSL_KEYSTORE_PASSWORD` | `trademaster` | Keystore password |
| `SSL_CLIENT_AUTH` | `none` | Client certificate authentication |
| `REQUIRE_SSL` | `false` | Force HTTPS redirect |
| `HTTP2_ENABLED` | `true` | Enable HTTP/2 support |

## Security Features

The SSL/TLS configuration includes:

- ✅ **TLS 1.3 & TLS 1.2 Support**: Modern TLS protocols only
- ✅ **Strong Cipher Suites**: AES-256-GCM, AES-128-GCM, ECDHE key exchange
- ✅ **HTTP/2 Support**: Improved performance over HTTP/1.1
- ✅ **HSTS Headers**: HTTP Strict Transport Security (production)
- ✅ **Secure Headers**: CSP, X-Frame-Options, X-Content-Type-Options
- ✅ **HTTPS Redirect**: Automatic HTTP to HTTPS redirect
- ✅ **Compression**: Response compression for better performance

## Certificate Details

The generated development certificate includes:

- **Algorithm**: RSA 2048-bit
- **Validity**: 365 days
- **Common Name**: localhost
- **SAN Extensions**: localhost, trading-service, 127.0.0.1
- **Organization**: TradeMaster
- **Format**: PKCS12 (.p12)

## Testing SSL Configuration

1. **Verify Certificate**:
   ```bash
   keytool -list -v -keystore trading-service.p12 -storepass trademaster
   ```

2. **Test HTTPS Endpoint**:
   ```bash
   curl -k https://localhost:8083/actuator/health
   ```

3. **Check SSL Grade** (production):
   - Use SSL Labs: https://www.ssllabs.com/ssltest/
   - Should achieve A+ rating with HSTS enabled

## Troubleshooting

### Certificate Not Trusted (Development)
This is expected with self-signed certificates. Options:
- Add `-k` flag to curl
- Add exception in browser
- Import certificate to system trust store

### Port Already in Use
If port 8083 is busy:
```bash
export SERVER_PORT=8443
./gradlew bootRun
```

### Permission Denied
Ensure keystore file has proper permissions:
```bash
chmod 644 trading-service.p12
```

### Certificate Expired
Regenerate the development certificate:
```bash
rm trading-service.p12
./generate-ssl-cert.sh
```

## Security Best Practices

1. ✅ Use strong passwords for keystore (production)
2. ✅ Rotate certificates before expiration
3. ✅ Monitor certificate expiration dates
4. ✅ Use CA-signed certificates in production
5. ✅ Enable HSTS in production environments
6. ✅ Regularly update cipher suites
7. ✅ Monitor SSL Labs reports

## Production Deployment Checklist

- [ ] CA-signed certificate installed
- [ ] Strong keystore password set
- [ ] HSTS enabled (`REQUIRE_SSL=true`)
- [ ] Certificate monitoring configured
- [ ] SSL Labs test passed (A+ grade)
- [ ] Load balancer SSL termination configured (if applicable)
- [ ] Certificate renewal process automated

## Support

For SSL configuration issues:
1. Check application logs for SSL errors
2. Verify keystore path and password
3. Ensure Java has access to keystore file
4. Test with openssl s_client for connection issues