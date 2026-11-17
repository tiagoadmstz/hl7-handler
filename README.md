# HL7 Handler (`hl7-handler`)

Biblioteca Java de alta performance, desacoplada e extensível para **leitura, escrita, detecção de versão e data binding de mensagens HL7 v2**, com suporte a framing de rede MLLP e de-para semântico orientado a **YAML externo**.

---

## 💡 O que é HL7?

**HL7** significa **Health Level Seven** (*Nível de Saúde Sete*).
O número **7** faz alusão direta à **7ª camada do Modelo OSI (a Camada de Aplicação)**, focando na semântica, integridade e padronização dos dados clínicos trocados entre sistemas de saúde (laboratórios, prontuários eletrônicos, farmácias, radiologia e faturamento).

---

## 🚀 Funcionalidades Principais

1. **Detecção Automática de Versão (`MSH-12`)**:
   - Identifica versões como `2.3`, `2.4`, `2.5`, etc. diretamente no cabeçalho antes do parse completo.
2. **Utilitário de Rede / MLLP ByteStream (`network`)**:
   - Lê e grava mensagens em sockets/streams de rede utilizando o protocolo padrão MLLP (`<VT> 0x0B` ... `<FS> 0x1C <CR> 0x0D`).
3. **Mapeamento De-Para Estruturado via YAML Externo**:
   - Traduz campos crípticos do HL7 (ex: `PID.5.1`, `MSH.9.2`) para nomes de negócio semânticos através de um arquivo `.yaml` configurado externamente, gerando JSON limpo.
4. **Data Binding Dinâmico em VO (Value Object / POJO)**:
   - Converte mensagens HL7 diretamente em classes do seu domínio com segurança de tipos.
5. **Arquitetura SOLID**:
   - Extensibilidade total para adicionar novas versões sem alterar as existentes (Open/Closed Principle).
6. **Módulo de Exemplos Isolado**:
   - Exemplos em **Java Puro**, **Spring Boot** e **Quarkus** mantidos em módulo desacoplado do ciclo de build da biblioteca core.

---

## 📦 Qual artefato usar

A biblioteca é publicada em três artefatos com a mesma API (mesmo pacote `io.github.tdteixeira.hl7handler`). Cada artefato é um projeto independente, com código e testes próprios, escritos na sintaxe do seu Java mínimo. Uma correção precisa ser aplicada nos três. Use apenas um por projeto:

| Artefato | Java | Jackson |
|---|---|---|
| `hl7-handler-core` | 17 ou superior | 3 (`tools.jackson`) |
| `hl7-handler-java11` | 11 a 16 | 2 (`com.fasterxml.jackson`) |
| `hl7-handler-java8` | 8 | 2 (`com.fasterxml.jackson`) |

