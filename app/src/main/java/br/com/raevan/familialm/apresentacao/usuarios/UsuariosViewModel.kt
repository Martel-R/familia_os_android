package br.com.raevan.familialm.apresentacao.usuarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.Familia
import br.com.raevan.familialm.dominio.modelos.TipoPerfil
import br.com.raevan.familialm.dominio.modelos.Usuario
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class FiltroStatusMembro(val rotulo: String) {
    TODOS("Todos"),
    ATIVOS("Ativos"),
    INATIVOS("Inativos")
}

data class EstadoUsuarios(
    val carregando: Boolean = false,
    val salvando: Boolean = false,
    val familiaAtiva: Familia? = null,
    val usuarioLogado: Usuario? = null,
    val membros: List<Usuario> = emptyList(),
    val filtroStatus: FiltroStatusMembro = FiltroStatusMembro.TODOS,
    val mensagemFeedback: String? = null,
    val erro: String? = null
)

class UsuariosViewModel : ViewModel() {
    private val repositorioUsuarios = ProvedorDependencias.repositorioUsuarios

    private val _estado = MutableStateFlow(EstadoUsuarios(carregando = true))
    val estado: StateFlow<EstadoUsuarios> = _estado.asStateFlow()

    init {
        carregarDados()
    }

    fun carregarDados() {
        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, erro = null) }
            try {
                combine(
                    repositorioUsuarios.obterUsuarioLogado(),
                    repositorioUsuarios.obterFamiliaAtiva(),
                    repositorioUsuarios.listarTodosMembrosFamilia(incluirInativos = true)
                ) { logado, familia, lista ->
                    Triple(logado, familia, lista)
                }.collect { (logado, familia, lista) ->
                    _estado.update {
                        it.copy(
                            carregando = false,
                            usuarioLogado = logado,
                            familiaAtiva = familia,
                            membros = lista
                        )
                    }
                }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        carregando = false,
                        erro = "Não foi possível carregar os membros da família."
                    )
                }
            }
        }
    }

    fun selecionarFiltro(filtro: FiltroStatusMembro) {
        _estado.update { it.copy(filtroStatus = filtro) }
    }

    fun criarNovoMembro(
        nome: String,
        email: String,
        senha: String,
        perfil: TipoPerfil,
        avatarEmoji: String,
        saldoInicial: Int
    ) {
        if (nome.isBlank() || email.isBlank() || senha.length < 6) {
            _estado.update { it.copy(erro = "Informe nome, e-mail válido e senha com no mínimo 6 caracteres.") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(salvando = true, erro = null, mensagemFeedback = null) }
            val resultado = repositorioUsuarios.criarMembroFamilia(
                nome = nome,
                email = email,
                senha = senha,
                perfil = perfil,
                avatarEmoji = avatarEmoji,
                saldoMoedas = saldoInicial
            )
            resultado.fold(
                onSuccess = {
                    _estado.update {
                        it.copy(
                            salvando = false,
                            mensagemFeedback = "Membro '${nome.trim()}' criado e vinculado à família com sucesso!"
                        )
                    }
                    carregarDados()
                },
                onFailure = { err ->
                    _estado.update {
                        it.copy(
                            salvando = false,
                            erro = err.message ?: "Erro ao criar membro da família."
                        )
                    }
                }
            )
        }
    }

    fun editarMembro(
        usuarioId: String,
        nome: String,
        perfil: TipoPerfil,
        avatarEmoji: String?,
        ativo: Boolean
    ) {
        viewModelScope.launch {
            _estado.update { it.copy(salvando = true, erro = null) }
            val ok = repositorioUsuarios.atualizarMembroFamilia(
                usuarioId = usuarioId,
                nome = nome,
                perfil = perfil,
                avatarEmoji = avatarEmoji,
                ativo = ativo
            )
            _estado.update {
                it.copy(
                    salvando = false,
                    mensagemFeedback = if (ok) "Dados de '$nome' atualizados!" else null,
                    erro = if (!ok) "Não foi possível atualizar o membro." else null
                )
            }
            if (ok) carregarDados()
        }
    }

    fun ajustarMoedas(usuarioId: String, nomeMembro: String, deltaMoedas: Int) {
        viewModelScope.launch {
            repositorioUsuarios.atualizarSaldoMoedas(usuarioId, deltaMoedas)
            val sinal = if (deltaMoedas >= 0) "+$deltaMoedas" else "$deltaMoedas"
            _estado.update {
                it.copy(mensagemFeedback = "Saldo de $nomeMembro ajustado ($sinal 🪙)!")
            }
            carregarDados()
        }
    }

    fun limparMensagens() {
        _estado.update { it.copy(mensagemFeedback = null, erro = null) }
    }
}
