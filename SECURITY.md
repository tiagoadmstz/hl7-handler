# Política de Segurança

## Versões suportadas

Apenas a última versão `0.x` (minor mais recente) recebe correções de segurança.

| Versão | Suporte |
|---|---|
| Última `0.x` publicada | Sim |
| Demais versões | Não |

## Reportando uma vulnerabilidade

**Não abra uma issue pública.** Reporte pelo recurso de relatórios privados do GitHub:

1. Acesse a aba **Security** do repositório.
2. Clique em **Report a vulnerability**.

Ou abra diretamente: https://github.com/tiagoadmstz/hl7-handler/security/advisories/new

Informe, se possível:

- descrição da vulnerabilidade e o impacto esperado;
- versão afetada;
- passos para reproduzir (uma mensagem HL7 de exemplo, com dados fictícios, ajuda muito).

## Prazos

- **Confirmação do recebimento:** em até 7 dias.
- **Plano de correção ou resposta:** em até 30 dias.

Os prazos são de resposta, não de correção: a correção depende da gravidade e da complexidade do caso.

## Escopo

Estão no escopo:

- **Entrada HL7 não confiável:** mensagens malformadas que causem exceção não tratada, travamento, consumo excessivo de memória ou CPU, ou leitura de dados de uma mensagem em outra.
- **Frames MLLP acima do limite:** `MllpStreamReader` rejeita mensagens maiores que 1 MB por padrão (`DEFAULT_MAX_MESSAGE_SIZE`), com `MllpFramingException`. Contornar esse limite é considerado vulnerabilidade.
- **Carregamento de mapeamento YAML:** arquivos de mapeamento de terceiros que causem execução de código, leitura de arquivos fora do esperado ou consumo excessivo de recursos durante o carregamento.

Fora do escopo: problemas da aplicação que usa a biblioteca (autenticação, autorização, armazenamento de dados) e vulnerabilidades em dependências que não sejam exploráveis através da API pública.
