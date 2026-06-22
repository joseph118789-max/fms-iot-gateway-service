# Multi-stage Dockerfile for iot-gateway-service.
# Stage 1: build with Maven + JDK 21
# Stage 2: minimal JRE 21 runtime, non-root user, healthcheck
#
# Build:
#   docker build -t 192.168.1.239:5000/iot-gateway-service:0.1.0-SNAPSHOT .
#
# Push:
#   docker push 192.168.1.239:5000/iot-gateway-service:0.1.0-SNAPSHOT
#
# Run locally:
#   DB_URL=jdbc:postgresql://pg-legacy.ti:5432/ubiqtrac?currentSchema=telemetry \
#   DB_USER=telemetry_writer \
#   DB_PASSWORD=<from-k8s-secret> \
#   docker run -p 8080:8080 \
#     -e DB_URL -e DB_USER -e DB_PASSWORD \
#     192.168.1.239:5000/iot-gateway-service:0.1.0-SNAPSHOT

# --- Build stage ---
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Copy source and build
COPY pom.xml ./
COPY src ./src
RUN mvn -B -ntp -DskipTests package

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user
RUN groupadd --system --gid 1001 fms && \
    useradd --system --uid 1001 --gid fms --no-create-home fms

COPY --from=build /workspace/target/iot-gateway-service-*.jar /app/app.jar

USER fms

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

# K8s mounts env vars from Secret iot-gateway-creds
ENV DB_URL="" \
    DB_USER="" \
    DB_PASSWORD="" \
    TRACCAR_BASE_URL="" \
    TRACCAR_API_KEY=""

ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
