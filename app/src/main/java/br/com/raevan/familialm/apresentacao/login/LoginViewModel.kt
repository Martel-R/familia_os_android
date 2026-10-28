package br.com.raevan.familialm.apresentacao.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dados.rede.ClienteSupabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
        _usuarioAutenticado.value = (sessao != null)
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
                _usuarioAutenticado.value = true
            } catch (e: Exception) {
                _mensagemErro.value = "Falha no login: credenciais inválidas ou sem conexão."
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
            _usuarioAutenticado.value = false
        }
    }

    fun limparErro() {
        _mensagemErro.value = null
    }
}
