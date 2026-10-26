package br.com.raevan.familialm.dominio.modelos

data class ConquistaMural(
    val id: String,
    val titulo: String,
    val descricao: String?,
    val usuarioId: String? = null,
    val nomeMembro: String? = null,
    val dataConquista: String,
    val caminhoFoto: String? = null,
    val totalReacoes: Int = 0
)

data class FotoAlbum(
    val id: String,
    val titulo: String,
    val autorNome: String,
    val dataEnvio: String,
    val ehPrivado: Boolean = false,
    val urlMiniatura: String = ""
)
