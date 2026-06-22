#!/bin/bash
# codegen-and-build.sh — iot-gateway-service
# Runs jOOQ codegen + Maven package for iot-gateway-service.
# Phase 2b Track 2, 2026-06-22
#
# Prerequisites:
#   - Java 17 (project pom declares java.version=17)
#   - Maven 3.8.x on this host
#   - K8s Secret iot-gateway-creds in iot-gateway namespace (DB password)
#   - PostgreSQL reachable at 192.168.1.239:5432, telemetry schema exists
#
# Usage: ./codegen-and-build.sh [phase]
#   phase: codegen | compile | package (default: compile)

set -euo pipefail

PHASE="${1:-compile}"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

# Java 17 (project pom declares java.version=17)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# K3s API tunnel (SSH forward from openclaw01 → k3s01)
KUBECONFIG=/tmp/kubeconfig-tunnel.yaml

# Ensure SSH tunnel is up
if ! nc -z 127.0.0.1 6443 2>/dev/null; then
    echo "[setup] starting SSH tunnel for K8s API..."
    ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 \
        -o ServerAliveInterval=30 -o ExitOnForwardFailure=yes \
        -f -N -L 6443:10.0.0.1:6443 user@192.168.1.150
    sleep 2
fi

# Fetch DB password from K8s Secret via k3s01 (Case A retrieval)
echo "[setup] reading DB password from K8s Secret..."
DB_PASSWORD=$(ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 \
    user@192.168.1.150 \
    "KUBECONFIG=/etc/rancher/k3s/k3s.yaml kubectl get secret iot-gateway-creds -n iot-gateway -o jsonpath='{.data.DB_PASSWORD}' | base64 -d")
echo "[setup] DB password retrieved (length: ${#DB_PASSWORD})"

# Codegen env vars
export CODEGEN_JDBC_URL="jdbc:postgresql://192.168.1.239:5432/ubiqtrac?currentSchema=telemetry"
export CODEGEN_JDBC_USERNAME="telemetry_writer"
export CODEGEN_JDBC_PASSWORD="$DB_PASSWORD"

# Step 1: jOOQ codegen (jooq.skip=false to enable)
echo "[step 1] jOOQ codegen..."
mvn -B -Djooq.skip=false \
    -Dcodegen.jdbc.url="$CODEGEN_JDBC_URL" \
    -Dcodegen.jdbc.username="$CODEGEN_JDBC_USERNAME" \
    -Dcodegen.jdbc.password="$CODEGEN_JDBC_PASSWORD" \
    generate-sources

# Step 2: compile
if [ "$PHASE" = "codegen" ]; then
    echo ""
    echo "=========================================="
    echo "Codegen complete. Generated sources at:"
    echo "  $PROJECT_DIR/target/generated-sources/jooq/"
    echo "=========================================="
    exit 0
fi

echo "[step 2] Maven compile..."
mvn -B -DskipTests compile

echo ""
echo "=========================================="
echo "Compile complete. Wire up PositionRepository to jOOQ-generated Tables.POSITION."
echo "=========================================="
