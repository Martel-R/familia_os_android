package br.com.raevan.familialm.apresentacao.financas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import br.com.raevan.familialm.apresentacao.componentes.CartaoResumoFinanceiro
import br.com.raevan.familialm.apresentacao.componentes.DistintivoStatusPagamento
import br.com.raevan.familialm.apresentacao.componentes.formatarMoeda
import br.com.raevan.familialm.dominio.modelos.*
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaFinancas(
    viewModel: FinancasViewModel
) {
    val estado by viewModel.estado.collectAsState()
    var exibirDialogoNovaTransacao by remember { mutableStateOf(false) }
    var cofrinhoParaAporte by remember { mutableStateOf<Cofrinho?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Finanças Familiares",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { exibirDialogoNovaTransacao = true },
                containerColor = VerdeEsmeralda,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Conta / Receita")
            }
        }
    ) { preenchimento ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(preenchimento)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CartaoResumoFinanceiro(
                    saldoLivre = estado.resumo.saldoLivreAlocacao,
                    receitas = estado.resumo.totalReceitas,
                    despesasPendentes = estado.resumo.totalDespesasPendentes
                )
            }

            // Abas de visualização
            item {
                TabRow(
                    selectedTabIndex = estado.abaSelecionada,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = estado.abaSelecionada == 0,
                        onClick = { viewModel.selecionarAba(0) },
                        text = { Text("Contas Fixas", fontSize = 13.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 1,
                        onClick = { viewModel.selecionarAba(1) },
                        text = { Text("Assinaturas", fontSize = 13.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 2,
                        onClick = { viewModel.selecionarAba(2) },
                        text = { Text("Cofrinhos", fontSize = 13.sp) }
                    )
                }
            }

            when (estado.abaSelecionada) {
                0 -> { // Contas Fixas & Despesas Gerais
                    val contas = estado.transacoes.filter { it.tipo != TipoTransacao.ASSINATURA }
                    items(contas.size) { indice ->
                        val tx = contas[indice]
                        ItemCardTransacao(
                            transacao = tx,
                            aoAlternarStatus = { viewModel.alternarStatusPagamento(tx.id) }
                        )
                    }
                }
                1 -> { // Assinaturas Recorrentes
                    val assinaturas = estado.transacoes.filter { it.tipo == TipoTransacao.ASSINATURA }
                    items(assinaturas.size) { indice ->
                        val tx = assinaturas[indice]
                        ItemCardTransacao(
                            transacao = tx,
                            aoAlternarStatus = { viewModel.alternarStatusPagamento(tx.id) }
                        )
                    }
                }
                2 -> { // Cofrinhos / Metas Conjuntas
                    items(estado.cofrinhos.size) { indice ->
                        val cof = estado.cofrinhos[indice]
                        ItemCardCofrinho(
                            cofrinho = cof,
                            aoFazerAporte = { cofrinhoParaAporte = cof }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    // Diálogo para Adicionar Nova Transação
    if (exibirDialogoNovaTransacao) {
        DialogoNovaTransacao(
            aoFechar = { exibirDialogoNovaTransacao = false },
            aoSalvar = { desc, valor, tipo, venc ->
                viewModel.adicionarTransacao(desc, valor, tipo, venc)
                exibirDialogoNovaTransacao = false
            }
        )
    }

    // Diálogo de Aporte para Cofrinho
    if (cofrinhoParaAporte != null) {
        val cof = cofrinhoParaAporte!!
        var valorTexto by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { cofrinhoParaAporte = null },
            title = { Text("Aporte para ${cof.nome}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Digite o valor para adicionar ao cofrinho:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = valorTexto,
                        onValueChange = { valorTexto = it },
                        label = { Text("Valor (R$)") },
                        placeholder = { Text("Ex: 150.00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val valor = valorTexto.toDoubleOrNull() ?: 0.0
                        if (valor > 0) {
                            viewModel.depositarNoCofrinho(cof.id, valor)
                        }
                        cofrinhoParaAporte = null
                    }
                ) {
                    Text("Confirmar Aporte")
                }
            },
            dismissButton = {
                TextButton(onClick = { cofrinhoParaAporte = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ItemCardTransacao(
    transacao: TransacaoFinanceira,
    aoAlternarStatus: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transacao.descricao,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Vencimento: ${transacao.dataVencimento}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transacao.observacoes != null) {
                    Text(
                        text = transacao.observacoes,
                        fontSize = 11.sp,
                        color = VerdeEsmeralda
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (transacao.tipo == TipoTransacao.RECEITA) "+" else "-") + formatarMoeda(transacao.valor),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (transacao.tipo == TipoTransacao.RECEITA) VerdeEsmeralda else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.clickable { aoAlternarStatus() }
                ) {
                    DistintivoStatusPagamento(transacao.status)
                }
            }
        }
    }
}

@Composable
fun ItemCardCofrinho(
    cofrinho: Cofrinho,
    aoFazerAporte: () -> Unit
) {
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
                Text(
                    text = cofrinho.nome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(
                    onClick = { aoFazerAporte() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Fazer Aporte",
                        tint = VerdeEsmeralda
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { cofrinho.progressoPercentual },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = VerdeEsmeralda,
                trackColor = VerdeEsmeraldaClaro
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Guardado: ${formatarMoeda(cofrinho.saldoAtual)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VerdeEsmeralda
                )
                Text(
                    text = "Meta: ${formatarMoeda(cofrinho.metaValor)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DialogoNovaTransacao(
    aoFechar: () -> Unit,
    aoSalvar: (descricao: String, valor: Double, tipo: TipoTransacao, vencimento: String) -> Unit
) {
    var descricao by remember { mutableStateOf("") }
    var valorTexto by remember { mutableStateOf("") }
    var vencimento by remember { mutableStateOf("2026-11-20") }
    var tipo by remember { mutableStateOf(TipoTransacao.DESPESA_FIXA) }

    AlertDialog(
        onDismissRequest = { aoFechar() },
        title = { Text("Novo Lançamento", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição da Conta / Receita") },
                    placeholder = { Text("Ex: Conta de Gás") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = valorTexto,
                    onValueChange = { valorTexto = it },
                    label = { Text("Valor (R$)") },
                    placeholder = { Text("Ex: 125.50") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vencimento,
                    onValueChange = { vencimento = it },
                    label = { Text("Data de Vencimento") },
                    placeholder = { Text("AAAA-MM-DD") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valor = valorTexto.toDoubleOrNull() ?: 0.0
                    if (descricao.isNotBlank() && valor > 0.0) {
                        aoSalvar(descricao, valor, tipo, vencimento)
                    }
                }
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = { aoFechar() }) {
                Text("Cancelar")
            }
        }
    )
}
