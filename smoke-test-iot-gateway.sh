#!/bin/bash
# smoke-test-iot-gateway.sh — 3-tier smoke test for iot-gateway-service
# Phase 2b Track 2, 2026-06-22
#
# Verifies:
#   Tier 1: /actuator/health → 200 (overall UP)
#   Tier 2: /actuator/health/readiness → 200 (DB connection)
#   Tier 3: /api/v1/positions/latest/{deviceId} with JWT → 200, unauth → 401
#           /api/v1/positions?deviceId=...&page=...&size=... → 200 with paginated body
#
# Prerequisites:
#   - iot-gateway-service deployed in iot-gateway namespace
#   - Keycloak test user creds in /opt/phase1-rotation/keycloak-test-user.env
#   - SSH access to k3s01 (192.168.1.150) as user
#
# Usage: ./smoke-test-iot-gateway.sh [device_id]
#   device_id: which device to query (default: 2 — has test row from Option C)

set -uo pipefail

DEVICE_ID="${1:-2}"
NS="iot-gateway"
POD=$(ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 user@192.168.1.150 \
    "KUBECONFIG=/etc/rancher/k3s/k3s.yaml kubectl get pod -n $NS -l app=iot-gateway-service -o jsonpath='{.items[0].metadata.name}'" 2>/dev/null)
if [ -z "$POD" ]; then
    echo "FAIL: no iot-gateway-service pod found in $NS"
    exit 1
fi
echo "Target pod: $POD"

# Load test user creds
source /opt/phase1-rotation/keycloak-test-user.env
KC_TEST_USER_PASSWORD="$KC_TEST_USER_PASSWORD"

# Get JWT from Keycloak
JWT=$(curl -s -X POST "http://192.168.1.239:8180/auth/realms/fms/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=ubiqtrac-v4" \
    -d "username=$KC_TEST_USER" \
    -d "password=$KC_TEST_USER_PASSWORD" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("access_token", ""))')
if [ -z "$JWT" ]; then
    echo "FAIL: could not get JWT from Keycloak"
    exit 1
fi
echo "JWT obtained (length: ${#JWT})"

# Helper: run wget inside pod
pwget() {
    local headers="$1"
    local path="$2"
    if [ -n "$headers" ]; then
        ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 user@192.168.1.150 \
            "KUBECONFIG=/etc/rancher/k3s/k3s.yaml kubectl exec -n $NS $POD -c iot-gateway-service -- wget -qO- $headers $path" 2>/dev/null
    else
        ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 user@192.168.1.150 \
            "KUBECONFIG=/etc/rancher/k3s/k3s.yaml kubectl exec -n $NS $POD -c iot-gateway-service -- wget -qO- $path" 2>/dev/null
    fi
}

pwget_verbose() {
    local headers="$1"
    local path="$2"
    ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 user@192.168.1.150 \
        "KUBECONFIG=/etc/rancher/k3s/k3s.yaml kubectl exec -n $NS $POD -c iot-gateway-service -- wget $headers -S -O /dev/null $path 2>&1 | grep -E 'HTTP/|HTTP ' | head -2" 2>/dev/null
}

PASS=0
FAIL=0

assert_status() {
    local label="$1"
    local expected="$2"
    local actual="$3"
    if [ "$actual" = "$expected" ]; then
        echo "  ✅ PASS $label (HTTP $actual)"
        PASS=$((PASS+1))
    else
        echo "  ❌ FAIL $label (expected HTTP $expected, got $actual)"
        FAIL=$((FAIL+1))
    fi
}

# === Tier 1: liveness ===
echo ""
echo "=== Tier 1: liveness ==="
RESP=$(pwget "" "http://localhost:8080/actuator/health")
echo "  body: $RESP"
echo "$RESP" | grep -q '"status":"UP"' && assert_status "tier-1 liveness UP" "200" "200" || assert_status "tier-1 liveness UP" "200" "fail"

# === Tier 2: readiness (DB) ===
echo ""
echo "=== Tier 2: readiness (DB connectivity) ==="
RESP=$(pwget "" "http://localhost:8080/actuator/health/readiness")
echo "  body: $RESP"
echo "$RESP" | grep -q '"status":"UP"' && assert_status "tier-2 readiness UP" "200" "200" || assert_status "tier-2 readiness UP" "200" "fail"

# === Tier 3: authenticated GET ===
echo ""
echo "=== Tier 3: authenticated GET /api/v1/positions/latest/$DEVICE_ID ==="
RESP=$(pwget "--header=Authorization: Bearer $JWT" "http://localhost:8080/api/v1/positions/latest/$DEVICE_ID")
echo "  body: $RESP"
if echo "$RESP" | grep -q "\"deviceId\":$DEVICE_ID"; then
    assert_status "tier-3 latest returns deviceId=$DEVICE_ID" "200" "200"
else
    # 404 is also valid (no data) — check if response is well-formed
    if [ -z "$RESP" ]; then
        # Could be 404, check with verbose
        HTTP=$(pwget_verbose "--header=Authorization: Bearer $JWT" "http://localhost:8080/api/v1/positions/latest/$DEVICE_ID" | grep -oE 'HTTP/[0-9.]+ [0-9]+' | head -1 | awk '{print $2}')
        if [ "$HTTP" = "404" ]; then
            echo "  ⚠️  No data for deviceId=$DEVICE_ID (HTTP 404) — insert test row first if expected"
            FAIL=$((FAIL+1))
        else
            assert_status "tier-3 latest (auth)" "200" "$HTTP"
        fi
    fi
fi

# === Tier 3: unauthenticated → 401 ===
echo ""
echo "=== Tier 3: unauthenticated → 401 ==="
HTTP=$(pwget_verbose "" "http://localhost:8080/api/v1/positions/latest/$DEVICE_ID" | grep -oE 'HTTP/[0-9.]+ [0-9]+' | head -1 | awk '{print $2}')
assert_status "tier-3 unauth blocks" "401" "${HTTP:-fail}"

# === Tier 3: list with pagination ===
echo ""
echo "=== Tier 3: list /api/v1/positions?deviceId=$DEVICE_ID&page=0&size=10 ==="
RESP=$(pwget "--header=Authorization: Bearer $JWT" "http://localhost:8080/api/v1/positions?deviceId=$DEVICE_ID&page=0&size=10")
echo "  body: $RESP"
if echo "$RESP" | grep -q '"totalElements"'; then
    assert_status "tier-3 list paginated" "200" "200"
else
    assert_status "tier-3 list paginated" "200" "fail"
fi

echo ""
echo "=========================================="
echo "Smoke test results: $PASS pass, $FAIL fail"
echo "=========================================="
exit $FAIL
