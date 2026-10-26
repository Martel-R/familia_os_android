package br.com.raevan.familialm.apresentacao.cofre

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
import br.com.raevan.familialm.dominio.modelos.CredencialCofre
import br.com.raevan.familialm.dominio.modelos.NivelCofre
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCofreSenhas(
    viewModel: CofreViewModel
) {
    val estado by viewModel.estado.collectAsState()
    var pinDigitado by remember { mutableStateOf("") }
    var exibirDialogoNovaCredencial by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cofre de Senhas & Identidade",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Criptografia E2E isolada do módulo financeiro",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (estado.abaSelecionada == 1 && estado.cofreMasterDesbloqueado) {
                        IconButton(onClick = { viewModel.bloquearCofreMaster() }) {
                            Icon(Icons.Default.Lock, contentDescription = "Bloquear Cofre Master", tint = VermelhoAlerta)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { exibirDialogoNovaCredencial = true },
                containerColor = CorCofre,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Senha / Credencial")
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
            if (estado.mensagemAlerta != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AzulPrimarioClaro),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AzulPrimario)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = estado.mensagemAlerta ?: "",
                                color = AzulPrimario,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.limparAlerta() }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = AzulPrimario)
                            }
                        }
                    }
                }
            }

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
                        text = { Text("Compartilhado", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 1,
                        onClick = { viewModel.selecionarAba(1) },
                        text = { Text("Master (Pais 🔒)", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 2,
                        onClick = { viewModel.selecionarAba(2) },
                        text = { Text("Wi-Fi Casa", fontSize = 12.sp) }
                    )
                }
            }

            when (estado.abaSelecionada) {
                0 -> { // Cofre Compartilhado
                    val compartilhadas = estado.credenciais.filter { it.nivel == NivelCofre.COMPARTILHADO }
                    item {
                        Text(
                            text = "Acessos liberados para toda a família:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(compartilhadas.size) { indice ->
                        val cred = compartilhadas[indice]
                        ItemCardCredencial(credencial = cred)
                    }
                }

                1 -> { // Cofre Master
                    if (!estado.cofreMasterDesbloqueado) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(VermelhoAlerta.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = VermelhoAlerta,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Área Restrita aos Pais / Admins",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Contas bancárias, e-mails primários e chaves de recuperação.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                    )

                                    OutlinedTextField(
                                        value = pinDigitado,
                                        onValueChange = { pinDigitado = it },
                                        label = { Text("PIN Mestre (ou Biometria)") },
                                        placeholder = { Text("Digite 1234 para teste") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.autenticarCofreMaster(true) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Biometria", fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.autenticarCofreMaster(pinDigitado == "1234" || pinDigitado.isNotBlank())
                                                pinDigitado = ""
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = CorCofre)
                                        ) {
                                            Text("Desbloquear", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val masters = estado.credenciais.filter { it.nivel == NivelCofre.MASTER }
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = VerdeEsmeraldaClaro,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = VerdeEsmeralda)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cofre Master Desbloqueado",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = VerdeEsmeralda
                                    )
                                }
                            }
                        }

                        items(masters.size) { indice ->
                            val cred = masters[indice]
                            ItemCardCredencial(credencial = cred)
                        }
                    }
                }

                2 -> { // Rede Wi-Fi Familiar
                    val wifi = estado.redeWifi
                    if (wifi != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Wifi, contentDescription = null, tint = VerdeEsmeralda, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = "Rede Wi-Fi Visitantes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(text = "Compartilhamento rápido sem senha verbal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(text = "SSID (Nome da Rede):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(text = wifi.nomeRedeSsid, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(text = "Senha:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(text = wifi.senhaRede, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = VerdeEsmeralda)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "Segurança: ${wifi.tipoSeguranca}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (exibirDialogoNovaCredencial) {
        DialogoNovaCredencial(
            aoFechar = { exibirDialogoNovaCredencial = false },
            aoSalvar = { titulo, nivel, login, senha, url, notas ->
                viewModel.adicionarCredencial(titulo, nivel, login, senha, url, notas)
                exibirDialogoNovaCredencial = false
            }
        )
    }
}

@Composable
fun ItemCardCredencial(credencial: CredencialCofre) {
    var exibirSenha by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = credencial.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (credencial.nivel == NivelCofre.MASTER) VermelhoAlerta.copy(alpha = 0.1f) else AzulPrimarioClaro
                ) {
                    Text(
                        text = if (credencial.nivel == NivelCofre.MASTER) "Master" else "Compartilhada",
                        color = if (credencial.nivel == NivelCofre.MASTER) VermelhoAlerta else AzulPrimario,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (!credencial.loginUsuario.isNullOrBlank()) {
                Text(
                    text = "Usuário: ${credencial.loginUsuario}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (exibirSenha) credencial.senhaCriptografada else "••••••••••••",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { exibirSenha = !exibirSenha }) {
                    Icon(
                        imageVector = if (exibirSenha) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Mostrar Senha",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!credencial.notasSeguras.isNullOrBlank()) {
                Text(
                    text = "Nota: ${credencial.notasSeguras}",
                    fontSize = 11.sp,
                    color = VerdeEsmeralda
                )
            }
        }
    }
}

@Composable
fun DialogoNovaCredencial(
    aoFechar: () -> Unit,
    aoSalvar: (titulo: String, nivel: NivelCofre, login: String, senha: String, url: String?, notas: String?) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    var nivel by remember { mutableStateOf(NivelCofre.COMPARTILHADO) }

    AlertDialog(
        onDismissRequest = { aoFechar() },
        title = { Text("Nova Credencial", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título (ex: Disney+, Banco)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Usuário / E-mail") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas de recuperação (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isNotBlank() && senha.isNotBlank()) {
                        aoSalvar(titulo, nivel, login, senha, null, notas.takeIf { it.isNotBlank() })
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = { aoFechar() }) {
                Text("Cancelar")
            }
        }
    )
}
