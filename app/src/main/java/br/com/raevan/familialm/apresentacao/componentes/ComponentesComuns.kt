package br.com.raevan.familialm.apresentacao.componentes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.raevan.familialm.dados.sessao.GerenciadorSessao
import br.com.raevan.familialm.dominio.modelos.Familia
import br.com.raevan.familialm.dominio.modelos.StatusPagamento
import br.com.raevan.familialm.dominio.modelos.Usuario
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import br.com.raevan.familialm.nucleo.tema.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val FormatadorMoeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

fun formatarMoeda(valor: Double): String = FormatadorMoeda.format(valor)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperiorHomeHub(
    usuario: Usuario?,
    titulo: String = "Familia OS",
    familia: Familia? = null,
    aoClicarPerfil: () -> Unit = {},
    aoFamiliaAtualizada: () -> Unit = {}
) {
    var exibirModalFamilia by remember { mutableStateOf(false) }

    val nomeFamiliaExibicao = familia?.nome
        ?: try { GerenciadorSessao.obterInstancia().obterNomeFamiliaAtiva() } catch (_: Exception) { "Minha Família" }
    val codigoConviteExibicao = familia?.codigoConvite?.takeIf { it.isNotBlank() }
        ?: try { GerenciadorSessao.obterInstancia().obterCodigoConviteAtivo() } catch (_: Exception) { "MARTEL" }
    val familiaIdExibicao = familia?.id
        ?: usuario?.familiaId
        ?: try { GerenciadorSessao.obterInstancia().obterFamiliaIdAtiva() } catch (_: Exception) { GerenciadorSessao.DEFAULT_FAMILIA_ID }

    TopAppBar(
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = titulo,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AzulPrimario.copy(alpha = 0.12f),
                        modifier = Modifier.clickable { exibirModalFamilia = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "🏠 $nomeFamiliaExibicao",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AzulPrimario
                            )
                            if (codigoConviteExibicao.isNotBlank()) {
                                Text(
                                    text = " • $codigoConviteExibicao",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = VerdeEsmeralda
                                )
                            }
                        }
                    }
                }
                if (usuario != null) {
                    Text(
                        text = "Olá, ${usuario.nome}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            if (usuario != null) {
                // Indicador de Moedas / Gamificação Familiar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AmareloOuro.copy(alpha = 0.18f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Moedas",
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${usuario.saldoMoedas} 🪙",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE65100)
                        )
                    }
                }

                // Avatar do Usuário
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AzulPrimario)
                        .clickable {
                            exibirModalFamilia = true
                            aoClicarPerfil()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = usuario.nome.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )

    if (exibirModalFamilia) {
        DialogoEspacoFamiliaMultiTenant(
            familiaId = familiaIdExibicao,
            nomeAtual = nomeFamiliaExibicao,
            codigoConvite = codigoConviteExibicao,
            usuarioId = usuario?.id,
            aoFechar = { exibirModalFamilia = false },
            aoFamiliaAtualizada = {
                exibirModalFamilia = false
                aoFamiliaAtualizada()
            }
        )
    }
}

