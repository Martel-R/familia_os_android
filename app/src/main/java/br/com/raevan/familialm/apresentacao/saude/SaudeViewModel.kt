package br.com.raevan.familialm.apresentacao.saude

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.*
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class EstadoSaude(
    val carregando: Boolean = false,
    val usuarioLogado: Usuario? = null,
    val checkinsHumor: List<CheckinSaude> = emptyList(),
    val fichasMedicas: List<FichaMedica> = emptyList(),
    val medicamentos: List<MedicamentoLembrete> = emptyList(),
    val vacinas: List<VacinaRegistro> = emptyList(),
    val consultas: List<ConsultaExameRegistro> = emptyList(),
    val metricas: List<MetricaVitalRegistro> = emptyList(),
    val abaSelecionada: Int = 0, // 0: SOS, 1: Medicamentos, 2: Vacinas, 3: Consultas, 4: Métricas, 5: Humor
    val modalSosAberto: Boolean = false,
    val mensagemSucesso: String? = null
)

class SaudeViewModel : ViewModel() {
    private val repositorioSaude = ProvedorDependencias.repositorioSaude
    private val repositorioUsuarios = ProvedorDependencias.repositorioUsuarios

    private val _estado = MutableStateFlow(EstadoSaude(carregando = true))
    val estado: StateFlow<EstadoSaude> = _estado.asStateFlow()

    init {
        observarDados()
    }

    private fun observarDados() {
        viewModelScope.launch {
            combine(
                repositorioUsuarios.obterUsuarioLogado(),
                repositorioSaude.listarCheckinsHumor(),
                repositorioSaude.listarFichasMedicas(),
                repositorioSaude.listarLembretesMedicamento()
            ) { usuario, checkins, fichas, medicamentos ->
                TuplaBase(usuario, checkins, fichas, medicamentos)
            }.combine(repositorioSaude.listarVacinas()) { base, vacinas ->
                TuplaComVacinas(base, vacinas)
            }.combine(repositorioSaude.listarConsultasExames()) { base, consultas ->
                TuplaComConsultas(base, consultas)
            }.combine(repositorioSaude.listarMetricasVitais()) { base, metricas ->
                _estado.value.copy(
                    carregando = false,
                    usuarioLogado = base.base.base.usuario,
                    checkinsHumor = base.base.base.checkins,
                    fichasMedicas = base.base.base.fichas,
                    medicamentos = base.base.base.medicamentos,
                    vacinas = base.base.vacinas,
                    consultas = base.consultas,
                    metricas = metricas
                )
            }.collect { novoEstado ->
                _estado.value = novoEstado
            }
        }
    }

    fun selecionarAba(indice: Int) {
        _estado.update { it.copy(abaSelecionada = indice) }
    }

    fun alternarModalSos(aberto: Boolean) {
        _estado.update { it.copy(modalSosAberto = aberto) }
    }

    fun alternarMedicamentoTomado(medicamentoId: String) {
        viewModelScope.launch {
            repositorioSaude.alternarMedicamentoTomado(medicamentoId)
        }
    }

    fun alternarConsultaConcluida(id: String) {
        viewModelScope.launch {
            repositorioSaude.alternarConsultaConcluida(id)
        }
    }

    fun registrarCheckin(nivelHumor: Int, notaDiario: String) {
        val usuario = estado.value.usuarioLogado ?: return
        viewModelScope.launch {
            val novoCheckin = CheckinSaude(
                id = UUID.randomUUID().toString(),
                usuarioId = usuario.id,
                nomeUsuario = usuario.nome.split(" ").first(),
                nivelHumor = nivelHumor,
                notasDiario = notaDiario,
                criadoEm = "Hoje"
            )
            repositorioSaude.registrarCheckin(novoCheckin)
            _estado.update { it.copy(mensagemSucesso = "Check-in de humor registrado com sucesso!") }
        }
    }

    fun limparMensagemSucesso() {
        _estado.update { it.copy(mensagemSucesso = null) }
    }

    private data class TuplaBase(
        val usuario: Usuario,
        val checkins: List<CheckinSaude>,
        val fichas: List<FichaMedica>,
        val medicamentos: List<MedicamentoLembrete>
    )

    private data class TuplaComVacinas(
        val base: TuplaBase,
        val vacinas: List<VacinaRegistro>
    )

    private data class TuplaComConsultas(
        val base: TuplaComVacinas,
        val consultas: List<ConsultaExameRegistro>
    )
}
