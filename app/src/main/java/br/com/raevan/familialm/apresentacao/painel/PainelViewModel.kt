package br.com.raevan.familialm.apresentacao.painel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.*
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EstadoPainel(
    val carregando: Boolean = false,
    val usuarioLogado: Usuario? = null,
    val resumoFinanceiro: ResumoFinanceiro = ResumoFinanceiro(0.0, 0.0, 0.0, 0.0, 0.0),
    val tarefasDoDia: List<TarefaRotina> = emptyList(),
    val proximoEvento: Evento? = null,
    val dispositivosAtivos: List<DispositivoAutomacao> = emptyList(),
    val redeWifi: RedeWifiFamiliar? = null,
    val mensagemAlerta: String? = null
)

class PainelViewModel : ViewModel() {
    private val repositorioUsuarios = ProvedorDependencias.repositorioUsuarios
    private val repositorioFinancas = ProvedorDependencias.repositorioFinancas
    private val repositorioRotinas = ProvedorDependencias.repositorioRotinas
    private val repositorioEventos = ProvedorDependencias.repositorioEventos
    private val repositorioAutomacao = ProvedorDependencias.repositorioAutomacao
    private val repositorioCofre = ProvedorDependencias.repositorioCofre

    private val _estado = MutableStateFlow(EstadoPainel(carregando = true))
    val estado: StateFlow<EstadoPainel> = _estado.asStateFlow()

    init {
        observarDados()
    }

    private fun observarDados() {
        viewModelScope.launch {
            val fluxoGrupo1 = combine(
                repositorioUsuarios.obterUsuarioLogado(),
                repositorioFinancas.obterResumoFinanceiro(),
                repositorioRotinas.listarTarefas()
            ) { usuario, resumo, tarefas ->
                Triple(usuario, resumo, tarefas)
            }

            val fluxoGrupo2 = combine(
                repositorioEventos.listarEventos(),
                repositorioAutomacao.listarDispositivos(),
                repositorioCofre.obterRedeWifiFamiliar()
            ) { eventos, dispositivos, wifi ->
                Triple(eventos, dispositivos, wifi)
            }

            combine(fluxoGrupo1, fluxoGrupo2) { grupo1, grupo2 ->
                val (usuario, resumo, tarefas) = grupo1
                val (eventos, dispositivos, wifi) = grupo2

                EstadoPainel(
                    carregando = false,
                    usuarioLogado = usuario,
                    resumoFinanceiro = resumo,
                    tarefasDoDia = tarefas.take(4),
                    proximoEvento = eventos.firstOrNull(),
                    dispositivosAtivos = dispositivos.filter { it.estaLigado },
                    redeWifi = wifi
                )
            }.collect { novoEstado ->
                _estado.value = novoEstado
            }
        }
    }

    fun alternarConclusaoTarefa(tarefaId: String) {
        viewModelScope.launch {
            val moedas = repositorioRotinas.alternarConclusaoTarefa(tarefaId)
            if (moedas > 0) {
                _estado.update { it.copy(mensagemAlerta = "+$moedas moedas ganhas! Parabéns!") }
            }
        }
    }

    fun alternarDispositivo(dispositivoId: String) {
        viewModelScope.launch {
            repositorioAutomacao.alternarDispositivo(dispositivoId)
        }
    }

    fun limparMensagemAlerta() {
        _estado.update { it.copy(mensagemAlerta = null) }
    }
}
