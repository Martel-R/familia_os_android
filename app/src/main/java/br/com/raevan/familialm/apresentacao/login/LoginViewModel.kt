package br.com.raevan.familialm.apresentacao.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dados.rede.ClienteSupabase
import br.com.raevan.familialm.dados.remoto.dto.FamiliaDto
import br.com.raevan.familialm.dados.sessao.GerenciadorSessao
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginViewModel : ViewModel() {

    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando.asStateFlow()

    private val _mensagemErro = MutableStateFlow<String?>(null)
    val mensagemErro: StateFlow<String?> = _mensagemErro.asStateFlow()

    private val _usuarioAutenticado = MutableStateFlow(false)
    val usuarioAutenticado: StateFlow<Boolean> = _usuarioAutenticado.asStateFlow()

    init {
        verificarSessao()
    }

    fun verificarSessao() {
        val sessao = try { ClienteSupabase.client.auth.currentSessionOrNull() } catch (_: Exception) { null }
        val temSessaoSalva = try { GerenciadorSessao.obterInstancia().temSessaoSalva() } catch (_: Exception) { false }
        _usuarioAutenticado.value = (sessao != null || temSessaoSalva)
    }

    private suspend fun sincronizarTenantPorEmail(emailUsuario: String) = withContext(Dispatchers.IO) {
        try {
            val emailLimpo = emailUsuario.trim()
            if (emailLimpo.isBlank()) return@withContext
            val authUserId = try { ClienteSupabase.client.auth.currentUserOrNull()?.id ?: "" } catch (_: Exception) { "" }
            val url = URL(
                "https://familia.martel.page/api/tenant?email=${java.net.URLEncoder.encode(emailLimpo, "UTF-8")}&authUserId=${java.net.URLEncoder.encode(authUserId, "UTF-8")}"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", ClienteSupabase.USER_AGENT_PADRAO)
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (conn.responseCode in 200..299) {
                val txt = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(txt)
                val famObj = json.optJSONObject("familia")
                if (famObj != null) {
                    val famId = famObj.optString("id").takeIf { it.isNotBlank() } ?: GerenciadorSessao.DEFAULT_FAMILIA_ID
                    val famNome = famObj.optString("nome").takeIf { it.isNotBlank() } ?: "Minha Família"
                    val famCodigo = famObj.optString("codigo_convite").takeIf { it.isNotBlank() } ?: "MARTEL"
                    GerenciadorSessao.obterInstancia().salvarFamiliaAtiva(famId, famNome, famCodigo)
                }
            }
        } catch (_: Exception) {}
    }

    fun fazerLogin(emailDigitado: String, senhaDigitada: String) {
        if (emailDigitado.isBlank() || senhaDigitada.isBlank()) {
            _mensagemErro.value = "Informe o e-mail e a senha de acesso."
            return
        }

        viewModelScope.launch {
            _carregando.value = true
            _mensagemErro.value = null
            try {
                ClienteSupabase.client.auth.signInWith(Email) {
                    email = emailDigitado.trim()
                    password = senhaDigitada
                }
                try {
                    GerenciadorSessao.obterInstancia().salvarUltimoEmail(emailDigitado.trim())
                } catch (_: Exception) {}

                sincronizarTenantPorEmail(emailDigitado.trim())
                _usuarioAutenticado.value = true
            } catch (e: Exception) {
                _mensagemErro.value = "Falha no login: credenciais inválidas ou sem conexão."
            } finally {
                _carregando.value = false
            }
        }
    }

    fun registrarFamiliaOuConvite(
        nomeUsuario: String,
        emailDigitado: String,
        senhaDigitada: String,
        nomeFamilia: String,
        codigoConvite: String,
        modoConvite: Boolean
    ) {
        if (nomeUsuario.isBlank() || emailDigitado.isBlank() || senhaDigitada.isBlank()) {
            _mensagemErro.value = "Preencha seu nome, e-mail e senha."
            return
        }
        if (!modoConvite && nomeFamilia.isBlank()) {
            _mensagemErro.value = "Informe o nome da sua Família."
            return
        }
        if (modoConvite && codigoConvite.isBlank()) {
            _mensagemErro.value = "Informe o código de convite da Família."
            return
        }

        viewModelScope.launch {
            _carregando.value = true
            _mensagemErro.value = null
            try {
                withContext(Dispatchers.IO) {
                    val url = URL("https://familia.martel.page/api/tenant")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("User-Agent", ClienteSupabase.USER_AGENT_PADRAO)
                        doOutput = true
                        connectTimeout = 15000
                        readTimeout = 15000
                    }

                    val body = JSONObject().apply {
                        put("acao", "registrar_familia")
                        put("nomeUsuario", nomeUsuario.trim())
                        put("email", emailDigitado.trim())
                        put("password", senhaDigitada)
                        put("nomeFamilia", nomeFamilia.trim())
                        put("codigoConvite", codigoConvite.trim().uppercase())
                        put("modoConvite", modoConvite)
                    }

                    conn.outputStream.use { os ->
                        os.write(body.toString().toByteArray(Charsets.UTF_8))
                    }

                    val status = conn.responseCode
                    val respostaTexto = (if (status in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()
                        ?.use { it.readText() } ?: "{}"

                    if (status !in 200..299) {
                        val jsonErr = try { JSONObject(respostaTexto).optString("error") } catch (_: Exception) { "" }
                        throw IllegalStateException(jsonErr.ifBlank { "Erro ao registrar família ($status)." })
                    }

                    try {
                        val famObj = JSONObject(respostaTexto).optJSONObject("familia")
                        if (famObj != null) {
                            val famId = famObj.optString("id").takeIf { it.isNotBlank() } ?: GerenciadorSessao.DEFAULT_FAMILIA_ID
                            val famNome = famObj.optString("nome").takeIf { it.isNotBlank() } ?: nomeFamilia.trim()
                            val famCodigo = famObj.optString("codigo_convite").takeIf { it.isNotBlank() } ?: ""
                            GerenciadorSessao.obterInstancia().salvarFamiliaAtiva(famId, famNome, famCodigo)
                        }
                    } catch (_: Exception) {}
                }

                ClienteSupabase.client.auth.signInWith(Email) {
                    email = emailDigitado.trim()
                    password = senhaDigitada
                }
                try {
                    GerenciadorSessao.obterInstancia().salvarUltimoEmail(emailDigitado.trim())
                } catch (_: Exception) {}

                sincronizarTenantPorEmail(emailDigitado.trim())
                _usuarioAutenticado.value = true
            } catch (e: Exception) {
                _mensagemErro.value = e.message ?: "Não foi possível concluir o cadastro da família."
            } finally {
                _carregando.value = false
            }
        }
    }

    fun fazerLogout() {
        _usuarioAutenticado.value = false
        _mensagemErro.value = null
        try {
            kotlinx.coroutines.runBlocking {
                GerenciadorSessao.obterInstancia().deleteSession()
            }
        } catch (_: Exception) {}
        viewModelScope.launch {
            try {
                ClienteSupabase.client.auth.signOut()
            } catch (_: Exception) {}
        }
    }

    fun limparErro() {
        _mensagemErro.value = null
    }
}
