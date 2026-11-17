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
