package br.com.raevan.familialm.apresentacao.eventos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.Convidado
import br.com.raevan.familialm.dominio.modelos.Cotacao
import br.com.raevan.familialm.dominio.modelos.Evento
import br.com.raevan.familialm.dominio.modelos.StatusRsvp
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EstadoEventos(
    val carregando: Boolean = false,
    val eventos: List<Evento> = emptyList(),
    val eventoSelecionadoId: String? = null,
    val convidados: List<Convidado> = emptyList(),
    val cotacoes: List<Cotacao> = emptyList(),
    val abaSelecionada: Int = 0 // 0: Convidados (RSVP), 1: Central de Cotações
)

class EventosViewModel : ViewModel() {
    private val repositorioEventos = ProvedorDependencias.repositorioEventos

    private val _estado = MutableStateFlow(EstadoEventos(carregando = true))
    val estado: StateFlow<EstadoEventos> = _estado.asStateFlow()

    init {
        observarEventos()
    }

    private fun observarEventos() {
        viewModelScope.launch {
            repositorioEventos.listarEventos().collect { listaEventos ->
                val idPrimeiro = listaEventos.firstOrNull()?.id
                _estado.update {
                    it.copy(
                        carregando = false,
                        eventos = listaEventos,
                        eventoSelecionadoId = it.eventoSelecionadoId ?: idPrimeiro
                    )
                }
                if (idPrimeiro != null) {
                    carregarDetalhesEvento(idPrimeiro)
                }
            }
        }
    }

    fun selecionarEvento(eventoId: String) {
        _estado.update { it.copy(eventoSelecionadoId = eventoId) }
        carregarDetalhesEvento(eventoId)
    }

    fun selecionarAba(indice: Int) {
        _estado.update { it.copy(abaSelecionada = indice) }
    }

    private fun carregarDetalhesEvento(eventoId: String) {
        viewModelScope.launch {
            combine(
                repositorioEventos.listarConvidados(eventoId),
                repositorioEventos.listarCotacoes(eventoId)
            ) { convs, cots ->
                convs to cots
            }.collect { (convs, cots) ->
                _estado.update {
                    it.copy(convidados = convs, cotacoes = cots)
                }
            }
        }
    }

    fun alternarRsvp(convidadoId: String, novoStatus: StatusRsvp) {
        viewModelScope.launch {
            repositorioEventos.atualizarStatusRsvp(convidadoId, novoStatus)
        }
    }
}
