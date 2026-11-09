package br.com.raevan.familialm.apresentacao.cofre

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import br.com.raevan.familialm.dominio.modelos.RedeWifiFamiliar
import br.com.raevan.familialm.nucleo.seguranca.GerenciadorBiometria
import br.com.raevan.familialm.nucleo.wifi.UtilitarioWifiAndroid
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
import br.com.raevan.familialm.dominio.modelos.CredencialCofre
import br.com.raevan.familialm.dominio.modelos.NivelCofre
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCofreSenhas(
    viewModel: CofreViewModel
) {
    val contexto = LocalContext.current
    val activity = contexto as? FragmentActivity
    val estado by viewModel.estado.collectAsState()
    var pinDigitado by remember { mutableStateOf("") }
    var exibirDialogoNovaCredencial by remember { mutableStateOf(false) }
    var exibirDialogoRedeWifi by remember { mutableStateOf(false) }
    var redeWifiEditando by remember { mutableStateOf<RedeWifiFamiliar?>(null) }
    var redeWifiQrCode by remember { mutableStateOf<RedeWifiFamiliar?>(null) }

    val lancadorImportarPrintsWifi = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val extraidas = uris.mapNotNull { uri ->
                UtilitarioWifiAndroid.decodificarWifiDeUri(contexto, uri)
            }
            viewModel.importarRedesWifiEmLote(extraidas)
        }
    }

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
                            text = "Senhas e Redes Wi-Fi compartilhadas da família",
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
                onClick = {
                    if (estado.abaSelecionada == 2) {
                        redeWifiEditando = null
                        exibirDialogoRedeWifi = true
                    } else {
                        exibirDialogoNovaCredencial = true
                    }
                },
                containerColor = CorCofre,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = if (estado.abaSelecionada == 2) Icons.Default.Wifi else Icons.Default.Add,
                    contentDescription = "Adicionar"
                )
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
                        ItemCardCredencial(
                            credencial = cred,
                            aoExcluir = { viewModel.excluirCredencial(cred.id) }
                        )
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
                                            onClick = {
                                                if (activity != null && GerenciadorBiometria.podeAutenticarComBiometria(contexto)) {
                                                    GerenciadorBiometria.solicitarAutenticacao(
                                                        activity = activity,
                                                        titulo = "Desbloquear Cofre Master",
                                                        subtitulo = "Confirme sua digital ou reconhecimento facial",
                                                        aoSucesso = { viewModel.autenticarCofreMaster(true) },
                                                        aoErro = { }
                                                    )
                                                } else {
                                                    viewModel.autenticarCofreMaster(true)
                                                }
                                            },
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
                            ItemCardCredencial(
                                credencial = cred,
                                aoExcluir = { viewModel.excluirCredencial(cred.id) }
                            )
                        }
                    }
                }

                2 -> { // Redes Wi-Fi Compartilhadas da Família (Multi-Tenant)
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = VerdeEsmeralda.copy(alpha = 0.08f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VerdeEsmeralda.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = VerdeEsmeralda,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Redes Wi-Fi Compartilhadas (${estado.redesWifi.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Para importar todas as redes salvas do seu telefone: abra Configurações → Wi-Fi → Compartilhar (QR Code), tire print de cada uma e importe todas de uma vez abaixo!",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { lancadorImportarPrintsWifi.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = VerdeEsmeralda),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Importar Prints QR", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            redeWifiEditando = null
                                            exibirDialogoRedeWifi = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Nova Rede", fontSize = 12.sp)
                                    }
                                }

                                if (estado.redesWifi.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = {
                                            val msg = UtilitarioWifiAndroid.salvarRedesNoSistemaAndroid(contexto, estado.redesWifi)
                                            viewModel.exibirAlerta(msg)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.PhonelinkSetup, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Salvar Todas as Redes neste Celular (1 Toque)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    items(estado.redesWifi.size) { indice ->
                        val rede = estado.redesWifi[indice]
                        ItemCardRedeWifiFamiliar(
                            rede = rede,
                            ehPrincipal = indice == 0,
                            aoSalvarNoCelular = {
                                val msg = UtilitarioWifiAndroid.salvarRedesNoSistemaAndroid(contexto, listOf(rede))
                                viewModel.exibirAlerta(msg)
                            },
                            aoCopiarSenha = {
                                UtilitarioWifiAndroid.copiarSenhaWifi(contexto, rede)
                                viewModel.exibirAlerta("Senha da rede '${rede.nomeRedeSsid}' copiada!")
                            },
                            aoExibirQrCode = { redeWifiQrCode = rede },
                            aoEditar = {
                                redeWifiEditando = rede
                                exibirDialogoRedeWifi = true
                            },
                            aoExcluir = { viewModel.excluirRedeWifi(rede.id) }
                        )
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

    if (exibirDialogoRedeWifi) {
        DialogoRedeWifiFamiliar(
            redeInicial = redeWifiEditando,
            aoFechar = {
                exibirDialogoRedeWifi = false
                redeWifiEditando = null
            },
            aoSalvar = { id, apelido, ssid, senha, seguranca, visitantes ->
                viewModel.salvarOuEditarRedeWifi(id, apelido, ssid, senha, seguranca, visitantes)
                exibirDialogoRedeWifi = false
                redeWifiEditando = null
            }
        )
    }

    if (redeWifiQrCode != null) {
        DialogoQrCodeWifi(
            rede = redeWifiQrCode!!,
            aoFechar = { redeWifiQrCode = null }
        )
    }
}

@Composable
fun ItemCardCredencial(
    credencial: CredencialCofre,
    aoExcluir: () -> Unit = {}
) {
    val contexto = LocalContext.current
    val activity = contexto as? FragmentActivity
    var exibirSenha by remember { mutableStateOf(false) }
    var exibirDialogoPin by remember { mutableStateOf(false) }
    var pinDigitado by remember { mutableStateOf("") }
    var erroPin by remember { mutableStateOf(false) }

    val alternarExibicao = {
        if (exibirSenha) {
            exibirSenha = false
        } else {
            if (activity != null && GerenciadorBiometria.podeAutenticarComBiometria(contexto)) {
                GerenciadorBiometria.solicitarAutenticacao(
                    activity = activity,
                    titulo = "Visualizar Senha",
                    subtitulo = "Confirme sua identidade para exibir a senha de ${credencial.titulo}",
                    aoSucesso = { exibirSenha = true },
                    aoErro = { exibirDialogoPin = true }
                )
            } else {
                exibirDialogoPin = true
            }
        }
    }

    if (exibirDialogoPin) {
        AlertDialog(
            onDismissRequest = {
                exibirDialogoPin = false
                pinDigitado = ""
                erroPin = false
            },
            title = { Text("Autenticação Necessária", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Digite sua senha de acesso ou PIN para visualizar:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinDigitado,
                        onValueChange = { pinDigitado = it; erroPin = false },
                        label = { Text("Senha / PIN") },
                        singleLine = true,
                        isError = erroPin,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (erroPin) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Senha não pode ser vazia.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pinDigitado.isNotBlank()) {
                        exibirSenha = true
                        exibirDialogoPin = false
                        pinDigitado = ""
                        erroPin = false
                    } else {
                        erroPin = true
                    }
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    exibirDialogoPin = false
                    pinDigitado = ""
                    erroPin = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

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
                IconButton(onClick = { alternarExibicao() }) {
                    Icon(
                        imageVector = if (exibirSenha) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Mostrar Senha",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { aoExcluir() }) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir Credencial",
                        tint = MaterialTheme.colorScheme.error
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = nivel == NivelCofre.COMPARTILHADO,
                        onClick = { nivel = NivelCofre.COMPARTILHADO },
                        label = { Text("Compartilhado") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = nivel == NivelCofre.MASTER,
                        onClick = { nivel = NivelCofre.MASTER },
                        label = { Text("Master 🔒") },
                        modifier = Modifier.weight(1f)
                    )
                }
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

@Composable
fun ItemCardRedeWifiFamiliar(
    rede: RedeWifiFamiliar,
    ehPrincipal: Boolean,
    aoSalvarNoCelular: () -> Unit,
    aoCopiarSenha: () -> Unit,
    aoExibirQrCode: () -> Unit,
    aoEditar: () -> Unit,
    aoExcluir: () -> Unit
) {
    var mostrarSenha by remember { mutableStateOf(true) }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(VerdeEsmeralda.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = VerdeEsmeralda,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = rede.apelido.ifBlank { rede.nomeRedeSsid },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (ehPrincipal) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = VerdeEsmeralda.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Principal",
                                        color = VerdeEsmeralda,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "SSID: ${rede.nomeRedeSsid} • ${rede.tipoSeguranca}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = aoExibirQrCode) {
                        Icon(Icons.Default.QrCode2, contentDescription = "Mostrar QR Code", tint = AzulPrimario)
                    }
                    IconButton(onClick = aoEditar) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = aoExcluir) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Senha do Wi-Fi:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (mostrarSenha) rede.senhaRede.ifBlank { "(Sem senha / Aberta)" } else "••••••••••••",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = VerdeEsmeralda
                        )
                    }
                    IconButton(onClick = { mostrarSenha = !mostrarSenha }) {
                        Icon(
                            imageVector = if (mostrarSenha) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Mostrar/Ocultar Senha"
                        )
                    }
                    IconButton(onClick = aoCopiarSenha) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar Senha",
                            tint = AzulPrimario
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = aoSalvarNoCelular,
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeEsmeralda),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.WifiFind, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salvar no Celular", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = aoExibirQrCode,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("QR Code", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DialogoRedeWifiFamiliar(
    redeInicial: RedeWifiFamiliar?,
    aoFechar: () -> Unit,
    aoSalvar: (id: String?, apelido: String, ssid: String, senha: String, seguranca: String, ehVisitantes: Boolean) -> Unit
) {
    val contexto = LocalContext.current
    var apelido by remember { mutableStateOf(redeInicial?.apelido ?: "") }
    var ssid by remember { mutableStateOf(redeInicial?.nomeRedeSsid ?: "") }
    var senha by remember { mutableStateOf(redeInicial?.senhaRede ?: "") }
    var seguranca by remember { mutableStateOf(redeInicial?.tipoSeguranca ?: "WPA2/WPA3") }
    var ehVisitantes by remember { mutableStateOf(redeInicial?.ehRedeVisitantes ?: false) }
    var redesProximas by remember { mutableStateOf<List<String>>(emptyList()) }

    val lancadorPermissaoLocalizacao = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            val conectado = UtilitarioWifiAndroid.obterSsidConectadoAtual(contexto)
            if (!conectado.isNullOrBlank()) {
                ssid = conectado
                if (apelido.isBlank()) apelido = conectado
            }
            redesProximas = UtilitarioWifiAndroid.listarSsidsProximos(contexto)
        }
    }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = {
            Text(
                text = if (redeInicial == null) "Nova Rede Wi-Fi da Família" else "Editar Rede Wi-Fi",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val conectado = UtilitarioWifiAndroid.obterSsidConectadoAtual(contexto)
                        if (!conectado.isNullOrBlank()) {
                            ssid = conectado
                            if (apelido.isBlank()) apelido = conectado
                            redesProximas = UtilitarioWifiAndroid.listarSsidsProximos(contexto)
                        } else {
                            lancadorPermissaoLocalizacao.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Detectar Wi-Fi Conectado / Próximos", fontSize = 12.sp)
                }

                if (redesProximas.isNotEmpty()) {
                    Text("Redes detectadas por perto (toque para preencher):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(redesProximas.size) { idx ->
                            val nomeSsid = redesProximas[idx]
                            FilterChip(
                                selected = ssid == nomeSsid,
                                onClick = {
                                    ssid = nomeSsid
                                    if (apelido.isBlank()) apelido = nomeSsid
                                },
                                label = { Text(nomeSsid, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = apelido,
                    onValueChange = { apelido = it },
                    label = { Text("Nome / Local (ex: Casa 5G, Casa da Vó)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("Nome Exato da Rede (SSID)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha do Wi-Fi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = seguranca,
                    onValueChange = { seguranca = it },
                    label = { Text("Segurança (WPA2/WPA3, WEP, Aberta)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Rede de Visitantes", fontSize = 13.sp)
                    Switch(checked = ehVisitantes, onCheckedChange = { ehVisitantes = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ssid.isNotBlank()) {
                        aoSalvar(
                            redeInicial?.id,
                            apelido.ifBlank { ssid },
                            ssid,
                            senha,
                            seguranca,
                            ehVisitantes
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VerdeEsmeralda)
            ) {
                Text("Salvar e Compartilhar")
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DialogoQrCodeWifi(
    rede: RedeWifiFamiliar,
    aoFechar: () -> Unit
) {
    val qrBitmap = remember(rede) {
        UtilitarioWifiAndroid.gerarBitmapQrCodeWifi(rede, 560)
    }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = {
            Text(
                text = "QR Code Wi-Fi • ${rede.apelido.ifBlank { rede.nomeRedeSsid }}",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Aponte a câmera de qualquer celular para conectar automaticamente:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (qrBitmap != null) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code Wi-Fi ${rede.nomeRedeSsid}",
                            modifier = Modifier
                                .size(220.dp)
                                .padding(12.dp)
                        )
                    }
                }
                Text(
                    text = "Rede: ${rede.nomeRedeSsid}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Senha: ${rede.senhaRede}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = VerdeEsmeralda
                )
            }
        },
        confirmButton = {
            Button(onClick = aoFechar) {
                Text("Fechar")
            }
        }
    )
}
