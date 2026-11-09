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
    val familiaAtiva: Familia? = null,
    val resumoFinanceiro: ResumoFinanceiro = ResumoFinanceiro(0.0, 0.0, 0.0, 0.0, 0.0),
    val tarefasDoDia: List<TarefaRotina> = emptyList(),
    val proximoEvento: Evento? = null,
    val dispositivosAtivos: List<DispositivoAutomacao> = emptyList(),
    val redeWifi: RedeWifiFamiliar? = null,
    val ultimoCheckinHumor: CheckinSaude? = null,
    val mensagemAlerta: String? = null
)

class PainelViewModel : ViewModel() {
    private val repositorioUsuarios = ProvedorDependencias.repositorioUsuarios
    private val repositorioFinancas = ProvedorDependencias.repositorioFinancas
    private val repositorioRotinas = ProvedorDependencias.repositorioRotinas
    private val repositorioEventos = ProvedorDependencias.repositorioEventos
    private val repositorioAutomacao = ProvedorDependencias.repositorioAutomacao
    private val repositorioCofre = ProvedorDependencias.repositorioCofre
    private val repositorioSaude = ProvedorDependencias.repositorioSaude

    private val _estado = MutableStateFlow(EstadoPainel(carregando = true))
    val estado: StateFlow<EstadoPainel> = _estado.asStateFlow()

    init {
        observarDados()
    }

    private fun observarDados() {
        viewModelScope.launch {
            val fluxoGrupo1 = combine(
                repositorioUsuarios.obterUsuarioLogado(),
                repositorioUsuarios.obterFamiliaAtiva(),
                repositorioFinancas.obterResumoFinanceiro(),
                repositorioRotinas.listarTarefas()
            ) { usuario, familia, resumo, tarefas ->
                 Pair(Pair(usuario, familia), Pair(resumo, tarefas))
            }

            val fluxoGrupo2 = combine(
                repositorioEventos.listarEventos(),
                repositorioAutomacao.listarDispositivos(),
                repositorioCofre.obterRedeWifiFamiliar(),
                repositorioSaude.listarCheckinsHumor()
            ) { eventos, dispositivos, wifi, checkins ->
                Pair(Pair(eventos, dispositivos), Pair(wifi, checkins))
            }

            combine(fluxoGrupo1, fluxoGrupo2) { grupo1, grupo2 ->
                val (usuario, familia) = grupo1.first
                val (resumo, tarefas) = grupo1.second
                val (eventos, dispositivos) = grupo2.first
                val (wifi, checkins) = grupo2.second

                val meuUltimoCheckin = if (usuario != null) {
                    checkins.firstOrNull { it.usuarioId == usuario.id } ?: checkins.firstOrNull()
                } else {
                    checkins.firstOrNull()
                }

                EstadoPainel(
                    carregando = false,
                    usuarioLogado = usuario,
                    familiaAtiva = familia,
                    resumoFinanceiro = resumo,
                    tarefasDoDia = tarefas.take(4),
                    proximoEvento = eventos.firstOrNull(),
                    dispositivosAtivos = dispositivos.filter { it.estaLigado },
                    redeWifi = wifi,
                    ultimoCheckinHumor = meuUltimoCheckin
                )
            }.collect { novoEstado ->
                _estado.value = novoEstado
            }
        }
    }

    fun recarregarPainel() {
        observarDados()
    }

    fun alternarConclusaoTarefa(tarefaId: String) {
        viewModelScope.launch {
            val moedas = repositorioRotinas.alternarConclusaoTarefa(tarefaId)
            if (moedas > 0) {
                _estado.update { it.copy(mensagemAlerta = "+$moedas moedas ganhas! Parabéns!") }
            }
            observarDados()
        }
    }

    fun alternarDispositivo(dispositivoId: String) {
        viewModelScope.launch {
            repositorioAutomacao.alternarDispositivo(dispositivoId)
        }
    }

    fun registrarCheckinHumorRapido(nivel: Int) {
        viewModelScope.launch {
            val usuario = _estado.value.usuarioLogado ?: repositorioUsuarios.obterUsuarioLogado().firstOrNull()
            val rotulo = when (nivel) {
                1 -> "Muito Mal 😢"
                2 -> "Para Baixo 😕"
                3 -> "Neutro 😐"
                4 -> "Bem 🙂"
                else -> "Excelente 😄"
            }
            val checkin = CheckinSaude(
                id = java.util.UUID.randomUUID().toString(),
                usuarioId = usuario?.id ?: "",
                nomeUsuario = usuario?.nome ?: "Membro",
                nivelHumor = nivel,
                notasDiario = "Check-in rápido pela Tela Inicial ($rotulo)",
                criadoEm = "Agora mesmo"
            )
            repositorioSaude.registrarCheckin(checkin)
            _estado.update {
                it.copy(
                    ultimoCheckinHumor = checkin,
                    mensagemAlerta = "Check-in de humor registrado: $rotulo"
                )
            }
            observarDados()
        }
    }

    fun limparMensagemAlerta() {
        _estado.update { it.copy(mensagemAlerta = null) }
    }
}
