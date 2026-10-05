FROM payara/server-full:6.2025.4-jdk21

COPY movie-service/build/libs/movie-service.war ${DEPLOY_DIR}/movie-service.war
