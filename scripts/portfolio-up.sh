#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PARENT="$(cd "$ROOT/.." && pwd)"
PROC="$PARENT/fiapx-processor-service"

if [[ ! -d "$PROC" ]]; then
  echo "Esperado sibling em $PROC (compose referencia o processor)." >&2
  exit 1
fi

echo "==> Stack completa FIAPx (api + processor + infra)"
docker compose up -d --build

echo "==> Aguardando API"
for i in $(seq 1 60); do
  if curl -fsS -u fiapx:fiapx123 "http://localhost:8080/api/actuator/health" >/dev/null 2>&1; then
    echo "OK API em http://localhost:8080/api"
    echo "Swagger: http://localhost:8080/api/swagger-ui.html"
    echo "Auth: fiapx / fiapx123"
    exit 0
  fi
  sleep 5
done

echo "Timeout aguardando API. Logs:" >&2
docker compose logs --tail=80
exit 1
