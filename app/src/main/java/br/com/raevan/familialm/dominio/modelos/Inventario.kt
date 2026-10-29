package br.com.raevan.familialm.dominio.modelos

data class ContatoAgenda(
    val id: String,
    val nome: String,
    val especialidade: String? = null,
    val telefone: String,
    val ehEmergencia: Boolean = false,
    val observacoes: String? = null,
    val email: String? = null,
    val selecionado: Boolean = false,
    val categoria: String = "amigos",
    val membroId: String? = null,
    val grauParentesco: String? = null
)

data class ItemInventario(
    val id: String,
    val nome: String,
    val localizacao: String,
    val quantidade: Int = 1,
    val dataCompra: String? = null,
    val dataFimGarantia: String? = null,
    val caminhoNotaFiscal: String? = null,
    val caminhoManual: String? = null
)

data class HistoricoManutencao(
    val id: String,
    val itemId: String? = null,
    val nomeItem: String? = null,
    val prestadorId: String? = null,
    val nomePrestador: String? = null,
    val descricao: String,
    val dataServico: String,
    val custo: Double? = null,
    val dataProximaManutencao: String? = null
)
