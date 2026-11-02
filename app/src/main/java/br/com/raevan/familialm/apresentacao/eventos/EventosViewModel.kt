package br.com.raevan.familialm.apresentacao.eventos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dados.sessao.GerenciadorSessao
import br.com.raevan.familialm.dominio.modelos.Convidado
import br.com.raevan.familialm.dominio.modelos.Cotacao
import br.com.raevan.familialm.dominio.modelos.Evento
import br.com.raevan.familialm.dominio.modelos.StatusCotacao
import br.com.raevan.familialm.dominio.modelos.StatusRsvp
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class EstadoEventos(
    val carregando: Boolean = false,
    val eventos: List<Evento> = emptyList(),
    val eventoSelecionadoId: String? = null,
    val convidados: List<Convidado> = emptyList(),
    val cotacoes: List<Cotacao> = emptyList(),
    val abaSelecionada: Int = 0, // 0: Convidados (RSVP), 1: Central de Cotações
    val mensagemFeedback: String? = null
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
            _estado.value.eventoSelecionadoId?.let { carregarDetalhesEvento(it) }
        }
    }

    fun criarNovoEvento(nome: String, dataEvento: String, descricao: String?, orcamento: Double) {
        if (nome.isBlank() || dataEvento.isBlank()) return
        viewModelScope.launch {
            repositorioEventos.adicionarEvento(nome, dataEvento, descricao, orcamento)
            _estado.update { it.copy(mensagemFeedback = "Evento '$nome' criado!") }
            observarEventos()
        }
    }

    fun adicionarConvidado(nome: String, telefone: String?, restricoes: String?) {
        val evId = _estado.value.eventoSelecionadoId ?: return
        if (nome.isBlank()) return
        viewModelScope.launch {
            repositorioEventos.adicionarConvidado(evId, nome, telefone, restricoes)
            _estado.update { it.copy(mensagemFeedback = "Convidado '$nome' adicionado!") }
            carregarDetalhesEvento(evId)
        }
    }

    fun adicionarCotacao(itemOuServico: String, fornecedor: String, valor: Double, observacoes: String?) {
        val evId = _estado.value.eventoSelecionadoId ?: return
        if (itemOuServico.isBlank() || fornecedor.isBlank()) return
        viewModelScope.launch {
            repositorioEventos.adicionarCotacao(
                Cotacao(
                    id = UUID.randomUUID().toString(),
                    eventoId = evId,
                    itemOuServico = itemOuServico,
                    fornecedor = fornecedor,
                    valor = valor,
                    status = StatusCotacao.EM_ANALISE,
                    linkAnexo = null,
                    observacoes = observacoes
                )
            )
            _estado.update { it.copy(mensagemFeedback = "Cotação registrada!") }
            carregarDetalhesEvento(evId)
        }
    }

    fun enviarConviteWhatsapp(convidado: Convidado, evento: Evento) {
        val tel = convidado.telefone?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            val familiaId = try {
                GerenciadorSessao.obterInstancia().obterFamiliaIdAtiva()
            } catch (_: Exception) {
                GerenciadorSessao.DEFAULT_FAMILIA_ID
            }
            val enviado = withContext(Dispatchers.IO) {
                try {
                    val conn = (URL("https://familia.martel.page/api/whatsapp/send-invite")
                        .openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        doOutput = true
                        connectTimeout = 12000
                        readTimeout = 12000
                    }
                    val body = JSONObject().apply {
                        put("telefone", tel)
                        put("nomeConvidado", convidado.nome)
                        put("nomeEvento", evento.nome)
                        put("dataEvento", evento.dataEvento)
                        put("descricaoEvento", evento.descricao ?: "")
                        put("familiaId", familiaId)
                    }
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    conn.responseCode in 200..299
                } catch (_: Exception) {
                    false
                }
            }
            _estado.update {
                it.copy(
                    mensagemFeedback = if (enviado) {
                        "Convite WhatsApp enviado para ${convidado.nome}!"
                    } else {
                        "Não foi possível disparar via Evolution API. Verifique a instância WhatsApp."
                    }
                )
            }
        }
    }

    fun limparMensagem() {
        _estado.update { it.copy(mensagemFeedback = null) }
    }
}
