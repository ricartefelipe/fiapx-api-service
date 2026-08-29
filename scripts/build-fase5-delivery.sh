#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DELIVERY="$ROOT/docs/delivery"
WORK="$(mktemp -d)"
PARENT="$(dirname "$ROOT")"
API_SRC="$ROOT"
PROCESSOR_SRC="$PARENT/fiapx-processor-service"
OUTPUT="$DELIVERY/fase5.zip"

RSYNC_EXCLUDES=(
  --exclude '.git'
  --exclude 'target'
  --exclude '.idea'
  --exclude '.vscode'
)

API_EXCLUDES=(
  "${RSYNC_EXCLUDES[@]}"
  --exclude 'docs/delivery/video-frames'
  --exclude 'docs/delivery/fase5'
  --exclude 'docs/delivery/fase5.zip'
  --exclude 'docs/delivery/fase5-microsservicos.zip'
  --exclude 'docs/delivery/fiapx-api-service.zip'
  --exclude 'docs/delivery/fiapx-processor-service.zip'
)

cleanup() {
  rm -rf "$WORK"
}
trap cleanup EXIT

if [[ ! -d "$PROCESSOR_SRC" ]]; then
  echo "Repositório processor não encontrado: $PROCESSOR_SRC" >&2
  exit 1
fi

if [[ ! -f "$DELIVERY/fiapx-fase5-demo.mp4" ]]; then
  echo "ERRO: vídeo obrigatório ausente: $DELIVERY/fiapx-fase5-demo.mp4" >&2
  exit 1
fi

if [[ ! -f "$DELIVERY/entrega-portal-fase5.md" ]]; then
  echo "ERRO: markdown de entrega ausente: $DELIVERY/entrega-portal-fase5.md" >&2
  exit 1
fi

echo ">> Gerando PDF..."
(
  cd "$DELIVERY"
  npx --yes md-to-pdf entrega-portal-fase5.md --stylesheet pdf-print.css
)

echo ">> Montando fase5.zip (PDF + vídeo + código dos dois microsserviços)..."
PACK="$WORK/portal-pack"
mkdir -p "$PACK/fiapx-api-service" "$PACK/fiapx-processor-service"

rsync -a --delete "${API_EXCLUDES[@]}" "$API_SRC/" "$PACK/fiapx-api-service/"
rsync -a --delete "${RSYNC_EXCLUDES[@]}" "$PROCESSOR_SRC/" "$PACK/fiapx-processor-service/"

cp "$DELIVERY/entrega-portal-fase5.pdf" "$DELIVERY/fiapx-fase5-demo.mp4" "$PACK/"

rm -f "$OUTPUT"
(
  cd "$PACK"
  zip -qr "$OUTPUT" .
)

echo ">> Validando pacote do portal..."
LIST=$(unzip -l "$OUTPUT")
echo "$LIST" | rg -q "entrega-portal-fase5.pdf" || { echo "ERRO: PDF ausente no fase5.zip"; exit 1; }
echo "$LIST" | rg -q "fiapx-fase5-demo.mp4" || { echo "ERRO: vídeo ausente no fase5.zip"; exit 1; }
echo "$LIST" | rg -q "VideoStatusListenerIntegrationTest" || { echo "ERRO: código desatualizado no pacote"; exit 1; }
echo "$LIST" | rg -q "fiapx-processor-service/" || { echo "ERRO: processor ausente no pacote"; exit 1; }

rm -f "$DELIVERY/fase5-microsservicos.zip" "$DELIVERY/fiapx-api-service.zip" "$DELIVERY/fiapx-processor-service.zip"
rm -rf "$DELIVERY/fase5"

echo ">> Conteúdo final:"
unzip -l "$OUTPUT" | sed -n '1,35p'
ls -lh "$OUTPUT" "$DELIVERY/entrega-portal-fase5.pdf" "$DELIVERY/fiapx-fase5-demo.mp4"
echo ""
echo "OK: $OUTPUT pronto para upload no portal (não versionar no git)"
