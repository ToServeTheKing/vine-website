# syntax=docker/dockerfile:1

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY . .
# Resolve the platform from the Gitea Maven registry (creds via BuildKit secrets); the Maven build also
# runs the Vite/React SPA build (frontend-maven-plugin) and folds it into the jar. Tests are skipped here
# because Testcontainers needs a Docker daemon — they run via `mvn verify`, not in the image build.
RUN --mount=type=secret,id=maven_user --mount=type=secret,id=maven_token \
    MAVEN_USER="$(cat /run/secrets/maven_user)" MAVEN_TOKEN="$(cat /run/secrets/maven_token)" \
    mvn -B -ntp -s .gitea/ci-settings.xml -DskipTests package \
 && cp "$(ls target/itsthevine-*.jar | grep -v original | head -1)" app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ---------- runtime ----------
FROM eclipse-temurin:25-jre-alpine AS runtime
# libwebp-tools supplies cwebp, which ProductPhotoService shells out to when an editor uploads a
# photo. Alpine's build is musl-native — the Java webp writers on Maven Central bundle glibc natives
# that will not load here, and no pure-Java webp encoder exists.
RUN apk -U upgrade --no-cache && apk add --no-cache curl libwebp-tools
RUN addgroup -S spring && adduser -S -D -H -h /app -s /sbin/nologin -G spring spring
WORKDIR /app

COPY --from=build --chown=spring:spring /src/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /src/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /src/extracted/application/ ./

USER spring
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ARG GIT_SHA=unknown
LABEL org.opencontainers.image.title="itsthevine" \
      org.opencontainers.image.source="https://git.thebennett.net/thevine/itsthevine" \
      org.opencontainers.image.revision="${GIT_SHA}"

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
