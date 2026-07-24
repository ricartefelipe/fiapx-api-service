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

### 1. Subir a infraestrutura

```bash
docker compose -f docker-compose.infra.yml up -d
```

### 2. Subir o serviço

```bash
./mvnw spring-boot:run
```

O serviço sobe na porta **8080** com context-path `/api`.

Swagger UI: `http://localhost:8080/api/swagger-ui.html`

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
