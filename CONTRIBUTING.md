# Guia de Contribuição

Obrigado por considerar contribuir para o **HL7 Handler**! Este documento fornece orientações e instruções para fazer contribuições significativas ao projeto.

## 📋 Código de Conduta

Por favor, leia e respeite nosso [Código de Conduta](./CODE_OF_CONDUCT.md). Esperamos que todos os colaboradores sigam padrões de cordialidade e profissionalismo.

## 🐛 Reportando Bugs

Antes de criar um relatório de bug, verifique se o problema já não foi reportado. Se encontrar um bug:

1. **Descreva o comportamento observado** e o **comportamento esperado**
2. **Forneça exemplos específicos** para demonstrar o problema
3. **Descreva o ambiente**:
   - Versão do Java (ex: Java 25)
   - Versão do Maven
   - Sistema operacional
   - Versão do HL7 Handler
4. **Inclua logs e stack traces** se aplicável

### Template para Report de Bug
```markdown
**Descrição do Bug**
Uma descrição clara do que é o bug.

**Passos para Reproduzir**
1. ...
2. ...

**Comportamento Esperado**
O que deveria acontecer?

**Comportamento Observado**
O que realmente aconteceu?

**Ambiente**
- Java: [versão]
- Maven: [versão]
- SO: [ex: Windows 11, Ubuntu 24.04]
- Versão do hl7-handler: [versão]

**Stack Trace (se disponível)**
```
...
```
```

## 🚀 Sugerindo Melhorias

Se tiver uma ideia para uma nova funcionalidade:

1. Verifique se a sugestão já não existe
2. Forneça uma **descrição clara** do comportamento esperado
3. Liste **exemplos de uso**
4. Descreva como isso seria **benéfico** para o projeto

## 📝 Pull Request

### Antes de Começar

1. **Fork** o repositório
2. **Clone** seu fork localmente
3. **Crie uma branch** baseada em `main`:
   ```bash
   git checkout -b feature/descricao-clara
   git checkout -b fix/corrige-problema-xyz
   ```

### Desenvolvendo

1. **Siga o estilo de código** do projeto (clean code, SOLID). Os três módulos são independentes: toda mudança é feita no `hl7-handler-core`, no `hl7-handler-java11` e no `hl7-handler-java8`, cada um na sintaxe do seu Java mínimo
2. **Escreva testes** para novas funcionalidades
3. **Atualize a documentação** se necessário
4. **Execute testes localmente**:
   ```bash
   mvn clean install
   mvn clean verify
   ```

### Enviando seu PR

1. **Commit** com mensagens claras e descritivas:
   ```bash
   git commit -m "feat: adiciona suporte a mapeamento YAML customizado"
   git commit -m "fix: corrige parsing de campos repetidos"
   ```

2. **Push** para sua branch
3. **Abra um Pull Request** com:
   - Título claro e conciso
   - Descrição detalhada do que foi alterado
   - Referência a issues relacionadas (ex: `Closes #123`)
   - Screenshots/exemplos se relevante

### Padrões de Commit

Use **conventional commits**:
- `feat:` nova funcionalidade
- `fix:` correção de bug
- `docs:` mudanças de documentação
- `style:` formatação, sem mudanças lógicas
- `refactor:` refatoração de código
- `test:` adição ou modificação de testes
- `chore:` atualização de build, dependências, etc.

**Exemplo:**
```
feat(mapping): adiciona suporte a de-para com templates Jinja2

Permite usar templates Jinja2 em arquivos YAML de mapeamento
para transformações mais complexas.

Closes #42
```

## 🧪 Testes

- **Toda** nova funcionalidade deve ter testes
- **Bugs** devem ter testes que reproduzem o problema
- Use **AssertJ** para asserções fluentes
- Organize testes em pacotes paralelos à estrutura do código

Exemplo:
```java
@Test
void shouldParseHl7V25Message() {
    String hl7 = "MSH|^~\\&|LAB|HOSP|||20261109||ADT^A01|MSG001|P|2.5\r";
    
    Hl7Message message = Hl7Handler.reader()
        .from(hl7)
        .toMessage();
    
    assertThat(message.get("MSH.12")).isEqualTo("2.5");
}
```

## 📚 Documentação

- Atualize o `README.md` se alterou o comportamento público
- Adicione JavaDocs para classes e métodos públicos
- Use `code-block` com exemplos reais

## 🔍 Processo de Review

1. Pelo menos **um membro da equipe** revisor deve aprovar
2. **Testes** devem passar
3. **Cobertura de código** não deve diminuir
4. Sem **conflitos** com a branch principal

## 🎯 Áreas de Contribuição Bem-Vinda

- 🐛 **Bugs**: correções de comportamentos inesperados
- ✨ **Novas Versões HL7**: adicionar suporte v2.7, v2.8, etc.
- 📖 **Documentação**: melhorar guias, exemplos
- 🚀 **Performance**: otimizações
- ♿ **Acessibilidade**: melhorar tratamento de erros e mensagens
- 🧪 **Testes**: cobertura adicional

## 💬 Dúvidas?

- Abra uma [Issue](https://github.com/tiagoadmstz/hl7-handler/issues) com label `question`
- Participe das discussões na [Discussions](https://github.com/tiagoadmstz/hl7-handler/discussions)

---

**Obrigado por contribuir! 🙏**
