---
name: review-guidelines
description: Roteiro de revisão de código e de pull request do hl7-handler. Usar quando pedirem para revisar um PR, uma branch, um diff ou um conjunto de arquivos deste repositório, antes de dar qualquer parecer.
---

# Revisão de código — hl7-handler

A revisão usa **todas** as regras aplicáveis, não só esta lista: ler também a skill `code-style` inteira e conferir cada arquivo tocado contra ela.

## 1. Preparação

1. Delimitar o diff: `git diff --stat <base>...HEAD` (base = `main`, salvo pedido diferente).
2. Ler os arquivos tocados **inteiros**, não só as linhas alteradas.
3. Ler a skill `code-style`.
4. Rodar o build: `./mvnw -B verify`. Os três módulos precisam passar, cada um na sua JVM (conferir as linhas `Toolchain in maven-surefire-plugin`).

## 2. Paridade entre os módulos

É o ponto que mais escapa neste projeto.

- [ ] A mudança aparece nos **três** módulos (`core`, `java11`, `java8`), com a mesma intenção.
- [ ] Os mesmos testes foram criados ou alterados nos três.
- [ ] As mesmas amostras estão em `src/test/resources` dos três.
- [ ] **API pública idêntica.** Comparar as assinaturas públicas dos três jars:
  ```bash
  mkdir -p target
  for m in core java11 java8; do
    jar=$(ls hl7-handler-$m/target/hl7-handler-$m-*.jar | grep -v -e sources -e javadoc)
    unzip -Z1 "$jar" | grep '\.class$' | grep -v 'module-info' | sed 's/\.class$//; s#/#.#g' | sort \
      | xargs javap -public -cp "$jar" | grep -v '^Compiled from' > target/api-$m.txt
  done
  diff target/api-core.txt target/api-java11.txt; diff target/api-core.txt target/api-java8.txt
  ```
  Diferenças aceitas, e só estas:
  - tipos do Jackson nas assinaturas (`tools.jackson.*` no core × `com.fasterxml.jackson.*` nos outros), por exemplo `Hl7JsonBinder(JsonMapper)` e `getObjectMapper()`;
  - `record` no core × classe `final` nos outros (`extends java.lang.Record`, ordem e `final` de `equals`/`hashCode`/`toString`). Os acessores precisam ter os mesmos nomes.

  `java11` × `java8` têm de sair **idênticos**.
- [ ] A sintaxe foi rebaixada corretamente (tabela da `code-style`), sem recurso de versão mais nova escondido.

## 3. Checagens por busca

Rodar nas linhas adicionadas (`git diff <base>...HEAD -U0 | grep '^+'`):

| Procurar | Por quê |
|---|---|
| `? .* ? ` na mesma expressão | Ternário aninhado |
| `!\w+\.matches(` | Regex deve descrever o inválido |
| `io\.github\.[a-z.]+\.[A-Z]` fora de `import`/`package` | Nome qualificado no corpo |
| `^import .*\*;` | Import com asterisco |
| `^import module` fora de `hl7-handler-examples` | `import module` em módulo sem Java 25 |
| `"\{\\"` ou `StringBuilder` perto de JSON | JSON montado à mão |
| `'\^'`, `"\^"`, `'&'`, `"&"`, `'~'` em parser/writer | Delimitador fixo no código |
| `catch \(.*\) \{\s*\}` | `catch` vazio |
| Identificador em português (`nome`, `campo`, `valor`, `mensagem` como variável ou método) | Código em inglês |
| `Claude`, `IA`, `AI`, `assistente`, `gerado` | Rastro de IA |
| `<!--` em `pom.xml` | Comentário em arquivo de build |
| CNS/CPF com 11 ou 15 dígitos, nomes reais em amostras | Dados reais em teste |

## 4. Corretude HL7

- [ ] Funciona para todas as versões da matriz (2.1 a 2.9): a mudança não depende de campo que só existe em algumas versões sem tratar a ausência.
- [ ] Respeita os delimitadores do MSH-2, inclusive o MSH-2 curto e o de 5 caracteres.
- [ ] Índices 1-based na API pública.
- [ ] Repetições, componentes e subcomponentes preservados (gravar depois de ler não perde dados).
- [ ] MLLP: frame truncado, frames colados e limite de tamanho tratados.
- [ ] XML (v3, CDA, FHIR XML): fábrica segura, sem `DOCTYPE`/XXE.
- [ ] Contrato documentado (javadoc, README) mantido: se o código não cumpre o que está documentado, corrigir o código, não apagar a documentação, salvo decisão explícita.

## 5. Testes e documentação

- [ ] Um método por caso, `@DisplayName` em português, AssertJ.
- [ ] Bug corrigido tem teste que reproduz o bug.
- [ ] `CHANGELOG.md` atualizado em `[Não Lançado]` quando muda comportamento ou API.
- [ ] README atualizado quando muda uso público.
- [ ] Dependência nova ou atualizada: versão publicada há pelo menos duas semanas, compatível com o Java do módulo.

## 6. Resultado

- Entregar um **relatório resumido**: achados em ordem de gravidade, cada um com arquivo:linha, o problema, o cenário que quebra e a correção sugerida. Separar "bug" de "estilo".
- Achado em código antigo que não foi tocado no diff: citar como observação, sem pedir correção no PR.
- **Não aplicar as correções por conta própria:** apresentar o relatório e perguntar quais aplicar.
- Correções aplicadas vão em poucos commits agrupados por tema, não um por achado.
