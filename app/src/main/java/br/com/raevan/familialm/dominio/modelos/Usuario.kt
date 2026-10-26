package br.com.raevan.familialm.dominio.modelos

enum class TipoPerfil(val rotulo: String) {
    PAI_ADMIN("Pai / Administrador"),
    FILHO_MEMBRO("Filho / Membro"),
    CUIDADOR("Cuidador / Apoio")
}

data class Usuario(
    val id: String,
    val nome: String,
    val perfil: TipoPerfil,
    val saldoMoedas: Int = 0,
    val avatarUrl: String? = null,
    val criadoEm: String = ""
)
