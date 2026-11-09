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
    val redesWifi: List<RedeWifiFamiliar> = emptyList(),
    val cofreMasterDesbloqueado: Boolean = false,
    val abaSelecionada: Int = 0, // 0: Compartilhado, 1: Master (Restrito aos Pais), 2: Redes Wi-Fi
    val mensagemAlerta: String? = null
)

class CofreViewModel : ViewModel() {
    private val repositorioCofre = ProvedorDependencias.repositorioCofre

    private val _estado = MutableStateFlow(EstadoCofre(carregando = true))
    val estado: StateFlow<EstadoCofre> = _estado.asStateFlow()

    init {
        observarDados()
    }

    fun observarDados() {
        viewModelScope.launch {
            combine(
                repositorioCofre.listarCredenciais(incluirMaster = true),
                repositorioCofre.listarRedesWifiFamilia()
            ) { creds, listaWifi ->
                _estado.value.copy(
                    carregando = false,
                    credenciais = creds,
                    redesWifi = listaWifi,
                    redeWifi = listaWifi.firstOrNull()
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
            observarDados()
        }
    }

    fun excluirCredencial(credencialId: String) {
        viewModelScope.launch {
            repositorioCofre.excluirCredencial(credencialId)
            _estado.update { it.copy(mensagemAlerta = "Credencial removida do cofre!") }
            observarDados()
        }
    }

    fun salvarOuEditarRedeWifi(
        id: String?,
        apelido: String,
        ssid: String,
        senha: String,
        seguranca: String,
        ehVisitantes: Boolean = false
    ) {
        if (ssid.isBlank()) {
            _estado.update { it.copy(mensagemAlerta = "Informe o nome da rede (SSID) do Wi-Fi.") }
            return
        }
        viewModelScope.launch {
            val novaRede = RedeWifiFamiliar(
                id = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                apelido = apelido.trim().ifBlank { ssid.trim() },
                nomeRedeSsid = ssid.trim(),
                senhaRede = senha.trim(),
                tipoSeguranca = seguranca.ifBlank { "WPA2/WPA3" },
                ehRedeVisitantes = ehVisitantes
            )
            val listaAtual = _estado.value.redesWifi.toMutableList()
            val idx = listaAtual.indexOfFirst {
                (id != null && it.id == id) || it.nomeRedeSsid.equals(novaRede.nomeRedeSsid, ignoreCase = true)
            }
            if (idx >= 0) {
                listaAtual[idx] = novaRede
            } else {
                listaAtual.add(novaRede)
            }
            repositorioCofre.salvarListaRedesWifiFamilia(listaAtual)
            _estado.update {
                it.copy(
                    redesWifi = listaAtual,
                    redeWifi = listaAtual.firstOrNull(),
                    mensagemAlerta = "Rede Wi-Fi '${novaRede.nomeRedeSsid}' salva e compartilhada com a família!"
                )
            }
        }
    }

    fun excluirRedeWifi(redeId: String) {
        viewModelScope.launch {
            val novaLista = _estado.value.redesWifi.filterNot { it.id == redeId }
            repositorioCofre.salvarListaRedesWifiFamilia(novaLista)
            _estado.update {
                it.copy(
                    redesWifi = novaLista,
                    redeWifi = novaLista.firstOrNull(),
                    mensagemAlerta = "Rede Wi-Fi removida do Cofre da Família."
                )
            }
        }
    }

    fun importarRedesWifiEmLote(novasRedes: List<RedeWifiFamiliar>) {
        if (novasRedes.isEmpty()) {
            _estado.update {
                it.copy(
                    mensagemAlerta = "Nenhum QR Code de Wi-Fi foi encontrado na(s) imagem(ns). Em Configurações → Wi-Fi → Compartilhar, tire print da tela com o QR Code e selecione aqui!"
                )
            }
            return
        }
        viewModelScope.launch {
            val listaAtual = _estado.value.redesWifi.toMutableList()
            var adicionadasOuAtualizadas = 0
            for (nova in novasRedes) {
                if (nova.nomeRedeSsid.isBlank()) continue
                val idx = listaAtual.indexOfFirst {
                    it.nomeRedeSsid.equals(nova.nomeRedeSsid, ignoreCase = true)
                }
                if (idx >= 0) {
                    listaAtual[idx] = nova.copy(id = listaAtual[idx].id)
                } else {
                    listaAtual.add(nova)
                }
                adicionadasOuAtualizadas++
            }
            repositorioCofre.salvarListaRedesWifiFamilia(listaAtual)
            _estado.update {
                it.copy(
                    redesWifi = listaAtual,
                    redeWifi = listaAtual.firstOrNull(),
                    mensagemAlerta = "✅ $adicionadasOuAtualizadas rede(s) Wi-Fi importada(s) e compartilhada(s) com toda a família!"
                )
            }
        }
    }

    fun exibirAlerta(mensagem: String) {
        _estado.update { it.copy(mensagemAlerta = mensagem) }
    }

    fun limparAlerta() {
        _estado.update { it.copy(mensagemAlerta = null) }
    }
}
