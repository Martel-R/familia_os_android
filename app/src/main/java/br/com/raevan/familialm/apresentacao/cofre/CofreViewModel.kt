package br.com.raevan.familialm.apresentacao.cofre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.CredencialCofre
import br.com.raevan.familialm.dominio.modelos.NivelCofre
import br.com.raevan.familialm.dominio.modelos.RedeWifiFamiliar
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class EstadoCofre(
    val carregando: Boolean = false,
    val credenciais: List<CredencialCofre> = emptyList(),
    val redeWifi: RedeWifiFamiliar? = null,
    val cofreMasterDesbloqueado: Boolean = false,
    val abaSelecionada: Int = 0, // 0: Compartilhado, 1: Master (Restrito aos Pais), 2: Rede Wi-Fi
    val mensagemAlerta: String? = null
)

class CofreViewModel : ViewModel() {
    private val repositorioCofre = ProvedorDependencias.repositorioCofre

    private val _estado = MutableStateFlow(EstadoCofre(carregando = true))
    val estado: StateFlow<EstadoCofre> = _estado.asStateFlow()

    init {
        observarDados()
    }

    private fun observarDados() {
        viewModelScope.launch {
            combine(
                repositorioCofre.listarCredenciais(incluirMaster = true),
                repositorioCofre.obterRedeWifiFamiliar()
            ) { creds, wifi ->
                _estado.value.copy(
                    carregando = false,
                    credenciais = creds,
                    redeWifi = wifi
                )
            }.collect { novoEstado ->
                _estado.value = novoEstado
            }
        }
    }

    fun selecionarAba(indice: Int) {
        _estado.update { it.copy(abaSelecionada = indice) }
    }

    fun autenticarCofreMaster(pinCorreto: Boolean) {
        if (pinCorreto) {
            _estado.update { it.copy(cofreMasterDesbloqueado = true, mensagemAlerta = "Cofre Master desbloqueado com sucesso!") }
        } else {
            _estado.update { it.copy(mensagemAlerta = "PIN de segurança incorreto!") }
        }
    }

    fun bloquearCofreMaster() {
        _estado.update { it.copy(cofreMasterDesbloqueado = false) }
    }

    fun adicionarCredencial(
        titulo: String,
        nivel: NivelCofre,
        login: String,
        senha: String,
        url: String?,
        notas: String?
    ) {
        viewModelScope.launch {
            val novaCred = CredencialCofre(
                id = UUID.randomUUID().toString(),
                titulo = titulo,
                nivel = nivel,
                loginUsuario = login,
                senhaCriptografada = senha,
                urlAcesso = url,
                notasSeguras = notas
            )
            repositorioCofre.adicionarCredencial(novaCred)
            _estado.update { it.copy(mensagemAlerta = "Credencial salva com sucesso!") }
        }
    }

    fun limparAlerta() {
        _estado.update { it.copy(mensagemAlerta = null) }
    }
}
