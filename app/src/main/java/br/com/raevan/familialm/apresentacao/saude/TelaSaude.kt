package br.com.raevan.familialm.apresentacao.saude

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.raevan.familialm.dominio.modelos.*
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaSaude(
    viewModel: SaudeViewModel
) {
    val estado by viewModel.estado.collectAsState()
    var humorSelecionado by remember { mutableStateOf(5) }
    var notaDiarioTexto by remember { mutableStateOf("") }
    var consultaEditando by remember { mutableStateOf<ConsultaExameRegistro?>(null) }
    val contexto = LocalContext.current

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
                            text = "Medicamentos, vacinas, exames, métricas e SOS",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.alternarModalSos(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = VermelhoAlerta),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SOS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

            // Abas de seleção com rolagem horizontal
            item {
                ScrollableTabRow(
                    selectedTabIndex = estado.abaSelecionada,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    edgePadding = 8.dp,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = estado.abaSelecionada == 0,
                        onClick = { viewModel.selecionarAba(0) },
                        text = { Text("Fichas SOS 🚨", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 1,
                        onClick = { viewModel.selecionarAba(1) },
                        text = { Text("Remédios 💊 (${estado.medicamentos.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 2,
                        onClick = { viewModel.selecionarAba(2) },
                        text = { Text("Vacinas 💉 (${estado.vacinas.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 3,
                        onClick = { viewModel.selecionarAba(3) },
                        text = { Text("Consultas 📋 (${estado.consultas.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 4,
                        onClick = { viewModel.selecionarAba(4) },
                        text = { Text("Métricas 📊 (${estado.metricas.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 5,
                        onClick = { viewModel.selecionarAba(5) },
                        text = { Text("Humor 🙂", fontSize = 12.sp) }
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

                    items(estado.fichasMedicas) { ficha ->
                        ItemCardFichaMedica(ficha)
                    }
                }

                1 -> { // Medicamentos
                    item {
                        Text(
                            text = "Remédios com alarme e controle diário:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(estado.medicamentos) { med ->
                        ItemCardMedicamento(
                            medicamento = med,
                            aoAlternar = { viewModel.alternarMedicamentoTomado(med.id) }
                        )
                    }
                }

                2 -> { // Carteira de Vacinação
                    item {
                        Text(
                            text = "Carteira digital de vacinas da família:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (estado.vacinas.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Nenhuma vacina registrada ainda.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    } else {
                        items(estado.vacinas) { vacina ->
                            ItemCardVacina(vacina)
                        }
                    }
                }

                3 -> { // Consultas & Exames
                    item {
                        Text(
                            text = "Consultas e exames agendados:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (estado.consultas.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Nenhum agendamento pendente.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    } else {
                        items(estado.consultas) { consulta ->
                            ItemCardConsultaExame(
                                consulta = consulta,
                                aoAlternar = { viewModel.alternarConsultaConcluida(consulta.id) },
                                aoEditar = { consultaEditando = it },
                                aoExcluir = { viewModel.excluirConsultaExame(consulta.id) }
                            )
                        }
                    }
                }

                4 -> { // Métricas Vitais
                    item {
                        Text(
                            text = "Diário de pressão, glicemia, peso, altura e temperatura:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (estado.metricas.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Nenhuma medição registrada ainda.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    } else {
                        items(estado.metricas) { metrica ->
                            ItemCardMetricaVital(metrica)
                        }
                    }
                }

                5 -> { // Check-in de Humor
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Como você está se sentindo hoje?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val opcoes = listOf(
                                        Pair(1, "😢"),
                                        Pair(2, "🙁"),
                                        Pair(3, "😐"),
                                        Pair(4, "🙂"),
                                        Pair(5, "😃")
                                    )

                                    opcoes.forEach { (nivel, emoji) ->
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (humorSelecionado == nivel) AzulPrimarioClaro else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable { humorSelecionado = nivel },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = emoji, fontSize = 24.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = notaDiarioTexto,
                                    onValueChange = { notaDiarioTexto = it },
                                    label = { Text("Nota do Diário (Opcional)") },
                                    placeholder = { Text("Ex: Dia muito produtivo, passeamos no parque...") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        viewModel.registrarCheckin(humorSelecionado, notaDiarioTexto)
                                        notaDiarioTexto = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Registrar Sentimento do Dia")
                                }
                            }
                        }
                    }

                    items(estado.checkinsHumor) { checkin ->
                        ItemCardCheckin(checkin)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal SOS Emergência
    if (estado.modalSosAberto) {
        AlertDialog(
            onDismissRequest = { viewModel.alternarModalSos(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚨", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Central SOS Familiar", fontWeight = FontWeight.Bold, color = VermelhoAlerta)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Discagem rápida de emergência:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:192"))
                                contexto.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VermelhoAlerta),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🚑 192 SAMU", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:193"))
                                contexto.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LaranjaAcolhedor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🚒 193 Bombeiro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:190"))
                                contexto.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🚓 190 Polícia", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Dados Vitais Rápidos:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                    estado.fichasMedicas.forEach { ficha ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = ficha.nomeUsuario, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "Tipo: ${ficha.tipoSanguineo ?: "-"}",
                                        fontWeight = FontWeight.Bold,
                                        color = VermelhoAlerta,
                                        fontSize = 12.sp
                                    )
                                }
                                if (!ficha.alergias.isNullOrBlank()) {
                                    Text(text = "⚠️ Alergias: ${ficha.alergias}", fontSize = 11.sp, color = LaranjaAcolhedor)
                                }
                                if (!ficha.numeroPlanoSaude.isNullOrBlank()) {
                                    Text(text = "🏥 Plano Saúde: ${ficha.numeroPlanoSaude}", fontSize = 11.sp)
                                }
                                if (!ficha.numeroPlanoOdontologico.isNullOrBlank()) {
                                    Text(text = "🦷 Plano Odonto: ${ficha.numeroPlanoOdontologico}", fontSize = 11.sp)
                                }
                                if (!ficha.carteirinhaPlanoUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "💳 Ver Carteirinha Saúde",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = AzulPrimario,
                                        modifier = Modifier.clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ficha.carteirinhaPlanoUrl))
                                            contexto.startActivity(intent)
                                        }
                                    )
                                }
                                if (!ficha.carteirinhaOdontoUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🦷 Ver Carteirinha Odonto",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = VerdeEsmeralda,
                                        modifier = Modifier.clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ficha.carteirinhaOdontoUrl))
                                            contexto.startActivity(intent)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.alternarModalSos(false) }) {
                    Text("Fechar")
                }
            }
        )
    }

    if (consultaEditando != null) {
        val consulta = consultaEditando!!
        var tituloEdit by remember(consulta) { mutableStateOf(consulta.titulo) }
        var clinicaEdit by remember(consulta) { mutableStateOf(consulta.especialistaOuClinica ?: "") }
        var obsEdit by remember(consulta) { mutableStateOf(consulta.observacoes ?: "") }

        AlertDialog(
            onDismissRequest = { consultaEditando = null },
            title = {
                Text(
                    text = "Alterar ${if (consulta.tipo == "consulta") "Consulta" else "Exame"}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tituloEdit,
                        onValueChange = { tituloEdit = it },
                        label = { Text("Título / Especialidade") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clinicaEdit,
                        onValueChange = { clinicaEdit = it },
                        label = { Text("Médico ou Clínica") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = obsEdit,
                        onValueChange = { obsEdit = it },
                        label = { Text("Observações / Laudo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.atualizarConsultaExame(
                            consulta.copy(
                                titulo = tituloEdit,
                                especialistaOuClinica = clinicaEdit.ifBlank { null },
                                observacoes = obsEdit.ifBlank { null }
                            )
                        )
                        consultaEditando = null
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { consultaEditando = null }) {
                    Text("Cancelar")
                }
            }
        )
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
                    text = "Plano Saúde: ${ficha.numeroPlanoSaude}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!ficha.numeroPlanoOdontologico.isNullOrBlank()) {
                Text(
                    text = "Plano Odonto: ${ficha.numeroPlanoOdontologico}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val contexto = LocalContext.current
            if (!ficha.carteirinhaPlanoUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ficha.carteirinhaPlanoUrl))
                        contexto.startActivity(intent)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver Carteirinha de Saúde", fontSize = 12.sp)
                }
            }

            if (!ficha.carteirinhaOdontoUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ficha.carteirinhaOdontoUrl))
                        contexto.startActivity(intent)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver Carteirinha Odonto", fontSize = 12.sp)
                }
            }

            if (!ficha.resumoClinicoIa.isNullOrBlank() || !ficha.orientacoesMedicoIa.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🧠 Parecer da IA & Guia para a Consulta Médica",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!ficha.resumoClinicoIa.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🩺 Resumo Clínico: ${ficha.resumoClinicoIa}",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (!ficha.orientacoesMedicoIa.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💬 O que conversar com o Médico (Seu Norte):\n${ficha.orientacoesMedicoIa}",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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

@Composable
fun ItemCardVacina(vacina: VacinaRegistro) {
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
                Text(
                    text = vacina.nomeVacina,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AzulPrimarioClaro
                ) {
                    Text(
                        text = vacina.dose,
                        color = AzulPrimario,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Membro: ${vacina.nomeMembro} • Aplicada em: ${vacina.dataAplicacao}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!vacina.dataProximaDose.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Próxima Dose: ${vacina.dataProximaDose}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VerdeEsmeralda
                )
            }

            if (!vacina.lote.isNullOrBlank() || !vacina.localAplicacao.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Lote: ${vacina.lote ?: "-"} • Local: ${vacina.localAplicacao ?: "-"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ItemCardConsultaExame(
    consulta: ConsultaExameRegistro,
    aoAlternar: () -> Unit,
    aoEditar: (ConsultaExameRegistro) -> Unit,
    aoExcluir: () -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandido = !expandido }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = consulta.concluido,
                    onCheckedChange = { aoAlternar() },
                    colors = CheckboxDefaults.colors(checkedColor = VerdeEsmeralda)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (consulta.tipo == "consulta") AzulPrimarioClaro else Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = if (consulta.tipo == "consulta") "Consulta" else "Exame",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (consulta.tipo == "consulta") AzulPrimario else VerdeEsmeralda,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = consulta.titulo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${consulta.nomeMembro} • ${consulta.especialistaOuClinica ?: "Clínica Geral"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Data: ${consulta.dataAgendada.replace("T", " às ").substring(0, minOf(19, consulta.dataAgendada.length))}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = { aoEditar(consulta) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { aoExcluir() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", modifier = Modifier.size(18.dp), tint = VermelhoAlerta)
                }
            }

            if (expandido) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📝 Parâmetros & Observações Clínicas:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = consulta.observacoes ?: "Nenhuma observação detalhada registrada.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!consulta.dataRetorno.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "📅 Data de Retorno: ${consulta.dataRetorno}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VerdeEsmeralda
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCardMetricaVital(metrica: MetricaVitalRegistro) {
    val icone = when (metrica.tipoMetrica) {
        "pressao" -> "🩸"
        "glicemia" -> "🍬"
        "peso" -> "⚖️"
        "altura" -> "📏"
        else -> "🌡️"
    }

    val valorFormatado = if (metrica.tipoMetrica == "pressao" && metrica.valorSecundario != null) {
        "${metrica.valorPrincipal.toInt()} x ${metrica.valorSecundario.toInt()} ${metrica.unidade}"
    } else {
        "${metrica.valorPrincipal} ${metrica.unidade}"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icone, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${metrica.tipoMetrica.replaceFirstChar { it.uppercase() }} • ${metrica.nomeMembro}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = metrica.dataMedicao.substring(0, minOf(10, metrica.dataMedicao.length)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = valorFormatado,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AzulPrimario
                )
                if (!metrica.observacao.isNullOrBlank()) {
                    Text(
                        text = "Obs: ${metrica.observacao}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
