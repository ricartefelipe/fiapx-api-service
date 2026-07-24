# GitFlow — FIAP X Video Platform

Fluxo oficial dos repositórios. **Features nunca vão direto para `main`.**

## Branches

| Branch | Base | Merge em | Uso |
|--------|------|----------|-----|
| `feature/*` | `develop` | `develop` | Funcionalidades |
| `fix/*` | `develop` | `develop` | Correções não urgentes |
| `release/*` | `develop` | `main` + sync `develop` | Preparar versão |
| `hotfix/*` | `main` | `main` + sync `develop` | Correção urgente em produção |
| `develop` | — | — | Integração contínua |
| `main` | — | — | Produção |

## Fluxo diário (feature)

```bash
git fetch origin
git checkout develop && git pull origin develop
git checkout -b feature/minha-tarefa

git push -u origin feature/minha-tarefa
gh pr create --base develop --title "feat: ..."
```

## Release (develop → main)

```bash
git checkout develop && git pull origin develop
git checkout -b release/v1.0.0
gh pr create --base main --title "release: v1.0.0"
```

## Hotfix (urgente em produção)

```bash
git checkout main && git pull origin main
git checkout -b hotfix/descricao-curta
gh pr create --base main --title "fix: ..."
```

## O que o CI bloqueia

- PR de `feature/*` ou `fix/*` **direto para `main`** (workflow `enforce-gitflow`)
