package br.com.raevan.familialm.apresentacao.eventos

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
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
import br.com.raevan.familialm.dominio.modelos.Convidado
import br.com.raevan.familialm.dominio.modelos.Cotacao
import br.com.raevan.familialm.dominio.modelos.StatusCotacao
import br.com.raevan.familialm.dominio.modelos.StatusRsvp
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaEventos(
    viewModel: EventosViewModel,
    aoVoltar: () -> Unit = {}
) {
    val estado by viewModel.estado.collectAsState()
    val eventoAtivo = estado.eventos.find { it.id == estado.eventoSelecionadoId }
    var exibirDialogoNovoEvento by remember { mutableStateOf(false) }
    var exibirDialogoItemAba by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Planejamento de Eventos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "RSVP de convidados, WhatsApp e cotações",
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
                actions = {
                    IconButton(onClick = { exibirDialogoNovoEvento = true }) {
                        Icon(Icons.Default.EventAvailable, contentDescription = "Novo Evento", tint = CorEventos)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (eventoAtivo != null) {
                FloatingActionButton(
                    onClick = { exibirDialogoItemAba = true },
                    containerColor = CorEventos,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Convidado ou Cotação")
                }
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
            // Seletor de Eventos
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(estado.eventos.size) { indice ->
                        val ev = estado.eventos[indice]
                        FilterChip(
                            selected = estado.eventoSelecionadoId == ev.id,
                            onClick = { viewModel.selecionarEvento(ev.id) },
                            label = { Text(ev.nome) }
                        )
                    }
                }
            }

            if (eventoAtivo != null) {
                // Card de Destaque do Evento
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CorEventos.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CorEventos.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Celebration, contentDescription = null, tint = CorEventos)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = eventoAtivo.nome,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📅 ${eventoAtivo.dataEvento}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CorEventos
                            )
                            if (!eventoAtivo.descricao.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = eventoAtivo.descricao,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Estimado: ${formatarMoeda(eventoAtivo.orcamentoEstimado ?: 0.0)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Gasto Real: ${formatarMoeda(eventoAtivo.totalGastoReal)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VerdeEsmeralda
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val contexto = LocalContext.current
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_INSERT).apply {
                                        data = CalendarContract.Events.CONTENT_URI
                                        putExtra(CalendarContract.Events.TITLE, eventoAtivo.nome)
                                        putExtra(CalendarContract.Events.DESCRIPTION, eventoAtivo.descricao ?: "Evento do Familia OS")
                                    }
                                    contexto.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📅 Adicionar à Google Agenda", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Abas do Evento
                item {
                    TabRow(
                        selectedTabIndex = estado.abaSelecionada,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = estado.abaSelecionada == 0,
                            onClick = { viewModel.selecionarAba(0) },
                            text = { Text("Convidados (RSVP)", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = estado.abaSelecionada == 1,
                            onClick = { viewModel.selecionarAba(1) },
                            text = { Text("Central de Cotações", fontSize = 12.sp) }
                        )
                    }
                }

                if (estado.abaSelecionada == 0) {
                    val confirmados = estado.convidados.count { it.status == StatusRsvp.CONFIRMADO }
                    item {
                        Text(
                            text = "$confirmados confirmados de ${estado.convidados.size} convidados:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(estado.convidados.size) { indice ->
                        val cnv = estado.convidados[indice]
                        ItemCardConvidado(
                            convidado = cnv,
                            aoAlternarStatus = { novo -> viewModel.alternarRsvp(cnv.id, novo) },
                            aoEnviarWhatsapp = { viewModel.enviarConviteWhatsapp(cnv, eventoAtivo) }
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "Cotações de fornecedores para aprovação:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(estado.cotacoes.size) { indice ->
                        val cot = estado.cotacoes[indice]
                        ItemCardCotacao(cotacao = cot)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (exibirDialogoNovoEvento) {
        var nome by remember { mutableStateOf("") }
        var data by remember { mutableStateOf(java.time.LocalDate.now().plusDays(15).toString()) }
        var descricao by remember { mutableStateOf("") }
        var orcamento by remember { mutableStateOf("1500") }

        AlertDialog(
            onDismissRequest = { exibirDialogoNovoEvento = false },
            title = { Text("Novo Evento Familiar 🎉", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do Evento") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = data, onValueChange = { data = it }, label = { Text("Data (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = orcamento, onValueChange = { orcamento = it }, label = { Text("Orçamento Estimado (R$)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição / Local") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nome.isNotBlank()) {
                        viewModel.criarNovoEvento(nome, data, descricao, orcamento.toDoubleOrNull() ?: 0.0)
                        exibirDialogoNovoEvento = false
                    }
                }) { Text("Criar Evento") }
            },
            dismissButton = {
                TextButton(onClick = { exibirDialogoNovoEvento = false }) { Text("Cancelar") }
            }
        )
    }

    if (exibirDialogoItemAba && eventoAtivo != null) {
        if (estado.abaSelecionada == 0) {
            var nome by remember { mutableStateOf("") }
            var telefone by remember { mutableStateOf("") }
            var restricoes by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { exibirDialogoItemAba = false },
                title = { Text("Adicionar Convidado", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do Convidado") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = telefone, onValueChange = { telefone = it }, label = { Text("WhatsApp (ex: 5596999999999)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = restricoes, onValueChange = { restricoes = it }, label = { Text("Restrições Alimentares (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (nome.isNotBlank()) {
                            viewModel.adicionarConvidado(nome, telefone, restricoes)
                            exibirDialogoItemAba = false
                        }
                    }) { Text("Salvar") }
                },
                dismissButton = {
                    TextButton(onClick = { exibirDialogoItemAba = false }) { Text("Cancelar") }
                }
            )
        } else {
            var itemServico by remember { mutableStateOf("") }
            var fornecedor by remember { mutableStateOf("") }
            var valor by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { exibirDialogoItemAba = false },
                title = { Text("Nova Cotação de Fornecedor", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = itemServico, onValueChange = { itemServico = it }, label = { Text("Item ou Serviço (ex: Buffet)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = fornecedor, onValueChange = { fornecedor = it }, label = { Text("Fornecedor") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = valor, onValueChange = { valor = it }, label = { Text("Valor (R$)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (itemServico.isNotBlank() && fornecedor.isNotBlank()) {
                            viewModel.adicionarCotacao(itemServico, fornecedor, valor.toDoubleOrNull() ?: 0.0, null)
                            exibirDialogoItemAba = false
                        }
                    }) { Text("Salvar") }
                },
                dismissButton = {
                    TextButton(onClick = { exibirDialogoItemAba = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
fun ItemCardConvidado(
    convidado: Convidado,
    aoAlternarStatus: (StatusRsvp) -> Unit,
    aoEnviarWhatsapp: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = convidado.nome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (convidado.telefone != null) {
                        Text(
                            text = convidado.telefone,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!convidado.telefone.isNullOrBlank()) {
                    IconButton(onClick = aoEnviarWhatsapp) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar Convite WhatsApp",
                            tint = VerdeEsmeralda
                        )
                    }
                }

                // Status RSVP
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (convidado.status) {
                        StatusRsvp.CONFIRMADO -> VerdeEsmeraldaClaro
                        StatusRsvp.PENDENTE -> Color(0xFFFFF3E0)
                        StatusRsvp.RECUSADO -> Color(0xFFFFEBEE)
                    },
                    modifier = Modifier.clickable {
                        val proximo = when (convidado.status) {
                            StatusRsvp.CONFIRMADO -> StatusRsvp.RECUSADO
                            StatusRsvp.RECUSADO -> StatusRsvp.PENDENTE
                            StatusRsvp.PENDENTE -> StatusRsvp.CONFIRMADO
                        }
                        aoAlternarStatus(proximo)
                    }
                ) {
                    Text(
                        text = convidado.status.rotulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = when (convidado.status) {
                            StatusRsvp.CONFIRMADO -> VerdeEsmeralda
                            StatusRsvp.PENDENTE -> Color(0xFFE65100)
                            StatusRsvp.RECUSADO -> VermelhoAlerta
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!convidado.restricoesAlimentares.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFF8E1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Restrição: ${convidado.restricoesAlimentares}",
                        fontSize = 11.sp,
                        color = Color(0xFFF57F17),
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemCardCotacao(cotacao: Cotacao) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Text(
                    text = cotacao.itemOuServico,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Fornecedor: ${cotacao.fornecedor}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (cotacao.observacoes != null) {
                    Text(
                        text = cotacao.observacoes,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatarMoeda(cotacao.valor),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (cotacao.status) {
                        StatusCotacao.APROVADA -> VerdeEsmeraldaClaro
                        StatusCotacao.EM_ANALISE -> Color(0xFFFFF3E0)
                        StatusCotacao.REJEITADA -> Color(0xFFFFEBEE)
                    }
                ) {
                    Text(
                        text = cotacao.status.rotulo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (cotacao.status) {
                            StatusCotacao.APROVADA -> VerdeEsmeralda
                            StatusCotacao.EM_ANALISE -> Color(0xFFE65100)
                            StatusCotacao.REJEITADA -> VermelhoAlerta
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
