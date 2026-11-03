package br.com.raevan.familialm.apresentacao.compras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.ItemCompraExtraido
import br.com.raevan.familialm.dominio.modelos.ReciboCompra
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class EstadoCompras(
    val carregando: Boolean = false,
    val analisandoIa: Boolean = false,
    val salvandoNota: Boolean = false,
    val recibos: List<ReciboCompra> = emptyList(),
    val reciboExtraidoPendente: ReciboCompra? = null,
    val mensagemSucesso: String? = null,
    val mensagemErro: String? = null
)

class ComprasViewModel : ViewModel() {
    private val repositorioCompras = ProvedorDependencias.repositorioCompras

    private val _estado = MutableStateFlow(EstadoCompras(carregando = true))
    val estado: StateFlow<EstadoCompras> = _estado.asStateFlow()

    init {
        observarDados()
    }

    fun observarDados() {
        viewModelScope.launch {
            _estado.update { it.copy(carregando = true) }
            repositorioCompras.listarRecibos()
                .catch { emit(emptyList()) }
                .collect { lista ->
                    _estado.update {
                        it.copy(
                            carregando = false,
                            recibos = lista
                        )
                    }
                }
        }
    }

    fun analisarNotaFiscalComIa(bytesArquivo: ByteArray, mimeType: String, nomeArquivo: String) {
        viewModelScope.launch {
            _estado.update {
                it.copy(
                    analisandoIa = true,
                    mensagemErro = null,
                    mensagemSucesso = null
                )
            }
            try {
                val reciboExtraido = repositorioCompras.extrairNotaComIa(
                    bytesArquivo = bytesArquivo,
                    mimeType = mimeType,
                    nomeArquivo = nomeArquivo
                )
                _estado.update {
                    it.copy(
                        analisandoIa = false,
                        reciboExtraidoPendente = reciboExtraido
                    )
                }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        analisandoIa = false,
                        mensagemErro = "Não foi possível extrair os itens com a IA: ${e.message ?: "Erro desconhecido"}"
                    )
                }
            }
        }
    }

    fun abrirLancamentoManualComItens() {
        _estado.update {
            it.copy(
                reciboExtraidoPendente = ReciboCompra(
                    id = UUID.randomUUID().toString(),
                    fornecedor = "",
                    dataCompra = java.time.LocalDate.now().toString(),
                    valorTotal = 0.0,
                    itens = emptyList()
                ),
                mensagemErro = null
            )
        }
    }

    fun cancelarRevisaoNota() {
        _estado.update { it.copy(reciboExtraidoPendente = null) }
    }

    fun confirmarRegistroNota(
        fornecedor: String,
        dataCompra: String,
        valorTotal: Double,
        itens: List<ItemCompraExtraido>,
        lancarEmFinancas: Boolean
    ) {
        viewModelScope.launch {
            _estado.update { it.copy(salvandoNota = true, mensagemErro = null) }
            try {
                repositorioCompras.salvarReciboComItens(
                    fornecedor = fornecedor,
                    dataCompra = dataCompra,
                    valorTotal = valorTotal,
                    itens = itens,
                    lancarEmFinancas = lancarEmFinancas
                )
                _estado.update {
                    it.copy(
                        salvandoNota = false,
                        reciboExtraidoPendente = null,
                        mensagemSucesso = "Nota de $fornecedor registrada com ${itens.size} itens!"
                    )
                }
                observarDados()
            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        salvandoNota = false,
                        mensagemErro = "Erro ao salvar nota: ${e.message}"
                    )
                }
            }
        }
    }

    fun adicionarItemEmRecibo(
        reciboId: String,
        nomeProduto: String,
        quantidade: Double,
        valorUnitario: Double,
        categoria: String
    ) {
        viewModelScope.launch {
            repositorioCompras.adicionarItemAoRecibo(
                reciboId = reciboId,
                nomeProduto = nomeProduto,
                quantidade = quantidade,
                valorUnitario = valorUnitario,
                categoriaSugerida = categoria
            )
            _estado.update { it.copy(mensagemSucesso = "Item '$nomeProduto' adicionado à nota!") }
            observarDados()
        }
    }

    fun excluirRecibo(reciboId: String) {
        viewModelScope.launch {
            repositorioCompras.excluirRecibo(reciboId)
            _estado.update { it.copy(mensagemSucesso = "Nota fiscal removida.") }
            observarDados()
        }
    }

    fun excluirItemRecibo(itemId: String) {
        viewModelScope.launch {
            repositorioCompras.excluirItemRecibo(itemId)
            observarDados()
        }
    }

    fun escanearNovoCupom(estabelecimento: String, valorTotal: Double) {
        viewModelScope.launch {
            repositorioCompras.simularOcrCupomFiscal(estabelecimento, valorTotal)
            _estado.update { it.copy(mensagemSucesso = "Cupom de $estabelecimento digitalizado e lançado!") }
            observarDados()
        }
    }

    fun limparMensagem() {
        _estado.update { it.copy(mensagemSucesso = null, mensagemErro = null) }
    }
}
