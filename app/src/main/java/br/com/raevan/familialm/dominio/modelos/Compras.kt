package br.com.raevan.familialm.dominio.modelos

data class ItemCompraExtraido(
    val id: String,
    val reciboId: String,
    val nomeProduto: String,
    val quantidade: Double = 1.0,
    val valorUnitario: Double,
    val categoriaSugerida: String? = null
)

data class ReciboCompra(
    val id: String,
    val fornecedor: String?,
    val dataCompra: String?,
    val valorTotal: Double,
    val caminhoImagemRecibo: String? = null,
    val itens: List<ItemCompraExtraido> = emptyList(),
    val criadoEm: String = ""
)
