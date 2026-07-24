# Entregável Fase 5 — Hackathon FIAP X

Pasta de referência para o portal do aluno e o pacote zipado.

## Documentos

| Arquivo | Descrição |
|---------|-----------|
| [../entrega-portal-fase5.md](../entrega-portal-fase5.md) | Documento principal de entrega (gerar PDF a partir deste arquivo) |
| [../../fase5/matriz-conformidade.md](../../fase5/matriz-conformidade.md) | Matriz PDF vs implementação (requisito, status, evidência, gap) |
| [../../fase5/arquitetura.md](../../fase5/arquitetura.md) | Arquitetura detalhada |
| [../../fase5/como-rodar-localmente.md](../../fase5/como-rodar-localmente.md) | Execução local e E2E |

## Repositórios

- https://github.com/ricartefelipe/fiapx-api-service
- https://github.com/ricartefelipe/fiapx-processor-service

## Gerar PDF de entrega

```bash
cd fiapx-api-service/docs/delivery
pandoc entrega-portal-fase5.md -o entrega-portal-fase5.pdf --css=pdf-print.css
```
