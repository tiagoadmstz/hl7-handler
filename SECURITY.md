# Política de Segurança

## Reportando Vulnerabilidades

**NÃO abra uma issue pública** para reportar vulnerabilidades de segurança. 

Em vez disso, por favor reporte vulnerabilidades por email para:

📧 **security@hl7-handler.dev** (substitua pelo email real do projeto)

Ao reportar uma vulnerabilidade, forneça:

1. **Descrição detalhada** da vulnerabilidade
2. **Versão(ns) afetada(s)** do projeto
3. **Passos para reproduzir** (se possível)
4. **Impacto potencial** na segurança
5. **Sugestões de correção** (opcional)

## Esperamos uma Resposta em:

- **24 horas**: Confirmação do recebimento
- **72 horas**: Resposta inicial com plano de ação
- **30 dias**: Plano de divulgação coordenado

## Processo de Divulgação

1. Você reporta a vulnerabilidade
2. Nós confirmamos o recebimento e iniciamos investigação
3. Desenvolvemos uma correção em uma branch privada
4. Publicamos um patch release e divulgamos responsavelmente
5. Um CVE é gerado (se aplicável)

## Prioridades

Vulnerabilidades são priorizadas por:

1. **CRÍTICA**: Execução remota de código, autenticação contornável
2. **ALTA**: Injeção SQL, XSS, CSRF
3. **MÉDIA**: Divulgação de informações, DoS
4. **BAIXA**: Outras vulnerabilidades

## Conformidade e Boas Práticas

Este projeto segue:

- 🛡️ [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- 🔍 [CWE Top 25](https://cwe.mitre.org/top25/)
- 📋 Verificação de dependências com ferramentas como OWASP Dependency-Check

## Dependências

Mantemos nossas dependências atualizadas e auditadas regularmente:

```bash
# Verificar vulnerabilidades conhecidas
mvn dependency-check:check
```

## Testes de Segurança

O projeto inclui:

- ✅ Validação de entrada
- ✅ Tratamento seguro de erros
- ✅ Sem hardcoding de secrets
- ✅ Proteção contra injeção de código

## Responsáveis pela Segurança

- **Mantainer Líder**: [Seu Nome/Equipe]
- **Revisor de Segurança**: [Nome]

---

**Agradecemos sua cooperação em manter o HL7 Handler seguro! 🙏**
