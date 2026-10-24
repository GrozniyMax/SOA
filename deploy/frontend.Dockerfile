# frontend: статика Vite-сборки + nginx с TLS :8444.
# /movie-service/ и /oscar/ проксируются на gateway (same-origin API,
# VITE_*_API — относительные пути).
FROM nginx:alpine

RUN rm /etc/nginx/conf.d/default.conf
COPY deploy/nginx.conf /etc/nginx/conf.d/soa.conf
COPY frontend/dist /usr/share/nginx/html

EXPOSE 8444