@Composable
fun DialogoEspacoFamiliaMultiTenant(
    familiaId: String,
    nomeAtual: String,
    codigoConvite: String,
    usuarioId: String?,
    aoFechar: () -> Unit,
    aoFamiliaAtualizada: () -> Unit
) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    var novoNomeFamilia by remember(nomeAtual) { mutableStateOf(nomeAtual) }
    var codigoParaVincular by remember { mutableStateOf("") }
    var copiado by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }
    var mensagemFeedback by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏠 Espaço da Família", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Compartilhe o código abaixo para convidar familiares ou gerencie o espaço da sua casa:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Card do Código de Convite
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AzulPrimario.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AzulPrimario.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "CÓDIGO DE CONVITE DA FAMÍLIA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = codigoConvite.ifBlank { "MARTEL" },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AzulPrimario
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText("Código Família", codigoConvite.ifBlank { "MARTEL" })
                                        )
                                        copiado = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(if (copiado) "✅ Copiado" else "📋 Copiar", fontSize = 11.sp)
                                }
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Entre na nossa família ($nomeAtual) no Familia OS usando o código de convite: ${codigoConvite.ifBlank { "MARTEL" }}\nAcesse: https://familia.martel.page/login"
                                            )
                                        }
                                        contexto.startActivity(Intent.createChooser(intent, "Compartilhar Convite"))
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = AzulPrimario)
                                }
                            }
                        }
                    }
                }

                // Editar Nome da Família
                OutlinedTextField(
                    value = novoNomeFamilia,
                    onValueChange = { novoNomeFamilia = it },
                    label = { Text("Nome da Família") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (novoNomeFamilia.isNotBlank() && !salvando) {
                            salvando = true
                            escopo.launch {
                                val ok = ProvedorDependencias.repositorioUsuarios.atualizarNomeFamilia(
                                    familiaId = familiaId,
                                    novoNome = novoNomeFamilia
                                )
                                salvando = false
                                mensagemFeedback = if (ok) "Nome da família atualizado!" else "Nome salvo localmente!"
                                aoFamiliaAtualizada()
                            }
                        }
                    },
                    enabled = !salvando && novoNomeFamilia.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (salvando) "Salvando..." else "Salvar Nome da Família")
                }

                HorizontalDivider()

                // Trocar de Família por Código de Convite
                Text(
                    text = "Entrar em outra Família com Código:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = codigoParaVincular,
                        onValueChange = { codigoParaVincular = it.uppercase() },
                        placeholder = { Text("Ex: SILVA123") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (codigoParaVincular.isNotBlank() && !salvando) {
                                salvando = true
                                escopo.launch {
                                    val fam = ProvedorDependencias.repositorioUsuarios.vincularPorCodigoConvite(
                                        usuarioId = usuarioId ?: "",
                                        codigoConvite = codigoParaVincular
                                    )
                                    salvando = false
                                    if (fam != null) {
                                        mensagemFeedback = "Vinculado à família ${fam.nome}!"
                                        aoFamiliaAtualizada()
                                    } else {
                                        mensagemFeedback = "Código não encontrado."
                                    }
                                }
                            }
                        },
                        enabled = !salvando && codigoParaVincular.isNotBlank()
                    ) {
                        Text("Vincular")
                    }
                }

                mensagemFeedback?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VerdeEsmeralda
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = aoFechar) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun CartaoResumoFinanceiro(
    saldoLivre: Double,
    receitas: Double,
    despesasPendentes: Double,
    modifier: Modifier = Modifier,
    aoClicarVerDetalhes: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { aoClicarVerDetalhes() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1E5B94), Color(0xFF1565C0), Color(0xFF00897B))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Balanço de Livre Alocação",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Ver Finanças",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formatarMoeda(saldoLivre),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Receitas",
                                tint = Color(0xFFB8F5B8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Receitas",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = formatarMoeda(receitas),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Contas Pendentes",
                                tint = Color(0xFFFFCDD2),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Contas a Pagar",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = formatarMoeda(despesasPendentes),
                            color = Color(0xFFFFCDD2),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DistintivoStatusPagamento(status: StatusPagamento) {
    val (corFundo, corTexto) = when (status) {
        StatusPagamento.PAGO -> VerdeEsmeraldaClaro to VerdeEsmeralda
        StatusPagamento.PENDENTE -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        StatusPagamento.ATRASADO -> Color(0xFFFFEBEE) to VermelhoAlerta
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = corFundo
    ) {
        Text(
            text = status.rotulo,
            color = corTexto,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun CartaoItemModulo(
    titulo: String,
    subtitulo: String,
    icone: ImageVector,
    corDestaque: Color,
    modifier: Modifier = Modifier,
    aoClicar: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { aoClicar() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(corDestaque.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icone,
                    contentDescription = titulo,
                    tint = corDestaque,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitulo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
