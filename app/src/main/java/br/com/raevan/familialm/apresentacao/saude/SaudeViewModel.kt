package br.com.raevan.familialm.apresentacao.saude

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.CheckinSaude
import br.com.raevan.familialm.dominio.modelos.FichaMedica
import br.com.raevan.familialm.dominio.modelos.MedicamentoLembrete
import br.com.raevan.familialm.dominio.modelos.Usuario
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
    val abaSelecionada: Int = 0, // 0: Ficha de Emergência, 1: Check-in de Humor, 2: Medicamentos
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
                _estado.value.copy(
                    carregando = false,
                    usuarioLogado = usuario,
                    checkinsHumor = checkins,
                    fichasMedicas = fichas,
                    medicamentos = medicamentos
                )
            }.collect { novoEstado ->
                _estado.value = novoEstado
            }
        }
    }

    fun selecionarAba(indice: Int) {
        _estado.update { it.copy(abaSelecionada = indice) }
    }

    fun alternarMedicamentoTomado(medicamentoId: String) {
        viewModelScope.launch {
            repositorioSaude.alternarMedicamentoTomado(medicamentoId)
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
}
