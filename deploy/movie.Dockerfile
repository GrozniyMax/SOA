FROM payara/server-full:6.2025.4-jdk21

COPY deploy/payara/import-cert.sh ${SCRIPT_DIR}/init.d/import-cert.sh
COPY deploy/payara/pre-boot-commands.asadmin ${PREBOOT_COMMANDS}
COPY movie-service/build/libs/movie-service.war ${DEPLOY_DIR}/movie-service.war
