FROM alpine:latest
RUN apk add --update --no-cache netcat-openbsd

COPY scripts /opt/scripts
WORKDIR /opt/scripts

EXPOSE 8000

ENTRYPOINT [ "/bin/sh", "run-server.sh" ]
# inject parameters: [fileSize in MB]
CMD [ "100" ]