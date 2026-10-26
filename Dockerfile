# Runtime-only image: the jar is built beforehand (CI: `./gradlew clean bootJar` in the shared
# spring-boot-service workflow, locally: `./gradlew bootJar`) and copied in, exactly as in the
# admin and file backends. Nothing is compiled inside the image build.
FROM eclipse-temurin:25-jre-alpine
EXPOSE 8000

RUN apk add --no-cache curl su-exec
RUN mkdir -p /var/www/backend
# The ids are placeholders: container-entrypoint.sh gives the account VEMPAIN_UID:VEMPAIN_GID when the container starts
RUN adduser -D -h /var/www/backend -u 6666 -H vempain

WORKDIR /var/www/backend

COPY service/build/libs/vempain-website-backend.jar ./vempain-website-backend.jar
COPY container-entrypoint.sh /usr/local/bin/container-entrypoint.sh
RUN chmod 755 /usr/local/bin/container-entrypoint.sh

ENTRYPOINT ["/usr/local/bin/container-entrypoint.sh"]
CMD ["-jar", "/var/www/backend/vempain-website-backend.jar"]
