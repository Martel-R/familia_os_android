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
    val contatoEmergencia: String? = null,
    val carteirinhaPlanoUrl: String? = null,
    val numeroPlanoOdontologico: String? = null,
    val carteirinhaOdontoUrl: String? = null,
    val resumoClinicoIa: String? = null,
    val orientacoesMedicoIa: String? = null,
    val atualizadoIaEm: String? = null
)

data class MedicamentoLembrete(
    val id: String,
    val nomeRemedio: String,
    val dosagem: String,
    val horario: String,
    val membroFamiliar: String,
    val foiTomadoHoje: Boolean = false
)

data class VacinaRegistro(
    val id: String,
    val usuarioId: String,
    val nomeMembro: String = "Membro",
    val nomeVacina: String,
    val dose: String = "1ª Dose",
    val dataAplicacao: String,
    val dataProximaDose: String? = null,
    val lote: String? = null,
    val localAplicacao: String? = null
)

data class ConsultaExameRegistro(
    val id: String,
    val usuarioId: String,
    val nomeMembro: String = "Membro",
    val tipo: String, // "consulta" ou "exame"
    val titulo: String,
    val especialistaOuClinica: String? = null,
    val dataAgendada: String? = null,
    val dataRetorno: String? = null,
    val observacoes: String? = null,
    val resultado: String? = null,
    val status: String = "agendado",
    val contatoId: String? = null,
    val eventoId: String? = null,
    val concluido: Boolean = false
)

data class MetricaVitalRegistro(
    val id: String,
    val usuarioId: String,
    val nomeMembro: String = "Membro",
    val tipoMetrica: String, // 'pressao', 'glicemia', 'peso', 'altura', 'temperatura'
    val valorPrincipal: Double,
    val valorSecundario: Double? = null,
    val unidade: String,
    val observacao: String? = null,
    val dataMedicao: String
)
