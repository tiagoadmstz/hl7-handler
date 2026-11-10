---
name: code-style
description: Estilo de código do hl7-handler (Java nos três módulos core/java11/java8, testes, dependências, comentários). Usar antes de escrever ou alterar qualquer código Java, pom.xml ou teste neste repositório, e como checklist na revisão.
---

# Estilo de código — hl7-handler

Regras para todo código deste repositório. Na dúvida sobre algo que não está aqui, seguir o arquivo vizinho.

## Idiomas

| O quê | Idioma |
|---|---|
| Classes, métodos, variáveis, constantes, parâmetros, nomes de arquivo | Inglês |
| Javadoc e mensagens de exceção da biblioteca | Inglês |
| `@DisplayName` dos testes | Português |
| README, CHANGELOG, CONTRIBUTING, templates do GitHub | Português |
| Mensagens de commit | Inglês |

Termos do padrão HL7 (`MSH`, `PID`, `OBX`, `ACK`, `MLLP`, nomes de campo) ficam como no padrão.

## Os três módulos

Cada módulo tem código próprio. A mudança é escrita no `hl7-handler-core` e portada, nesta ordem, para o `hl7-handler-java11` e o `hl7-handler-java8`, rebaixando a sintaxe:

| Recurso | core (17) | java11 | java8 |
|---|---|---|---|
| `record` | Sim | Não → classe `final` com acessores no mesmo formato (`fieldSeparator()`), `equals`/`hashCode`/`toString` | Não → idem |
| Pattern matching (`instanceof X x`) | Sim | Não → `instanceof` + cast | Não → idem |
| `switch` com seta / expressão | Sim | Não → `switch` clássico ou `if` | Não → idem |
| Text blocks (`"""`) | Sim | Não → concatenação | Não → idem |
| `var` | Sim | Sim | Não |
| `List.of`, `Map.of`, `Set.of` | Sim | Sim | Não → `Collections.emptyList()`, `singletonList`, `Arrays.asList`, `unmodifiableList` |
| `InputStream.readAllBytes()` | Sim | Sim | Não → laço com buffer |
| `String.isBlank()`, `strip()`, `repeat()` | Sim | Sim | Não → `trim().isEmpty()` etc. |
| `ByteArrayOutputStream.toString(Charset)` | Sim | Sim | Não → `new String(out.toByteArray(), charset)` |
| `Optional.isEmpty()`, `orElseThrow()` sem argumento | Sim | Sim | Não → `!isPresent()`, `orElseThrow(Supplier)` |
| `List.getFirst()`/`addFirst()` (Java 21) | **Não** (mínimo é 17) → `get(0)`, `add(0, x)` | Não | Não |
| Jackson | 3 (`tools.jackson.*`, exceções unchecked) | 2 (`com.fasterxml.jackson.*`, `JsonProcessingException` checked) | 2 |

- A API pública (classes, assinaturas, nomes) é **idêntica** nos três. Muda só a implementação.
- O `--release` de cada módulo barra API nova, mas **não barra sintaxe** que o javac aceita em todas as versões. Conferir a tabela acima.

## Java

