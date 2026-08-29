# Matriz de conformidade — Fase 5 Hackathon FIAP X

Documento de referência: **POSTECH - SOAT - Fase 5 - Hacka.pdf** (5 páginas).

Legenda de status:
- **Conforme** — requisito atendido com evidência verificável
- **Parcial** — atendido com ressalva documentada
- **N/A** — item recomendado, não obrigatório no PDF
- **Pendente** — depende de ação externa ainda não concluída

> **Estado do repositório remoto (GitHub `main`):** API e Processor com testes de integração Testcontainers, gate JaCoCo ≥70% no CI e correções de conformidade mergeadas via GitFlow.

---

## 1. Contexto e desafio (PDF p. 2)

| ID | Requisito (PDF) | Status | Evidência | Gap / observação |
|----|-----------------|--------|-----------|------------------|
| CTX-01 | Evoluir projeto base que extrai frames de vídeo e gera ZIP | **Parcial** | `fiapx-processor-service` — `FfmpegFrameExtractor`, `ZipArchiveService`, `VideoProcessingService` | Funcionalidade equivalente reimplementada; **não é fork/evolução direta** do projeto base FIAP |
| CTX-02 | Aplicar desenho de arquitetura | Conforme | `docs/fase5/arquitetura.md`, diagrama Mermaid | — |
| CTX-03 | Desenvolvimento de microsserviços | Conforme | Dois repositórios: API + Processor | — |
| CTX-04 | Qualidade de software | **Conforme** | Testes unitários + integração (Testcontainers), JaCoCo ≥70%, CI verde | — |
| CTX-05 | Mensageria | Conforme | RabbitMQ topic exchange `fiapx.events` | — |

---

## 2. Requisitos funcionais essenciais (PDF p. 3)

| ID | Requisito (PDF) | Status | Evidência | Gap / observação |
|----|-----------------|--------|-----------|------------------|
| RF-01 | Processar mais de um vídeo ao mesmo tempo | **Conforme** | Fila `video.processing`; listener `concurrency: 3`, `max-concurrency: 10`; `scripts/verify-conformidade.sh` (2 uploads paralelos) | — |
| RF-02 | Em picos, o sistema não deve perder requisição | Conforme | Filas duráveis; API retorna `202 Accepted`; persistência antes de publicar evento | — |
| RF-03 | Sistema protegido por usuário e senha | **Conforme** | Spring Security HTTP Basic Auth; testes de 401 e isolamento por usuário | Credenciais demo: `fiapx` / `fiapx123` |
| RF-04 | Listagem de status dos vídeos de um usuário | Conforme | `GET /api/videos` e `GET /api/videos/{id}`; filtro por `userId` autenticado | Status: `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED` |
| RF-05 | Em caso de erro, usuário pode ser notificado (e-mail ou outro meio) | **Conforme** | `CompositeNotificationService` + `EmailNotificationService`; MailHog :8025; teste integração com GreenMail | — |

---

## 3. Requisitos técnicos (PDF p. 3)

| ID | Requisito (PDF) | Status | Evidência | Gap / observação |
|----|-----------------|--------|-----------|------------------|
| RT-01 | Persistir os dados | Conforme | PostgreSQL + JPA + Liquibase | Tabelas `users`, `video_jobs` |
| RT-02 | Arquitetura escalável | Conforme | API stateless; processor horizontal via fila; `arquitetura.md` § Escalabilidade | Docker Compose local; K8s não exigido |
| RT-03 | Versionado no GitHub | Conforme | https://github.com/ricartefelipe/fiapx-api-service e https://github.com/ricartefelipe/fiapx-processor-service | GitFlow documentado |
| RT-04 | Testes que garantem qualidade | **Conforme** | `./mvnw -Pci clean verify` — 29 testes API, 10 testes Processor; Testcontainers (PostgreSQL + RabbitMQ); gate JaCoCo ≥70% | — |
| RT-05 | CI/CD da aplicação | **Parcial** | `.github/workflows/ci.yml` — build, testes, push GHCR | **CI sim; CD/deploy em ambiente alvo não implementado** |

