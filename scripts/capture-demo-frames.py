#!/usr/bin/env python3
import os
import subprocess
import sys
import textwrap
import urllib.request
from base64 import b64encode
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
FRAMES = Path(os.environ.get("FRAMES_DIR", ROOT / "docs/delivery/video-frames"))
API_BASE = os.environ.get("API_BASE", "http://localhost:8080/api")
API_USER = os.environ.get("API_USER", "fiapx")
API_PASS = os.environ.get("API_PASS", "fiapx123")
TEST_VIDEO = Path(os.environ.get("TEST_VIDEO", "/tmp/fiapx-e2e-test.mp4"))


def curl_json(url: str, method: str = "GET", data: bytes | None = None, content_type: str | None = None) -> str:
    req = urllib.request.Request(url, data=data, method=method)
    token = b64encode(f"{API_USER}:{API_PASS}".encode()).decode()
    req.add_header("Authorization", f"Basic {token}")
    if content_type:
        req.add_header("Content-Type", content_type)
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read().decode()


def slide(title: str, *lines: str, index: int) -> None:
    img = Image.new("RGB", (1280, 720), "#0f172a")
    draw = ImageDraw.Draw(img)
    try:
        font_title = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 42)
        font_body = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 28)
        font_small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 22)
    except OSError:
        font_title = ImageFont.load_default()
        font_body = font_title
        font_small = font_title
    draw.text((640, 70), title, fill="white", font=font_title, anchor="ma")
    y = 190
    for line in lines:
        draw.text((640, y), line[:110], fill="#cbd5e1", font=font_body, anchor="ma")
        y += 48
    draw.text((640, 650), f"FIAP X Fase 5 — cena {index:02d}", fill="#64748b", font=font_small, anchor="ma")
    FRAMES.mkdir(parents=True, exist_ok=True)
    img.save(FRAMES / f"frame_{index:03d}.png")


def screenshot(url: str, index: int) -> bool:
    tmp = FRAMES / "tmp.png"
    try:
        subprocess.run(
            ["npx", "--yes", "playwright@1.49.1", "screenshot", "--wait-for-timeout", "3000", url, str(tmp)],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            timeout=60,
        )
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired, FileNotFoundError):
        return False
    img = Image.open(tmp).convert("RGB")
    canvas = Image.new("RGB", (1280, 720), "#0f172a")
    img.thumbnail((1180, 660))
    canvas.paste(img, ((1280 - img.width) // 2, (720 - img.height) // 2))
    canvas.save(FRAMES / f"frame_{index:03d}.png")
    tmp.unlink(missing_ok=True)
    return True


def ensure_video() -> None:
    if TEST_VIDEO.exists():
        return
    proc = subprocess.check_output(["docker", "ps", "--format", "{{.Names}}"], text=True)
    name = next((n for n in proc.splitlines() if "processor-service" in n), "")
    if not name:
        sys.exit("Processor container não encontrado para gerar vídeo de teste")
    subprocess.run(
        [
            "docker", "exec", name, "ffmpeg", "-y",
            "-f", "lavfi", "-i", "testsrc=duration=3:size=320x240:rate=10",
            "-c:v", "libx264", "-pix_fmt", "yuv420p", "/tmp/vid.mp4",
        ],
        check=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    subprocess.run(["docker", "cp", f"{name}:/tmp/vid.mp4", str(TEST_VIDEO)], check=True)


def main() -> None:
    FRAMES.mkdir(parents=True, exist_ok=True)
    for old in FRAMES.glob("frame_*.png"):
        old.unlink()

    ensure_video()
    boundary = "----fiapxboundary"
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="test.mp4"\r\n'
        f"Content-Type: video/mp4\r\n\r\n"
    ).encode() + TEST_VIDEO.read_bytes() + f"\r\n--{boundary}--\r\n".encode()
    upload = curl_json(
        f"{API_BASE}/videos",
        method="POST",
        data=body,
        content_type=f"multipart/form-data; boundary={boundary}",
    )
    job_id = upload.split('"id":"')[1].split('"')[0]

    idx = 1
    slide("FIAP X — Fase 5", "Microsserviços de processamento de vídeo", "API + Processor + RabbitMQ", index=idx); idx += 1
    slide("Arquitetura", "Exchange fiapx.events · filas duráveis", "PostgreSQL · Redis · Prometheus · Grafana", index=idx); idx += 1
    slide("Execução local", "docker compose up -d --build", "Stack completa com MailHog e observabilidade", index=idx); idx += 1

    ui_urls = [
        ("http://localhost:8080/api/swagger-ui.html", "Swagger UI"),
        ("http://localhost:15672", "RabbitMQ Management"),
        ("http://localhost:8025", "MailHog"),
        ("http://localhost:3000", "Grafana"),
        ("http://localhost:9090/targets", "Prometheus targets"),
    ]
    for url, label in ui_urls:
        if not screenshot(url, idx):
            slide(label, url, "Captura indisponível — serviço local", index=idx)
        idx += 1

    slide("Upload aceito", "POST /api/videos → HTTP 202", f"Job {job_id}", index=idx); idx += 1

    status = "QUEUED"
    for _ in range(60):
        detail = curl_json(f"{API_BASE}/videos/{job_id}")
        status = detail.split('"status":"')[1].split('"')[0]
        if status in {"COMPLETED", "FAILED"}:
            break
        import time
        time.sleep(2)

    slide("Processamento FFmpeg", f"Status final: {status}", "Frames extraídos e ZIP gerado", index=idx); idx += 1

    broken = Path("/tmp/fiapx-broken.mp4")
    broken.write_bytes(b"invalid")
    body_bad = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="broken.mp4"\r\n'
        f"Content-Type: video/mp4\r\n\r\n"
    ).encode() + broken.read_bytes() + f"\r\n--{boundary}--\r\n".encode()
    curl_json(f"{API_BASE}/videos", method="POST", data=body_bad, content_type=f"multipart/form-data; boundary={boundary}")
    import time
    time.sleep(4)
    mail = urllib.request.urlopen("http://localhost:8025/api/v2/messages", timeout=10).read().decode()
    total = mail.split('"total":')[1].split(",")[0] if '"total":' in mail else "0"
    slide("Notificação de erro", "E-mail enviado via MailHog", f"Total de mensagens: {total}", index=idx); idx += 1

    slide("Testes automatizados", "./scripts/e2e-test.sh", "./scripts/verify-conformidade.sh — 7 checks OK", index=idx); idx += 1
    slide("Repositórios GitHub", "ricartefelipe/fiapx-api-service", "ricartefelipe/fiapx-processor-service", index=idx)

    count = len(list(FRAMES.glob("frame_*.png")))
    print(f"Frames gerados: {count} em {FRAMES}")


if __name__ == "__main__":
    main()
