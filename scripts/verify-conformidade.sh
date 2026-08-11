#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080/api}"
PROCESSOR_HEALTH="${PROCESSOR_HEALTH:-http://localhost:8081/actuator/health}"
MAILHOG_API="${MAILHOG_API:-http://localhost:8025/api/v2/messages}"
PROMETHEUS_TARGETS="${PROMETHEUS_TARGETS:-http://localhost:9090/api/v1/targets}"
API_USER="${API_USER:-fiapx}"
API_PASS="${API_PASS:-fiapx123}"
POLL_INTERVAL="${POLL_INTERVAL:-3}"
MAX_WAIT="${MAX_WAIT:-180}"
TEST_VIDEO="${TEST_VIDEO:-/tmp/fiapx-e2e-test.mp4}"
INVALID_VIDEO="${INVALID_VIDEO:-/tmp/fiapx-conformidade-invalid.mp4}"
DEMO_EMAIL="${DEMO_EMAIL:-fiapx@fiapx.local}"

passed=0
failed=0

pass() {
  echo "OK  $1"
  passed=$((passed + 1))
}

fail() {
  echo "FALHA  $1"
  failed=$((failed + 1))
}

extract_job_id() {
  echo "$1" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4
}

extract_status() {
  echo "$1" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4
}

wait_for_health() {
  local url="$1"
  local label="$2"
  local deadline=$((SECONDS + MAX_WAIT))
  until curl -fsS "${url}" >/dev/null 2>&1; do
    if (( SECONDS >= deadline )); then
      fail "${label} indisponível em ${url}"
      return 1
    fi
    sleep 2
  done
  pass "${label} healthy (${url})"
}

wait_for_job_status() {
  local job_id="$1"
  local expected="$2"
  local deadline=$((SECONDS + MAX_WAIT))
  local status=""
  while (( SECONDS < deadline )); do
    local response
    response="$(curl -fsS -u "${API_USER}:${API_PASS}" "${API_BASE}/videos/${job_id}")"
    status="$(extract_status "${response}")"
    if [[ "${status}" == "${expected}" ]]; then
      echo "${status}"
      return 0
    fi
    if [[ "${expected}" != "FAILED" && "${status}" == "FAILED" ]]; then
      echo "${response}"
      return 1
    fi
    sleep "${POLL_INTERVAL}"
  done
  echo "${status:-TIMEOUT}"
  return 1
}

ensure_test_video() {
  if [[ -f "${TEST_VIDEO}" ]]; then
    return 0
  fi
  if [[ -f /tmp/fiapx-e2e-test.mp4 ]]; then
    TEST_VIDEO="/tmp/fiapx-e2e-test.mp4"
    pass "reutilizando vídeo de teste ${TEST_VIDEO}"
    return 0
  fi
  if command -v ffmpeg >/dev/null 2>&1; then
    ffmpeg -y -f lavfi -i testsrc=duration=3:size=320x240:rate=10 \
      -f lavfi -i sine=frequency=440:duration=3 \
      -c:v libx264 -pix_fmt yuv420p -c:a aac "${TEST_VIDEO}" >/dev/null 2>&1
    pass "vídeo de teste gerado em ${TEST_VIDEO}"
    return 0
  fi
  local processor_container
  processor_container="$(docker ps --format '{{.Names}}' 2>/dev/null | grep processor-service | head -1 || true)"
  if [[ -n "${processor_container}" ]] && docker exec "${processor_container}" which ffmpeg >/dev/null 2>&1; then
    docker exec "${processor_container}" ffmpeg -y -f lavfi -i testsrc=duration=3:size=320x240:rate=10 \
      -f lavfi -i sine=frequency=440:duration=3 \
      -c:v libx264 -pix_fmt yuv420p -c:a aac /tmp/fiapx-conformidade-test.mp4 >/dev/null 2>&1
    docker cp "${processor_container}:/tmp/fiapx-conformidade-test.mp4" "${TEST_VIDEO}" >/dev/null 2>&1
    if [[ -f "${TEST_VIDEO}" ]]; then
      pass "vídeo de teste gerado via container ${processor_container}"
      return 0
    fi
  fi
  fail "ffmpeg ausente no host; defina TEST_VIDEO apontando para um .mp4 válido"
  return 1
}

echo "=== Verificação de conformidade — FIAP X Fase 5 ==="

wait_for_health "${API_BASE}/actuator/health" "API" || true
wait_for_health "${PROCESSOR_HEALTH}" "Processor" || true

