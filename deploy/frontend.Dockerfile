# Dynamic upstream SRV resolution is available in Nginx OSS since 1.27.3.
FROM nginx:1.28-alpine

RUN rm /etc/nginx/conf.d/default.conf
COPY deploy/nginx.conf /etc/nginx/conf.d/soa.conf
COPY frontend/dist /usr/share/nginx/html

EXPOSE 8443 8444
