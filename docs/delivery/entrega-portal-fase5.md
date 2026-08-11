# Hackathon FIAP X — Fase 5 — Entrega no portal do aluno (PDF)

**Grupo:** Oficina Turbo (106)  
**Aluno:** Felipe Ricarte Magalhães  

---

## 1. Dois repositórios de microsserviços (Fase 5 — FIAP X)

| # | Repositório | Responsabilidade | Porta |
|---|-------------|------------------|-------|
| 1 | https://github.com/ricartefelipe/fiapx-api-service | API REST — autenticação, upload, listagem de status, download do ZIP | 8080 |
| 2 | https://github.com/ricartefelipe/fiapx-processor-service | Worker assíncrono — extração de frames com FFmpeg e geração do ZIP | 8081 |

**Branches:** `main` (estável, CI verde), `develop` (integração).  
**Acesso ao avaliador SOAT:** usuário **`soat-architecture`** com leitura nos dois repositórios.

**Documentação técnica no repositório central:**  
https://github.com/ricartefelipe/fiapx-api-service/tree/main/docs/fase5  

---

## 2. Arquitetura — Microsserviços e fluxo de processamento

### 2.1 Visão geral

```
                    ┌─────────────────────────────────────────┐
                    │         RabbitMQ (fiapx.events)          │
                    │      Topic Exchange — routing keys       │
                    └──────────────┬──────────────────────────┘
                                   │
         ┌─────────────────────────▼──────────────────────────┐
         │              fiapx-api-service                      │
         │   Spring Boot 4 · PostgreSQL · Redis · Liquibase   │
         │   HTTP Basic Auth · Porta 8080                      │
         └─────────────────────────┬──────────────────────────┘
                                   │ video.requested
                                   │
         ┌─────────────────────────▼──────────────────────────┐
         │           fiapx-processor-service                     │
         │   Spring Boot 4 · FFmpeg · volume compartilhado      │
         │   Porta 8081 (actuator)                               │
         └──────────────────────────────────────────────────────┘
```

**Infraestrutura compartilhada (Docker Compose):** PostgreSQL, RabbitMQ, Redis, Prometheus e Grafana sobem junto com os dois microsserviços.

### 2.2 Fluxo upload → fila → FFmpeg → ZIP → download

| Etapa | Componente | Ação |
|-------|------------|------|
| 1 | Cliente | `POST /api/videos` (multipart) com HTTP Basic Auth |
| 2 | API Service | Persiste `VideoJob` (status `QUEUED`), salva arquivo e publica `video.requested` |
| 3 | RabbitMQ | Entrega mensagem na fila `video.processing` |
| 4 | Processor Service | Publica `video.processing`, extrai frames com FFmpeg, gera ZIP no volume compartilhado |
| 5 | Processor Service | Publica `video.completed` ou `video.failed` |
| 6 | API Service | Atualiza status do job; em erro, notifica via log e e-mail (MailHog) |
| 7 | Cliente | `GET /api/videos` e `GET /api/videos/{id}`; download do ZIP quando `COMPLETED` |

### 2.3 Eventos RabbitMQ

| Exchange | Routing Key | Produtor | Consumidor |
|----------|-------------|----------|------------|
| fiapx.events | video.requested | API | Processor |
| fiapx.events | video.processing | Processor | API |
| fiapx.events | video.completed | Processor | API |
| fiapx.events | video.failed | Processor | API |

---

## 3. Stack técnica

| Componente | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.0.6 |
| API REST | Spring Web + Spring Security (HTTP Basic Auth) |
| Mensageria | RabbitMQ + Spring AMQP |
| Banco relacional | PostgreSQL + Liquibase |
| Cache | Redis (listagem de jobs por usuário, TTL 5 min) |
| Processamento de vídeo | FFmpeg (via ProcessBuilder no processor) |
| Documentação API | SpringDoc OpenAPI / Swagger UI |
| Testes | JUnit 5 + Mockito |
| Cobertura | JaCoCo |
| CI/CD | GitHub Actions |
| Containers | Docker + Docker Compose |
| Observabilidade | Micrometer + Prometheus + Grafana |

---

## 4. Requisitos do hackathon — checklist

Matriz detalhada (requisito × status × evidência × gap): [`docs/fase5/matriz-conformidade.md`](../fase5/matriz-conformidade.md)

| Requisito | Status | Evidência / gap |
|---|---|---|
| Processar mais de um vídeo simultaneamente | **Parcial** | Fila + worker assíncrono configurados; teste paralelo só em `scripts/verify-conformidade.sh` (local) |
| Não perder requisições em picos de carga | Conforme | Filas duráveis; API retorna 202 Accepted |
| Proteção por usuário e senha | Conforme | HTTP Basic Auth (`fiapx` / `fiapx123`) |
| Listagem de status dos vídeos por usuário | Conforme | `GET /api/videos` e `GET /api/videos/{id}` |
| Notificação em caso de erro | **Parcial** | Código + MailHog; **GitHub `main` ainda com `spring.mail` quebrado** até push |
| Persistência de dados | Conforme | PostgreSQL (`users`, `video_jobs`) |
| Arquitetura escalável | Conforme | API e processor desacoplados via fila |
| Versionamento no GitHub | Conforme | Dois repositórios com GitFlow |
| Testes automatizados | **Parcial** | `./mvnw -Pci clean verify`; JaCoCo ~54%; sem Testcontainers/RabbitMQ real |
| CI/CD | **Parcial** | CI (build, testes, imagem GHCR); **sem pipeline de deploy** |
| Docker Compose funcional | Conforme | `docker compose up -d --build` |
| Teste E2E (fluxo feliz) | Conforme | `./scripts/e2e-test.sh` |
| Verificação ampliada (paralelo, falha, mail, métricas) | **Parcial** | `./scripts/verify-conformidade.sh` — local; não no GitHub até push |
| Vídeo de apresentação (≤ 10 min) | **Pendente** | Obrigatório no PDF; ainda não gravado/enviado |
| Evolução do projeto base FIAP | **Parcial** | Reimplementação equivalente, não fork do repo base |

