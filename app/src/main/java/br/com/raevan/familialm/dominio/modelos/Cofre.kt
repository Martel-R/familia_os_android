package br.com.raevan.familialm.dominio.modelos

enum class NivelCofre(val rotulo: String) {
    MASTER("Master / Restrito aos Pais"),
    COMPARTILHADO("Compartilhado com a Família")
}

data class CredencialCofre(
    val id: String,
    val titulo: String,
    val nivel: NivelCofre,
    val loginUsuario: String? = null,
    val senhaCriptografada: String,
    val urlAcesso: String? = null,
    val notasSeguras: String? = null,
    val criadoPor: String? = null
)

data class RedeWifiFamiliar(
    val nomeRedeSsid: String,
    val senhaRede: String,
    val tipoSeguranca: String = "WPA2/WPA3",
    val ehRedeVisitantes: Boolean = true
)
