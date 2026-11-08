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
    val inserirAutomaticamente: Boolean = true,
    val recibos: List<ReciboCompra> = emptyList(),
    val reciboExtraidoPendente: ReciboCompra? = null,
    val bytesImagemPendente: ByteArray? = null,
    val mimeTypePendente: String? = null,
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

    fun alternarInserirAutomaticamente(ativo: Boolean) {
        _estado.update { it.copy(inserirAutomaticamente = ativo) }
    }

    fun analisarNotaFiscalComIa(bytesArquivo: ByteArray, mimeType: String, nomeArquivo: String) {
        viewModelScope.launch {
            _estado.update {
                it.copy(
                    analisandoIa = true,
                    bytesImagemPendente = bytesArquivo,
                    mimeTypePendente = mimeType,
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
                val autoInserir = _estado.value.inserirAutomaticamente
                val temDadosValidos = reciboExtraido.itens.isNotEmpty() || reciboExtraido.valorTotal > 0.0

                if (autoInserir && temDadosValidos) {
                    val fornecedorFinal = reciboExtraido.fornecedor?.ifBlank { "Supermercado" } ?: "Supermercado"
                    val dataFinal = reciboExtraido.dataCompra ?: java.time.LocalDate.now().toString()
                    repositorioCompras.salvarReciboComItens(
                        fornecedor = fornecedorFinal,
                        dataCompra = dataFinal,
                        valorTotal = reciboExtraido.valorTotal,
                        itens = reciboExtraido.itens,
                        lancarEmFinancas = true,
                        bytesImagem = bytesArquivo,
                        mimeTypeImagem = mimeType
                    )
                    _estado.update {
                        it.copy(
                            analisandoIa = false,
                            reciboExtraidoPendente = null,
                            bytesImagemPendente = null,
                            mimeTypePendente = null,
                            mensagemSucesso = "Compra em $fornecedorFinal inserida com ${reciboExtraido.itens.size} itens extraídos pela câmera!"
                        )
                    }
                    observarDados()
                } else {
                    _estado.update {
                        it.copy(
                            analisandoIa = false,
                            reciboExtraidoPendente = reciboExtraido
                        )
                    }
                }
            } catch (e: Exception) {
                val msgLimpa = (e.message ?: "Erro desconhecido").substringBefore("URL:").trim()
                _estado.update {
                    it.copy(
                        analisandoIa = false,
                        mensagemErro = "Não foi possível processar a nota: $msgLimpa"
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
                bytesImagemPendente = null,
                mimeTypePendente = null,
                mensagemErro = null
            )
        }
    }

    fun cancelarRevisaoNota() {
        _estado.update {
            it.copy(
                reciboExtraidoPendente = null,
                bytesImagemPendente = null,
                mimeTypePendente = null
            )
        }
    }

    fun confirmarRegistroNota(
        fornecedor: String,
        dataCompra: String,
        valorTotal: Double,
        itens: List<ItemCompraExtraido>,
        lancarEmFinancas: Boolean
    ) {
        viewModelScope.launch {
            val bytesImg = _estado.value.bytesImagemPendente
            val mimeImg = _estado.value.mimeTypePendente
            _estado.update { it.copy(salvandoNota = true, mensagemErro = null) }
            try {
                repositorioCompras.salvarReciboComItens(
                    fornecedor = fornecedor,
                    dataCompra = dataCompra,
                    valorTotal = valorTotal,
                    itens = itens,
                    lancarEmFinancas = lancarEmFinancas,
                    bytesImagem = bytesImg,
                    mimeTypeImagem = mimeImg
                )
                _estado.update {
                    it.copy(
                        salvandoNota = false,
                        reciboExtraidoPendente = null,
                        bytesImagemPendente = null,
                        mimeTypePendente = null,
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
