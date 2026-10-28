package br.com.raevan.familialm.dominio.modelos

enum class RecorrenciaTarefa(val rotulo: String) {
    UNICA("Única"),
    DIARIA("Diária"),
    SEMANAL("Semanal"),
    MENSAL("Mensal")
}

data class TarefaRotina(
    val id: String,
    val titulo: String,
    val responsavelId: String? = null,
    val nomeResponsavel: String? = null,
    val recorrencia: RecorrenciaTarefa = RecorrenciaTarefa.DIARIA,
    val moedasRecompensa: Int = 5,
    val dataLimite: String? = null,
    val estaConcluida: Boolean = false,
    val atualizadoEm: String = "",
    val diasSemana: String? = null,
    val diaMes: Int? = null,
    val abertaParaTodos: Boolean = false,
    val ultimaConclusaoData: String? = null
)

data class ItemLojaPrivilegio(
    val id: String,
    val titulo: String,
    val custoMoedas: Int,
    val icone: String? = null,
    val estoqueDisponivel: Int? = null,
    val descricao: String = ""
)
