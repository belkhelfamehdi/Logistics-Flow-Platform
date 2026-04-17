#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${ROOT_DIR}/logs"
PID_FILE="${ROOT_DIR}/.services.pids"

wait_http() {
  local name="$1"
  local url="$2"
  local retries="${3:-120}"
  local delay="${4:-2}"

  echo "Waiting for ${name} on ${url} ..."
  for ((i=1; i<=retries; i++)); do
    if curl -s "${url}" >/dev/null 2>&1; then
      echo "${name} is up."
      return 0
    fi
    sleep "${delay}"
  done

  echo "Timeout while waiting for ${name} (${url}). Check logs in ${LOG_DIR}."
  return 1
}

start_service() {
  local name="$1"
  local dir="$2"
  local log_file="${LOG_DIR}/${name}.log"

  echo "Starting ${name}..."
  (
    cd "${ROOT_DIR}/${dir}"
    ./mvnw spring-boot:run
  ) >"${log_file}" 2>&1 &

  local pid=$!
  echo "${name}:${pid}" >> "${PID_FILE}"
  echo "  pid=${pid}"
  echo "  log=${log_file}"
}

mkdir -p "${LOG_DIR}"
: > "${PID_FILE}"

echo "Starting Kafka + Zookeeper..."
cd "${ROOT_DIR}"
docker compose up -d

echo "Ensuring Kafka topic logistics-events exists..."
docker exec kafka kafka-topics --create --if-not-exists \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 1 \
  --topic logistics-events >/dev/null 2>&1 || true

start_service "config-server" "config-server"
wait_http "config-server" "http://localhost:8880"

start_service "discovery-service" "discovery-service"
wait_http "discovery-service" "http://localhost:8761"

start_service "auth-service" "auth-service"
wait_http "auth-service" "http://localhost:8084/auth/csrf"

start_service "order-service" "order-service"
start_service "inventory-service" "inventory-service"
start_service "shipment-service" "shipment-service"

sleep 5
start_service "api-gateway" "api-gateway"
wait_http "api-gateway" "http://localhost:8090"

echo
echo "All services started."
echo "Gateway:  http://localhost:8090"
echo "Eureka:   http://localhost:8761"
echo "Logs:     ${LOG_DIR}"
echo "PIDs:     ${PID_FILE}"
echo
echo "Stop command: ./stop-all.sh"
