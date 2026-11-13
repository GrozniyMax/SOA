FROM python:3.12-alpine

RUN pip install --no-cache-dir dnslib==0.9.26
WORKDIR /app
COPY eureka_dns.py .
USER 65534:65534
EXPOSE 8053/tcp
CMD ["python", "eureka_dns.py"]
