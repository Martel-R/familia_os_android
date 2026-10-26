package br.com.raevan.familialm.apresentacao.rotinas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.ItemLojaPrivilegio
import br.com.raevan.familialm.dominio.modelos.TarefaRotina
import br.com.raevan.familialm.dominio.modelos.Usuario
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EstadoRotinas(
    val carregando: Boolean = false,
    val usuarioLogado: Usuario? = null,
    val membrosFamilia: List<Usuario> = emptyList(),
    val tarefas: List<TarefaRotina> = emptyList(),
    val itensLoja: List<ItemLojaPrivilegio> = emptyList(),
    val abaSelecionada: Int = 0, // 0: Tarefas e Hábitos, 1: Loja de Privilégios
    val membroFiltroId: String? = null,
    val mensagemSucesso: String? = null,
    val mensagemErro: String? = null
)

class RotinasViewModel : ViewModel() {
    private val repositorioRotinas = ProvedorDependencias.repositorioRotinas
    private val repositorioUsuarios = ProvedorDependencias.repositorioUsuarios

    private val _estado = MutableStateFlow(EstadoRotinas(carregando = true))
    val estado: StateFlow<EstadoRotinas> = _estado.asStateFlow()

    init {
        observarDados()
    }

    private fun observarDados() {
        viewModelScope.launch {
            combine(
                repositorioUsuarios.obterUsuarioLogado(),
                repositorioUsuarios.listarMembrosFamilia(),
                repositorioRotinas.listarTarefas(),
                repositorioRotinas.listarItensLoja()
            ) { usuario, membros, tarefas, itens ->
                _estado.value.copy(
                    carregando = false,
                    usuarioLogado = usuario,
                    membrosFamilia = membros,
                    tarefas = tarefas,
                    itensLoja = itens
                )
            }.collect { novoEstado ->
                _estado.value = novoEstado
            }
        }
    }

    fun selecionarAba(indice: Int) {
        _estado.update { it.copy(abaSelecionada = indice) }
    }

    fun selecionarMembroFiltro(membroId: String?) {
        _estado.update { it.copy(membroFiltroId = membroId) }
    }

    fun alternarConclusaoTarefa(tarefaId: String) {
        viewModelScope.launch {
            val moedas = repositorioRotinas.alternarConclusaoTarefa(tarefaId)
            if (moedas > 0) {
                _estado.update { it.copy(mensagemSucesso = "Parabéns! +$moedas moedas adicionadas à sua carteira! 🪙") }
            }
        }
    }

    fun resgatarPrivilegio(item: ItemLojaPrivilegio) {
        val usuario = estado.value.usuarioLogado ?: return
        if (usuario.saldoMoedas < item.custoMoedas) {
            _estado.update { it.copy(mensagemErro = "Moedas insuficientes! Conclua mais tarefas primeiro.") }
            return
        }

        viewModelScope.launch {
            val sucesso = repositorioRotinas.resgatarItemLoja(usuario.id, item.id)
            if (sucesso) {
                _estado.update { it.copy(mensagemSucesso = "Privilégio '${item.titulo}' resgatado com sucesso! Aproveite!") }
            }
        }
    }

    fun limparMensagens() {
        _estado.update { it.copy(mensagemSucesso = null, mensagemErro = null) }
    }
}
