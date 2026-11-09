package br.com.raevan.familialm.apresentacao.painel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import br.com.raevan.familialm.apresentacao.componentes.BarraSuperiorHomeHub
import br.com.raevan.familialm.apresentacao.componentes.CartaoResumoFinanceiro
import br.com.raevan.familialm.apresentacao.componentes.formatarMoeda
import br.com.raevan.familialm.dominio.modelos.RecorrenciaTarefa
import br.com.raevan.familialm.dominio.modelos.TarefaRotina
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPainelPrincipal(
    viewModel: PainelViewModel,
    aoNavegarParaFinancas: () -> Unit,
    aoNavegarParaRotinas: () -> Unit,
    aoNavegarParaSaude: () -> Unit,
    aoNavegarParaCofre: () -> Unit,
    aoNavegarParaEventos: () -> Unit,
    aoNavegarParaAutomacao: () -> Unit,
    aoNavegarParaCompras: () -> Unit = {}
) {
    val estado by viewModel.estado.collectAsState()
    var exibirDialogoWifi by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BarraSuperiorHomeHub(
                usuario = estado.usuarioLogado,
                titulo = "Familia OS",
                familia = estado.familiaAtiva,
                aoFamiliaAtualizada = { viewModel.recarregarPainel() }
            )
        }
    ) { preenchimentoInterno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(preenchimentoInterno)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Notificação de moedas ganhas
            if (estado.mensagemAlerta != null) {
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
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = VerdeEsmeralda
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = estado.mensagemAlerta ?: "",
                                color = VerdeEsmeralda,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.limparMensagemAlerta() }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = VerdeEsmeralda)
                            }
                        }
                    }
                }
            }

            // Balanço Financeiro
            item {
                CartaoResumoFinanceiro(
                    saldoLivre = estado.resumoFinanceiro.saldoLivreAlocacao,
                    receitas = estado.resumoFinanceiro.totalReceitas,
                    despesasPendentes = estado.resumoFinanceiro.totalDespesasPendentes,
                    aoClicarVerDetalhes = aoNavegarParaFinancas
                )
            }

            // Ações Rápidas da Casa
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botão Emergência Médica
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { aoNavegarParaSaude() },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFEBEE),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = "Ficha Emergência",
                                tint = VermelhoAlerta,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Emergência",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = VermelhoAlerta
                                )
                                Text(
                                    text = "Fichas & Alergias",
                                    fontSize = 10.sp,
                                    color = VermelhoAlerta.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Botão Wi-Fi Visitantes
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { exibirDialogoWifi = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE8F5E9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Wi-Fi Visitantes",
                                tint = VerdeEsmeralda,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Wi-Fi Casa",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = VerdeEsmeralda
                                )
                                Text(
                                    text = "Senha Visitantes",
                                    fontSize = 10.sp,
                                    color = VerdeEsmeralda.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Atalho Direto: Importar Compras com Câmera (IA)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { aoNavegarParaCompras() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CorCompras.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CorCompras.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CorCompras.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Importar Compra com Câmera",
                                tint = CorCompras,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Compras Inteligentes • Câmera IA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CorCompras
                            )
                            Text(
                                text = "Fotografe a nota fiscal para extrair e inserir todos os itens comprados",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = CorCompras
                        )
                    }
                }
            }

            // Check-in de Humor na Tela Inicial
            item {
                val contextoLocal = androidx.compose.ui.platform.LocalContext.current
                val ultimoHumor = estado.ultimoCheckinHumor
                val opcoesHumor = listOf(
                    Triple(1, "😢", "Mal"),
                    Triple(2, "😕", "Baixo"),
                    Triple(3, "😐", "Neutro"),
                    Triple(4, "🙂", "Bem"),
                    Triple(5, "😄", "Ótimo")
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CorSaude.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CorSaude.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = CorSaude,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Como você está hoje?",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (ultimoHumor != null) {
                                            val emojiAtual = opcoesHumor.firstOrNull { it.first == ultimoHumor.nivelHumor }?.second ?: "🙂"
                                            val textoAtual = opcoesHumor.firstOrNull { it.first == ultimoHumor.nivelHumor }?.third ?: "Bem"
                                            "Último registro: $emojiAtual $textoAtual"
                                        } else {
                                            "Toque em um emoji para registrar seu humor"
                                        },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(onClick = aoNavegarParaSaude) {
                                Text("Saúde →", fontSize = 12.sp, color = CorSaude, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            opcoesHumor.forEach { (nivel, emoji, rotulo) ->
                                val selecionado = ultimoHumor?.nivelHumor == nivel
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.registrarCheckinHumorRapido(nivel)
                                            br.com.raevan.familialm.nucleo.widgets.WidgetCheckinHumorProvider
                                                .sincronizarHumorLocal(contextoLocal, nivel)
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selecionado) CorSaude.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    border = if (selecionado) {
                                        androidx.compose.foundation.BorderStroke(1.5.dp, CorSaude)
                                    } else {
                                        null
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = emoji, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = rotulo,
                                            fontSize = 10.sp,
                                            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selecionado) CorSaude else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Próximo Evento Familiar
            if (estado.proximoEvento != null) {
                item {
                    val evento = estado.proximoEvento!!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aoNavegarParaEventos() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CorEventos.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CorEventos.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(CorEventos.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Celebration,
                                    contentDescription = null,
                                    tint = CorEventos,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Próximo Evento da Família",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CorEventos
                                )
                                Text(
                                    text = evento.nome,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = evento.dataEvento,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Seção de Tarefas e Rotinas do Dia
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tarefas e Hábitos de Hoje",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { aoNavegarParaRotinas() }) {
                        Text("Ver todas", color = AzulPrimario)
                    }
                }
            }

            // Itens de tarefas
            items(estado.tarefasDoDia.size) { indice ->
                val tarefa = estado.tarefasDoDia[indice]
                ItemTarefaPainel(
                    tarefa = tarefa,
                    usuarioLogadoId = estado.usuarioLogado?.id,
                    aoAlternar = { viewModel.alternarConclusaoTarefa(tarefa.id) }
                )
            }

            // Dispositivos Ativos / Automação Rápida
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Casa Conectada (IoT)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { aoNavegarParaAutomacao() }) {
                        Text("Painel IoT", color = AzulPrimario)
                    }
                }
            }

            items(estado.dispositivosAtivos.size) { indice ->
                val disp = estado.dispositivosAtivos[indice]
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Power,
                                contentDescription = null,
                                tint = VerdeEsmeralda,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = disp.nome,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${disp.comodo} • ${disp.valorStatus}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = disp.estaLigado,
                            onCheckedChange = { viewModel.alternarDispositivo(disp.id) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Modal de Informações da Rede Wi-Fi Familiar
    if (exibirDialogoWifi && estado.redeWifi != null) {
        val wifi = estado.redeWifi!!
        AlertDialog(
            onDismissRequest = { exibirDialogoWifi = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = VerdeEsmeralda,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Wi-Fi — ${estado.familiaAtiva?.nome ?: "Minha Família"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Compartilhe facilmente o acesso à rede de convidados:")
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Nome da Rede (SSID):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = wifi.nomeRedeSsid, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Senha de Acesso:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = wifi.senhaRede, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VerdeEsmeralda)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { exibirDialogoWifi = false }) {
                    Text("OK, Fechar")
                }
            }
        )
    }
}

