package io.github.tdteixeira.hl7handler;

public class PacienteAdmissaoVO {
    private String mensagemId;
    private String tipoMensagem;
    private String evento;
    private String versaoHl7;
    private PacienteDetails paciente;
    private InternacaoDetails internacao;

    public static class PacienteDetails {
        private String prontuario;
        private String nome;
        private String sobrenome;
        private String dataNascimento;
        private String sexo;
        private String telefone;
        private EnderecoDetails endereco;

        public String getProntuario() { return prontuario; }
        public void setProntuario(String prontuario) { this.prontuario = prontuario; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getSobrenome() { return sobrenome; }
        public void setSobrenome(String sobrenome) { this.sobrenome = sobrenome; }
        public String getDataNascimento() { return dataNascimento; }
        public void setDataNascimento(String dataNascimento) { this.dataNascimento = dataNascimento; }
        public String getSexo() { return sexo; }
        public void setSexo(String sexo) { this.sexo = sexo; }
        public String getTelefone() { return telefone; }
        public void setTelefone(String telefone) { this.telefone = telefone; }
        public EnderecoDetails getEndereco() { return endereco; }
        public void setEndereco(EnderecoDetails endereco) { this.endereco = endereco; }
    }

    public static class EnderecoDetails {
        private String logradouro;
        private String cidade;
        private String uf;
        private String cep;

        public String getLogradouro() { return logradouro; }
        public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
        public String getCidade() { return cidade; }
        public void setCidade(String cidade) { this.cidade = cidade; }
        public String getUf() { return uf; }
        public void setUf(String uf) { this.uf = uf; }
        public String getCep() { return cep; }
        public void setCep(String cep) { this.cep = cep; }
    }

    public static class InternacaoDetails {
        private String leito;
        private String quarto;
        private String medico;

        public String getLeito() { return leito; }
        public void setLeito(String leito) { this.leito = leito; }
        public String getQuarto() { return quarto; }
        public void setQuarto(String quarto) { this.quarto = quarto; }
        public String getMedico() { return medico; }
        public void setMedico(String medico) { this.medico = medico; }
    }

    public String getMensagemId() { return mensagemId; }
    public void setMensagemId(String mensagemId) { this.mensagemId = mensagemId; }
    public String getTipoMensagem() { return tipoMensagem; }
    public void setTipoMensagem(String tipoMensagem) { this.tipoMensagem = tipoMensagem; }
    public String getEvento() { return evento; }
    public void setEvento(String evento) { this.evento = evento; }
    public String getVersaoHl7() { return versaoHl7; }
    public void setVersaoHl7(String versaoHl7) { this.versaoHl7 = versaoHl7; }
    public PacienteDetails getPaciente() { return paciente; }
    public void setPaciente(PacienteDetails paciente) { this.paciente = paciente; }
    public InternacaoDetails getInternacao() { return internacao; }
    public void setInternacao(InternacaoDetails internacao) { this.internacao = internacao; }
}
