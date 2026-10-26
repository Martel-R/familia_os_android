package br.com.raevan.familialm.apresentacao.compras

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.raevan.familialm.apresentacao.componentes.formatarMoeda
import br.com.raevan.familialm.dominio.modelos.ReciboCompra
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaComprasInteligentes(
    viewModel: ComprasViewModel,
    aoVoltar: () -> Unit = {}
) {
    val estado by viewModel.estado.collectAsState()
    var exibirDialogoScanner by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Compras Inteligentes & OCR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Extração automática de cupons e controle de despensa",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { aoVoltar() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { exibirDialogoScanner = true },
                containerColor = CorCompras,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) },
                text = { Text("Escanear Cupom", fontWeight = FontWeight.Bold) }
            )
        }
    ) { preenchimento ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(preenchimento)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (estado.mensagemSucesso != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VerdeEsmeraldaClaro),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerdeEsmeralda)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = estado.mensagemSucesso ?: "",
                                color = VerdeEsmeralda,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.limparMensagem() }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = VerdeEsmeralda)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Recibos e cupons fiscais digitalizados:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(estado.recibos.size) { indice ->
                val recibo = estado.recibos[indice]
                ItemCardRecibo(recibo = recibo)
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (exibirDialogoScanner) {
        DialogoSimularOcr(
            aoFechar = { exibirDialogoScanner = false },
            aoConfirmar = { estab, valTotal ->
                viewModel.escanearNovoCupom(estab, valTotal)
                exibirDialogoScanner = false
            }
        )
    }
}

@Composable
fun ItemCardRecibo(recibo: ReciboCompra) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recibo.fornecedor ?: "Estabelecimento Comercial",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = recibo.dataCompra ?: "Data Recente",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = formatarMoeda(recibo.valorTotal),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = VerdeEsmeralda
                )
            }

            if (recibo.itens.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Itens Identificados pelo OCR:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                recibo.itens.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• ${item.nomeProduto}",
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatarMoeda(item.valorUnitario),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoSimularOcr(
    aoFechar: () -> Unit,
    aoConfirmar: (estabelecimento: String, valorTotal: Double) -> Unit
) {
    var mercado by remember { mutableStateOf("") }
    var valorTexto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { aoFechar() },
        title = { Text("Escanear Cupom Fiscal (OCR)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Aponte a câmera para a Nota Fiscal NFC-e ou cupom:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = mercado,
                    onValueChange = { mercado = it },
                    label = { Text("Nome do Estabelecimento") },
                    placeholder = { Text("Ex: Supermercado Nazaré") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = valorTexto,
                    onValueChange = { valorTexto = it },
                    label = { Text("Valor Total do Cupom (R$)") },
                    placeholder = { Text("Ex: 245.90") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valor = valorTexto.toDoubleOrNull() ?: 0.0
                    if (mercado.isNotBlank() && valor > 0.0) {
                        aoConfirmar(mercado, valor)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CorCompras)
            ) {
                Text("Processar Cupom")
            }
        },
        dismissButton = {
            TextButton(onClick = { aoFechar() }) {
                Text("Cancelar")
            }
        }
    )
}
