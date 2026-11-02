package br.com.raevan.familialm.apresentacao.automacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dados.sessao.GerenciadorSessao
import br.com.raevan.familialm.dominio.modelos.CameraFrigate
import br.com.raevan.familialm.dominio.modelos.DispositivoAutomacao
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ConfigWhatsappFamilia(
    val url: String = "https://evolution.martel.page",
    val apiKey: String = "",
    val instance: String = "familia_os",
    val statusConexao: String = "desconectado",
    val qrCodeBase64: String? = null
)

data class EstadoAutomacao(
    val carregando: Boolean = false,
    val dispositivos: List<DispositivoAutomacao> = emptyList(),
    val cameras: List<CameraFrigate> = emptyList(),
    val abaSelecionada: Int = 0, // 0: Dispositivos IoT, 1: Câmeras Frigate NVR, 2: WhatsApp Evolution API
    val configWhatsapp: ConfigWhatsappFamilia = ConfigWhatsappFamilia(),
    val carregandoWhatsapp: Boolean = false,
    val mensagemFeedback: String? = null
)

class AutomacaoViewModel : ViewModel() {
    private val repositorioAutomacao = ProvedorDependencias.repositorioAutomacao

    private val _estado = MutableStateFlow(EstadoAutomacao(carregando = true))
    val estado: StateFlow<EstadoAutomacao> = _estado.asStateFlow()

    init {
        observarDados()
        carregarConfigWhatsapp()
    }

    private fun observarDados() {
        viewModelScope.launch {
            combine(
                repositorioAutomacao.listarDispositivos(),
                repositorioAutomacao.listarCameras()
            ) { dispositivos, cameras ->
                _estado.value.copy(
                    carregando = false,
                    dispositivos = dispositivos,
                    cameras = cameras
                )
            }.collect { novoEstado ->
                _estado.value = novoEstado
            }
        }
    }

    fun selecionarAba(indice: Int) {
        _estado.update { it.copy(abaSelecionada = indice) }
        if (indice == 2) carregarConfigWhatsapp()
    }

    fun alternarDispositivo(dispositivoId: String) {
        viewModelScope.launch {
            repositorioAutomacao.alternarDispositivo(dispositivoId)
        }
    }

    fun carregarConfigWhatsapp() {
        viewModelScope.launch {
            _estado.update { it.copy(carregandoWhatsapp = true) }
            val familiaId = try {
                GerenciadorSessao.obterInstancia().obterFamiliaIdAtiva()
            } catch (_: Exception) {
                GerenciadorSessao.DEFAULT_FAMILIA_ID
            }

            withContext(Dispatchers.IO) {
                try {
                    val connCfg = (URL("https://familia.martel.page/api/whatsapp/config?familiaId=$familiaId")
                        .openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 10000
                    }
                    var urlEvo = _estado.value.configWhatsapp.url
                    var apiKeyEvo = _estado.value.configWhatsapp.apiKey
                    var instEvo = _estado.value.configWhatsapp.instance
                    if (connCfg.responseCode in 200..299) {
                        val txt = connCfg.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(txt)
                        urlEvo = json.optString("url", urlEvo)
                        apiKeyEvo = json.optString("apiKey", apiKeyEvo)
                        instEvo = json.optString("instance", instEvo)
                    }

                    val connInst = (URL("https://familia.martel.page/api/whatsapp/instance?familiaId=$familiaId")
                        .openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 10000
                    }
                    var statusCon = "desconectado"
                    if (connInst.responseCode in 200..299) {
                        val txt = connInst.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(txt)
                        statusCon = json.optString("state", "desconectado")
                    }

                    _estado.update {
                        it.copy(
                            carregandoWhatsapp = false,
                            configWhatsapp = ConfigWhatsappFamilia(
                                url = urlEvo,
                                apiKey = apiKeyEvo,
                                instance = instEvo,
                                statusConexao = statusCon
                            )
                        )
                    }
                } catch (_: Exception) {
                    _estado.update { it.copy(carregandoWhatsapp = false) }
                }
            }
        }
    }

    fun salvarConfigWhatsapp(url: String, apiKey: String, instance: String) {
        viewModelScope.launch {
            _estado.update { it.copy(carregandoWhatsapp = true, mensagemFeedback = null) }
            val familiaId = try {
                GerenciadorSessao.obterInstancia().obterFamiliaIdAtiva()
            } catch (_: Exception) {
                GerenciadorSessao.DEFAULT_FAMILIA_ID
            }

            withContext(Dispatchers.IO) {
                try {
                    val conn = (URL("https://familia.martel.page/api/whatsapp/config")
                        .openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        doOutput = true
                        connectTimeout = 12000
                        readTimeout = 12000
                    }
                    val body = JSONObject().apply {
                        put("url", url.trim())
                        put("apiKey", apiKey.trim())
                        put("instance", instance.trim())
                        put("familiaId", familiaId)
                    }
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    val ok = conn.responseCode in 200..299
                    _estado.update {
                        it.copy(
                            carregandoWhatsapp = false,
                            configWhatsapp = it.configWhatsapp.copy(url = url.trim(), apiKey = apiKey.trim(), instance = instance.trim()),
                            mensagemFeedback = if (ok) "Configuração do WhatsApp da família salva!" else "Erro ao salvar configuração."
                        )
                    }
                } catch (_: Exception) {
                    _estado.update {
                        it.copy(carregandoWhatsapp = false, mensagemFeedback = "Falha de conexão ao salvar WhatsApp.")
                    }
                }
            }
        }
    }

    fun limparMensagem() {
        _estado.update { it.copy(mensagemFeedback = null) }
    }
}
