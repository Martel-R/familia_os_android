package br.com.raevan.familialm.apresentacao.navegacao

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class DestinoNavegacao(
    val rota: String,
    val titulo: String,
    val icone: ImageVector
) {
    object Login : DestinoNavegacao("login", "Login", Icons.Default.Lock)
    object Painel : DestinoNavegacao("painel", "Início", Icons.Default.Home)
    object Financas : DestinoNavegacao("financas", "Finanças", Icons.Default.AccountBalanceWallet)
    object Rotinas : DestinoNavegacao("rotinas", "Rotinas", Icons.Default.Checklist)
    object Saude : DestinoNavegacao("saude", "Saúde", Icons.Default.Favorite)
    object Cofre : DestinoNavegacao("cofre", "Cofre", Icons.Default.Lock)
    object Eventos : DestinoNavegacao("eventos", "Eventos", Icons.Default.Celebration)
    object Inventario : DestinoNavegacao("inventario", "Inventário", Icons.Default.Inventory2)
    object Mural : DestinoNavegacao("mural", "Mural", Icons.Default.EmojiEvents)
    object Automacao : DestinoNavegacao("automacao", "Automação", Icons.Default.Sensors)
    object Compras : DestinoNavegacao("compras", "Compras", Icons.AutoMirrored.Filled.ReceiptLong)
    object Contatos : DestinoNavegacao("contatos", "Contatos", Icons.Default.Call)
    object MaisModulos : DestinoNavegacao("mais_modulos", "Mais", Icons.Default.Widgets)

    companion object {
        val itensBarraInferior = listOf(
            Painel,
            Financas,
            Rotinas,
            Saude,
            Cofre,
            MaisModulos
        )
    }
}
