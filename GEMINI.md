# hl7-handler — Regras e Diretrizes para Gemini / Antigravity

Biblioteca Java para mensagens HL7 v2: parser estrutural multi-versão, detecção de versão (MSH-12), transporte MLLP, writer e data binding em JSON/VO guiado por mapeamento YAML (de-para).

## Módulos e Tecnologias

Três módulos independentes com a mesma API pública (`io.github.tdteixeira.hl7handler`):

| Módulo | Java mínimo | Jackson | Sintaxe |
|---|---|---|---|
| `hl7-handler-core` | 17 | 3 (`tools.jackson`) | Java 17: `record`, pattern matching em `instanceof`, `switch` com seta |
| `hl7-handler-java11` | 11 (até 16) | 2 (`com.fasterxml.jackson`) | Java 11: `var`, `List.of`, `readAllBytes`; sem `record` e sem pattern matching |
| `hl7-handler-java8` | 8 | 2 (`com.fasterxml.jackson`) | Java 8: sem APIs Java 9+ (`List.of`, `readAllBytes`, `isBlank`), sem `var` |

Exemplos em `hl7-handler-examples` (perfil `-Pexamples`) usam `hl7-handler-core` e rodam em JDK 25 com Spring Boot 4.1.2 e Quarkus 3.40.

## Regra Fundamental: Independência dos Módulos

- Cada módulo possui **código e testes próprios**, inclusive as amostras em `src/test/resources`. A duplicação é intencional.
- **Proibido:** criar pasta de código compartilhado, `build-helper` com fontes de outro módulo ou artefato comum.
- **Ordem obrigatória de implementação:**
  1. `hl7-handler-core`;
  2. `hl7-handler-java11`, rebaixando a sintaxe;
  3. `hl7-handler-java8`, rebaixando a sintaxe.
- Os mesmos testes devem existir e passar nos três módulos. A compilação usa `--release` do Java mínimo correspondente.
- Toda alteração na API pública deve ser espelhada nos três módulos simultaneamente.

## Comandos de Build e Ambiente

- Compilação com JDK 25, testes executados na JVM alvo configurada no `~/.m2/toolchains.xml`:
  - `core`: testado em JDK 17 e JDK 25
  - `java11`: testado em JDK 11 (`C:\Users\fastw\.jdks\jdk-11.0.33+10`)
  - `java8`: testado em JDK 8
- **Importante:** Nunca alterar `JAVA_HOME` nem o `PATH` do sistema operacional.
- Execução:
  - `./mvnw verify`: valida e roda testes de todos os três módulos.
  - `./mvnw verify -pl hl7-handler-core`: apenas o core.
  - `./mvnw install -Pexamples`: constrói os exemplos (requer Java 17+ apenas nesta execução).

## Skills do Projeto

Localizadas em `.agents/skills/` (também espelhadas em `.claude/skills/`):
- `code-style` (`.agents/skills/code-style/SKILL.md`): Padrões de código detalhados, tabela de sintaxe por versão Java, regras de import.
- `review-guidelines` (`.agents/skills/review-guidelines/SKILL.md`): Checklist de revisão de PRs, diffs e paridade entre módulos.
- `pull-request` (`.agents/skills/pull-request/SKILL.md`): Formato de título e descrição de PRs.

## Convenções Críticas

1. **Sem import com asterisco:** Importações explícitas sempre. Checkstyle `AvoidStarImport` falha a compilação se houver asteriscos.
2. **Sem comentários em `pom.xml`:** Manter poms limpos sem comentários.
3. **Sem menção a IA:** Nunca incluir referências a IA, prompts, assistentes ou coautoria em commits, javadocs, comentários ou código.
4. **Commits:** Conventional commits em inglês, **apenas título**, sem corpo (ex: `feat(parser): decode hex escape sequences`).
5. **Índices 1-based:** Toda API pública usa numeração 1-based conforme padrão HL7 (`getField(1)` = primeiro campo após delimitador).
6. **Delimitadores dinâmicos:** Nunca chumbar `'^'`, `'&'`, `'~'`, `'|'`. Usar sempre `Hl7Delimiters` extraído da mensagem.
7. **JSON exclusivamente por Jackson:** Nunca serializar ou concatenar JSON manualmente com String ou StringBuilder.
8. **Testes:** JUnit 5 + AssertJ, um método por caso, sem `@ParameterizedTest`, dados fictícios em `src/test/resources/samples/`.
9. **CHANGELOG.md:** Registrar mudanças em `[Não Lançado]` a cada alteração de API ou comportamento.
