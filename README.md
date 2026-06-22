# IoT Gateway Service — Phase 2b

FMS IoT Gateway: polls Traccar for position/telemetry data, normalizes, stores in `telemetry` schema.

## Architecture

```
Traccar ──webhook──> IoT Gateway ──> telemetry.tc_position
                    └─> REST API ──> Clients
```

## Build

```bash
# Local build (no codegen)
./mvnw clean package

# Build with jOOQ codegen (requires K8s access to fms-iot-gateway namespace)
mvn clean package -Pcodegen
```

## Run

```bash
# Requires DB credentials from K8s Secret iot-gateway-creds
export DB_URL=jdbc:postgresql://pg-legacy.ti:5432/ubiqtrac?currentSchema=telemetry
export DB_USER=telemetry_writer
export DB_PASSWORD=$(kubectl get secret iot-gateway-creds -n fms-iot-gateway -o jsonpath='{.data.DB_PASSWORD}' | base64 -d)

java -jar target/iot-gateway-service-*.jar
```

## API

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/positions?deviceId={id}&page=0&size=20 | List positions for device |
| GET | /api/v1/positions/latest/{deviceId} | Latest position for device |
| POST | /api/v1/traccar/position | Traccar webhook (public) |
| GET | /api/v1/meta/info | Service info |

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| DB_URL | Yes | JDBC URL with schema=telemetry |
| DB_USER | Yes | Database user (telemetry_writer) |
| DB_PASSWORD | Yes | From K8s Secret iot-gateway-creds |
| TRACCAR_BASE_URL | No | Traccar API base URL |
| TRACCAR_API_KEY | No | Traccar API key |

## Database

- Schema: `telemetry`
- Tables: `tc_position` (Traccar position records), `tc_device` (Traccar device registry)
- Credentials: `telemetry_writer` role (read/write)

## Conventions

Mirrors [Phase 2a (fms-device-service)](../v4/phase2a/) build wiring contract:
- Spring Boot 3.3.5, Java 21, jOOQ 3.19.x
- Keycloak JWT auth (except /traccar webhook and /actuator endpoints)
- Env-driven secrets, no hardcoded fallbacks
- Non-root container, read-only root filesystem
- K8s liveness/readiness probes at /actuator/health/{liveness,readiness}
