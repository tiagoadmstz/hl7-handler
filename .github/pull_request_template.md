### Descrição
<!-- Resuma em poucas linhas o que esta alteração faz. -->

### Motivação
<!-- Qual problema está sendo resolvido ou qual necessidade motivou a alteração? -->

### Alterações
<!-- Liste apenas os principais pontos. Diga se a mudança foi feita nos três módulos (core, java11, java8). -->
-

### Validação
<!-- Como você verificou que a alteração funciona? Testes automatizados, testes manuais, JDKs usados. -->
-

### Como testar
<!-- Apenas os passos necessários para validar a alteração (comando, amostra HL7 usada). -->
N/A

### Observações
<!-- Restrições, impactos conhecidos, quebra de API, dependências novas. Em correção de bug: de onde o bug veio. (Opcional) -->
N/A

### Links relacionados
<!-- Issue, PR dependente, provável causa do bug ou documentação, sempre em formato de link. -->
-

### Checklist
- [ ] Alteração aplicada em `hl7-handler-core`, `hl7-handler-java11` e `hl7-handler-java8`
- [ ] Mesmos testes nos três módulos, passando em JDK 8, 11, 17 e 25 (`./mvnw verify`)
- [ ] Amostras de teste só com dados fictícios
- [ ] `CHANGELOG.md` atualizado em `[Não Lançado]` (se muda comportamento ou API)
- [ ] README atualizado (se muda o uso público)
- [ ] Dependências novas ou atualizadas publicadas há pelo menos duas semanas