```xml
<dependency>
    <groupId>io.github.tdteixeira.hl7handler</groupId>
    <artifactId>hl7-handler-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

---

## 🧬 Versões HL7 suportadas

O parser é um só para todas as versões v2 (2.1 a 2.9). A versão lida em `MSH-12` escolhe um perfil em `hl7-handler-core/src/main/resources/hl7/profiles/` (um `.properties` por versão) e a estratégia que define o `MSH-9` do ACK, o uso de `MSH-18`, o 5º caractere de `MSH-2`, o formato de data (TS ou DTM) e as coordenadas padrão do evento e do ID do paciente.

Versão desconhecida nunca bloqueia o parse: a leitura segue com o perfil mais próximo e o aviso aparece em `Hl7Message.warnings()`. As amostras de cada versão ficam em `src/test/resources/samples/versions/` de cada módulo.

| Versão | O que muda | Como a lib trata | Perfil |
|---|---|---|---|
| 2.1 | `MSH-9` só com o tipo; evento no `EVN-1`; sem `MSH-18` | evento lido em `EVN.1`; ACK com `MSH-9` = `ACK`; `MSH-18` não é copiado | 2.1 |
| 2.2 | `MSH-9` = tipo^evento; sem `MSH-18` | evento lido em `MSH.9.2`; ACK com `ACK^evento`; ID do paciente em `PID.2.1` | 2.2 |
| 2.3 | `MSH-18` (charset) disponível; ID do paciente em `PID-2` | charset do `MSH-18` aplicado; ACK com `ACK^evento`; ID em `PID.2.1` | 2.3 |
| 2.3.1 | `MSH-9.3` com a estrutura da mensagem; ID do paciente em `PID-3` | ACK com `ACK^evento^ACK`; ID em `PID.3.1` | 2.3.1 |
| 2.4 | `MSH-9.3` com a estrutura da mensagem | ACK com `ACK^evento^ACK`; ID em `PID.3.1` | 2.4 |
| 2.5 / 2.5.1 | `TS` com fuso horário | `MSH-7` do ACK em TS com offset (`yyyyMMddHHmmssZ`); ACK com `ACK^evento^ACK`; ID em `PID.3.1` | 2.5 / 2.5.1 |
| 2.6 | `DTM` no lugar de `TS` (com microssegundos e fuso); `MSH-18` costuma vir como `UNICODE UTF-8` em monitores multiparamétricos (ex.: gateways de sinais vitais) | `MSH-7` do ACK em DTM; ID em `PID.3.1` | 2.6 |
| 2.7 / 2.7.1 | `MSH-2` com 5º caractere de truncamento (`#`) | o `#` é aceito e repetido no `MSH-2` do ACK | 2.7 / 2.7.1 |
| 2.8 | Mesmas regras de 2.7.1 (DTM e truncamento) | idem; é também o fallback de 2.8.x e 2.9 | 2.8 |
| 2.9 e outras numéricas sem perfil próprio (ex.: 3.0) | não há perfil próprio | texto da versão preservado (`MSH-12`, `Hl7Version`); usa o perfil mais próximo abaixo, 2.8, com aviso | 2.8 |
| Desconhecida (sem `MSH-12` ou valor não numérico) | não se aplica | usa o perfil 2.5 (baseline), com aviso | 2.5 |

---

## 📦 Estrutura do Repositório

```
hl7-handler/
├── pom.xml                                  <- POM raiz (multi-módulo, toolchains)
├── hl7-handler-core/                        <- Artefato Java 17+ (Jackson 3, module-info)
│   ├── src/main/java/io/github/tdteixeira/hl7handler/
│   │   ├── Hl7Handler.java                  <- Fachada fluente pública
│   │   ├── api/                             <- Interfaces públicas (Hl7Reader, etc.)
│   │   ├── binding/                         <- Jackson JSON & Data Binding em VOs
│   │   ├── mapping/                         <- De-Para via YAML (YamlLoader, Transformer)
│   │   ├── model/                           <- Hl7Message, Hl7Segment, Hl7Field, etc.
│   │   ├── network/                         <- MllpStreamReader (Stream / ByteStream)
│   │   ├── parser/                          <- Parsers e detecção de versão
│   │   └── writer/                          <- Serializador HL7 v2
│   └── src/test/resources/
│       ├── samples/versions/                <- Uma mensagem por versão HL7 (2.1 a 2.9) e casos de borda
│       └── mappings/                        <- Exemplos de De-Para YAML
├── hl7-handler-java11/                      <- Artefato Java 11 a 16 (Jackson 2, module-info), mesma estrutura
├── hl7-handler-java8/                       <- Artefato Java 8 (Jackson 2, Automatic-Module-Name), mesma estrutura
└── hl7-handler-examples/                    <- Módulo ignorado no build padrão (-Pexamples)
    ├── example-stub/                        <- Stubs, Mocks, VOs compartilhados e MockMllpServer
    ├── example-pure-java/                   <- Exemplo executável em Java puro
    ├── example-pure-java8/                  <- Exemplo em Java 8 (hl7-handler-java8)
    ├── example-spring/                      <- Exemplo API REST com Spring Boot
    └── example-quarkus/                     <- Exemplo API REST com Quarkus
```

---

## 🛠️ Como Compilar

O projeto inclui o **Maven Wrapper** (`mvnw` / `mvnw.cmd`), garantindo que não seja necessária a instalação prévia do Maven.

### Pré-requisito: Maven Toolchains

O build compila com o JDK 25 e roda os testes de cada artefato na JVM do seu Java mínimo (`core` em 17 e 25, `java11` em 11, `java8` em 8). Os JDKs são localizados pelo `~/.m2/toolchains.xml`, independentemente do `JAVA_HOME`:

