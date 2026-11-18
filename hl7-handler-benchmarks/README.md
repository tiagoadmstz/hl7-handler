# hl7-handler-benchmarks

Benchmarks JMH do parser e do mapeamento do `hl7-handler-core`. O módulo não é publicado e só entra no build com o perfil `benchmarks`, então o `./mvnw verify` padrão não o compila.

## Pré-requisitos

- Maven com toolchains do `~/.m2/toolchains.xml` (JDK 25 para compilar o módulo e rodar o JMH).
- JDK 25 disponível no caminho usado nos comandos abaixo. Ajustar o caminho se a instalação for outra.

## Build

Na raiz do repositório:

```bash
./mvnw -q -Pbenchmarks -pl .,hl7-handler-core,hl7-handler-benchmarks install -DskipTests
./mvnw -q -Pbenchmarks -pl hl7-handler-benchmarks dependency:build-classpath -Dmdep.outputFile=target/cp.txt
```

A primeira linha instala o `hl7-handler-core` no repositório local. O `.` instala também o POM pai, sem o qual a resolução do core pelo `dependency:build-classpath` falha. O módulo não é empacotado com shade: o classpath é montado a partir das dependências.

## Execução

Na raiz do repositório, no Git Bash (o `cat` monta o classpath do arquivo gerado):

```bash
"C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot/bin/java" -cp "hl7-handler-benchmarks/target/classes;$(cat hl7-handler-benchmarks/target/cp.txt)" org.openjdk.jmh.Main -rf json -rff hl7-handler-benchmarks/target/after.json
```

O separador do classpath é `;` (Windows). Para gerar a linha de base, trocar `after.json` por `baseline.json`.

A execução completa leva cerca de 35 segundos. Os resultados saem no console e no arquivo JSON indicado em `-rff`.

## Cenários

| Benchmark | O que mede |
|---|---|
| `parseAdtV25` | `DefaultHl7Parser.parse` de uma ADT v2.5 (`samples/sample-v25.hl7`) |
| `parseMindrayOruV26` | `DefaultHl7Parser.parse` de uma ORU v2.6 de monitor (`samples/versions/v26-mindray-oru.hl7`) |
| `parseOruWith200Obx` | `DefaultHl7Parser.parse` de uma ORU v2.5 fictícia com 200 OBX, com componentes, subcomponentes e repetições |
| `mapPaciente` | `Hl7Processor.read(...).toMap()` com o mapeamento `mappings/paciente-mapping.yaml` |

Configuração JMH: `AverageTime`, microssegundos, 1 fork, 3 iterações de aquecimento de 1 s e 5 de medição de 1 s.
