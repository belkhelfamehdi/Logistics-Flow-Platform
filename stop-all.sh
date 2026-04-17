#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="${ROOT_DIR}/.services.pids"

if [[ -f "${PID_FILE}" ]]; then
  echo "Stopping Spring services..."
  while IFS=: read -r name pid; do
    if [[ -n "${pid}" ]] && kill -0 "${pid}" >/dev/null 2>&1; then
      echo "  stopping ${name} (pid ${pid})"
      kill "${pid}" || true
    else
      echo "  ${name} already stopped"
    fi
  done < "${PID_FILE}"

  rm -f "${PID_FILE}"
else
  echo "No PID file found, skipping Spring process shutdown."
fi

echo "Stopping Kafka + Zookeeper..."
cd "${ROOT_DIR}"
docker compose down

echo "Done."
