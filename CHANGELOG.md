# Histórico de Alterações

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere a [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [Não Lançado]

### Adicionado
- `Hl7Version` transformada em tipo imutável aberto e comparável (`Comparable<Hl7Version>`), preservando versões como `2.9` e `2.8.2` sem forçá-las a `UNKNOWN`
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

### Mudado
- Refatoração inicial da arquitetura para separar concerns
- `hl7-handler-core` passa a exigir Java 17 (antes 25) e usa Jackson 3
- Build com JDK 25 via Maven Toolchains; testes rodam em JDK 8, 11, 17 e 25
- Nos artefatos de Java 8 e 11, `Hl7Delimiters` é uma classe final (no core continua `record`)

### Consertado
- `fromMshHeader` não invade campos subsequentes com MSH-2 curto
- Eliminação de delimitadores fixos (`&` e `^`) no parser e modelo

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
