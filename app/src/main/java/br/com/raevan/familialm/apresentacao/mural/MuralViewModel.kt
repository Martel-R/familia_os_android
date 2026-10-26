package br.com.raevan.familialm.apresentacao.mural

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.ConquistaMural
import br.com.raevan.familialm.dominio.modelos.FotoAlbum
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EstadoMural(
    val carregando: Boolean = false,
    val conquistas: List<ConquistaMural> = emptyList(),
    val fotos: List<FotoAlbum> = emptyList(),
    val abaSelecionada: Int = 0 // 0: Mural de Vitórias, 1: Álbum Privado de Fotos
)

class MuralViewModel : ViewModel() {
    private val repositorioMural = ProvedorDependencias.repositorioMural

    private val _estado = MutableStateFlow(EstadoMural(carregando = true))
    val estado: StateFlow<EstadoMural> = _estado.asStateFlow()

    init {
        observarDados()
    }

    private fun observarDados() {
        viewModelScope.launch {
            combine(
                repositorioMural.listarConquistas(),
                repositorioMural.listarFotosAlbum()
            ) { conquistas, fotos ->
                EstadoMural(
                    carregando = false,
                    conquistas = conquistas,
                    fotos = fotos,
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

    fun reagirConquista(conquistaId: String) {
        viewModelScope.launch {
            repositorioMural.adicionarReacao(conquistaId)
        }
    }
}
