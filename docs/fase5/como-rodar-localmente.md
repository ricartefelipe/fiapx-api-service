# Execução local — Fase 5 FIAP X

## Pré-requisitos

- Docker e Docker Compose
- Repositórios `fiapx-api-service` e `fiapx-processor-service` lado a lado no mesmo diretório pai
- Opcional: Java 21 e Maven para desenvolvimento fora de container
- Opcional: FFmpeg no host para gerar vídeo de teste no script E2E

## Opção A — Stack completa com Docker Compose (recomendado)

Um comando sobe infraestrutura, API e processor com volume compartilhado:

```bash
cd fiapx-api-service
docker compose up -d --build
```

Aguarde os serviços ficarem healthy:

```bash
docker compose ps
```

| Serviço            | URL / Porta                          |
|--------------------|--------------------------------------|
| API                | http://localhost:8080/api            |
| Swagger UI         | http://localhost:8080/api/swagger-ui.html |
| Processor          | http://localhost:8081/actuator/health |
| PostgreSQL         | localhost:5432                       |
| RabbitMQ           | localhost:5672 (UI: 15672)           |
| Redis              | localhost:6379                       |
| Prometheus         | localhost:9090                       |
| Grafana            | localhost:3000 (admin/admin)         |
| MailHog            | localhost:8025 (UI) / 1025 (SMTP)    |

Credenciais da API: `fiapx` / `fiapx123`

### Teste E2E automatizado (fluxo feliz)

```bash
chmod +x scripts/e2e-test.sh
./scripts/e2e-test.sh
```

O script gera um vídeo curto com ffmpeg (se necessário), envia upload, aguarda `COMPLETED` e valida o download do ZIP.

### Verificação de conformidade ampliada

Requer stack Docker Compose em execução (`docker compose up -d --build`).

```bash
chmod +x scripts/verify-conformidade.sh
./scripts/verify-conformidade.sh
```

| Verificação | Endpoint / evidência |
|-------------|------------------------|
| Health API | http://localhost:8080/api/actuator/health |
| Health Processor | http://localhost:8081/actuator/health |
| Upload paralelo (2 jobs) | `POST /api/videos` × 2 em paralelo |
| Falha + e-mail | arquivo inválido → MailHog http://localhost:8025 |
| Prometheus targets | http://localhost:9090/targets (fiapx-api, fiapx-processor UP) |

### Teste E2E manual

```bash
ffmpeg -y -f lavfi -i testsrc=duration=3:size=320x240:rate=10 \
  -f lavfi -i sine=frequency=440:duration=3 \
  -c:v libx264 -pix_fmt yuv420p -c:a aac /tmp/test.mp4

curl -u fiapx:fiapx123 -F "file=@/tmp/test.mp4" http://localhost:8080/api/videos

curl -u fiapx:fiapx123 http://localhost:8080/api/videos/{jobId}

curl -u fiapx:fiapx123 -OJ http://localhost:8080/api/videos/{jobId}/download
```

### Parar a stack

```bash
docker compose down
```

Para remover volumes (dados do PostgreSQL e arquivos de vídeo):

```bash
docker compose down -v
```

## Opção B — Infraestrutura em Docker + apps no host

### 1. Subir infraestrutura

```bash
cd fiapx-api-service
docker compose -f docker-compose.infra.yml up -d
```

### 2. Preparar diretórios compartilhados

```bash
mkdir -p /tmp/fiapx/uploads /tmp/fiapx/output
export UPLOAD_DIR=/tmp/fiapx/uploads
export OUTPUT_DIR=/tmp/fiapx/output
```

### 3. Iniciar API

```bash
cd fiapx-api-service
./mvnw spring-boot:run
```

### 4. Iniciar processor (porta 8081)

```bash
cd fiapx-processor-service
./mvnw spring-boot:run
```

Health local: http://localhost:8081/actuator/health

## Fluxo de eventos

1. API recebe upload, persiste job `QUEUED` e publica `video.requested`
2. Processor consome, extrai frames com FFmpeg, gera ZIP e publica `video.completed` ou `video.failed`
3. API atualiza status (`PROCESSING`, `COMPLETED`, `FAILED`) e notifica falha via log + e-mail (MailHog)

## Testes unitários e integração

```bash
./mvnw -Pci clean verify
```

Em ambos os repositórios.
