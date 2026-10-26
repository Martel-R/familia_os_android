package br.com.raevan.familialm.apresentacao.automacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.CameraFrigate
import br.com.raevan.familialm.dominio.modelos.DispositivoAutomacao
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EstadoAutomacao(
    val carregando: Boolean = false,
    val dispositivos: List<DispositivoAutomacao> = emptyList(),
    val cameras: List<CameraFrigate> = emptyList(),
    val abaSelecionada: Int = 0 // 0: Dispositivos IoT, 1: Câmeras Frigate NVR
)

class AutomacaoViewModel : ViewModel() {
    private val repositorioAutomacao = ProvedorDependencias.repositorioAutomacao

    private val _estado = MutableStateFlow(EstadoAutomacao(carregando = true))
    val estado: StateFlow<EstadoAutomacao> = _estado.asStateFlow()

    init {
        observarDados()
    }

    private fun observarDados() {
        viewModelScope.launch {
            combine(
                repositorioAutomacao.listarDispositivos(),
                repositorioAutomacao.listarCameras()
            ) { dispositivos, cameras ->
                EstadoAutomacao(
                    carregando = false,
                    dispositivos = dispositivos,
                    cameras = cameras,
                    abaSelecionada = _estado.value.abaSelecionada
                )
            }.collect { novoEstado ->
                _estado.value = novoEstado
            }
        }
    }

    fun selecionarAba(indice: Int) {
        _estado.update { it.copy(abaSelecionada = indice) }
    }

    fun alternarDispositivo(dispositivoId: String) {
        viewModelScope.launch {
            repositorioAutomacao.alternarDispositivo(dispositivoId)
        }
    }
}
