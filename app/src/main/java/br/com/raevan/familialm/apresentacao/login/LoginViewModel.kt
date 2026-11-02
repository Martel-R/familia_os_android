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
        val sessao = ClienteSupabase.client.auth.currentSessionOrNull()
        val temSessaoSalva = try { GerenciadorSessao.obterInstancia().temSessaoSalva() } catch (_: Exception) { false }
        _usuarioAutenticado.value = (sessao != null || temSessaoSalva)
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
                    GerenciadorSessao.obterInstancia().salvarUltimoEmail(emailDigitado)
                } catch (_: Exception) {}

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
                }

                ClienteSupabase.client.auth.signInWith(Email) {
                    email = emailDigitado.trim()
                    password = senhaDigitada
                }
                try {
                    GerenciadorSessao.obterInstancia().salvarUltimoEmail(emailDigitado)
                } catch (_: Exception) {}

                _usuarioAutenticado.value = true
            } catch (e: Exception) {
                _mensagemErro.value = e.message ?: "Não foi possível concluir o cadastro da família."
            } finally {
                _carregando.value = false
            }
        }
    }

    fun fazerLogout() {
        viewModelScope.launch {
            try {
                ClienteSupabase.client.auth.signOut()
            } catch (_: Exception) {}
            try {
                GerenciadorSessao.obterInstancia().deleteSession()
            } catch (_: Exception) {}
            _usuarioAutenticado.value = false
        }
    }

    fun limparErro() {
        _mensagemErro.value = null
    }
}
