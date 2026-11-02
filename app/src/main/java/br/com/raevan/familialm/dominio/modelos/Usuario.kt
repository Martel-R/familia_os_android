package br.com.raevan.familialm.dominio.modelos

enum class TipoPerfil(val rotulo: String) {
    PAI_ADMIN("Pai / Administrador"),
    FILHO_MEMBRO("Filho / Membro"),
    CUIDADOR("Cuidador / Apoio")
}

data class Familia(
    val id: String,
    val nome: String,
    val codigoConvite: String? = null,
    val plano: String = "familiar"
)

data class Usuario(
    val id: String,
    val nome: String,
    val perfil: TipoPerfil,
    val saldoMoedas: Int = 0,
    val avatarUrl: String? = null,
    val criadoEm: String = "",
    val familiaId: String? = null,
    val email: String? = null
)