---

## 4. Stack tecnológica recomendada (PDF p. 3)

| ID | Item recomendado (PDF) | Status | Evidência | Gap / observação |
|----|------------------------|--------|-----------|------------------|
| ST-01 | Docker + Kubernetes ou Docker Compose | Conforme | `docker-compose.yml` sobe stack completa | K8s: N/A (Compose atende) |
| ST-02 | RabbitMQ, Kafka ou similar | Conforme | RabbitMQ 4 + Spring AMQP | — |
| ST-03 | PostgreSQL + Redis (cache) | Conforme | PostgreSQL + Redis cache em `VideoJobService.listJobs` | — |
| ST-04 | Prometheus + Grafana, ELK ou similar | **Conforme** | Prometheus scrape + Grafana provisionado; `scripts/verify-conformidade.sh` valida targets UP | ELK: N/A |
| ST-05 | GitHub Actions ou similar | Conforme | Workflows CI em ambos repositórios | — |

---

## 5. Entregáveis (PDF p. 4)

| ID | Entregável (PDF) | Status | Evidência | Gap / observação |
|----|------------------|--------|-----------|------------------|
| E-01 | Documentação da arquitetura proposta | Conforme | `docs/fase5/arquitetura.md` + `docs/delivery/entrega-portal-fase5.md` | — |
| E-02 | Script de criação do banco ou outros recursos | Conforme | Liquibase `changelog-master.yaml`; `scripts/db/seed-demo-user.sql` | Liquibase executa no startup |
| E-03 | Link do GitHub do(s) projeto(s) | Conforme | Repositórios listados na entrega | — |
| E-04 | Vídeo ≤ 10 min (documentação, arquitetura, projeto funcionando) | **Conforme** | `fiapx-fase5-demo.mp4` no `fase5.zip` (~4m46s, sem áudio) | — |

---

## 6. Eventos RabbitMQ (implementação)

| Routing Key | Produtor | Consumidor | Fila |
|-------------|----------|------------|------|
| `video.requested` | API | Processor | `video.processing` |
| `video.processing` | Processor | API | `video.api.processing` |
| `video.completed` | Processor | API | `video.api.completed` |
| `video.failed` | Processor | API | `video.api.failed` |

---

## 7. Scripts de verificação

| Script | O que valida |
|--------|--------------|
| `scripts/e2e-test.sh` | Upload → COMPLETED → download ZIP |
| `scripts/verify-conformidade.sh` | Health API+processor, 2 uploads paralelos, falha + e-mail MailHog, targets Prometheus UP |

Pré-requisito: `docker compose up -d --build` com stack healthy.

---

## 8. Cobertura de testes automatizados

| Repositório | Testes | Integração | JaCoCo (instruções) |
|-------------|--------|------------|---------------------|
| `fiapx-api-service` | 29 | PostgreSQL + RabbitMQ + GreenMail (e-mail) | ≥70% (gate no CI) |
| `fiapx-processor-service` | 10 | RabbitMQ + script FFmpeg simulado | ≥70% (gate no CI) |

Classes cobertas por integração: `VideoStatusListener`, `VideoEventPublisher`, fluxo `video.requested` → `processing` → `completed`/`failed`, segurança HTTP Basic, notificação por e-mail.

---

## 9. Itens fora do escopo do PDF (não inventar requisitos)

- Kubernetes em produção (sem manifests no repositório)
- SonarCloud com token no CI (propriedades existem; análise automática opcional)
- JWT/OAuth (PDF exige apenas usuário e senha)
- Frontend web dedicado (cliente HTTP: curl, Swagger UI, Postman)

---

*Última revisão: 29/08/2026 — branch `feature/testes-integracao-qualidade`*