- **Guard clause:** verificar o caso inválido primeiro e sair cedo (`if (raw == null || raw.isEmpty()) { return ...; }`); o caminho principal fica sem aninhamento.
- **Sem ternário aninhado.** Para fallback com mais de um nível, `if`/`else if`. Um ternário simples é aceito.
- **Sem helper de uso único:** predicado, método privado ou constante usado uma vez só fica inline, com um comentário curto se a regra não for óbvia. Extrair quando houver reuso ou quando o trecho for longo de verdade.
- **Regex de validação descreve o formato inválido:** em vez de `!valor.matches("\\d+")`, usar `valor.matches("(?s)(?!\\d+\\z).*")`. Com lookahead negativo, sempre `(?s)` e `\z`, e testar um valor com quebra de linha.
- **Sem nome qualificado no corpo** (`io.github...Classe.metodo()`): usar `import`. Sem imports não usados.
- **Sem import com asterisco** (`import pacote.*;` nem `import static Classe.*;`): importar só o tipo ou membro usado, um por linha. Vale para `src/main` e `src/test` dos três módulos, inclusive para tipos do próprio projeto (`model.Hl7Field`, `model.Hl7Segment`...).
  - `import module` (Java 25) só onde o `release` é 25, ou seja, nos exemplos. Nos três módulos da biblioteca (17, 11 e 8) ele não compila: lá, import explícito.
  - Nos exemplos, tipos da JDK vêm de `import module java.base;` (logo após o `package`), sem `import java.*` avulso. Spring, Jakarta e hl7-handler ficam com import explícito: os exemplos rodam no classpath, e o `import module` só enxerga módulos do module path.
  - A IDE não pode recolher imports em `*`: o `.editorconfig` já fixa o limite em 999 para o IntelliJ. Em outra IDE, desligar o "import on demand".
  - O build barra: Checkstyle `AvoidStarImport` na fase `validate`, em todos os módulos e nos testes. Na biblioteca é o Checkstyle 9.3, o último que roda com o Maven em Java 8; ele não lê `module-info.java` (excluído) nem `import module`. Os exemplos sobrescrevem `checkstyle.version` com uma versão que lê Java 25.
- **JSON só por Jackson:** nunca montar JSON concatenando `String` ou com `StringBuilder`; serializar um `Map` ou um VO. Vale também para corpo de erro em exemplos.
- **Imutabilidade:** campos `final`, coleções expostas como `Collections.unmodifiableList(...)`, cópia defensiva no construtor (padrão de `Hl7Segment`/`Hl7Field`).
- **Índices HL7 são 1-based** na API pública (`getField(3)` = PID-3); converter para 0-based só dentro do método.
- **Delimitadores nunca fixos no código:** usar os de `Hl7Delimiters` da mensagem (`^`, `&`, `~` literais só em `Hl7Delimiters.DEFAULT`).
- **Exceções:** sem `catch` vazio, salvo com comentário dizendo por que o erro é ignorado. Não engolir exceção de parse: ou converte em resultado documentado (ex.: `""` para caminho inexistente) ou relança com contexto.
- **Javadoc** em toda classe e método público, curto, dizendo o que faz e o que devolve em caso de ausência.

## Comentários

- Só quando explicam o **porquê** (regra do padrão HL7, contorno de bug, decisão não óbvia). Não narrar o que o código faz.
- **Nenhum comentário em `pom.xml`** ou outro arquivo de build.
- Nenhuma menção a IA, assistente ou "tarefa/pedido" em código, comentário, javadoc ou documentação.
- Anotação temporária durante o trabalho: `// TODO(remove before done): ...`, removida antes de terminar.

## Testes

- JUnit 5 + AssertJ. **Um método por caso**; sem `@ParameterizedTest`.
- Nome do método em inglês (`shouldReadVersion26MindrayObservation`); `@DisplayName` em português, descrevendo o comportamento.
- Cada módulo tem a mesma classe de teste com os mesmos casos. Teste novo entra nos três.
- **Amostras com dados fictícios** em `src/test/resources/samples/` (nunca nome, CNS, CPF ou prontuário real). Uma amostra por versão HL7 em `samples/versions/`.
- Bug corrigido = teste que reproduz o bug, nos três módulos.

## Dependências e build

- Versões fixadas no `pom.xml` pai, publicadas há pelo menos duas semanas.
- Nenhuma dependência nova sem necessidade. O que a JDK resolve (XML, datas, regex) fica na JDK.
- O `hl7-handler-java8` só aceita dependência que rode em Java 8. Conferir o bytecode (major 52).
- Não alterar o `JAVA_HOME` nem o `java` do `PATH`: os JDKs vêm do `~/.m2/toolchains.xml`.
