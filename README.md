# fiapx-api-service

**Responsabilidade:** API REST — autenticação, upload de vídeos, listagem de status e download do ZIP.

Hackathon SOAT — Fase 5 | FIAP X | Microsserviço 1 de 2

---

## Stack

| Componente | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4 |
| Banco | PostgreSQL |
| Cache | Redis |
| Migrations | Liquibase |
| Mensageria | RabbitMQ |
| Segurança | HTTP Basic Auth |
| Observabilidade | Micrometer + Prometheus |
| Testes | JUnit 5 + JaCoCo |
| CI/CD | GitHub Actions |

---

## Repositórios relacionados

| Repositório | Responsabilidade |
|---|---|
| [fiapx-api-service](https://github.com/ricartefelipe/fiapx-api-service) | API, auth, upload, status |
| [fiapx-processor-service](https://github.com/ricartefelipe/fiapx-processor-service) | Processamento assíncrono de vídeos |

---

## Como rodar localmente

### Stack completa (recomendado)

Requer o repositório `fiapx-processor-service` no mesmo diretório pai:

```bash
docker compose up -d --build
```

Swagger UI: `http://localhost:8080/api/swagger-ui.html`

Credenciais: `fiapx` / `fiapx123`

Teste E2E: `./scripts/e2e-test.sh`

Documentação detalhada: `docs/fase5/como-rodar-localmente.md`

### Apenas infraestrutura + app no host

```bash
docker compose -f docker-compose.infra.yml up -d
./mvnw spring-boot:run
```

---

## Testes

```bash
./mvnw -Pci clean verify
```

---

## CI/CD

- Pipeline em `.github/workflows/ci.yml`
- GitFlow documentado em `docs/GITFLOW.md`
- PR de feature → `develop`; release → `main`
