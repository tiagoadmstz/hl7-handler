package io.github.tdteixeira.hl7handler.example.stub.model;

public record EnderecoVO(
    String logradouro,
    String cidade,
    String uf,
    String cep
) {

    // Getters are automatically provided by the record
    // Setters are not applicable for records as they are immutable 

    @Override
    public String toString() {
        return logradouro + ", " + cidade + " - " + uf + " (" + cep + ")";
    }
}
