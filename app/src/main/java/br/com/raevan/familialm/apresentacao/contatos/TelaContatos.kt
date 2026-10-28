package br.com.raevan.familialm.apresentacao.contatos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.raevan.familialm.dominio.modelos.ContatoAgenda
import br.com.raevan.familialm.nucleo.tema.CorContatos
import br.com.raevan.familialm.nucleo.tema.VermelhoAlerta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaContatos(
    viewModel: ContatosViewModel,
    aoVoltar: () -> Unit
) {
    val context = LocalContext.current
    val contatosTelefone by viewModel.contatosTelefone.collectAsStateWithLifecycle()
    val contatosSistema by viewModel.contatosSistema.collectAsStateWithLifecycle()
    val termoBusca by viewModel.termoBusca.collectAsStateWithLifecycle()
    val sincronizando by viewModel.sincronizando.collectAsStateWithLifecycle()
    val mensagemFeedback by viewModel.mensagemFeedback.collectAsStateWithLifecycle()

    var abaSelecionada by remember { mutableIntStateOf(0) } // 0 = Aparelho, 1 = Sistema

    // Gerenciador de permissão READ_CONTACTS
    var temPermissaoContatos by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val solicitadorPermissao = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedida ->
        temPermissaoContatos = concedida
        if (concedida) {
            viewModel.lerContatosDoAparelho(context)
        }
    }

    // Carregar contatos automaticamente se já tiver permissão
    LaunchedEffect(temPermissaoContatos) {
        if (temPermissaoContatos && contatosTelefone.isEmpty()) {
            viewModel.lerContatosDoAparelho(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Agenda & Contatos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "Sincronização com o Familia OS",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { preenchimento ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(preenchimento)
                .padding(horizontal = 16.dp)
        ) {
            // Banner de Feedback / Notificação
            mensagemFeedback?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { viewModel.limparFeedback() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Campo de Busca
            OutlinedTextField(
                value = termoBusca,
                onValueChange = { viewModel.definirBusca(it) },
                placeholder = { Text("Buscar por nome ou número...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (termoBusca.isNotBlank()) {
                        IconButton(onClick = { viewModel.definirBusca("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Botão Principal de Sincronização
            Button(
                onClick = {
                    if (!temPermissaoContatos) {
                        solicitadorPermissao.launch(Manifest.permission.READ_CONTACTS)
                    } else if (contatosTelefone.isEmpty()) {
                        viewModel.lerContatosDoAparelho(context)
                    } else {
                        viewModel.sincronizarComSistema(apenasSelecionados = true)
                    }
                },
                enabled = !sincronizando,
                colors = ButtonDefaults.buttonColors(containerColor = CorContatos),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (sincronizando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Sincronizando com o Supabase...", fontWeight = FontWeight.Bold)
                } else if (!temPermissaoContatos) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Autorizar e Ler Contatos do Celular", fontWeight = FontWeight.Bold)
                } else if (contatosTelefone.isEmpty()) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ler Contatos do Aparelho", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Sincronizar com o Familia OS (${contatosTelefone.count { it.selecionado }})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Abas de visualização
            TabRow(
                selectedTabIndex = abaSelecionada,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = abaSelecionada == 0,
                    onClick = { abaSelecionada = 0 },
                    text = { Text("Agenda do Celular (${contatosTelefone.size})") }
                )
                Tab(
                    selected = abaSelecionada == 1,
                    onClick = { abaSelecionada = 1 },
                    text = { Text("No Sistema (${contatosSistema.size})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista da Aba 0: Agenda do Celular
            if (abaSelecionada == 0) {
                if (!temPermissaoContatos) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Permissão necessária para ler os contatos da sua agenda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FilledTonalButton(onClick = { solicitadorPermissao.launch(Manifest.permission.READ_CONTACTS) }) {
                                Text("Conceder Acesso")
                            }
                        }
                    }
                } else {
                    val filtrados = contatosTelefone.filter {
                        termoBusca.isBlank() ||
                                it.nome.contains(termoBusca, ignoreCase = true) ||
                                it.telefone.contains(termoBusca)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filtrados.size} contatos listados",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = {
                                val todosSelecionados = contatosTelefone.all { it.selecionado }
                                viewModel.selecionarTodos(!todosSelecionados)
                            }
                        ) {
                            val todosSelecionados = contatosTelefone.all { it.selecionado }
                            Text(if (todosSelecionados) "Desmarcar todos" else "Marcar todos")
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtrados, key = { it.id }) { contato ->
                            ItemContatoLinha(
                                contato = contato,
                                aoAlternarSelecao = { viewModel.alternarSelecaoContato(contato.id) },
                                aoLigar = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contato.telefone}"))
                                    context.startActivity(intent)
                                },
                                aoChamarWhatsapp = {
                                    val limpo = contato.telefone.replace(Regex("[^0-9]"), "")
                                    val url = "https://api.whatsapp.com/send?phone=55$limpo"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }

            // Lista da Aba 1: Contatos Sincronizados no Sistema
            if (abaSelecionada == 1) {
                val filtrados = contatosSistema.filter {
                    termoBusca.isBlank() ||
                            it.nome.contains(termoBusca, ignoreCase = true) ||
                            it.telefone.contains(termoBusca)
                }

                if (filtrados.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum contato sincronizado no sistema ainda.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtrados, key = { it.id }) { contato ->
                            ItemContatoSistemaLinha(
                                contato = contato,
                                aoLigar = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contato.telefone}"))
                                    context.startActivity(intent)
                                },
                                aoChamarWhatsapp = {
                                    val limpo = contato.telefone.replace(Regex("[^0-9]"), "")
                                    val url = "https://api.whatsapp.com/send?phone=55$limpo"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemContatoLinha(
    contato: ContatoAgenda,
    aoAlternarSelecao: () -> Unit,
    aoLigar: () -> Unit,
    aoChamarWhatsapp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = contato.selecionado,
                onCheckedChange = { aoAlternarSelecao() }
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CorContatos.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contato.nome.take(1).uppercase(),
                    color = CorContatos,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contato.nome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contato.telefone,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = aoLigar) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Ligar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = aoChamarWhatsapp) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "WhatsApp",
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ItemContatoSistemaLinha(
    contato: ContatoAgenda,
    aoLigar: () -> Unit,
    aoChamarWhatsapp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (contato.ehEmergencia) VermelhoAlerta.copy(alpha = 0.15f) else CorContatos.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (contato.ehEmergencia) "🚨" else contato.nome.take(1).uppercase(),
                    color = if (contato.ehEmergencia) VermelhoAlerta else CorContatos,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contato.nome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contato.ehEmergencia) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EMERGÊNCIA",
                            color = VermelhoAlerta,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Text(
                    text = contato.especialidade ?: contato.telefone,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = aoLigar) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Ligar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = aoChamarWhatsapp) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "WhatsApp",
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