@Composable
fun ItemTarefaPainel(
    tarefa: TarefaRotina,
    usuarioLogadoId: String? = null,
    aoAlternar: () -> Unit
) {
    val hoje = java.time.LocalDate.now(java.time.ZoneId.of("America/Belem"))
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

    val ehAberta = tarefa.abertaParaTodos || tarefa.responsavelId == null
    val ehMinhaResponsabilidade = ehAberta || usuarioLogadoId == null || tarefa.responsavelId == usuarioLogadoId

    val disponivelHoje = when (tarefa.recorrencia) {
        RecorrenciaTarefa.DIARIA -> true
        RecorrenciaTarefa.SEMANAL -> {
            if (tarefa.diasSemana.isNullOrBlank()) {
                true
            } else {
                val dias = tarefa.diasSemana.split(",").map { it.trim() }
                dias.contains(diaSemanaId)
            }
        }
        RecorrenciaTarefa.MENSAL -> {
            if (tarefa.diaMes == null || tarefa.diaMes <= 0) {
                true
            } else {
                tarefa.diaMes == diaMesHoje
            }
        }
        RecorrenciaTarefa.UNICA -> true
    }

    val rotuloPeriodo = when (tarefa.recorrencia) {
        RecorrenciaTarefa.DIARIA -> "🔄 Diária"
        RecorrenciaTarefa.SEMANAL -> if (!tarefa.diasSemana.isNullOrBlank()) "📅 Semanal (${tarefa.diasSemana})" else "📅 Semanal"
        RecorrenciaTarefa.MENSAL -> if (tarefa.diaMes != null && tarefa.diaMes > 0) "🗓️ Mensal (Dia ${tarefa.diaMes})" else "🗓️ Mensal"
        RecorrenciaTarefa.UNICA -> "⚡ Única"
    }

    val podeConcluir = disponivelHoje && ehMinhaResponsabilidade

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (tarefa.estaConcluida) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (tarefa.estaConcluida) VerdeEsmeralda.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .clickable(enabled = podeConcluir) { aoAlternar() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = tarefa.estaConcluida,
                enabled = podeConcluir,
                onCheckedChange = { if (podeConcluir) aoAlternar() },
                colors = CheckboxDefaults.colors(checkedColor = VerdeEsmeralda)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tarefa.titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (tarefa.estaConcluida) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (tarefa.estaConcluida) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (ehAberta) "👥 Aberta para todos" else (tarefa.nomeResponsavel ?: "Responsável"),
                        fontSize = 11.sp,
                        color = if (ehAberta) VerdeEsmeralda else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (ehAberta) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = " • $rotuloPeriodo",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!disponivelHoje) {
                        Text(
                            text = " • 🔒 Bloqueada hoje",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AmareloOuro.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "+${tarefa.moedasRecompensa} 🪙",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
