package br.com.raevan.familialm.apresentacao.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.ContatoAgenda
import br.com.raevan.familialm.dominio.modelos.HistoricoManutencao
import br.com.raevan.familialm.dominio.modelos.ItemInventario
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.flow.*

data class EstadoInventario(
    val carregando: Boolean = false,
    val itens: List<ItemInventario> = emptyList(),
    val contatos: List<ContatoAgenda> = emptyList(),
    val manutencoes: List<HistoricoManutencao> = emptyList(),
    val termoBusca: String = "",
    val abaSelecionada: Int = 0 // 0: Itens da Casa, 1: Agenda de Contatos, 2: Histórico Manutenções
)

class InventarioViewModel : ViewModel() {
    private val repositorioInventario = ProvedorDependencias.repositorioInventario

    private val _termoBusca = MutableStateFlow("")
    private val _abaSelecionada = MutableStateFlow(0)

    val estado: StateFlow<EstadoInventario> = combine(
        _termoBusca,
        _abaSelecionada,
        repositorioInventario.listarItensInventario(),
        repositorioInventario.listarContatos(),
        repositorioInventario.listarHistoricoManutencoes()
    ) { termo, aba, todosItens, contatos, manutencoes ->
        val itensFiltrados = if (termo.isBlank()) todosItens
        else todosItens.filter {
            it.nome.contains(termo, ignoreCase = true) || it.localizacao.contains(termo, ignoreCase = true)
        }

        EstadoInventario(
            carregando = false,
            itens = itensFiltrados,
            contatos = contatos,
            manutencoes = manutencoes,
            termoBusca = termo,
            abaSelecionada = aba
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EstadoInventario(carregando = true))

    fun atualizarBusca(termo: String) {
        _termoBusca.value = termo
    }

    fun selecionarAba(indice: Int) {
        _abaSelecionada.value = indice
    }
}