---

## 5. Cobertura de testes e CI

### 5.1 JaCoCo (local, perfil `ci`)

| Serviço | Cobertura de instruções | Testes |
|---|---|---|
| fiapx-api-service | **53,6%** | Unitários (controller, service, security) + `@SpringBootTest` com H2 — **8 testes** |
| fiapx-processor-service | **56,2%** | Unitários (listener, ZIP, processing) + `@SpringBootTest` — **4 testes** |

> Cobertura medida com `./mvnw -Pci clean verify`. Testes usam H2 e mocks (sem Testcontainers). Camadas de integração (RabbitMQ listeners, FFmpeg real) concentram código ainda não coberto por testes unitários.

### 5.2 CI/CD — GitHub Actions

| Repositório | Workflow | Status em `main` |
|---|---|---|
| fiapx-api-service | `ci.yml` + `enforce-gitflow.yml` | ✅ Verde |
| fiapx-processor-service | `ci.yml` + `enforce-gitflow.yml` | ✅ Verde |

**Evidências:**  
- https://github.com/ricartefelipe/fiapx-api-service/actions  
- https://github.com/ricartefelipe/fiapx-processor-service/actions  

**Imagens Docker publicadas em:** `ghcr.io/ricartefelipe/fiapx-api-service` e `ghcr.io/ricartefelipe/fiapx-processor-service`

### 5.3 SonarCloud

| Item | Status |
|---|---|
| Propriedades no `pom.xml` | ✅ Configurado (`ricartefelipe_fiapx-api-service`, `ricartefelipe_fiapx-processor-service`) |
| Análise automática no CI | N/A — token SonarCloud ainda não integrado ao pipeline |
| Dashboards | N/A — aguardando primeira análise publicada |

---

## 6. Como executar

### 6.1 Pré-requisitos

- Docker e Docker Compose
- Repositórios `fiapx-api-service` e `fiapx-processor-service` no mesmo diretório pai
- Opcional: FFmpeg no host (para o script E2E gerar vídeo de teste)

### 6.2 Stack completa (recomendado)

```bash
cd fiapx-api-service
docker compose up -d --build
```

| Serviço | URL / Porta |
|---|---|
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| Processor (health) | http://localhost:8081/actuator/health |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) — dashboard **FIAP X — Visão geral** |
| MailHog (e-mail de falha) | http://localhost:8025 |

**Credenciais da API:** `fiapx` / `fiapx123`

### 6.3 Teste E2E automatizado (fluxo feliz)

```bash
cd fiapx-api-service
chmod +x scripts/e2e-test.sh
./scripts/e2e-test.sh
```

O script aguarda a API, envia upload, poll até `COMPLETED` e valida o download do ZIP.

### 6.4 Verificação de conformidade ampliada

```bash
cd fiapx-api-service
chmod +x scripts/verify-conformidade.sh
./scripts/verify-conformidade.sh
```

Valida: health da API (8080) e processor (8081), dois uploads paralelos, falha com arquivo inválido + e-mail no MailHog, targets Prometheus UP.

### 6.5 Testes unitários

```bash
./mvnw -Pci clean verify   # em cada repositório
```

---

## 7. GitFlow aplicado

Fluxo documentado em `docs/GITFLOW.md` (ambos os repositórios):

| Branch | Uso |
|---|---|
| `feature/*` | Novas funcionalidades → PR para `develop` |
| `fix/*` | Correções → PR para `develop` |
| `release/*` | Preparar versão → PR `develop` → `main` |
| `hotfix/*` | Correção urgente → PR para `main` + sync `develop` |

O workflow `enforce-gitflow.yml` **bloqueia** PR de `feature/*` direto para `main`.

**Release entregue:** PR #4 (`develop` → `main`) mergeado com CI verde em 24/07/2026.

---

## 8. Observabilidade

| Ferramenta | Acesso |
|---|---|
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |
| Métricas | `/actuator/prometheus` em API (8080) e Processor (8081) |

---

## 9. Vídeo demonstrativo (≤ 10 minutos, sem áudio)

**Arquivo local:** `docs/delivery/fiapx-fase5-demo.mp4` (~4 min 46 s, mudo)

**Como regenerar:**

```bash
docker compose up -d --build
python3 scripts/capture-demo-frames.py
SECONDS_PER_FRAME=22 ./scripts/generate-demo-video.sh
```

Conteúdo: arquitetura, capturas de Swagger/RabbitMQ/MailHog/Grafana/Prometheus, upload, processamento, erro + e-mail, testes e GitHub.

**Portal:** enviar o MP4 ou publicar no YouTube/Vimeo e colar o link no campo do aluno.

---

## 10. Pacote zipado (portal)

Arquivo **`fase5-microsservicos.zip`** enviado no portal contém:
- Código-fonte de `fiapx-api-service` (sem `target/`, sem `.git/`)
- Código-fonte de `fiapx-processor-service` (sem `target/`, sem `.git/`)
- Este PDF (`entrega-portal-fase5.pdf`)

Arquivo **`fase5.zip`** contém a pasta `fase5/` com o PDF e o ZIP de código.

---

*Entrega Fase 5 — Hackathon FIAP X — plataforma de processamento de vídeos em microsserviços com RabbitMQ, PostgreSQL e Docker Compose.*
