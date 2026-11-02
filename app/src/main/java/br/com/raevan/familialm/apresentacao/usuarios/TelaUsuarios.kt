package br.com.raevan.familialm.apresentacao.usuarios

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.raevan.familialm.dominio.modelos.TipoPerfil
import br.com.raevan.familialm.dominio.modelos.Usuario
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaUsuarios(
    viewModel: UsuariosViewModel,
    aoVoltar: () -> Unit = {}
) {
    val contexto = LocalContext.current
    val estado by viewModel.estado.collectAsState()
    var exibirModalNovoMembro by remember { mutableStateOf(false) }
    var membroParaEditar by remember { mutableStateOf<Usuario?>(null) }
    var membroParaMoedas by remember { mutableStateOf<Usuario?>(null) }
    var copiadoConvite by remember { mutableStateOf(false) }

    val membrosFiltrados = remember(estado.membros, estado.filtroStatus) {
        when (estado.filtroStatus) {
            FiltroStatusMembro.TODOS -> estado.membros
            FiltroStatusMembro.ATIVOS -> estado.membros.filter { it.ativo }
            FiltroStatusMembro.INATIVOS -> estado.membros.filter { !it.ativo }
        }
    }

    val nomeFamilia = estado.familiaAtiva?.nome ?: "Minha Família"
    val codigoConvite = estado.familiaAtiva?.codigoConvite?.ifBlank { "MARTEL" } ?: "MARTEL"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Membros & Contas da Família",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "Gestão multi-tenant de perfis, acessos e moedas",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.carregarDados() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { exibirModalNovoMembro = true },
                containerColor = AzulPrimario,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Novo Membro", fontWeight = FontWeight.Bold) }
            )
        }
    ) { preenchimento ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(preenchimento)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Multi-Tenant da Família e Código de Convite
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AzulPrimario.copy(alpha = 0.09f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AzulPrimario.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🏠 $nomeFamilia",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = AzulPrimario
                                )
                                Text(
                                    text = "Compartilhe o código para novos membros entrarem pelo app ou web:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = codigoConvite,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = VerdeEsmeralda,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Código Família", codigoConvite))
                                    copiadoConvite = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (copiadoConvite) "Código Copiado!" else "Copiar Código", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Venha fazer parte da $nomeFamilia no Familia OS! Use o código de convite: $codigoConvite em https://familia.martel.page/login"
                                        )
                                    }
                                    contexto.startActivity(Intent.createChooser(intent, "Convidar Membro"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compartilhar", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Alertas de Sucesso ou Erro
            estado.mensagemFeedback?.let { msg ->
                item {
                    Surface(
                        color = VerdeEsmeraldaClaro,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.limparMensagens() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerdeEsmeralda)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = VerdeEsmeralda, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            estado.erro?.let { err ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.limparMensagens() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Resumo Rápido
            item {
                val totalAtivos = estado.membros.count { it.ativo }
                val totalMoedas = estado.membros.sumOf { it.saldoMoedas }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${estado.membros.size}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AzulPrimario)
                            Text("Total Membros", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$totalAtivos", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = VerdeEsmeralda)
                            Text("Ativos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$totalMoedas 🪙", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFE65100))
                            Text("Moedas Casa", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Filtros de Status
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FiltroStatusMembro.entries.forEach { filtro ->
                        FilterChip(
                            selected = estado.filtroStatus == filtro,
                            onClick = { viewModel.selecionarFiltro(filtro) },
                            label = { Text(filtro.rotulo) }
                        )
                    }
                }
            }

            items(membrosFiltrados.size) { indice ->
                val membro = membrosFiltrados[indice]
                CartaoMembroFamilia(
                    membro = membro,
                    ehUsuarioLogado = estado.usuarioLogado?.id == membro.id,
                    aoClicarMoedas = { membroParaMoedas = membro },
                    aoClicarEditar = { membroParaEditar = membro }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (exibirModalNovoMembro) {
        DialogoNovoMembro(
            salvando = estado.salvando,
            aoFechar = { exibirModalNovoMembro = false },
            aoConfirmar = { nome, email, senha, perfil, emoji, moedas ->
                viewModel.criarNovoMembro(nome, email, senha, perfil, emoji, moedas)
                exibirModalNovoMembro = false
            }
        )
    }

    membroParaEditar?.let { membro ->
        DialogoEditarMembro(
            membro = membro,
            aoFechar = { membroParaEditar = null },
            aoConfirmar = { nome, perfil, emoji, ativo ->
                viewModel.editarMembro(membro.id, nome, perfil, emoji, ativo)
                membroParaEditar = null
            }
        )
    }

    membroParaMoedas?.let { membro ->
        DialogoAjustarMoedas(
            membro = membro,
            aoFechar = { membroParaMoedas = null },
            aoAjustar = { delta ->
                viewModel.ajustarMoedas(membro.id, membro.nome, delta)
                membroParaMoedas = null
            }
        )
    }
}

@Composable
private fun CartaoMembroFamilia(
    membro: Usuario,
    ehUsuarioLogado: Boolean,
    aoClicarMoedas: () -> Unit,
    aoClicarEditar: () -> Unit
) {
    val avatarTexto = membro.avatarUrl?.takeIf { it.isNotBlank() && it.length <= 4 }
        ?: membro.nome.take(1).uppercase()

    val (corPerfil, rotuloPerfil) = when (membro.perfil) {
        TipoPerfil.PAI_ADMIN -> AzulPrimario to "👑 Pai / Admin"
        TipoPerfil.FILHO_MEMBRO -> VerdeEsmeralda to "🌟 Filho / Membro"
        TipoPerfil.CUIDADOR -> Color(0xFF8E24AA) to "🤝 Cuidador / Apoio"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(corPerfil.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarTexto,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = corPerfil
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = membro.nome,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (ehUsuarioLogado) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AzulPrimario.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "Você",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AzulPrimario,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!membro.email.isNullOrBlank()) {
                        Text(
                            text = membro.email,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = corPerfil.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = rotuloPerfil,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = corPerfil,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (membro.ativo) VerdeEsmeraldaClaro else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = if (membro.ativo) "Ativo" else "Inativo",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (membro.ativo) VerdeEsmeralda else VermelhoAlerta,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmareloOuro.copy(alpha = 0.2f),
                    modifier = Modifier.clickable { aoClicarMoedas() }
                ) {
                    Text(
                        text = "${membro.saldoMoedas} 🪙",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = aoClicarMoedas) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajustar Moedas", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                FilledTonalButton(onClick = aoClicarEditar) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar Perfil", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DialogoNovoMembro(
    salvando: Boolean,
    aoFechar: () -> Unit,
    aoConfirmar: (nome: String, email: String, senha: String, perfil: TipoPerfil, emoji: String, moedas: Int) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var perfil by remember { mutableStateOf(TipoPerfil.FILHO_MEMBRO) }
    var emojiSelecionado by remember { mutableStateOf("👦") }
    var moedasTexto by remember { mutableStateOf("20") }
    val emojis = listOf("👨", "👩", "👦", "👧", "👶", "👵", "👴", "🧑‍⚕️")

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Novo Membro da Família", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Selecione um avatar:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis.size) { idx ->
                        val em = emojis[idx]
                        FilterChip(
                            selected = emojiSelecionado == em,
                            onClick = { emojiSelecionado = em },
                            label = { Text(em, fontSize = 18.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Membro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail de Acesso") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha Inicial (mín. 6 dígitos)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Perfil de Acesso:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TipoPerfil.entries.forEach { op ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { perfil = op }
                        ) {
                            RadioButton(selected = perfil == op, onClick = { perfil = op })
                            Text(op.rotulo, fontSize = 13.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = moedasTexto,
                    onValueChange = { moedasTexto = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Saldo Inicial de Moedas 🪙") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    aoConfirmar(
                        nome,
                        email,
                        senha,
                        perfil,
                        emojiSelecionado,
                        moedasTexto.toIntOrNull() ?: 0
                    )
                },
                enabled = !salvando && nome.isNotBlank() && email.isNotBlank() && senha.length >= 6
            ) {
                Text(if (salvando) "Criando..." else "Criar Conta")
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
private fun DialogoEditarMembro(
    membro: Usuario,
    aoFechar: () -> Unit,
    aoConfirmar: (nome: String, perfil: TipoPerfil, emoji: String?, ativo: Boolean) -> Unit
) {
    var nome by remember { mutableStateOf(membro.nome) }
    var perfil by remember { mutableStateOf(membro.perfil) }
    var emoji by remember { mutableStateOf(membro.avatarUrl ?: "👤") }
    var ativo by remember { mutableStateOf(membro.ativo) }
    val emojis = listOf("👨", "👩", "👦", "👧", "👶", "👵", "👴", "🧑‍⚕️")

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Editar Membro", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis.size) { idx ->
                        val em = emojis[idx]
                        FilterChip(
                            selected = emoji == em,
                            onClick = { emoji = em },
                            label = { Text(em, fontSize = 18.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Perfil de Acesso:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                TipoPerfil.entries.forEach { op ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { perfil = op }
                    ) {
                        RadioButton(selected = perfil == op, onClick = { perfil = op })
                        Text(op.rotulo, fontSize = 13.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Membro Ativo na Família", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = ativo, onCheckedChange = { ativo = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { aoConfirmar(nome, perfil, emoji, ativo) },
                enabled = nome.isNotBlank()
            ) {
                Text("Salvar")
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
private fun DialogoAjustarMoedas(
    membro: Usuario,
    aoFechar: () -> Unit,
    aoAjustar: (delta: Int) -> Unit
) {
    var valorCustom by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("🪙 Moedas de ${membro.nome}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Saldo atual: ${membro.saldoMoedas} moedas",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(onClick = { aoAjustar(10) }, modifier = Modifier.weight(1f)) {
                        Text("+10 🪙")
                    }
                    FilledTonalButton(onClick = { aoAjustar(50) }, modifier = Modifier.weight(1f)) {
                        Text("+50 🪙")
                    }
                    OutlinedButton(onClick = { aoAjustar(-10) }, modifier = Modifier.weight(1f)) {
                        Text("-10 🪙")
                    }
                }

                OutlinedTextField(
                    value = valorCustom,
                    onValueChange = { valorCustom = it.filter { ch -> ch.isDigit() || ch == '-' } },
                    label = { Text("Valor personalizado (use - para descontar)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = valorCustom.toIntOrNull() ?: 0
                    if (v != 0) aoAjustar(v)
                }
            ) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) {
                Text("Fechar")
            }
        }
    )
}
