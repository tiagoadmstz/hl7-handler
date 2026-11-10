# hl7-handler

Biblioteca Java para mensagens HL7 v2: parser, detecção de versão (MSH-12), leitura MLLP, writer e data binding em JSON/VO guiado por mapeamento YAML externo (de-para).

## Módulos

São três artefatos com **a mesma API pública** (pacote `io.github.tdteixeira.hl7handler`). Um projeto usa só um deles.

| Módulo | Java mínimo | Jackson | Sintaxe permitida |
|---|---|---|---|
| `hl7-handler-core` | 17 | 3 (`tools.jackson`) | Java 17: `record`, pattern matching em `instanceof`, `switch` com seta |
| `hl7-handler-java11` | 11 (até 16) | 2 (`com.fasterxml.jackson`) | Java 11: `var`, `List.of`, `readAllBytes`; **sem** `record` e sem pattern matching |
| `hl7-handler-java8` | 8 | 2 (`com.fasterxml.jackson`) | Java 8: **sem** `List.of`/`Map.of`, `readAllBytes`, `isBlank`, `ByteArrayOutputStream.toString(Charset)`, `var` |

`hl7-handler-examples` (perfil `-Pexamples`) traz exemplos em Java puro, Spring Boot 4 e Quarkus, todos usando o `hl7-handler-core`.

## Regra principal: módulos independentes

- Cada módulo tem **código e testes próprios**, inclusive as amostras em `src/test/resources`. A duplicação é intencional.
- **Não** criar pasta de código compartilhado, `build-helper` com fontes de outro módulo, nem artefato comum entre os módulos.
- Toda correção ou recurso novo é feito nos três módulos, nesta ordem:
  1. `hl7-handler-core`;
  2. `hl7-handler-java11`, rebaixando a sintaxe;
  3. `hl7-handler-java8`, rebaixando a sintaxe.
- Os mesmos testes devem existir e passar nos três. Se a sintaxe não for rebaixada, o `--release` do módulo impede a compilação.
- Mudança de API pública vale para os três ao mesmo tempo.

## Build

- O build compila com o JDK 25 (`--release` do Java mínimo de cada módulo) e roda os testes na JVM do módulo: `core` em 17 e 25, `java11` em 11, `java8` em 8. Os JDKs vêm do `~/.m2/toolchains.xml`.
- O Maven pode rodar em Java 8. Não alterar `JAVA_HOME` nem o `java` do `PATH` da máquina.
- Comandos:
  - `./mvnw verify`: os três artefatos com testes;
  - `./mvnw verify -pl hl7-handler-core`: um módulo só;
  - `./mvnw install -Pexamples`: exemplos. Exige o Maven rodando em Java 17+; passar o `JAVA_HOME` só nesse comando.
- No `hl7-handler-java11`, com o Maven em Java 8, o Maven não lê o `module-info` multi-release do Jackson 2. Por isso há `requires com.fasterxml.jackson.annotation` explícito e testes pelo classpath (`useModulePath=false`). O aviso "filename-based automodules" no build vem daí e é esperado.

## Skills do projeto (`.claude/skills/` e `.agents/skills/`)

Ler a skill correspondente **antes** de agir, mesmo que ela não tenha sido carregada automaticamente:

| Situação | Skill |
|---|---|
| Escrever ou alterar código, pom ou teste | `code-style` |
| Revisar PR, branch, diff ou arquivos | `review-guidelines` (e também `code-style`) |
| Abrir PR ou escrever descrição de PR/release | `pull-request` (template em `.github/pull_request_template.md`) |

## Fluxo de trabalho com IA: Planejamento vs Execução

- **Planejamento com modelo avançado:** Sempre realizar a análise de impacto, desenho da solução e elaboração do plano detalhado passo a passo utilizando um modelo/agente analítico mais avançado ("mais caro", ex: Claude Opus/Sonnet, Gemini Pro). Nunca iniciar a execução diretamente no código sem um plano estruturado e validado.
- **Execução com modelo eficiente:** Após o plano estar claramente delineado, validado e dividido em etapas, utilizar um modelo/agente mais rápido e econômico ("mais barato", ex: Claude Haiku, Gemini Flash) para a implementação cirúrgica, geração de código, testes e checagens nos três módulos.

## Convenções

- **Idiomas:** identificadores, nomes de arquivo, javadoc, mensagens de exceção e de commit em inglês. README, CHANGELOG, CONTRIBUTING, templates do GitHub e `@DisplayName` dos testes em português.
- **Testes:** JUnit 5 + AssertJ; um método por caso, sem `@ParameterizedTest`. Amostras HL7 sempre com dados fictícios.
- **Commits:** conventional commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`, `build:`, `ci:`), **só o título, sem corpo**, sem linhas de coautoria nem menção a IA. Poucos commits, agrupados por tema.
- **Comentários:** nada de comentários em `pom.xml` e arquivos de build. Em Java, só quando explicam o porquê.
- **Sem ternário aninhado.** Para fallback, usar `if`/`else`.
- **Sem import com asterisco.** Importar só o que é usado. Nos exemplos, JDK por `import module java.base;`; nos três módulos, import explícito. O Checkstyle (`AvoidStarImport`) barra no `validate`.
- **Dependências:** fixar versões publicadas há pelo menos duas semanas.
- **CHANGELOG.md:** registrar toda mudança de comportamento ou de API em `[Não Lançado]`.
- O detalhe de cada regra está na skill `code-style`.

## Compatibilidade de versões HL7

- O parser é um só para todas as versões v2 (2.1 a 2.9). Versão desconhecida nunca bloqueia o parse.
- As amostras por versão ficam em `src/test/resources/samples/versions/` de cada módulo, e `Hl7VersionCompatibilityTest` cobre uma por versão. Um caso novo de versão entra como amostra + teste nos três módulos.
