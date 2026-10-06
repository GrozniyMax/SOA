#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)/certs"
PASSWORD="${KEYSTORE_PASSWORD:-changeit}"
DOMAIN="${DOMAIN:-}"
CN="${CN:-localhost}"
EXTRA_SAN="${EXTRA_SAN:-DNS:helios.cs.ifmo.ru}"
SAN="DNS:localhost,DNS:movie,DNS:oscar,IP:127.0.0.1,${EXTRA_SAN}"

mkdir -p "$DIR"
cd "$DIR"

p12() {
  openssl pkcs12 -export -name "$1" \
    -inkey "$1.key" -in "$1.crt" \
    -out "$1.p12" -passout "pass:$PASSWORD"
}

rm -f movie-truststore.p12 server.crt server.key

if [ -n "$DOMAIN" ]; then
  LIVE="/etc/letsencrypt/live/$DOMAIN"
  for name in movie oscar frontend; do
    cp "$LIVE/fullchain.pem" "$name.crt"
    cp "$LIVE/privkey.pem" "$name.key"
  done
else
  openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
    -keyout server.key -out server.crt \
    -subj "/CN=$CN" \
    -addext "subjectAltName=$SAN"
  for name in movie oscar frontend; do
    cp server.crt "$name.crt"
    cp server.key "$name.key"
  done
  keytool -importcert -noprompt -alias movie -file server.crt \
    -keystore movie-truststore.p12 -storetype PKCS12 -storepass "$PASSWORD"
fi

p12 movie
p12 oscar
chmod 644 ./*.p12

ls -1 "$DIR"
