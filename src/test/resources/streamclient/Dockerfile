FROM alpine:latest
RUN apk add --update --no-cache netcat-openbsd

COPY scripts /opt/scripts
WORKDIR /opt/scripts

ENTRYPOINT [ "/bin/sh", "run-client.sh" ]
# inject parameters: [server-dns-name, server-port]
CMD [ "localhost", "8000" ]