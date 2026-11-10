package io.github.tdteixeira.hl7handler.example.stub.model;

public record PacienteVO(
    String idMensagem,
    String tipoEvento,
    String versaoHl7,
    String prontuario,
    String nome,
    String sobrenome,
    String dataNascimento,
    String sexo,
    EnderecoVO endereco
) {

    // Getters are automatically provided by the record
    // Setters are not applicable for records as they are immutable

    @Override
    public String toString() {
        return "PacienteVO{" +
                "prontuario='" + prontuario + '\'' +
                ", nomeCompleto='" + nome + " " + sobrenome + '\'' +
                ", sexo='" + sexo + '\'' +
                ", endereco=" + endereco +
                '}';
    }
}
