# Execução local — Fase 5 FIAP X

## Pré-requisitos

- Java 21
- Maven Wrapper (`./mvnw`)
- Docker e Docker Compose
- FFmpeg instalado no host (para o processor fora de container)

## 1. Subir infraestrutura

```bash
cd fiapx-api-service
docker compose -f docker-compose.infra.yml up -d
```

Serviços disponíveis:

| Serviço    | URL                         |
|------------|-----------------------------|
| PostgreSQL | localhost:5432              |
| RabbitMQ   | localhost:5672 (UI: 15672)  |
| Redis      | localhost:6379              |
| Prometheus | localhost:9090              |
| Grafana    | localhost:3000              |

## 2. Preparar diretórios compartilhados

```bash
mkdir -p /tmp/fiapx/uploads /tmp/fiapx/output
```

API e processor devem usar os mesmos caminhos:

```bash
export UPLOAD_DIR=/tmp/fiapx/uploads
export OUTPUT_DIR=/tmp/fiapx/output
```

## 3. Iniciar API

```bash
cd fiapx-api-service
./mvnw spring-boot:run
```

Credenciais demo: `fiapx` / `fiapx123` (via `APP_USER` / `APP_PASSWORD`).

## 4. Iniciar processor

```bash
cd fiapx-processor-service
./mvnw spring-boot:run
```

## 5. Enviar vídeo

```bash
curl -u fiapx:fiapx123 \
  -F "file=@/caminho/para/video.mp4" \
  http://localhost:8080/api/videos
```

## 6. Consultar status

```bash
curl -u fiapx:fiapx123 http://localhost:8080/api/videos
curl -u fiapx:fiapx123 http://localhost:8080/api/videos/{jobId}
```

## 7. Baixar ZIP (quando COMPLETED)

```bash
curl -u fiapx:fiapx123 -OJ \
  http://localhost:8080/api/videos/{jobId}/download
```

## Fluxo de eventos

1. API recebe upload, persiste job `QUEUED` e publica `video.requested`
2. Processor consome, extrai frames com FFmpeg, gera ZIP e publica `video.completed` ou `video.failed`
3. API atualiza status e registra notificação de erro via log

## Testes

```bash
./mvnw -Pci clean verify
```

Em ambos os repositórios.
