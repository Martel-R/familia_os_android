package br.com.raevan.familialm.dominio.modelos

enum class TipoTransacao(val rotulo: String) {
    RECEITA("Receita"),
    DESPESA_FIXA("Despesa Fixa"),
    DESPESA_VARIAVEL("Despesa Variável"),
    ASSINATURA("Assinatura / Mensalidade")
}

enum class StatusPagamento(val rotulo: String) {
    PENDENTE("Pendente"),
    PAGO("Pago"),
    ATRASADO("Atrasado")
}

data class CategoriaFinanceira(
    val id: String,
    val nome: String,
    val icone: String? = null,
    val cor: String? = null
)

data class TransacaoFinanceira(
    val id: String,
    val usuarioId: String? = null,
    val categoriaId: String? = null,
    val nomeCategoria: String? = null,
    val tipo: TipoTransacao,
    val descricao: String,
    val valor: Double,
    val dataVencimento: String,
    val dataPagamento: String? = null,
    val status: StatusPagamento = StatusPagamento.PENDENTE,
    val observacoes: String? = null,
    val criadoEm: String = ""
)

data class Cofrinho(
    val id: String,
    val nome: String,
    val metaValor: Double,
    val saldoAtual: Double = 0.0,
    val dataLimite: String? = null,
    val icone: String? = null
) {
    val progressoPercentual: Float
        get() = if (metaValor > 0.0) ((saldoAtual / metaValor).coerceIn(0.0, 1.0)).toFloat() else 0f
}

data class ResumoFinanceiro(
    val totalReceitas: Double,
    val totalDespesasPagas: Double,
    val totalDespesasPendentes: Double,
    val saldoLivreAlocacao: Double,
    val totalCofrinhos: Double
)
