# Matriz de conformidade — Fase 5 Hackathon FIAP X

Documento de referência: **POSTECH - SOAT - Fase 5 - Hacka.pdf** (5 páginas).

Legenda de status:
- **Conforme** — requisito atendido com evidência verificável
- **Parcial** — atendido com ressalva documentada
- **N/A** — item recomendado, não obrigatório no PDF
- **Pendente** — depende de ação do aluno (ex.: link do vídeo)

---

## 1. Contexto e desafio (PDF p. 2)

| ID | Requisito (PDF) | Status | Evidência | Gap / observação |
|----|-----------------|--------|-----------|------------------|
| CTX-01 | Evoluir projeto base que extrai frames de vídeo e gera ZIP | Conforme | `fiapx-processor-service` — `FfmpegFrameExtractor`, `ZipArchiveService`, `VideoProcessingService` | Funcionalidade core preservada |
| CTX-02 | Aplicar desenho de arquitetura | Conforme | `docs/fase5/arquitetura.md`, diagrama Mermaid | — |
| CTX-03 | Desenvolvimento de microsserviços | Conforme | Dois repositórios: API + Processor | — |
| CTX-04 | Qualidade de software | Conforme | Testes JUnit, JaCoCo, CI GitHub Actions | — |
| CTX-05 | Mensageria | Conforme | RabbitMQ topic exchange `fiapx.events` | — |

---

## 2. Requisitos funcionais essenciais (PDF p. 3)

| ID | Requisito (PDF) | Status | Evidência | Gap / observação |
|----|-----------------|--------|-----------|------------------|
| RF-01 | Processar mais de um vídeo ao mesmo tempo | Conforme | Fila `video.processing`; listener com `concurrency: 3`, `max-concurrency: 10` em `fiapx-processor-service/src/main/resources/application.yml` | Demonstrar no vídeo com 2+ uploads |
| RF-02 | Em picos, o sistema não deve perder requisição | Conforme | Filas duráveis (`QueueBuilder.durable`); API retorna `202 Accepted`; persistência antes de publicar evento | — |
| RF-03 | Sistema protegido por usuário e senha | Conforme | Spring Security HTTP Basic Auth; usuários em PostgreSQL (`users`) | Credenciais demo: `fiapx` / `fiapx123` |
| RF-04 | Listagem de status dos vídeos de um usuário | Conforme | `GET /api/videos` e `GET /api/videos/{id}`; filtro por `userId` autenticado | Status: `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED` |
| RF-05 | Em caso de erro, usuário pode ser notificado (e-mail ou outro meio) | Conforme | `CompositeNotificationService`: log estruturado + e-mail via MailHog (`EmailNotificationService`); `errorMessage` persistido no job | MailHog UI: http://localhost:8025 |

---

## 3. Requisitos técnicos (PDF p. 3)

| ID | Requisito (PDF) | Status | Evidência | Gap / observação |
|----|-----------------|--------|-----------|------------------|
| RT-01 | Persistir os dados | Conforme | PostgreSQL + JPA + Liquibase (`src/main/resources/db/changelog/changelog-master.yaml`) | Tabelas `users`, `video_jobs` |
| RT-02 | Arquitetura escalável | Conforme | API stateless; processor horizontal via fila; documentação em `arquitetura.md` § Escalabilidade | Docker Compose local; K8s não exigido |
| RT-03 | Versionado no GitHub | Conforme | https://github.com/ricartefelipe/fiapx-api-service e https://github.com/ricartefelipe/fiapx-processor-service | GitFlow documentado |
| RT-04 | Testes que garantam qualidade | Conforme | `./mvnw -Pci clean verify` — 8 testes API, 4 testes Processor (sessão 24/07/2026) | JaCoCo ~54% API, ~56% Processor |
| RT-05 | CI/CD da aplicação | Conforme | `.github/workflows/ci.yml` — build, testes, push GHCR | `enforce-gitflow.yml` bloqueia PR feature→main |

---

## 4. Stack tecnológica recomendada (PDF p. 3)

| ID | Item recomendado (PDF) | Status | Evidência | Gap / observação |
|----|------------------------|--------|-----------|------------------|
| ST-01 | Docker + Kubernetes ou Docker Compose | Conforme | `docker-compose.yml` sobe stack completa | K8s: N/A (Compose atende) |
| ST-02 | RabbitMQ, Kafka ou similar | Conforme | RabbitMQ 4 + Spring AMQP | — |
| ST-03 | PostgreSQL + Redis (cache) | Conforme | PostgreSQL + Redis cache em `VideoJobService.listJobs` (`@Cacheable`, TTL 5 min) | — |
| ST-04 | Prometheus + Grafana, ELK ou similar | Conforme | Prometheus scrape + Grafana dashboard `FIAP X — Visão geral` provisionado | ELK: N/A |
| ST-05 | GitHub Actions ou similar | Conforme | Workflows CI em ambos repositórios | — |

---

## 5. Entregáveis (PDF p. 4)

| ID | Entregável (PDF) | Status | Evidência | Gap / observação |
|----|------------------|--------|-----------|------------------|
| E-01 | Documentação da arquitetura proposta | Conforme | `docs/fase5/arquitetura.md` + `docs/delivery/entrega-portal-fase5.md` | — |
| E-02 | Script de criação do banco ou outros recursos | Conforme | Liquibase `changelog-master.yaml`; complemento manual `scripts/db/seed-demo-user.sql` | Liquibase executa no startup |
| E-03 | Link do GitHub do(s) projeto(s) | Conforme | Repositórios listados na entrega | — |
| E-04 | Vídeo ≤ 10 min (documentação, arquitetura, projeto funcionando) | Pendente | Roteiro em `docs/delivery/entrega-portal-fase5.md` § 9 | Aluno grava e envia link no portal |

---

## 6. Eventos RabbitMQ (implementação)

| Routing Key | Produtor | Consumidor | Fila |
|-------------|----------|------------|------|
| `video.requested` | API | Processor | `video.processing` |
| `video.processing` | Processor | API | `video.api.processing` |
| `video.completed` | Processor | API | `video.api.completed` |
| `video.failed` | Processor | API | `video.api.failed` |

---

## 7. Correções aplicadas nesta auditoria (24/07/2026)

| Correção | Motivo |
|----------|--------|
| Evento `video.processing` + status `PROCESSING` | PDF/roteiro de demo citam acompanhamento de status; estado existia no domínio mas nunca era atribuído |
| Cache Redis em `listJobs` | Stack recomendada cita Redis; estava provisionado mas sem uso no código |
| E-mail de falha via MailHog | RF-05 cita e-mail; log sozinho era frágil para avaliação |
| Dashboard Grafana provisionado | ST-04 recomenda Prometheus + Grafana com uso demonstrável |
| Esta matriz de conformidade | Entregável explícito solicitado na auditoria |

---

## 8. Itens fora do escopo do PDF (não inventar requisitos)

- Kubernetes em produção
- SonarCloud com token no CI (propriedades existem; análise automática opcional)
- JWT/OAuth (PDF exige apenas usuário e senha)
- Frontend web dedicado

---

*Última revisão: 24/07/2026 — branch `main`*
