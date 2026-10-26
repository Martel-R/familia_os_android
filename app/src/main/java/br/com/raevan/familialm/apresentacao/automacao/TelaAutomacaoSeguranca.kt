package br.com.raevan.familialm.apresentacao.automacao

import androidx.compose.foundation.background
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
import br.com.raevan.familialm.dominio.modelos.CameraFrigate
import br.com.raevan.familialm.dominio.modelos.DispositivoAutomacao
import br.com.raevan.familialm.dominio.modelos.TipoDispositivo
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaAutomacaoSeguranca(
    viewModel: AutomacaoViewModel,
    aoVoltar: () -> Unit = {}
) {
    val estado by viewModel.estado.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Automação & Segurança (IoT)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Integração local via Frigate NVR e Home Assistant",
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
        }
    ) { preenchimento ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(preenchimento)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Abas
            item {
                TabRow(
                    selectedTabIndex = estado.abaSelecionada,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = estado.abaSelecionada == 0,
                        onClick = { viewModel.selecionarAba(0) },
                        text = { Text("Dispositivos IoT", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 1,
                        onClick = { viewModel.selecionarAba(1) },
                        text = { Text("Câmeras NVR / Frigate", fontSize = 12.sp) }
                    )
                }
            }

            if (estado.abaSelecionada == 0) {
                item {
                    Text(
                        text = "Controle em tempo real dos ambientes da casa:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(estado.dispositivos.size) { indice ->
                    val disp = estado.dispositivos[indice]
                    ItemCardDispositivo(
                        dispositivo = disp,
                        aoAlternar = { viewModel.alternarDispositivo(disp.id) }
                    )
                }
            } else {
                item {
                    Text(
                        text = "Monitoramento perimetral com IA local (detecção de pessoas e veículos):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(estado.cameras.size) { indice ->
                    val camera = estado.cameras[indice]
                    ItemCardCamera(camera = camera)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ItemCardDispositivo(
    dispositivo: DispositivoAutomacao,
    aoAlternar: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (dispositivo.estaLigado) VerdeEsmeraldaClaro else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (dispositivo.tipo) {
                            TipoDispositivo.LUZ -> Icons.Default.Lightbulb
                            TipoDispositivo.CLIMATIZACAO -> Icons.Default.AcUnit
                            TipoDispositivo.PORTAO -> Icons.Default.Garage
                            TipoDispositivo.SENSOR -> Icons.Default.Sensors
                            TipoDispositivo.CAMERA -> Icons.Default.Videocam
                        },
                        contentDescription = null,
                        tint = if (dispositivo.estaLigado) VerdeEsmeralda else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = dispositivo.nome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${dispositivo.comodo} • ${dispositivo.valorStatus}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = dispositivo.estaLigado,
                onCheckedChange = { aoAlternar() },
                colors = SwitchDefaults.colors(checkedThumbColor = VerdeEsmeralda)
            )
        }
    }
}

@Composable
fun ItemCardCamera(camera: CameraFrigate) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = CorAutomacao, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = camera.nome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (camera.statusOnline) VerdeEsmeraldaClaro else Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = if (camera.statusOnline) "AO VIVO" else "OFFLINE",
                        color = if (camera.statusOnline) VerdeEsmeralda else VermelhoAlerta,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Localização: ${camera.localizacao}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!camera.ultimaDeteccao.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AzulPrimarioClaro.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Última Detecção IA: ${camera.ultimaDeteccao}",
                        fontSize = 11.sp,
                        color = AzulPrimario,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}
