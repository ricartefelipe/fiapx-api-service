#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080/api}"
API_USER="${API_USER:-fiapx}"
API_PASS="${API_PASS:-fiapx123}"
POLL_INTERVAL="${POLL_INTERVAL:-3}"
MAX_WAIT="${MAX_WAIT:-120}"
TEST_VIDEO="${TEST_VIDEO:-/tmp/fiapx-e2e-test.mp4}"

echo "Aguardando API em ${API_BASE}..."
deadline=$((SECONDS + MAX_WAIT))
until curl -fsS -u "${API_USER}:${API_PASS}" "${API_BASE}/actuator/health" >/dev/null 2>&1; do
  if (( SECONDS >= deadline )); then
    echo "Timeout aguardando API"
    exit 1
  fi
  sleep 2
done

if [[ ! -f "${TEST_VIDEO}" ]]; then
  if command -v ffmpeg >/dev/null 2>&1; then
    echo "Gerando vídeo de teste com ffmpeg..."
    ffmpeg -y -f lavfi -i testsrc=duration=3:size=320x240:rate=10 \
      -f lavfi -i sine=frequency=440:duration=3 \
      -c:v libx264 -pix_fmt yuv420p -c:a aac "${TEST_VIDEO}"
  else
    echo "ffmpeg não encontrado e ${TEST_VIDEO} inexistente"
    exit 1
  fi
fi

echo "Enviando upload..."
upload_response="$(curl -fsS -u "${API_USER}:${API_PASS}" -F "file=@${TEST_VIDEO}" "${API_BASE}/videos")"
job_id="$(echo "${upload_response}" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)"

if [[ -z "${job_id}" ]]; then
  echo "Falha ao obter job id: ${upload_response}"
  exit 1
fi

echo "Job criado: ${job_id}"

status=""
while (( SECONDS < deadline )); do
  job_response="$(curl -fsS -u "${API_USER}:${API_PASS}" "${API_BASE}/videos/${job_id}")"
  status="$(echo "${job_response}" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)"
  echo "Status: ${status}"
  if [[ "${status}" == "COMPLETED" ]]; then
    break
  fi
  if [[ "${status}" == "FAILED" ]]; then
    echo "Job falhou: ${job_response}"
    exit 1
  fi
  sleep "${POLL_INTERVAL}"
done

if [[ "${status}" != "COMPLETED" ]]; then
  echo "Timeout aguardando conclusão do job"
  exit 1
fi

download_dir="$(mktemp -d)"
pushd "${download_dir}" >/dev/null
curl -fsS -u "${API_USER}:${API_PASS}" -OJ "${API_BASE}/videos/${job_id}/download"
zip_file="$(ls -1 *.zip 2>/dev/null | head -1 || true)"
if [[ -z "${zip_file}" ]]; then
  echo "Download não retornou ZIP"
  exit 1
fi
if ! unzip -t "${zip_file}" >/dev/null 2>&1; then
  echo "ZIP inválido: ${zip_file}"
  exit 1
fi
popd >/dev/null
rm -rf "${download_dir}"

echo "E2E concluído com sucesso (job ${job_id})"
