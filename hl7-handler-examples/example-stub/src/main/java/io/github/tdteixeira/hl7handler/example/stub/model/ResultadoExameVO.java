package io.github.tdteixeira.hl7handler.example.stub.model;

public record ResultadoExameVO(
    String idMensagem,
    String prontuario,
    String nomePaciente,
    String codigoExame,
    String descricaoExame,
    String valor,
    String unidade,
    String intervaloReferencia,
    String interpretacao
) {

    // Getters are automatically provided by the record
    // Setters are not applicable for records as they are immutable

    @Override
    public String toString() {
        return "ResultadoExameVO{" +
                "exame='" + descricaoExame + " (" + codigoExame + ")'" +
                ", resultado='" + valor + " " + unidade + '\'' +
                ", referencia='" + intervaloReferencia + '\'' +
                ", paciente='" + nomePaciente + '\'' +
                '}';
    }
}
