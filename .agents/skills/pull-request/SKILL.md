---
name: pull-request
description: Como escrever título e descrição de pull request no hl7-handler, seguindo o template .github/pull_request_template.md. Usar ao abrir um PR, ao escrever ou revisar a descrição de um PR, ou ao preparar o texto de uma release.
---

# Pull request — hl7-handler

A descrição segue **exatamente** as seções de `.github/pull_request_template.md`, na mesma ordem. Ler o template antes de escrever.

## Título

- Mesmo formato do commit: `tipo(escopo opcional): resumo no imperativo`, em inglês, minúsculo depois dos dois-pontos, sem ponto final.
- Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`, `ci`, `perf`, `style`, `revert`.
- Escopo quando ajuda: `parser`, `mllp`, `mapping`, `writer`, `v3`, `fhir`, `build`, `ci`. Exemplo: `fix(mllp): keep the next frame when the trailer is missing`.

## Descrição

- **Português**, em tópicos curtos.
- Começa direto em `### Descrição`, sem linha de título.
- Apagar os comentários de instrução do template (`<!-- ... -->`). Nada de texto do template sobrando.
- Seção sem conteúdo: `N/A`. Não inventar seções novas.
- **Alterações:** só os pontos principais, agrupados por tema. Dizer explicitamente que a mudança foi feita nos três módulos (ou por que não).
- **Validação:** o que foi rodado e onde (ex.: `./mvnw verify`, testes em JDK 8, 11, 17 e 25; teste manual com qual amostra).
- **Como testar:** passos mínimos e reproduzíveis, com a amostra ou o comando.
- **Links:** sempre em markdown (`- [#12](url)`), nunca URL solta.
- Tamanho: o suficiente para revisar sem abrir o diff; mirar em até ~3000 caracteres.

## PR de correção de bug

Em **Observações**, explicar de onde o bug veio: o que mudou, quando e por que quebrou. Em **Links relacionados**, apontar o commit ou PR que provavelmente causou o problema, achado com `git log`/`git blame` (ex.: `- Provável causa: [abc1234](url)`).

## Checklist

O checklist do template é marcado com honestidade: item que não se aplica fica desmarcado com `(N/A)` ao lado, não marcado.

## Commits do PR

- Título só, sem corpo; nenhuma menção a IA ou coautoria.
- Poucos commits, agrupados por tema.
