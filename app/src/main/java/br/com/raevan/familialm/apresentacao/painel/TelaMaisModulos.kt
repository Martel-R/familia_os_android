package br.com.raevan.familialm.apresentacao.painel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.raevan.familialm.apresentacao.componentes.CartaoItemModulo
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaMaisModulos(
    aoNavegarParaEventos: () -> Unit,
    aoNavegarParaInventario: () -> Unit,
    aoNavegarParaMural: () -> Unit,
    aoNavegarParaAutomacao: () -> Unit,
    aoNavegarParaCompras: () -> Unit,
    aoNavegarParaContatos: () -> Unit,
    aoFazerLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Módulos do Sistema",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { preenchimento ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(preenchimento)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Acesse todos os módulos da plataforma familiar:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                CartaoItemModulo(
                    titulo = "Planejamento de Eventos",
                    subtitulo = "Aniversários, RSVP de convidados e cotações de fornecedores",
                    icone = Icons.Default.Celebration,
                    corDestaque = CorEventos,
                    aoClicar = aoNavegarParaEventos
                )
            }

            item {
                CartaoItemModulo(
                    titulo = "Inventário e Manutenções",
                    subtitulo = "Localização de itens, garantias e contatos de prestadores",
                    icone = Icons.Default.Inventory2,
                    corDestaque = CorInventario,
                    aoClicar = aoNavegarParaInventario
                )
            }

            item {
                CartaoItemModulo(
                    titulo = "Mural de Conquistas & Fotos",
                    subtitulo = "Linha do tempo de vitórias da família e álbum privado",
                    icone = Icons.Default.EmojiEvents,
                    corDestaque = CorMural,
                    aoClicar = aoNavegarParaMural
                )
            }

            item {
                CartaoItemModulo(
                    titulo = "Automação e Segurança",
                    subtitulo = "Câmeras Frigate NVR, iluminação e sensores da casa",
                    icone = Icons.Default.Sensors,
                    corDestaque = CorAutomacao,
                    aoClicar = aoNavegarParaAutomacao
                )
            }

            item {
                CartaoItemModulo(
                    titulo = "Compras Inteligentes & OCR",
                    subtitulo = "Digitalização de cupons fiscais e controle de compras",
                    icone = Icons.AutoMirrored.Filled.ReceiptLong,
                    corDestaque = CorCompras,
                    aoClicar = aoNavegarParaCompras
                )
            }

            item {
                CartaoItemModulo(
                    titulo = "Agenda de Contatos & Apoio",
                    subtitulo = "Telefones da família, emergência e sincronização da agenda",
                    icone = Icons.Default.Call,
                    corDestaque = CorContatos,
                    aoClicar = aoNavegarParaContatos
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = aoFazerLogout,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sair da Conta (Logout)", fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
