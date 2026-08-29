#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DELIVERY="$ROOT/docs/delivery"
WORK="$(mktemp -d)"
PARENT="$(dirname "$ROOT")"
API_SRC="$ROOT"
PROCESSOR_SRC="$PARENT/fiapx-processor-service"

cleanup() {
  rm -rf "$WORK"
}
trap cleanup EXIT

if [[ ! -d "$PROCESSOR_SRC" ]]; then
  echo "Repositório processor não encontrado: $PROCESSOR_SRC" >&2
  exit 1
fi

echo ">> Gerando PDF..."
(
  cd "$DELIVERY"
  npx --yes md-to-pdf entrega-portal-fase5.md --stylesheet pdf-print.css
)

if [[ ! -f "$DELIVERY/fiapx-fase5-demo.mp4" ]]; then
  echo "Vídeo demo ausente: $DELIVERY/fiapx-fase5-demo.mp4" >&2
  exit 1
fi

echo ">> Empacotando código dos microsserviços..."
STAGE="$WORK/fiapx-bundle"
mkdir -p "$STAGE"

rsync -a --delete \
  --exclude '.git' \
  --exclude 'target' \
  --exclude '.idea' \
  --exclude '.vscode' \
  --exclude 'docs/delivery/video-frames' \
  --exclude 'docs/delivery/fase5' \
  --exclude 'docs/delivery/fase5 (2)' \
  --exclude 'docs/delivery/fase5.zip' \
  --exclude 'docs/delivery/fase5-microsservicos.zip' \
  --exclude 'docs/delivery/fiapx-fase5-demo.mp4' \
  --exclude 'docs/delivery/entrega-portal-fase5.pdf' \
  "$API_SRC/" "$STAGE/fiapx-api-service/"

rsync -a --delete \
  --exclude '.git' \
  --exclude 'target' \
  --exclude '.idea' \
  --exclude '.vscode' \
  "$PROCESSOR_SRC/" "$STAGE/fiapx-processor-service/"

cp "$DELIVERY/entrega-portal-fase5.pdf" "$STAGE/"

(
  cd "$STAGE"
  zip -qr "$DELIVERY/fase5-microsservicos.zip" fiapx-api-service fiapx-processor-service entrega-portal-fase5.pdf
)

echo ">> Montando fase5.zip..."
FASE5_DIR="$WORK/fase5-pack"
mkdir -p "$FASE5_DIR/fase5"
cp "$DELIVERY/entrega-portal-fase5.pdf" "$DELIVERY/fiapx-fase5-demo.mp4" "$DELIVERY/fase5-microsservicos.zip" "$FASE5_DIR/fase5/"
cp "$DELIVERY/entrega-portal-fase5.pdf" "$DELIVERY/fiapx-fase5-demo.mp4" "$DELIVERY/fase5-microsservicos.zip" "$FASE5_DIR/"

(
  cd "$FASE5_DIR"
  zip -qr "$DELIVERY/fase5.zip" .
)

mkdir -p "$DELIVERY/fase5"
cp "$DELIVERY/entrega-portal-fase5.pdf" "$DELIVERY/fiapx-fase5-demo.mp4" "$DELIVERY/fase5-microsservicos.zip" "$DELIVERY/fase5/"

echo ">> Verificando conteúdo..."
unzip -l "$DELIVERY/fase5-microsservicos.zip" | rg "VideoStatusListenerIntegrationTest|matriz-conformidade|fake-ffmpeg" | head -5
ls -lh "$DELIVERY/fase5.zip" "$DELIVERY/fase5-microsservicos.zip" "$DELIVERY/entrega-portal-fase5.pdf"
echo "OK: pacote de entrega regenerado em $DELIVERY"
