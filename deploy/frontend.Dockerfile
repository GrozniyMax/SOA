# frontend: Vite-статика + nginx TLS :8444; /movie-service/ и /oscar/ → gateway.
FROM nginx:alpine

RUN rm /etc/nginx/conf.d/default.conf
COPY deploy/nginx.conf /etc/nginx/conf.d/soa.conf
COPY frontend/dist /usr/share/nginx/html

EXPOSE 8444
