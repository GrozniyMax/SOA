keytool -importkeystore -noprompt \
  -srckeystore /certs/movie.p12 -srcstoretype PKCS12 -srcstorepass "${KEYSTORE_PASSWORD:-changeit}" -srcalias movie \
  -destkeystore "${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/keystore.p12" -deststoretype PKCS12 \
  -deststorepass changeit -destkeypass changeit -destalias movie
