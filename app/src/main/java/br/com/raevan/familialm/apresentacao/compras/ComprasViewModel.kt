package br.com.raevan.familialm.apresentacao.compras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.ReciboCompra
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EstadoCompras(
    val carregando: Boolean = false,
    val recibos: List<ReciboCompra> = emptyList(),
    val mensagemSucesso: String? = null
)

class ComprasViewModel : ViewModel() {
    private val repositorioCompras = ProvedorDependencias.repositorioCompras

    private val _estado = MutableStateFlow(EstadoCompras(carregando = true))
    val estado: StateFlow<EstadoCompras> = _estado.asStateFlow()

    init {
        observarDados()
    }

    private fun observarDados() {
        viewModelScope.launch {
            repositorioCompras.listarRecibos().collect { lista ->
                _estado.value = EstadoCompras(carregando = false, recibos = lista)
            }
        }
    }

    fun escanearNovoCupom(estabelecimento: String, valorTotal: Double) {
        viewModelScope.launch {
            repositorioCompras.simularOcrCupomFiscal(estabelecimento, valorTotal)
            _estado.update { it.copy(mensagemSucesso = "Cupom de $estabelecimento digitalizado e lançado!") }
        }
    }

    fun limparMensagem() {
        _estado.update { it.copy(mensagemSucesso = null) }
    }
}
