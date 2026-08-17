# Build multi-module → runtime JRE 21 (gateway Spring Boot fat JAR)
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src

COPY pom.xml ./
COPY cmkerp-bom/pom.xml cmkerp-bom/
COPY cmkerp-shared-kernel/pom.xml cmkerp-shared-kernel/
COPY cmkerp-platform/pom.xml cmkerp-platform/
COPY cmkerp-stocks/pom.xml cmkerp-stocks/
COPY cmkerp-pos/pom.xml cmkerp-pos/
COPY cmkerp-gateway/pom.xml cmkerp-gateway/

RUN mvn -B -pl cmkerp-gateway -am dependency:go-offline -DskipTests || true

COPY cmkerp-bom cmkerp-bom
COPY cmkerp-shared-kernel cmkerp-shared-kernel
COPY cmkerp-platform cmkerp-platform
COPY cmkerp-stocks cmkerp-stocks
COPY cmkerp-pos cmkerp-pos
COPY cmkerp-gateway cmkerp-gateway

RUN mvn -B -pl cmkerp-gateway -am install -DskipTests \
    -Dskip.openapi.generation=true \
 && JAR="$(ls cmkerp-gateway/target/cmkerp-gateway-*.jar | grep -v original | head -1)" \
 && cp "$JAR" /src/gateway.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && useradd --system --uid 10001 --create-home cmk \
  && mkdir -p /app/logs \
  && chown -R cmk:cmk /app

COPY --from=build --chown=cmk:cmk /src/gateway.jar /app/app.jar

USER cmk
EXPOSE 8999

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=8 \
  CMD curl -fsS "http://127.0.0.1:8999/cmkerp-gateway/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
