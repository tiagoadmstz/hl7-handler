# Histórico de Alterações

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere a [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [Não Lançado]

### Adicionado
- Suporte a HL7 FHIR (R4 e R5): ponto de entrada fluente `Hl7Handler.fhir()` nos três módulos sem dependência de HAPI FHIR (apenas Jackson e JDK)
- `FhirProcessor` imutável e thread-safe, com `read(String)`, `read(InputStream)` e `readBytes(byte[])`
- `FhirResource` sobre árvore JSON imutável com `resourceType()`, `id()`, `versionId()`, `version()` (R4/R5) e resolução de caminhos (`get(path)` e `getAll(path)`)
- Mapeamento YAML para recursos FHIR com notação de propriedades, conversores de tipo, diretivas `fallback`, `padrao`, `valor`, `lista` + `item` e sobreposições `porVersao` (R4 vs R5)
- `FhirUnits`: conversão canônica de unidades fisiológicas IEEE 11073-10101 (MDC) para UCUM
- `FhirWriter`: serialização e montagem de recursos e Bundles a partir de Map ou VO (`write()`) e conversão direta de mensagens HL7 v2 `ORU^R01` para FHIR `Bundle` (`convert()`) com `Patient`, `Device` (identificador EUI-64) e `Observation`

## [0.2.0] - 2026-11-19

### Adicionado
- Mapeamento v2: `tipoPor` (`typeBy`) converte o valor pelo campo de tipo do item, como o OBX-2. A tabela padrão converte `NM` em número e `DT`/`DTM`/`TS` em data, e deixa os demais tipos como texto. `tipos` (`types`) muda ou amplia a tabela. Um OBX `CNE` ou `ST` deixa de derrubar o `toMap()` quando o mapeamento usa `tipoPor`
- Conversão de temperatura entre Celsius e Fahrenheit: `Hl7TemperatureUnit`, `Hl7Handler.builder().temperatureUnit(...)` e o metadado `temperatura`/`temperature` no YAML (o builder vence). O valor com `unidadePor` (`unitBy`) é convertido quando a unidade (MDC, UCUM ou símbolo) é de temperatura, e `ajustarUnidade` (`adjustUnit`) troca o nome da unidade. Desligada por padrão
- Leitor de HL7 v3 e CDA R2, separado do leitor v2: `Hl7Handler.v3()` devolve um `Hl7V3Builder`, que gera um `Hl7V3Processor` imutável e thread-safe, com `read(String)`, `read(InputStream)` (encoding lido da declaração XML) e `readBytes(byte[])`
- `Hl7V3Result` com `document()`, `toMap()` (calculado uma vez, em cache e não modificável), `toJson()`, `toRawJson()` (atributos como `@nome`, texto como `#text`) e `bindTo(Class)`
- `Hl7V3Document` com `format()` (`CDA` ou `MESSAGE`), `versionIdentifier()`, `rootName()`, `templateIds()`, `get(xpath)` (`""` quando ausente), `getAll(xpath)` e `warnings()`
- Strategy por tipo de documento (`io.github.tiagoadmstz.hl7handler.v3.version`): `CdaR2Strategy`, `Hl7V3MessageStrategy` e `Hl7V3StrategyRegistry`, que aceita estratégias customizadas (por exemplo, por `templateId`) com prioridade sobre as embutidas
- Mapeamento YAML v3 com XPath 1.0 e prefixos `hl7`, `sdtc` e `xsi`, mais os declarados em `namespaces:` ou em `Hl7V3Builder.namespace(...)`. Usa as mesmas diretivas do v2 (`xpath`/`path`, `fallback`, `padrao`, `tipo`, `valor`, `lista` + `item`), coordenadas semânticas `#documentId`, `#patientId` e outras, e `formato: CDA|MESSAGE`. É validado no `build()`
- Parser XML seguro, só com `javax.xml`: `DOCTYPE`, entidades externas e `XInclude` são recusados
- Writer v3 (`Hl7V3Builder.buildWriter()` → `Hl7V3Writer`): grava XML v3/CDA a partir de `Map` ou VO com o mesmo YAML da leitura, com `write`, `writeBytes` (UTF-8), template opcional (`template(...)`/`templateContent(...)`), raiz pela chave `raiz`/`root`, `indent(boolean)`, predicados `[@attr='valor']` e `[n]`, `lista` + `item` e datas convertidas para TS. Caminhos não graváveis falham no `buildWriter()`
- Constante com caminho (`{ xpath: ..., valor: ... }`) no mapeamento v3: a leitura devolve a constante e o writer a grava no XPath
- `Hl7V3Document.toXml()` para serializar de volta um documento lido
- ACK v3 `MCCI_IN000002UV01`: `Hl7V3AckGenerator`, `Hl7V3AckCode` (`AA`, `AE`, `AR`, `CA`, `CE`, `CR`) e os atalhos `Hl7Handler.generateV3Ack(...)`
- Validação por XSD na leitura e na escrita: `Hl7V3Builder.schema(Path|File|URL)` e `schemaValidation(STRICT|WARN)`; no modo `STRICT`, lança `Hl7V3SchemaValidationException` (subclasse de `Hl7ParseException`) com `errors()`; no `WARN`, os erros vão para `warnings()`
- `CcdaStrategy` embutida para C-CDA (US Realm Header), com `versionIdentifier` `C-CDA-R2.1`, `C-CDA-R2.0` ou `C-CDA-R1.1`, coordenadas do paciente, autor e custodiante e avisos de LOINC, `recordTarget` e `languageCode`; o registry padrão passa a consultar C-CDA, CDA e mensagem, nessa ordem
- `Hl7JsonBinder.toMap(Object)` para converter um VO em `Map`

## [0.1.0] - 2026-11-19

### Adicionado
- Preparo para publicação no GitHub Packages: JARs de `sources` e `javadoc` para `hl7-handler-core`, `hl7-handler-java11` e `hl7-handler-java8`, e versão controlada pela propriedade `revision` (POM publicado com a versão literal)
- `Hl7Processor`, imutável e thread-safe, criado por `Hl7Handler.builder().build()`, com `read(String)`, `readBytes(byte[])`, `readMllp(InputStream)` (devolve `null` em fim de stream) e `hasMapping()`
- `Hl7Result`, resultado de uma leitura parseada uma única vez, com `message()`, `toRawJson()`, `toMap()` (calculado uma vez, em cache e não modificável), `toJson()` e `bindTo(Class)`
- Hierarquia de exceções não verificadas com base `Hl7Exception`: `Hl7ParseException` (entrada ausente ou em branco e falha de leitura MLLP) e `Hl7BindingException` (serialização e desserialização JSON); `Hl7MappingException` e `MllpFramingException` passam a estender `Hl7Exception`
- `Hl7Handler.builder()` passa a aceitar `keepEmptyStrings(boolean)` para devolver strings vazias em vez de `null` nos campos ausentes e `converter(nome, conversor)` para registrar conversores customizados usados em `tipo` no YAML, validados já no `build()`
- **Quebra:** Validação do mapeamento YAML no carregamento (`Hl7MappingValidator`): coordenadas, especificações de lista, tipos e faixas de `versoes`/`porVersao` inválidos, além de chaves YAML duplicadas (leitura com `STRICT_DUPLICATE_DETECTION`), lançam `Hl7MappingException` com o caminho do nó no YAML; mapeamentos que antes carregavam com esses erros passam a falhar em `withMapping(...)`
- Mapeamento YAML semântico avançado por versão com suporte a faixas (`Hl7VersionRange`), sobreposição hierárquica por especificidade (`porVersao`), validação de versões suportadas (`versoes`) e exceção especializada `Hl7MappingException`
- Conversão tipada em mapeamentos YAML (`Hl7TypeConverter`) com suporte nativo a `texto`, `numero` (Long / Double), `data` (`Temporal` via `Hl7DateTime`), `booleano` (S/N, 1/0, SIM/NAO, TRUE/FALSE) e registro dinâmico de conversores customizados
- Mapeamento de coleções e repetições para listas estruturadas a partir de segmentos múltiplos (`lista: "OBX[*]"`) e repetições de campos (`lista: "PID.13[*]"`), guiado por template de `item`
- Suporte a constantes estáticas (`valor`), resolução de caminho alternativo com fallback (`fallback`), coordenadas semânticas (`@event`, `@patientId`) e controle de campos ausentes com retorno padrão `null` (ou strings vazias via `keepEmptyStrings`)
- Suíte completa de testes de mapeamento `Hl7MappingTest` com 14 novos cenários e paridade 100% nos três módulos (`hl7-handler-core`, `hl7-handler-java11`, `hl7-handler-java8`)
- Arquitetura Strategy Pattern por versão HL7 (`io.github.tiagoadmstz.hl7handler.version` e subpacotes `v21` a `v28`), desacoplando regras de MSH-9, MSH-18, truncamento MSH-2, timestamp de cabeçalho (TS/DTM) e coordenadas padrão de evento (`EVN.1`/`MSH.9.2`) e paciente (`PID.2.1`/`PID.3.1`) nos três módulos
- `Hl7VersionStrategyRegistry` thread-safe para resolução exata, floor e baseline (v2.5) de estratégias de versão
- **Quebra:** `Hl7Version` deixa de ser `enum` e passa a ser tipo imutável aberto e comparável (`Comparable<Hl7Version>`), preservando versões como `2.9` e `2.8.2` sem forçá-las a `UNKNOWN`; `switch`, `valueOf` e `ordinal()`/`name()` sobre `Hl7Version` deixam de funcionar
- **Quebra:** `Hl7Reader` ganha o método `withMapping(Map<String, Object>)`; implementações próprias de `Hl7Reader` precisam implementá-lo
- `Hl7VersionProfile` carregado a partir de perfis em `resources/hl7/profiles/*.properties` (2.1 a 2.8) com fallback automático para o perfil mais próximo abaixo
- Utilitário `Hl7DateTime` para interpretação robusta de timestamps HL7 `TS`/`DTM` com precisões variáveis e fusos horários (`OffsetDateTime`, `LocalDateTime`, `LocalDate`)
- Coleta de advertências de conformidade em `Hl7Message.warnings()` (para versões desconhecidas e fallbacks de perfil)
- `Hl7Handler.builder()` para instanciação e customização isoladas de leitores e registros de parsers sem efeitos colaterais globais
- Cache da mensagem parseada em `DefaultHl7Reader` para evitar múltiplos parses desnecessários em chamadas subsequentes
- Resiliência em `Hl7VersionDetector` para ignorar preâmbulos de lotes (`FHS`, `BHS`) e lixo antes do MSH
- `MllpFramingException` para sinalização de violações de moldura MLLP (truncamento por EOF, stream sem 0x0B e limite de tamanho excedido)
- Detecção automática de encoding da mensagem via `MSH-18` em `Hl7Charsets` (`ASCII`, `8859/1`, `8859/15`, `UNICODE`, `UTF-8`, `UTF-16`)
- Gerador de confirmação HL7 ciente da versão em `Hl7AckGenerator`, ecoando `MSH-12`, invertendo `MSH-3/4` ↔ `MSH-5/6`, gerando `MSH-9` e timestamp conforme a versão
- Suporte a pushback no `MllpStreamReader` para não perder mensagens em frames colados sem trailer `0x0D`
- Suporte a delimitadores reais e dinâmicos em `Hl7Delimiters.fromMshHeader`, suportando MSH-2 de 2 a 5 caracteres (inclusive caractere de truncamento da 2.7+)
- Tratamento e roundtrip de sequências de escape HL7 (`\F\`, `\S\`, `\T\`, `\R\`, `\E\`, `\Xhh\`, `\.br\`) via `Hl7Escapes`, preservando sequências de destaque e customizadas
- Preservação de repetições de campo em `Hl7Field`, `Hl7Writer` e exposição em lista no `toRawJson`
- `Hl7Message.get("PID")` serializando e devolvendo o segmento bruto
- Navegação por caminhos com notação de hífen (`PID-3.1`), repetições (`PID.3[2].1`) e ocorrência de segmento (`OBX[2].5`)
- Suporte inicial para HL7 v2.5
- Detecção automática de versão via `MSH-12`
- Leitor/gravador com protocolo MLLP (`network` module)
- Mapeamento de-para via YAML externo com `YamlLoader` e `Transformer`
- Data binding dinâmico em Value Objects (POJO) com Jackson
- Parser strategy pattern para diferentes versões HL7
- Artefatos independentes `hl7-handler-java8` (Java 8) e `hl7-handler-java11` (Java 11 a 16), com Jackson 2, código e testes próprios e a mesma API do `hl7-handler-core`
- `module-info` no `hl7-handler-core` e no `hl7-handler-java11`; `Automatic-Module-Name` no `hl7-handler-java8`
- Amostras e testes de compatibilidade por versão HL7 (2.1 a 2.9, sem MSH-12, MSH-2 curto e com 5 caracteres), em cada artefato
- Módulo `hl7-handler-benchmarks` com benchmarks JMH do parser e do mapeamento (`Hl7Processor.read(...).toMap()`), incluído só pelo perfil `benchmarks` e não publicado

### Mudado
- **Quebra:** groupId e pacote Java passam de io.github.tdteixeira.hl7handler para io.github.tiagoadmstz.hl7handler (inclusive o nome do módulo JPMS).
- **Quebra:** `Hl7Handler.builder().build()` passa a devolver `Hl7Processor`; para o `Hl7Reader` anterior usar `Hl7Handler.builder()...reader()`
- **Quebra:** `MllpFramingException` deixa de ser `IOException` e passa a estender `Hl7Exception` (não verificada); `MllpStreamReader` continua declarando `IOException` para erros de I/O
- **Quebra:** `Hl7JsonBinder` lança `Hl7BindingException` em vez de `RuntimeException`, inclusive em `bindTo` (valor que não converte para o tipo do campo, como texto em campo `int`, passa a ser `Hl7BindingException` e não `IllegalArgumentException` do Jackson)
- **Quebra:** Leitura de mapeamento YAML lança `Hl7MappingException` e leitura MLLP lança `Hl7ParseException` em vez de `UncheckedIOException`; entrada ausente ou em branco lança `Hl7ParseException` em vez de `IllegalStateException`, e `toMap()` sem mapeamento lança `Hl7MappingException` em vez de `IllegalStateException`
- Refatoração inicial da arquitetura para separar concerns
- `hl7-handler-core` passa a exigir Java 17 (antes 25) e usa Jackson 3
- Build com JDK 25 via Maven Toolchains; testes rodam em JDK 8, 11, 17 e 25
- Nos artefatos de Java 8 e 11, `Hl7Delimiters` é uma classe final (no core continua `record`)
- Strategies de versão passam a ler os dados do perfil `.properties` (fonte única), incluindo as novas chaves `event.coordinate` e `patient_id.coordinate`
- `DefaultHl7Parser` e `Hl7VersionDetector` deixam de usar `String.split` com regex: linhas, campos, repetições, componentes e subcomponentes são separados por varredura de caracteres (`indexOf`), com o mesmo resultado (campos vazios no fim são mantidos); o tempo do parser cai de 54% a 73% (ver a seção Desempenho do README)
- Exemplos Spring Boot e Quarkus usam um `Hl7Processor` singleton por arquivo de mapeamento, criado uma vez e reutilizado em cada mensagem

### Consertado
- **Quebra:** Mapa com chaves de negócio, como `exame: {valor: OBX.5, unidade: OBX.6}`, passa a ser lido como grupo e não mais como constante ou coordenada: um mapa só é diretiva quando todas as suas chaves pertencem ao conjunto dela (`lista`/`list`/`item`, `valor`/`value`/`tipo`/`type`, `hl7`/`path`/`campo`/`fallback`/`padrao`/`tipo`/`type`)
- `fromMshHeader` não invade campos subsequentes com MSH-2 curto
- Eliminação de delimitadores fixos (`&` e `^`) no parser e modelo
- **Quebra:** `MllpStreamReader.readMessage` não perde mais o byte seguinte ao trailer entre chamadas consecutivas; em stream sem mark e sem pushback, trailer ausente vira `MllpFramingException`
- `MllpStreamReader` ignora LF (0x0A) e CR fora de uma mensagem, em vez de lançar exceção com `requireFraming` ativo
- `Hl7AckGenerator` escapa o texto da MSA-3 e copia MSH-3 a MSH-6 e MSA-2 em forma crua, preservando componentes, inclusive no MSH-10 do ACK
- **Quebra:** `MllpStreamReader.wrap` respeita o charset do MSH-18 em vez de usar sempre o charset padrão: quadros com MSH-18 diferente do padrão mudam de bytes
- **Quebra:** `\Xhh\` passa a ser decodificado com o charset da mensagem (MSH-18), com UTF-8 como padrão

### Removido
- Dependências não usadas `commons-lang3` e `commons-collections4`

---

## Guia para Contribuidores

### Adicionado
Use para novos recursos, funcionalidades ou assets.

### Mudado
Use para mudanças em funcionalidades existentes.

### Descontinuado
Use para funcionalidades que serão removidas nas versões futuras.

### Removido
Use para funcionalidades removidas nesta versão.

### Consertado
Use para bug fixes.

### Segurança
Use em caso de vulnerabilidades.

---

## Convenção de Versionamento

- **MAJOR**: quebra de compatibilidade (ex: mudança em interfaces públicas)
- **MINOR**: adição de funcionalidades sem quebra de compatibilidade
- **PATCH**: correções de bugs

Exemplo: `1.2.3` = MAJOR 1, MINOR 2, PATCH 3

---

## Template para Nova Versão

```markdown
## [x.y.z] - AAAA-MM-DD

### Adicionado
- Nova funcionalidade A
- Nova funcionalidade B

### Mudado
- Comportamento X agora faz Y

### Descontinuado
- Classe Z será removida em v3.0.0

### Consertado
- `fromMshHeader` não invade campos subsequentes com MSH-2 curto
- Eliminação de delimitadores fixos (`&` e `^`) no parser e modelo

### Removido
- Suporte para Java < 21

### Consertado
- Bug em parsing de campo repetido
- Vazamento de memória em reader

### Segurança
- CVE-XXXX-XXXXX em dependência Jackson
```
