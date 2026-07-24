# Fase 5 — Hackathon FIAP X — Plano de Implementação

**Objetivo:** Evoluir o projeto base de processamento de vídeos para uma arquitetura de microsserviços escalável, com mensageria, persistência, autenticação, notificações e CI/CD.

**Contexto:** A FIAP X precisa de uma versão onde investidores enviem vídeos e façam download do ZIP com frames extraídos, suportando múltiplos vídeos simultâneos e picos de carga.

---

## Requisitos funcionais

- Processar mais de um vídeo ao mesmo tempo
- Não perder requisições em picos de carga
- Proteção por usuário e senha
- Listagem de status dos vídeos por usuário
- Notificação em caso de erro (e-mail ou outro meio)

## Requisitos técnicos

- Persistência de dados
- Arquitetura escalável
- Versionamento no GitHub
- Testes automatizados
- CI/CD

## Stack recomendada

| Camada | Tecnologia |
|--------|------------|
| Containers | Docker + Docker Compose (K8s futuro) |
| Mensageria | RabbitMQ |
| Banco | PostgreSQL + Redis |
| Monitoramento | Prometheus + Grafana |
| CI/CD | GitHub Actions |
| Backend | Java 21 + Spring Boot 4 |

---

## Repositórios

| Repositório | Responsabilidade | Porta |
|---|---|---|
| `fiapx-api-service` | Auth, upload, status, download | 8080 |
| `fiapx-processor-service` | Worker assíncrono de frames/ZIP | 8081 |

---

## Fluxo de processamento

```
Cliente → POST /v1/videos (upload)
          API Service → persiste VideoJob (PENDING)
                      → publica video.requested (RabbitMQ)
                      → retorna jobId

Processor Service → consome video.requested
                  → extrai frames do vídeo
                  → gera ZIP
                  → publica video.completed ou video.failed

API Service → consome video.completed/failed
            → atualiza status
            → notifica usuário (futuro)
```

---

## Entregáveis

### Documentação
- [ ] Documentação da arquitetura (`docs/fase5/arquitetura.md`)
- [ ] Script de criação do banco (Liquibase + `scripts/db/`)

### Código
- [x] Repositórios GitHub criados
- [ ] Upload multipart funcional
- [ ] Processamento real com FFmpeg/JavaCV
- [ ] Notificação por e-mail
- [ ] Testes com cobertura mínima
- [ ] CI verde

### Apresentação
- [ ] Vídeo de até 10 minutos (arquitetura + demo)

---

## Próximas tarefas

1. Implementar endpoint POST upload com MultipartFile
2. Integrar FFmpeg no processor-service
3. Consumer de eventos completed/failed na API
4. Serviço de notificação (e-mail)
5. Testes de integração com Testcontainers
6. Kubernetes manifests
7. Release develop → main
