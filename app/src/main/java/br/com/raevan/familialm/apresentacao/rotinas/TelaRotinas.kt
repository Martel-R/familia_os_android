package br.com.raevan.familialm.apresentacao.rotinas

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
import br.com.raevan.familialm.apresentacao.painel.ItemTarefaPainel
import br.com.raevan.familialm.dominio.modelos.ItemLojaPrivilegio
import br.com.raevan.familialm.dominio.modelos.RecorrenciaTarefa
import br.com.raevan.familialm.dominio.modelos.TarefaRotina
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaRotinas(
    viewModel: RotinasViewModel
) {
    val estado by viewModel.estado.collectAsState()
    val usuarioLogado = estado.usuarioLogado
    var exibirDialogoCriacao by remember { mutableStateOf(false) }

    val tarefasFiltradas = if (estado.membroFiltroId == null) {
        estado.tarefas
    } else {
        estado.tarefas.filter { it.responsavelId == estado.membroFiltroId }
    }

    val tarefasOrdenadas = remember(tarefasFiltradas, usuarioLogado) {
        val hoje = java.time.LocalDate.now()
        val diaMesHoje = hoje.dayOfMonth
        val diaSemanaId = when (hoje.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "Seg"
            java.time.DayOfWeek.TUESDAY -> "Ter"
            java.time.DayOfWeek.WEDNESDAY -> "Qua"
            java.time.DayOfWeek.THURSDAY -> "Qui"
            java.time.DayOfWeek.FRIDAY -> "Sex"
            java.time.DayOfWeek.SATURDAY -> "Sáb"
            java.time.DayOfWeek.SUNDAY -> "Dom"
        }

        tarefasFiltradas.sortedWith(
            compareBy<TarefaRotina> { t ->
                val ehAberta = t.abertaParaTodos || t.responsavelId == null
                val ehMinha = ehAberta || (usuarioLogado != null && t.responsavelId == usuarioLogado.id)
                val disp = when (t.recorrencia) {
                    RecorrenciaTarefa.DIARIA -> true
                    RecorrenciaTarefa.SEMANAL -> (t.diasSemana ?: "").split(",").map { it.trim() }.contains(diaSemanaId)
                    RecorrenciaTarefa.MENSAL -> t.diaMes == diaMesHoje
                    RecorrenciaTarefa.UNICA -> true
                }
                if (ehMinha && disp && !t.estaConcluida) 0 else if (t.estaConcluida) 1 else 2
            }.thenBy { it.titulo }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Rotinas & Gamificação",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Hábitos que geram moedas para as crianças",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AmareloOuro.copy(alpha = 0.2f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color(0xFFF57F17),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${estado.usuarioLogado?.saldoMoedas ?: 0} 🪙",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { exibirDialogoCriacao = true },
                containerColor = AzulPrimario,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
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
            // Notificações de Sucesso / Erro
            if (estado.mensagemSucesso != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VerdeEsmeraldaClaro),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = VerdeEsmeralda)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = estado.mensagemSucesso ?: "",
                                color = VerdeEsmeralda,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.limparMensagens() }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = VerdeEsmeralda)
                            }
                        }
                    }
                }
            }

            if (estado.mensagemErro != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = VermelhoAlerta)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = estado.mensagemErro ?: "",
                                color = VermelhoAlerta,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.limparMensagens() }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = VermelhoAlerta)
                            }
                        }
                    }
                }
            }

            // Abas: Tarefas ou Loja
            item {
                TabRow(
                    selectedTabIndex = estado.abaSelecionada,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = estado.abaSelecionada == 0,
                        onClick = { viewModel.selecionarAba(0) },
                        text = { Text("Tarefas & Hábitos", fontSize = 13.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 1,
                        onClick = { viewModel.selecionarAba(1) },
                        text = { Text("Loja de Privilégios 🎁", fontSize = 13.sp) }
                    )
                }
            }

            if (estado.abaSelecionada == 0) {
                // Filtro por membro
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = estado.membroFiltroId == null,
                                onClick = { viewModel.selecionarMembroFiltro(null) },
                                label = { Text("Todos") }
                            )
                        }
                        items(estado.membrosFamilia.size) { indice ->
                            val membro = estado.membrosFamilia[indice]
                            FilterChip(
                                selected = estado.membroFiltroId == membro.id,
                                onClick = { viewModel.selecionarMembroFiltro(membro.id) },
                                label = { Text("${membro.nome.split(" ").first()} (${membro.saldoMoedas}🪙)") }
                            )
                        }
                    }
                }

                items(tarefasOrdenadas.size) { indice ->
                    val tarefa = tarefasOrdenadas[indice]
                    ItemTarefaPainel(
                        tarefa = tarefa,
                        usuarioLogadoId = usuarioLogado?.id,
                        aoAlternar = { viewModel.alternarConclusaoTarefa(tarefa.id) }
                    )
                }
            } else {
                // Loja de Privilégios
                item {
                    Text(
                        text = "Troque moedas acumuladas por benefícios reais na casa:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(estado.itensLoja.size) { indice ->
                    val item = estado.itensLoja[indice]
                    ItemCardLoja(
                        item = item,
                        saldoDisponivel = estado.usuarioLogado?.saldoMoedas ?: 0,
                        aoResgatar = { viewModel.resgatarPrivilegio(item) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (exibirDialogoCriacao) {
        if (estado.abaSelecionada == 0) {
            var titulo by remember { mutableStateOf("") }
            var moedas by remember { mutableStateOf("10") }
            var abertaTodos by remember { mutableStateOf(true) }
            var responsavelId by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { exibirDialogoCriacao = false },
                title = { Text("Nova Tarefa / Hábito", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = titulo,
                            onValueChange = { titulo = it },
                            label = { Text("Título da Tarefa") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = moedas,
                            onValueChange = { moedas = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Recompensa em Moedas 🪙") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Aberta para todos os membros", fontSize = 13.sp)
                            Switch(checked = abertaTodos, onCheckedChange = { abertaTodos = it })
                        }
                        if (!abertaTodos && estado.membrosFamilia.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(estado.membrosFamilia.size) { idx ->
                                    val m = estado.membrosFamilia[idx]
                                    FilterChip(
                                        selected = responsavelId == m.id,
                                        onClick = { responsavelId = m.id },
                                        label = { Text(m.nome.split(" ").first()) }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (titulo.isNotBlank()) {
                                viewModel.adicionarNovaTarefa(
                                    titulo = titulo,
                                    responsavelId = responsavelId,
                                    recorrencia = RecorrenciaTarefa.DIARIA,
                                    moedasRecompensa = moedas.toIntOrNull() ?: 5,
                                    abertaParaTodos = abertaTodos
                                )
                                exibirDialogoCriacao = false
                            }
                        }
                    ) {
                        Text("Criar Tarefa")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exibirDialogoCriacao = false }) {
                        Text("Cancelar")
                    }
                }
            )
        } else {
            var titulo by remember { mutableStateOf("") }
            var descricao by remember { mutableStateOf("") }
            var custo by remember { mutableStateOf("30") }

            AlertDialog(
                onDismissRequest = { exibirDialogoCriacao = false },
                title = { Text("Novo Privilégio na Loja 🎁", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = titulo,
                            onValueChange = { titulo = it },
                            label = { Text("Título (ex: 1h extra de videogame)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = descricao,
                            onValueChange = { descricao = it },
                            label = { Text("Descrição") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = custo,
                            onValueChange = { custo = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Custo em Moedas 🪙") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (titulo.isNotBlank()) {
                                viewModel.adicionarNovoItemLoja(
                                    titulo = titulo,
                                    descricao = descricao,
                                    custoMoedas = custo.toIntOrNull() ?: 20
                                )
                                exibirDialogoCriacao = false
                            }
                        }
                    ) {
                        Text("Salvar Privilégio")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exibirDialogoCriacao = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun ItemCardLoja(
    item: ItemLojaPrivilegio,
    saldoDisponivel: Int,
    aoResgatar: () -> Unit
) {
    val temSaldo = saldoDisponivel >= item.custoMoedas

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AmareloOuro.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.descricao.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.descricao,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.custoMoedas} moedas 🪙",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFE65100)
                )
            }

            Button(
                onClick = { aoResgatar() },
                enabled = temSaldo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VerdeEsmeralda,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (temSaldo) "Resgatar" else "Bloqueado", fontSize = 12.sp)
            }
        }
    }
}
