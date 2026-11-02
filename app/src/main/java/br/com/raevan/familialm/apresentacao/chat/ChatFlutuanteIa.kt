package br.com.raevan.familialm.apresentacao.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.raevan.familialm.dados.sessao.GerenciadorSessao
import br.com.raevan.familialm.nucleo.tema.AzulPrimario
import br.com.raevan.familialm.nucleo.tema.VerdeEsmeralda
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class MensagemChatIa(
    val id: String,
    val papel: String, // "user" ou "assistant"
    val texto: String,
    val sql: String? = null
)

@Composable
fun BotaoChatFlutuanteIa(
    modifier: Modifier = Modifier
) {
    var aberto by remember { mutableStateOf(false) }
    var perguntaAtual by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()

    val nomeFamilia = remember(aberto) {
        try { GerenciadorSessao.obterInstancia().obterNomeFamiliaAtiva() } catch (_: Exception) { "Minha Família" }
    }

    var mensagens by remember {
        mutableStateOf(
            listOf(
                MensagemChatIa(
                    id = "boas-vindas",
                    papel = "assistant",
                    texto = "Olá! Sou o Assistente Inteligente da sua casa. Posso consultar finanças, tarefas, inventário, saúde e eventos da sua família em tempo real. Como posso ajudar?"
                )
            )
        )
    }

    val sugestoes = listOf(
        "O que temos no inventário?",
        "Quais contas temos pendentes?",
        "Quais são as tarefas de hoje?",
        "Quem tem mais moedas na família?"
    )

    fun enviarPergunta(textoPergunta: String) {
        val limpa = textoPergunta.trim()
        if (limpa.isBlank() || carregando) return

        val msgUser = MensagemChatIa(
            id = System.currentTimeMillis().toString(),
            papel = "user",
            texto = limpa
        )
        mensagens = mensagens + msgUser
        perguntaAtual = ""
        carregando = true

        escopo.launch {
            val familiaId = try {
                GerenciadorSessao.obterInstancia().obterFamiliaIdAtiva()
            } catch (_: Exception) {
                GerenciadorSessao.DEFAULT_FAMILIA_ID
            }

            val resposta = consultarAgenteIa(limpa, familiaId)
            mensagens = mensagens + MensagemChatIa(
                id = (System.currentTimeMillis() + 1).toString(),
                papel = "assistant",
                texto = resposta.first,
                sql = resposta.second
            )
            carregando = false
        }
    }

    SmallFloatingActionButton(
        onClick = { aberto = true },
        containerColor = AzulPrimario,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = modifier.size(50.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = "Assistente IA Familia OS"
        )
    }

    if (aberto) {
        Dialog(
            onDismissRequest = { aberto = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.82f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Cabeçalho
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AzulPrimario)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Assistente Familia OS",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "🏠 $nomeFamilia • Text-to-SQL",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        IconButton(onClick = { aberto = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                        }
                    }

                    // Chips de Sugestões Rápidas
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sugestoes.size) { i ->
                            val sug = sugestoes[i]
                            SuggestionChip(
                                onClick = { enviarPergunta(sug) },
                                label = { Text(sug, fontSize = 11.sp) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Lista de Mensagens
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(mensagens.size) { index ->
                            val msg = mensagens[index]
                            BolhaMensagemChat(mensagem = msg)
                        }

                        if (carregando) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = AzulPrimario
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Consultando dados da família...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Barra de Entrada
                    Surface(
                        tonalElevation = 3.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = perguntaAtual,
                                onValueChange = { perguntaAtual = it },
                                placeholder = { Text("Pergunte sobre contas, rotinas, estoque...", fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = { enviarPergunta(perguntaAtual) },
                                enabled = !carregando && perguntaAtual.isNotBlank(),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = AzulPrimario)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BolhaMensagemChat(mensagem: MensagemChatIa) {
    val ehUsuario = mensagem.papel == "user"
    var mostrarSql by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (ehUsuario) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (ehUsuario) 16.dp else 4.dp,
                bottomEnd = if (ehUsuario) 4.dp else 16.dp
            ),
            color = if (ehUsuario) AzulPrimario else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = mensagem.texto,
                    color = if (ehUsuario) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )

                if (!mensagem.sql.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { mostrarSql = !mostrarSql }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = VerdeEsmeralda,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (mostrarSql) "Ocultar SQL" else "Ver SQL gerado",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VerdeEsmeralda
                        )
                    }

                    if (mostrarSql) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = mensagem.sql,
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun consultarAgenteIa(pergunta: String, familiaId: String): Pair<String, String?> = withContext(Dispatchers.IO) {
    val endpoints = listOf(
        "https://familia.martel.page/api/chat",
        "https://ai-familia.martel.page/api/chat"
    )

    for (endpoint in endpoints) {
        try {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", br.com.raevan.familialm.dados.rede.ClienteSupabase.USER_AGENT_PADRAO)
                doOutput = true
                connectTimeout = 18000
                readTimeout = 25000
            }
            val payload = JSONObject().apply {
                put("pergunta", pergunta)
                put("question", pergunta)
                put("familia_id", familiaId)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode in 200..299) {
                val respTexto = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(respTexto)
                val answer = json.optString("resposta")
                    .ifBlank { json.optString("answer", "Consulta concluída.") }
                val sql = json.optString("sql_gerado")
                    .ifBlank { json.optString("sql", "") }
                    .takeIf { it.isNotBlank() }
                return@withContext answer to sql
            }
        } catch (_: Exception) {}
    }

    "Não foi possível conectar ao servidor de IA no momento. Verifique sua conexão com a internet." to null
}