if ! ensure_test_video; then
  echo
  echo "Resumo: ${passed} OK, ${failed} FALHA(S)"
  exit 1
fi

echo "Enviando 2 uploads em paralelo..."
tmp_a="$(mktemp)"
tmp_b="$(mktemp)"
curl -fsS -u "${API_USER}:${API_PASS}" -F "file=@${TEST_VIDEO}" "${API_BASE}/videos" >"${tmp_a}" &
pid_a=$!
curl -fsS -u "${API_USER}:${API_PASS}" -F "file=@${TEST_VIDEO}" "${API_BASE}/videos" >"${tmp_b}" &
pid_b=$!
wait "${pid_a}"
wait "${pid_b}"
upload_a="$(cat "${tmp_a}")"
upload_b="$(cat "${tmp_b}")"
rm -f "${tmp_a}" "${tmp_b}"

job_a="$(extract_job_id "${upload_a}")"
job_b="$(extract_job_id "${upload_b}")"

if [[ -n "${job_a}" && -n "${job_b}" && "${job_a}" != "${job_b}" ]]; then
  pass "upload paralelo criou jobs distintos (${job_a}, ${job_b})"
else
  fail "upload paralelo não retornou dois job ids distintos"
fi

status_a="$(wait_for_job_status "${job_a}" "COMPLETED" || true)"
status_b="$(wait_for_job_status "${job_b}" "COMPLETED" || true)"

if [[ "${status_a}" == "COMPLETED" && "${status_b}" == "COMPLETED" ]]; then
  pass "dois jobs concluídos em paralelo"
else
  fail "jobs paralelos não concluíram (A=${status_a}, B=${status_b})"
fi

echo "not a video file" > "${INVALID_VIDEO}"
mail_before="$(curl -fsS "${MAILHOG_API}" 2>/dev/null || echo '{}')"
mail_count_before="$(echo "${mail_before}" | grep -oE 'total":[0-9]+' | head -1 | cut -d: -f2 || echo 0)"

invalid_response="$(curl -fsS -u "${API_USER}:${API_PASS}" -F "file=@${INVALID_VIDEO}" "${API_BASE}/videos")"
invalid_job="$(extract_job_id "${invalid_response}")"

if [[ -z "${invalid_job}" ]]; then
  fail "upload inválido não retornou job id"
else
  invalid_status="$(wait_for_job_status "${invalid_job}" "FAILED" || true)"
  if [[ "${invalid_status}" == "FAILED" ]]; then
    pass "arquivo inválido resultou em status FAILED (${invalid_job})"
  else
    fail "arquivo inválido não falhou como esperado (status=${invalid_status})"
  fi
fi

sleep 2
mail_after="$(curl -fsS "${MAILHOG_API}" 2>/dev/null || echo '{}')"
mail_count_after="$(echo "${mail_after}" | grep -oE 'total":[0-9]+' | head -1 | cut -d: -f2 || echo 0)"

if [[ -n "${invalid_job}" ]] \
  && echo "${mail_after}" | grep -q "${DEMO_EMAIL}" \
  && echo "${mail_after}" | grep -q "${invalid_job}" \
  && [[ "${mail_count_after:-0}" -gt "${mail_count_before:-0}" ]]; then
  pass "MailHog recebeu e-mail de falha para ${DEMO_EMAIL} (job ${invalid_job})"
else
  fail "MailHog não registrou e-mail de falha (antes=${mail_count_before}, depois=${mail_count_after})"
fi

targets_json="$(curl -fsS "${PROMETHEUS_TARGETS}" 2>/dev/null || echo '{}')"
api_up="$(echo "${targets_json}" | grep -c 'fiapx-api' || true)"
processor_up="$(echo "${targets_json}" | grep -c 'fiapx-processor' || true)"
health_up="$(echo "${targets_json}" | grep -o '"health":"up"' | wc -l | tr -d ' ')"

if [[ "${health_up}" -ge 2 ]] && [[ "${api_up}" -ge 1 ]] && [[ "${processor_up}" -ge 1 ]]; then
  pass "Prometheus targets fiapx-api e fiapx-processor UP (${health_up} targets up)"
else
  fail "Prometheus targets incompletos (up=${health_up}, api refs=${api_up}, processor refs=${processor_up})"
fi

echo
echo "Resumo: ${passed} OK, ${failed} FALHA(S)"
if (( failed > 0 )); then
  exit 1
fi
echo "Conformidade verificada com sucesso."