```xml
<toolchains>
    <toolchain>
        <type>jdk</type>
        <provides><version>8</version></provides>
        <configuration><jdkHome>/caminho/para/jdk-8</jdkHome></configuration>
    </toolchain>
    <!-- repetir para as versões 11, 17 e 25 -->
</toolchains>
```

O Maven pode rodar em Java 8 para os três artefatos. Os exemplos (Spring Boot 4 e Quarkus 3.40) exigem que o próprio Maven rode em Java 17 ou superior.

### Build Padrão (os três artefatos)
```bash
# Windows
.\mvnw.cmd clean test

# Linux / macOS
./mvnw clean test
```

### Build incluindo os Exemplos (Java puro, Spring Boot e Quarkus)
```bash
# Windows
.\mvnw.cmd clean install -Pexamples

# Linux / macOS
./mvnw clean install -Pexamples
```

---

## 🚀 Como Executar os Exemplos

Os módulos de demonstração consomem o módulo compartilhado [`example-stub`](file:///C:/Users/fastw/DEV/hl7-handler/hl7-handler-examples/example-stub) e podem ser iniciados via Maven Wrapper:

### 1. Exemplo Java Puro (Console / Socket TCP MLLP)
Executa a demonstração em console, realizando parse, binding e comunicação via socket real com o `MockMllpServer`:
```bash
# Windows
.\mvnw.cmd exec:java -pl hl7-handler-examples/example-pure-java -D"exec.mainClass=io.github.tdteixeira.hl7handler.example.pure.PureJavaExample" -Pexamples

# Linux / macOS
./mvnw exec:java -pl hl7-handler-examples/example-pure-java -Dexec.mainClass="io.github.tdteixeira.hl7handler.example.pure.PureJavaExample" -Pexamples
```

Além do parse, do binding e do ACK pelo socket, a demonstração roda três casos:

1. **Laboratório**: ORU_R01 2.5 com seis OBX, lidos em uma lista com `lista: "OBX[*]"`, cada item com código, descrição, valor numérico, unidade e referência.
2. **Monitor 2.6**: sinais vitais com `MSH-18` `UNICODE UTF-8`, `MSH-3` com componentes e timestamps DTM. O exemplo gera o ACK e mostra `MSA-1`, `MSH-9` e `MSA-2`.
3. **Legado 2.3**: ADT_A01 com o ID do paciente em `PID-2`. O mapeamento usa `porVersao: "<2.3.1"` para ler `PID.2.1`; o mesmo arquivo é aplicado a um 2.5 e lê `PID.3.1`.

---

### 2. Exemplo Spring Boot (API REST)
Inicia o servidor Spring Boot na porta `8080`:
```bash
# Windows
.\mvnw.cmd spring-boot:run -pl hl7-handler-examples/example-spring -Pexamples

# Linux / macOS
./mvnw spring-boot:run -pl hl7-handler-examples/example-spring -Pexamples
```

* **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **Endpoints disponíveis**:
  * `GET http://localhost:8080/api/hl7/sample-patient` — Retorna o JSON semântico traduzido do paciente (`ADT_A01`).
  * `GET http://localhost:8080/api/hl7/sample-lab` — Retorna o JSON semântico do laudo laboratorial (`ORU_R01`).
  * `POST http://localhost:8080/api/hl7/patient-vo` — Recebe texto bruto HL7 (`text/plain`) e devolve o `PacienteVO` tipado.
  * `POST http://localhost:8080/api/hl7/ack` — Recebe texto bruto HL7 (`text/plain`) e devolve o ACK em `text/plain`.

#### Exemplo de ACK

Salve a mensagem em `mensagem.hl7` (um segmento por linha) e envie:

```bash
curl -X POST http://localhost:8080/api/hl7/ack \
  -H "Content-Type: text/plain" \
  --data-binary @mensagem.hl7
```

```
MSH|^~\&|APP_ORIGEM|HOSP_FICT|APP_DESTINO|HOSP_FICT|20261117090000||ADT^A01|MSG-FICT-1|P|2.5
PID|1||555010^^^HOSP_FICT^MR||PACIENTE^FICTICIO
```

No ACK devolvido, `MSA-1` é `AA`, `MSA-2` é `MSG-FICT-1`, e `MSH-3`/`MSH-4` são os `MSH-5`/`MSH-6` da mensagem enviada (`APP_DESTINO` e `HOSP_FICT`).

---

### 3. Exemplo Quarkus (API REST)
Inicia o Quarkus em modo desenvolvimento (Live Coding) na porta `8080`:
```bash
# Windows
.\mvnw.cmd quarkus:dev -pl hl7-handler-examples/example-quarkus -Pexamples

# Linux / macOS
./mvnw quarkus:dev -pl hl7-handler-examples/example-quarkus -Pexamples
```

* **Swagger UI**: [http://localhost:8080/q/swagger-ui](http://localhost:8080/q/swagger-ui)
* **Quarkus Dev UI**: [http://localhost:8080/q/dev](http://localhost:8080/q/dev)
* **Endpoints disponíveis**:
  * `GET http://localhost:8080/api/hl7/sample-patient`
  * `GET http://localhost:8080/api/hl7/sample-lab`
  * `POST http://localhost:8080/api/hl7/patient-vo`
  * `POST http://localhost:8080/api/hl7/ack` — Recebe texto bruto HL7 (`text/plain`) e devolve o ACK em `text/plain`.

---

### 4. Exemplo Java 8 (executado em JDK 8)
Usa o artefato `hl7-handler-java8` (Java 8, Jackson 2) e roda os mesmos três casos do exemplo Java puro. O código é Java 8: sem `var`, sem `List.of` e com imports explícitos. O módulo não depende do `example-stub`, porque o stub é compilado em Java 25; por isso as amostras e os mapeamentos ficam em `src/main/resources`.

O Maven monta o classpath (em Java 17 ou superior) e a execução usa um JDK 8 apontado em `JAVA8_HOME`:

```powershell
# Windows (PowerShell)
.\mvnw.cmd install -Pexamples -pl hl7-handler-examples/example-pure-java8 -am
.\mvnw.cmd dependency:build-classpath -pl hl7-handler-examples/example-pure-java8 -Dmdep.outputFile=target/classpath.txt -Pexamples
$cp = Get-Content hl7-handler-examples\example-pure-java8\target\classpath.txt
& "$env:JAVA8_HOME\bin\java" -cp "hl7-handler-examples\example-pure-java8\target\classes;$cp" io.github.tdteixeira.hl7handler.example.java8.PureJava8Example
```

```bash
# Linux / macOS
./mvnw install -Pexamples -pl hl7-handler-examples/example-pure-java8 -am
./mvnw dependency:build-classpath -pl hl7-handler-examples/example-pure-java8 -Dmdep.outputFile=target/classpath.txt -Pexamples
CP=$(cat hl7-handler-examples/example-pure-java8/target/classpath.txt)
"$JAVA8_HOME/bin/java" -cp "hl7-handler-examples/example-pure-java8/target/classes:$CP" io.github.tdteixeira.hl7handler.example.java8.PureJava8Example
```

---

## ⚡ Desempenho

O parser separa linhas, segmentos, repetições, componentes e subcomponentes por varredura de caracteres (`indexOf`), sem `String.split` com regex. Medição com JMH no módulo `hl7-handler-benchmarks`, tempo médio por operação (menor é melhor):

| Benchmark | Baseline (regex split) | Após (varredura) | Ganho |
|---|---|---|---|
| `parseAdtV25` | 24,70 µs ± 1,34 | 9,49 µs ± 3,03 | 61,6% |
| `parseMindrayOruV26` | 34,44 µs ± 0,97 | 9,45 µs ± 3,70 | 72,5% |
| `parseOruWith200Obx` | 1.275,92 µs ± 674,02 | 340,14 µs ± 47,02 | 73,3% |
| `mapPaciente` | 35,06 µs ± 35,80 | 16,25 µs ± 14,34 | 53,7% |

Ambiente: OpenJDK 25.0.1 (Temurin), AMD Ryzen 5 5625U (6 núcleos / 12 threads), Windows 11 Home. JMH 1.37 com `AverageTime`, microssegundos, 1 fork, 3 aquecimentos de 1 s e 5 medições de 1 s; `±` é o intervalo de confiança de 99,9% do JMH. Baseline e após foram medidos em execuções separadas na mesma máquina, então a variância é maior nos casos com erro alto. Comandos completos em [`hl7-handler-benchmarks/README.md`](hl7-handler-benchmarks/README.md).

## 💻 Exemplos de Uso no Código

### 1. Detecção Automática e Extração de Coordenadas
```java
String rawHl7 = "MSH|^~\\&|LAB|HOSP|||20261109||ADT^A01|MSG001|P|2.5\rPID|1||12345||SILVA^JOAO\r";

// Detectar versão
Hl7Version version = Hl7Handler.detectVersion(rawHl7); // Hl7Version.V2_5

// Navegar por coordenadas Segmento.Campo.Componente
Hl7Message message = Hl7Handler.reader().from(rawHl7).toMessage();
String sobrenome = message.get("PID.5.1"); // SILVA
String nome = message.get("PID.5.2");      // JOAO
```

---

### 2. Mapeamento De-Para via Arquivo YAML Externo
Defina seu arquivo `meu-mapeamento.yaml`:
```yaml
version: "2.5"
mappings:
  mensagemId: "MSH.10"
  tipo: "MSH.9.1"
  paciente:
    prontuario: "PID.3.1"
    nome: "PID.5.2"
    sobrenome: "PID.5.1"
```

E no seu código Java:
```java
// Gerar JSON semântico traduzido
String json = Hl7Handler.reader()
    .from(rawHl7)
    .withMapping(new File("caminho/para/meu-mapeamento.yaml"))
    .toJson();
```

#### Diretivas do mapeamento

Além da coordenada em texto (`"PID.5.1"`), um nó pode ser uma diretiva:

```yaml
mappings:
  nascimento: { hl7: "PID.7", tipo: data }
  convenio: { hl7: "IN1.4", fallback: "PV1.20", padrao: "PARTICULAR" }
  origem: { valor: "LAB" }
  resultados:
    lista: "OBX[*]"
    item:
      codigo: "3.1"
      valor: "5"
porVersao:
  "<2.3.1":
    paciente:
      prontuario: "PID.2.1"
```

- Coordenada: `hl7` / `path` / `campo`, com `fallback` / `padrao` e `tipo` / `type` opcionais.
- Constante: `valor` / `value`, com `tipo` opcional.
- Lista: `lista` / `list` (`SEG[*]` ou `SEG.n[*]`), com `item`.
- Um mapa só é lido como diretiva quando **todas** as suas chaves são da diretiva. Um grupo com outras chaves, como `exame: { valor: "OBX.5", unidade: "OBX.6" }`, continua sendo um grupo comum.
- Tipos embutidos: `texto`, `numero`, `data`, `booleano` (e os nomes em inglês).

##### Metadados e faixas de versão

- `version` e `description`: metadados informativos, ignorados na transformação.
- `mappings`: envoltório opcional do template. Se existir como mapa, só ele é transformado; sem ele, todas as chaves que não são metadados formam o template.
- `versoes` (ou `versions`): faixas de versão aceitas pelo arquivo. Se a versão da mensagem não satisfizer a faixa, `toMap` e `bindTo` lançam `Hl7MappingException`. É o único ponto em que a versão bloqueia a leitura, inclusive para versão desconhecida.
- `porVersao` (ou `byVersion`): sobreposições por faixa. Cada chave é uma faixa e o valor é um trecho do mapeamento, mesclado no template base. Se mais de uma faixa casar, a mais específica é aplicada por último e vence: versão exata, depois intervalo fechado (`>=2.3, <2.7`), depois faixa aberta (`>=2.3`), depois curinga (`2.x`, `*`).

Expressões aceitas em `versoes` e nas chaves de `porVersao` (operadores `=`, `==`, `!=`, `>`, `>=`, `<`, `<=`; a vírgula funciona como E):

| Expressão | Significa |
|---|---|
| `"2.5"` | somente 2.5 (não inclui 2.5.1) |
| `">=2.3"` | 2.3 e superiores |
| `"<2.3.1"` | anteriores a 2.3.1 (inclui 2.3) |
| `">=2.3, <2.7"` | de 2.3 até antes de 2.7 |
| `"2.x"` | qualquer 2.x |
| `"*"` | qualquer versão conhecida |

```yaml
versoes: ">=2.3, <2.9"
mappings:
  paciente:
    prontuario: "PID.3.1"
porVersao:
  "<2.3.1":
    paciente:
      prontuario: "PID.2.1"
```

##### Coordenadas e listas

- `SEG.campo.componente.subcomponente`, como `PID.5.1`. O hífen também vale (`PID-5.1`).
- `SEG[n].campo` escolhe a n-ésima ocorrência do segmento (`OBX[2].5`); `CAMPO[n]` escolhe a n-ésima repetição do campo (`PID.3[2].1`).
- `@event` (ou `EVENT`) lê o evento: `MSH-9.2` a partir de 2.2 e `EVN-1` em 2.1. `@patientId` (ou `PATIENT_ID`) lê o ID do paciente da versão: `PID-2.1` até 2.3 e `PID-3.1` a partir de 2.3.1.
- `lista: "OBX[*]"` percorre todas as ocorrências do segmento; `lista: "PID.13[*]"` percorre todas as repetições do campo.
- Dentro de `item`, a coordenada é relativa ao elemento da lista. Em lista de segmento, `"3.1"` é o campo 3, componente 1 do próprio segmento. Em lista de repetição de campo, `"1"` é o componente da repetição; a forma absoluta (`PID.13[*].1`) também funciona. Fora de `item`, coordenada relativa é rejeitada na validação.

#### Validação no carregamento

Todo `withMapping(...)` valida o YAML e lança `Hl7MappingException` com o caminho do erro, por exemplo `Invalid HL7 coordinate 'PID.x' at 'paciente.nome'`. São verificadas chaves duplicadas, coordenadas, listas, tipos e faixas de `versoes` e `porVersao`.

#### Opções do Builder

```java
Hl7Processor processor = Hl7Handler.builder()
    .keepEmptyStrings(true)
    .converter("maiusculo", valor -> valor == null ? null : valor.toUpperCase())
    .withMapping(new File("caminho/para/meu-mapeamento.yaml"))
    .build();

PacienteVO vo = processor.read(rawHl7).bindTo(PacienteVO.class);
```

---

### 3. Data Binding Direto para Value Object (VO)
```java
public class PacienteVO {
    private String mensagemId;
    private String tipo;
    private PacienteInfo paciente;
    // getters e setters...
}

PacienteVO vo = Hl7Handler.reader()
    .from(rawHl7)
    .withMapping(new File("caminho/para/meu-mapeamento.yaml"))
    .bindTo(PacienteVO.class);
```

---

### 4. Leitura Direta de Rede / Protocolo MLLP
```java
// Lendo de um socket de rede TCP/IP
try (InputStream in = socket.getInputStream()) {
    Hl7Message message = Hl7Handler.reader()
        .fromMllp(in)
        .toMessage();
}

// Empacotando resposta ACK em MLLP para enviar de volta pela rede:
byte[] ackMllpBytes = Hl7Handler.network().wrap("MSH|^~\\&|...|ACK^A01|...");
```

- `wrap` codifica a mensagem com o charset do MSH-18 (UTF-8 quando ausente). MLLP sobre UTF-16 não é suportado.
- Para ler várias mensagens chamando `readMessage` em sequência, passe um `PushbackInputStream` (ou um stream com `mark`) ou use `readAll`. Num stream comum, um quadro sem o `<CR>` final lança `MllpFramingException` em vez de perder bytes.

---

### ACK

`Hl7Handler.generateAck(mensagem)` gera um ACK `AA` com o texto `Message accepted`. `Hl7Handler.generateAck(mensagem, "AE", "texto")` escolhe o código e o texto. O código é gravado como informado: a biblioteca não o valida, mas o uso padrão é `AA` (aceita), `AE` (erro de aplicação) e `AR` (rejeitada).

```java
Hl7Message mensagem = Hl7Handler.reader().from(rawHl7).toMessage();

String ack = Hl7Handler.generateAck(mensagem);
String erro = Hl7Handler.generateAck(mensagem, "AE", "Paciente não encontrado");
```

Regras do ACK:

- `MSH-3`/`MSH-4` do ACK saem de `MSH-5`/`MSH-6` da mensagem recebida, e `MSH-5`/`MSH-6` do ACK saem de `MSH-3`/`MSH-4`. Os valores são copiados crus, com os componentes. Se a mensagem não trouxe `MSH-5`/`MSH-6`, o ACK usa `HL7_HANDLER` e `LOCAL`.
- `MSH-9` segue a versão: `ACK` (2.1), `ACK^evento` (2.2 e 2.3) e `ACK^evento^ACK` (2.3.1 em diante). Sem evento, usa `R01`.
- `MSH-10` do ACK é `ACK-` seguido do `MSH-10` recebido. Sem `MSH-10`, usa `ACK-` com o instante em segundos.
- `MSA-1` é o código; `MSA-2` é o `MSH-10` recebido, cru; `MSA-3` é o texto com escape HL7 (`|` vira `\F\`, `^` vira `\S\`, `~` vira `\R\`, `&` vira `\T\`, `\` vira `\E\` e quebra de linha vira `\.br\`).
- `MSH-12` é copiado cru. `MSH-7` segue o formato TS ou DTM da versão. `MSH-18` é copiado quando a versão o suporta (2.3 em diante) e a mensagem o trouxe.

### Charset (MSH-18)

| Valor do MSH-18 | Charset Java |
|---|---|
| `ASCII` | `US-ASCII` |
| `8859/1` ou `ISO-8859-1` | `ISO-8859-1` |
| `8859/2` ou `ISO-8859-2` | `ISO-8859-2` |
| `8859/15` ou `ISO-8859-15` | `ISO-8859-15` |
| `UNICODE`, `UNICODE UTF-8` ou `UTF-8` | `UTF-8` |
| `UNICODE UTF-16` ou `UTF-16` | `UTF-16` |
| Outro nome | `Charset.forName` com o próprio valor; se o nome não existir, o charset padrão da leitura |
| Ausente ou em branco | `UTF-8` |

Onde o charset é usado:

- Leitura MLLP (`readMessage`, `readAll`, `readMllp`): o charset é detectado dos bytes do próprio quadro, pelo `MSH-18`, antes de decodificar. Quadro sem `MSH-18` usa o charset do leitor, que é UTF-8 por padrão e muda com `Hl7Handler.network(charset)` ou `Hl7Handler.builder().charset(...)`.
- Parser: as sequências `\Xhh\` são decodificadas com o charset da mensagem (UTF-8 quando ausente).
- `wrap`: codifica a mensagem com o charset do `MSH-18` (UTF-8 quando ausente).
- MLLP sobre UTF-16 não é suportado: os bytes `0x0B`, `0x1C` e `0x0D` podem aparecer dentro dos caracteres UTF-16 e quebrar a moldura. Use UTF-8 na rede.

---

### Uso em aplicações (Hl7Processor)

Em aplicações, crie o `Hl7Processor` uma vez (bean singleton ou campo `static final`) e reutilize-o em todas as leituras:

```java
private static final Hl7Processor PROCESSOR = Hl7Handler.builder()
    .withMapping(new File("caminho/para/meu-mapeamento.yaml"))
    .build();

PacienteVO vo = PROCESSOR.read(rawHl7).bindTo(PacienteVO.class);
```

- O `Hl7Processor` é imutável e thread-safe: uma única instância pode atender várias threads sem sincronização.
- Cada `read` faz o parse da mensagem uma única vez. `toJson()`, `toMap()` e `bindTo(...)` usam o mesmo resultado, e o mapa mapeado é calculado uma vez e mantido em cache.
- `readBytes(byte[])` aceita bytes com ou sem moldura MLLP, e `readMllp(InputStream)` lê um quadro por chamada, devolvendo `null` no fim do stream.

### Erros

Todas as exceções da biblioteca estendem `Hl7Exception`, que é não verificada (`RuntimeException`):

- `Hl7Exception`: classe base, no pacote raiz `io.github.tdteixeira.hl7handler`.
- `Hl7ParseException`: entrada ausente ou em branco e falha de leitura MLLP.
- `Hl7BindingException`: falha de serialização ou desserialização JSON, inclusive no `bindTo`.
- `Hl7MappingException` (pacote `mapping`): mapeamento YAML inválido ou ausente.
- `MllpFramingException` (pacote `network`): moldura MLLP violada (truncada, sem `0x0B` ou acima do limite de tamanho).
