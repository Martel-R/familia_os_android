package br.com.raevan.familialm.dominio.modelos

data class CheckinSaude(
    val id: String,
    val usuarioId: String,
    val nomeUsuario: String? = null,
    val nivelHumor: Int, // 1: Muito Triste, 2: Triste, 3: Neutro, 4: Bem, 5: Muito Feliz
    val notasDiario: String? = null,
    val criadoEm: String = ""
)

data class FichaMedica(
    val id: String,
    val usuarioId: String,
    val nomeUsuario: String,
    val tipoSanguineo: String? = null,
    val alergias: String? = null,
    val medicamentosUsoContinuo: String? = null,
    val numeroPlanoSaude: String? = null,
    val contatoEmergencia: String? = null
)

data class MedicamentoLembrete(
    val id: String,
    val nomeRemedio: String,
    val dosagem: String,
    val horario: String,
    val membroFamiliar: String,
    val foiTomadoHoje: Boolean = false
)
