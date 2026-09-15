FROM gradle:9.1.0-jdk25 AS build

WORKDIR /build

COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle/wrapper ./gradle/wrapper
COPY api/build.gradle api/build.gradle
COPY service/build.gradle service/build.gradle
RUN ./gradlew dependencies --no-daemon

COPY api ./api
COPY service ./service
RUN ./gradlew :service:bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre-alpine

RUN apk add --no-cache curl
RUN addgroup -S vempain && adduser -S -G vempain vempain

WORKDIR /var/www/backend

COPY --from=build /build/service/build/libs/vempain-website-backend.jar ./vempain-website-backend.jar
RUN chown -R vempain:vempain /var/www/backend

USER vempain

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "/var/www/backend/vempain-website-backend.jar"]
