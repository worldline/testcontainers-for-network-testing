FROM alpine:latest
RUN apk add --update --no-cache netcat-openbsd

COPY scripts /opt/scripts
WORKDIR /opt/scripts

# inject the DNS name of the server and the payload as parameters
ENTRYPOINT [ "/bin/sh", "run-client.sh" ]
CMD [ "localhost", "8000", "this is just a test" ]