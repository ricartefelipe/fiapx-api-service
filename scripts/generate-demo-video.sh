#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRAMES="${FRAMES_DIR:-${ROOT}/docs/delivery/video-frames}"
OUTPUT="${OUTPUT:-${ROOT}/docs/delivery/fiapx-fase5-demo.mp4}"
SECONDS_PER_FRAME="${SECONDS_PER_FRAME:-25}"
API_BASE="${API_BASE:-http://localhost:8080/api}"
API_USER="${API_USER:-fiapx}"
API_PASS="${API_PASS:-fiapx123}"

mkdir -p "${FRAMES}"

if ! compgen -G "${FRAMES}/*.png" > /dev/null; then
  echo "Nenhum PNG em ${FRAMES}. Capture frames antes (browser ou script de captura)."
  exit 1
fi

index=1
for src in "${FRAMES}"/*.png; do
  printf -v dest "%s/frame_%03d.png" "${FRAMES}" "${index}"
  if [[ "${src}" != "${dest}" ]]; then
    cp "${src}" "${dest}"
  fi
  index=$((index + 1))
done

frame_count=$((index - 1))
duration=$((frame_count * SECONDS_PER_FRAME))
if (( duration > 600 )); then
  SECONDS_PER_FRAME=$((600 / frame_count))
  duration=600
fi

framerate="$(awk 'BEGIN { printf "%.6f", 1/'"${SECONDS_PER_FRAME}"' }')"

docker run --rm \
  -u "$(id -u):$(id -g)" \
  -v "${FRAMES}:/frames:ro" \
  -v "$(dirname "${OUTPUT}"):/out" \
  jrottenberg/ffmpeg:7.1-alpine \
  -y -framerate "${framerate}" -i "/frames/frame_%03d.png" \
  -c:v libx264 -pix_fmt yuv420p -movflags +faststart \
  -t "${duration}" "/out/$(basename "${OUTPUT}")"

echo "Vídeo gerado: ${OUTPUT} (${frame_count} cenas, ~${duration}s, sem áudio)"
