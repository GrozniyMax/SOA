#!/usr/bin/env bash
# Сертификаты для лабы №3:
#   gateway.p12             — PKCS12-keystore TLS-сервера api-gateway (:8443)
#   gateway-truststore.p12  — truststore с сертификатом gateway (для Feign-клиента oscar)
#   frontend.crt / frontend.key — TLS-сертификат nginx (:8444)
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)/certs"
PASSWORD="${KEYSTORE_PASSWORD:-changeit}"
DOMAIN="${DOMAIN:-}"
CN="${CN:-localhost}"
EXTRA_SAN="${EXTRA_SAN:-DNS:helios.cs.ifmo.ru}"
SAN="DNS:localhost,DNS:gateway,DNS:frontend,IP:127.0.0.1,${EXTRA_SAN}"

mkdir -p "$DIR"
cd "$DIR"

rm -f gateway.p12 gateway-truststore.p12 frontend.crt frontend.key gateway.crt gateway.key server.crt server.key \
      movie.p12 oscar.p12 movie-truststore.p12 movie.crt movie.key oscar.crt oscar.key

if [ -n "$DOMAIN" ]; then
  LIVE="/etc/letsencrypt/live/$DOMAIN"
  cp "$LIVE/fullchain.pem" gateway.crt
  cp "$LIVE/privkey.pem" gateway.key
  cp "$LIVE/fullchain.pem" frontend.crt
  cp "$LIVE/privkey.pem" frontend.key
else
  openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
    -keyout gateway.key -out gateway.crt \
    -subj "/CN=$CN" \
    -addext "subjectAltName=$SAN"
  cp gateway.crt frontend.crt
  cp gateway.key frontend.key
fi

# Keystore gateway (TLS-терминация)
openssl pkcs12 -export -name gateway \
  -inkey gateway.key -in gateway.crt \
  -out gateway.p12 -passout "pass:$PASSWORD"

# Truststore для Feign-клиента oscar (доверяем сертификату gateway)
keytool -importcert -noprompt -alias gateway -file gateway.crt \
  -keystore gateway-truststore.p12 -storetype PKCS12 -storepass "$PASSWORD"

chmod 644 ./*.p12 gateway.crt frontend.crt
chmod 600 gateway.key frontend.key

ls -1 "$DIR"
