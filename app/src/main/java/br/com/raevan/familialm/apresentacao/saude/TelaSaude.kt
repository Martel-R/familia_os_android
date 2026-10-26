package br.com.raevan.familialm.apresentacao.saude

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
import br.com.raevan.familialm.dominio.modelos.CheckinSaude
import br.com.raevan.familialm.dominio.modelos.FichaMedica
import br.com.raevan.familialm.dominio.modelos.MedicamentoLembrete
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaSaude(
    viewModel: SaudeViewModel
) {
    val estado by viewModel.estado.collectAsState()
    var humorSelecionado by remember { mutableStateOf(5) }
    var notaDiarioTexto by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Saúde Integral da Família",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Fichas de emergência, bem-estar e medicamentos",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerdeEsmeralda)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = estado.mensagemSucesso ?: "",
                                color = VerdeEsmeralda,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.limparMensagemSucesso() }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = VerdeEsmeralda)
                            }
                        }
                    }
                }
            }

            // Abas de seleção
            item {
                TabRow(
                    selectedTabIndex = estado.abaSelecionada,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = estado.abaSelecionada == 0,
                        onClick = { viewModel.selecionarAba(0) },
                        text = { Text("Emergência 🚨", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 1,
                        onClick = { viewModel.selecionarAba(1) },
                        text = { Text("Humor & Diário", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 2,
                        onClick = { viewModel.selecionarAba(2) },
                        text = { Text("Medicamentos", fontSize = 12.sp) }
                    )
                }
            }

            when (estado.abaSelecionada) {
                0 -> { // Fichas Médicas de Emergência
                    item {
                        Text(
                            text = "Acesso rápido a tipo sanguíneo, alergias e convênios:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(estado.fichasMedicas.size) { indice ->
                        val ficha = estado.fichasMedicas[indice]
                        ItemCardFichaMedica(ficha = ficha)
                    }
                }

                1 -> { // Check-in de Humor e Bem-estar
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Como você está se sentindo hoje?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Seletor de Emojis de Humor
                                val emojis = listOf("😢" to 1, "🙁" to 2, "😐" to 3, "🙂" to 4, "😃" to 5)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    emojis.forEach { (emoji, nivel) ->
                                        Surface(
                                            shape = CircleShape,
                                            color = if (humorSelecionado == nivel) AzulPrimarioClaro else MaterialTheme.colorScheme.surfaceVariant,
                                            border = if (humorSelecionado == nivel) androidx.compose.foundation.BorderStroke(2.dp, AzulPrimario) else null,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clickable { humorSelecionado = nivel }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = emoji, fontSize = 22.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = notaDiarioTexto,
                                    onValueChange = { notaDiarioTexto = it },
                                    label = { Text("Breve diário ou desabafo (opcional)") },
                                    placeholder = { Text("Ex: Caminhada no parque com a família...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        viewModel.registrarCheckin(humorSelecionado, notaDiarioTexto)
                                        notaDiarioTexto = ""
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(containerColor = CorSaude)
                                ) {
                                    Text("Salvar Check-in")
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Histórico de Registros Familiares:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    items(estado.checkinsHumor.size) { indice ->
                        val checkin = estado.checkinsHumor[indice]
                        ItemCardCheckin(checkin = checkin)
                    }
                }

                2 -> { // Medicamentos e Alarmes
                    item {
                        Text(
                            text = "Lembretes e controle de remédios com horário:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(estado.medicamentos.size) { indice ->
                        val med = estado.medicamentos[indice]
                        ItemCardMedicamento(
                            medicamento = med,
                            aoAlternar = { viewModel.alternarMedicamentoTomado(med.id) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ItemCardFichaMedica(ficha: FichaMedica) {
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
                    text = ficha.nomeUsuario,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (ficha.tipoSanguineo != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = "Tipo: ${ficha.tipoSanguineo}",
                            color = VermelhoAlerta,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!ficha.alergias.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Alergias: ${ficha.alergias}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE65100)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!ficha.medicamentosUsoContinuo.isNullOrBlank()) {
                Text(
                    text = "Uso Contínuo: ${ficha.medicamentosUsoContinuo}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (!ficha.numeroPlanoSaude.isNullOrBlank()) {
                Text(
                    text = "Plano: ${ficha.numeroPlanoSaude}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (!ficha.contatoEmergencia.isNullOrBlank()) {
                Text(
                    text = "Contato Rápido: ${ficha.contatoEmergencia}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AzulPrimario
                )
            }
        }
    }
}

@Composable
fun ItemCardCheckin(checkin: CheckinSaude) {
    val emoji = when (checkin.nivelHumor) {
        1 -> "😢"
        2 -> "🙁"
        3 -> "😐"
        4 -> "🙂"
        else -> "😃"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${checkin.nomeUsuario ?: "Membro"} • ${checkin.criadoEm}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (!checkin.notasDiario.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = checkin.notasDiario,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ItemCardMedicamento(
    medicamento: MedicamentoLembrete,
    aoAlternar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .clickable { aoAlternar() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = medicamento.foiTomadoHoje,
                onCheckedChange = { aoAlternar() },
                colors = CheckboxDefaults.colors(checkedColor = VerdeEsmeralda)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicamento.nomeRemedio,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (medicamento.foiTomadoHoje) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${medicamento.membroFamiliar} • ${medicamento.dosagem} às ${medicamento.horario}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (medicamento.foiTomadoHoje) VerdeEsmeraldaClaro else Color(0xFFFFF3E0)
            ) {
                Text(
                    text = if (medicamento.foiTomadoHoje) "Tomado" else "Pendente",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (medicamento.foiTomadoHoje) VerdeEsmeralda else Color(0xFFE65100),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
