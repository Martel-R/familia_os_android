package br.com.raevan.familialm.apresentacao.contatos

import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.ContatoAgenda
import br.com.raevan.familialm.dominio.repositorios.RepositorioInventario
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ContatosViewModel(
    private val repositorioInventario: RepositorioInventario = ProvedorDependencias.repositorioInventario
) : ViewModel() {

    private val _contatosTelefone = MutableStateFlow<List<ContatoAgenda>>(emptyList())
    val contatosTelefone: StateFlow<List<ContatoAgenda>> = _contatosTelefone.asStateFlow()

    val contatosSistema: StateFlow<List<ContatoAgenda>> = repositorioInventario
        .listarContatos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _termoBusca = MutableStateFlow("")
    val termoBusca: StateFlow<String> = _termoBusca.asStateFlow()

    private val _sincronizando = MutableStateFlow(false)
    val sincronizando: StateFlow<Boolean> = _sincronizando.asStateFlow()

    private val _mensagemFeedback = MutableStateFlow<String?>(null)
    val mensagemFeedback: StateFlow<String?> = _mensagemFeedback.asStateFlow()

    fun definirBusca(termo: String) {
        _termoBusca.value = termo
    }

    fun limparFeedback() {
        _mensagemFeedback.value = null
    }

    fun alternarSelecaoContato(contatoId: String) {
        _contatosTelefone.update { lista ->
            lista.map { if (it.id == contatoId) it.copy(selecionado = !it.selecionado) else it }
        }
    }

    fun selecionarTodos(selecionar: Boolean) {
        _contatosTelefone.update { lista ->
            lista.map { it.copy(selecionado = selecionar) }
        }
    }

    fun lerContatosDoAparelho(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val lista = mutableListOf<ContatoAgenda>()
            val resolver = context.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projecao = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            try {
                val cursor = resolver.query(
                    uri,
                    projecao,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )

                cursor?.use {
                    val nomeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val telefonesVistos = mutableSetOf<String>()

                    while (it.moveToNext()) {
                        val nome = if (nomeIdx >= 0) it.getString(nomeIdx) ?: "Sem Nome" else "Sem Nome"
                        val numero = if (numIdx >= 0) it.getString(numIdx) ?: "" else ""
                        val numeroFormatado = numero.replace(Regex("[^0-9+]"), "")

                        if (numeroFormatado.isNotBlank() && telefonesVistos.add(numeroFormatado)) {
                            lista.add(
                                ContatoAgenda(
                                    id = UUID.randomUUID().toString(),
                                    nome = nome,
                                    telefone = numero,
                                    especialidade = "Contato do Celular",
                                    ehEmergencia = false,
                                    selecionado = true // por padrão vem selecionado para fácil sincronização
                                )
                            )
                        }
                    }
                }

                _contatosTelefone.value = lista
                _mensagemFeedback.value = "${lista.size} contatos encontrados na agenda do celular."
            } catch (e: SecurityException) {
                _mensagemFeedback.value = "Permissão negada para ler os contatos do telefone."
            } catch (e: Exception) {
                _mensagemFeedback.value = "Erro ao ler contatos: ${e.message}"
            }
        }
    }

    fun sincronizarComSistema(apenasSelecionados: Boolean = true) {
        val contatosParaSincronizar = if (apenasSelecionados) {
            _contatosTelefone.value.filter { it.selecionado }
        } else {
            _contatosTelefone.value
        }

        if (contatosParaSincronizar.isEmpty()) {
            _mensagemFeedback.value = "Nenhum contato selecionado para sincronizar."
            return
        }

        viewModelScope.launch {
            _sincronizando.value = true
            try {
                val total = repositorioInventario.sincronizarContatos(contatosParaSincronizar)
                _mensagemFeedback.value = "Sucesso! $total contatos sincronizados com o Familia OS."
            } catch (e: Exception) {
                _mensagemFeedback.value = "Erro na sincronização: ${e.message}"
            } finally {
                _sincronizando.value = false
            }
        }
    }
}
