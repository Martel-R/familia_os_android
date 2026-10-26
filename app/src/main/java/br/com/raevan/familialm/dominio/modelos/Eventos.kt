package br.com.raevan.familialm.dominio.modelos

enum class StatusRsvp(val rotulo: String) {
    PENDENTE("Pendente"),
    CONFIRMADO("Confirmado"),
    RECUSADO("Recusado")
}

enum class StatusCotacao(val rotulo: String) {
    EM_ANALISE("Em Análise"),
    APROVADA("Aprovada"),
    REJEITADA("Rejeitada")
}

data class Evento(
    val id: String,
    val nome: String,
    val dataEvento: String,
    val descricao: String? = null,
    val orcamentoEstimado: Double? = null,
    val totalGastoReal: Double = 0.0,
    val criadoEm: String = ""
)

data class Convidado(
    val id: String,
    val eventoId: String,
    val nome: String,
    val telefone: String? = null,
    val status: StatusRsvp = StatusRsvp.PENDENTE,
    val restricoesAlimentares: String? = null
)

data class Cotacao(
    val id: String,
    val eventoId: String? = null,
    val itemOuServico: String,
    val fornecedor: String,
    val valor: Double,
    val status: StatusCotacao = StatusCotacao.EM_ANALISE,
    val linkAnexo: String? = null,
    val observacoes: String? = null
)
