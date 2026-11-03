package br.com.raevan.familialm.apresentacao.compras

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import br.com.raevan.familialm.apresentacao.componentes.formatarMoeda
import br.com.raevan.familialm.dominio.modelos.ItemCompraExtraido
import br.com.raevan.familialm.dominio.modelos.ReciboCompra
import br.com.raevan.familialm.nucleo.tema.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

private fun comprimirImagemSeNecessario(bytesOriginais: ByteArray): ByteArray {
    return try {
        val opcoesBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytesOriginais, 0, bytesOriginais.size, opcoesBounds)
        val largura = opcoesBounds.outWidth
        val altura = opcoesBounds.outHeight
        if (largura <= 0 || altura <= 0) return bytesOriginais

        var inSampleSize = 1
        val maxDim = 1800
        while (largura / inSampleSize > maxDim || altura / inSampleSize > maxDim) {
            inSampleSize *= 2
        }

        val opcoesDecode = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        val bitmap = BitmapFactory.decodeByteArray(bytesOriginais, 0, bytesOriginais.size, opcoesDecode)
            ?: return bytesOriginais
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    } catch (_: Throwable) {
        bytesOriginais
    }
}

private fun criarUriFotoTemporaria(contexto: Context): Pair<File, Uri> {
    val pasta = File(contexto.cacheDir, "notas").apply { mkdirs() }
    val arquivo = File(pasta, "cupom_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(
        contexto,
        "${contexto.packageName}.fileprovider",
        arquivo
    )
    return arquivo to uri
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaComprasInteligentes(
    viewModel: ComprasViewModel,
    aoVoltar: () -> Unit = {}
) {
    val estado by viewModel.estado.collectAsState()
    val contexto = LocalContext.current

    var busca by remember { mutableStateOf("") }
    var arquivoCameraAtual by remember { mutableStateOf<File?>(null) }
    var reciboParaAdicionarItem by remember { mutableStateOf<ReciboCompra?>(null) }

    // Launcher para capturar foto com a câmera em alta resolução
    val launcherCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { sucesso ->
        val arq = arquivoCameraAtual
        if (sucesso && arq != null && arq.exists() && arq.length() > 0) {
            try {
                val bytes = comprimirImagemSeNecessario(arq.readBytes())
                viewModel.analisarNotaFiscalComIa(
                    bytesArquivo = bytes,
                    mimeType = "image/jpeg",
                    nomeArquivo = arq.name
                )
            } catch (_: Throwable) {}
        }
    }

    val iniciarCapturaCamera = {
        try {
            val (arq, uri) = criarUriFotoTemporaria(contexto)
            arquivoCameraAtual = arq
            launcherCamera.launch(uri)
        } catch (_: Throwable) {}
    }

    val launcherPermissaoCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            iniciarCapturaCamera()
        }
    }

    // Launcher para importar imagem ou PDF do armazenamento do celular
    val launcherImportarArquivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val mimeType = contexto.contentResolver.getType(uri) ?: "image/jpeg"
                val bytesBrutos = contexto.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytesBrutos != null && bytesBrutos.isNotEmpty()) {
                    val bytesFinais = if (mimeType.startsWith("image/")) {
                        comprimirImagemSeNecessario(bytesBrutos)
                    } else {
                        bytesBrutos
                    }
                    val extensao = if (mimeType == "application/pdf") "pdf" else "jpg"
                    viewModel.analisarNotaFiscalComIa(
                        bytesArquivo = bytesFinais,
                        mimeType = if (mimeType.startsWith("image/")) "image/jpeg" else mimeType,
                        nomeArquivo = "nota_importada.$extensao"
                    )
                }
            } catch (_: Throwable) {}
        }
    }

    val acionarFotoCamera = {
        val temPermissao = ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (temPermissao) {
            iniciarCapturaCamera()
        } else {
            launcherPermissaoCamera.launch(Manifest.permission.CAMERA)
        }
    }

    val totalGasto = remember(estado.recibos) {
        estado.recibos.sumOf { it.valorTotal }
    }
    val totalItensComprados = remember(estado.recibos) {
        estado.recibos.sumOf { it.itens.size }
    }
    val recibosFiltrados = remember(estado.recibos, busca) {
        val termo = busca.trim().lowercase()
        if (termo.isEmpty()) {
            estado.recibos
        } else {
            estado.recibos.filter { r ->
                (r.fornecedor ?: "").lowercase().contains(termo) ||
                    r.itens.any {
                        it.nomeProduto.lowercase().contains(termo) ||
                            (it.categoriaSugerida ?: "").lowercase().contains(termo)
                    }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Compras Inteligentes & IA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Tire foto ou importe a nota para a IA extrair os itens",
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { acionarFotoCamera() },
                containerColor = CorCompras,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                text = { Text("Fotografar Nota (IA)", fontWeight = FontWeight.Bold) }
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
            // Card de Ações Principais: Tirar Foto c/ IA, Importar Nota (Foto/PDF) ou Manual
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🤖 Extração Inteligente de Nota Fiscal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Tire uma foto da nota ou importe um arquivo (imagem ou PDF) para a IA identificar o mercado e registrar todos os itens comprados.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { acionarFotoCamera() },
                                enabled = !estado.analisandoIa,
                                colors = ButtonDefaults.buttonColors(containerColor = CorCompras),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tirar Foto", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    launcherImportarArquivo.launch(arrayOf("image/*", "application/pdf"))
                                },
                                enabled = !estado.analisandoIa,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Importar Nota", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.abrirLancamentoManualComItens() },
                            enabled = !estado.analisandoIa,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Digitar Nota e Itens Manualmente", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Indicador de processamento da IA
            if (estado.analisandoIa) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CorCompras.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CorCompras,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                            Column {
                                Text(
                                    text = "Analisando nota fiscal com a IA...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CorCompras
                                )
                                Text(
                                    text = "Extraindo estabelecimento, data, valor total e todos os itens comprados.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Mensagens de Sucesso / Erro
            if (estado.mensagemSucesso != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VerdeEsmeraldaClaro),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerdeEsmeralda)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = estado.mensagemSucesso ?: "",
                                color = VerdeEsmeralda,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.limparMensagem() }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = VerdeEsmeralda)
                            }
                        }
                    }
                }
            }

            if (estado.mensagemErro != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = estado.mensagemErro ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.limparMensagem() }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Resumo Rápido
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total em Compras", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatarMoeda(totalGasto),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = VerdeEsmeralda
                            )
                        }
                    }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Itens Comprados", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$totalItensComprados itens (${estado.recibos.size} notas)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CorCompras
                            )
                        }
                    }
                }
            }

            // Busca por item ou estabelecimento
            item {
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    label = { Text("Buscar produto comprado ou mercado") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (busca.isNotEmpty()) {
                            IconButton(onClick = { busca = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "Notas fiscais e itens registrados (${recibosFiltrados.size}):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (recibosFiltrados.isEmpty() && !estado.carregando) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhuma nota fiscal encontrada.",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Toque em 'Tirar Foto' ou 'Importar Nota' para a IA extrair todos os itens da compra!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(recibosFiltrados.size) { indice ->
                val recibo = recibosFiltrados[indice]
                ItemCardRecibo(
                    recibo = recibo,
                    aoAdicionarItem = { reciboParaAdicionarItem = recibo },
                    aoExcluirRecibo = { viewModel.excluirRecibo(recibo.id) },
                    aoExcluirItem = { itemId -> viewModel.excluirItemRecibo(itemId) }
                )
            }

            item { Spacer(modifier = Modifier.height(84.dp)) }
        }
    }

    // Modal de revisão dos itens extraídos pela IA (ou lançamento manual completo)
    val reciboPendente = estado.reciboExtraidoPendente
    if (reciboPendente != null) {
        DialogoRevisaoNotaIa(
            reciboInicial = reciboPendente,
            salvando = estado.salvandoNota,
            aoCancelar = { viewModel.cancelarRevisaoNota() },
            aoConfirmar = { fornecedor, dataCompra, valorTotal, itens, lancarEmFinancas ->
                viewModel.confirmarRegistroNota(
                    fornecedor = fornecedor,
                    dataCompra = dataCompra,
                    valorTotal = valorTotal,
                    itens = itens,
                    lancarEmFinancas = lancarEmFinancas
                )
            }
        )
    }

    // Modal para adicionar um item extra a um recibo já salvo
    val reciboAlvo = reciboParaAdicionarItem
    if (reciboAlvo != null) {
        DialogoAdicionarItemAvulso(
            nomeFornecedor = reciboAlvo.fornecedor ?: "Nota Fiscal",
            aoFechar = { reciboParaAdicionarItem = null },
            aoSalvar = { nomeProd, qtd, valorUnit, cat ->
                viewModel.adicionarItemEmRecibo(
                    reciboId = reciboAlvo.id,
                    nomeProduto = nomeProd,
                    quantidade = qtd,
                    valorUnitario = valorUnit,
                    categoria = cat
                )
                reciboParaAdicionarItem = null
            }
        )
    }
}

@Composable
fun ItemCardRecibo(
    recibo: ReciboCompra,
    aoAdicionarItem: () -> Unit,
    aoExcluirRecibo: () -> Unit,
    aoExcluirItem: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandido = !expandido },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recibo.fornecedor ?: "Estabelecimento Comercial",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${recibo.dataCompra ?: "Data Recente"} • ${recibo.itens.size} itens",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatarMoeda(recibo.valorTotal),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = VerdeEsmeralda
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = aoAdicionarItem,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Item", fontSize = 11.sp)
                        }
                        IconButton(
                            onClick = aoExcluirRecibo,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Excluir Nota",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (expandido && recibo.itens.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Itens Comprados (${recibo.itens.size}):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                recibo.itens.forEach { item ->
                    val subtotal = item.quantidade * item.valorUnitario
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "• ${item.nomeProduto}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${item.quantidade}x ${formatarMoeda(item.valorUnitario)} • ${item.categoriaSugerida ?: "Geral"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatarMoeda(subtotal),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VerdeEsmeralda
                            )
                            IconButton(
                                onClick = { aoExcluirItem(item.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remover item",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
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
fun DialogoRevisaoNotaIa(
    reciboInicial: ReciboCompra,
    salvando: Boolean,
    aoCancelar: () -> Unit,
    aoConfirmar: (
        fornecedor: String,
        dataCompra: String,
        valorTotal: Double,
        itens: List<ItemCompraExtraido>,
        lancarEmFinancas: Boolean
    ) -> Unit
) {
    var fornecedor by remember(reciboInicial) { mutableStateOf(reciboInicial.fornecedor ?: "") }
    var dataCompra by remember(reciboInicial) {
        mutableStateOf(reciboInicial.dataCompra ?: java.time.LocalDate.now().toString())
    }
    var valorTotalTexto by remember(reciboInicial) {
        mutableStateOf(
            if (reciboInicial.valorTotal > 0.0) String.format(java.util.Locale.US, "%.2f", reciboInicial.valorTotal) else ""
        )
    }
    var lancarEmFinancas by remember { mutableStateOf(true) }
    val listaItens = remember(reciboInicial) {
        mutableStateListOf<ItemCompraExtraido>().apply {
            addAll(reciboInicial.itens)
        }
    }

    // Campos para adicionar novo item na revisão
    var novoNome by remember { mutableStateOf("") }
    var novaQtd by remember { mutableStateOf("1") }
    var novoValorUnit by remember { mutableStateOf("") }
    var novaCategoria by remember { mutableStateOf("Alimentação") }

    Dialog(
        onDismissRequest = { if (!salvando) aoCancelar() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (reciboInicial.itens.isNotEmpty()) "🤖 Itens Extraídos pela IA" else "🧾 Registrar Nota e Itens",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Confira os dados da nota e todos os itens antes de salvar",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { if (!salvando) aoCancelar() }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = fornecedor,
                            onValueChange = { fornecedor = it },
                            label = { Text("Supermercado / Estabelecimento") },
                            placeholder = { Text("Ex: Assaí Atacadista, Nazaré") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = dataCompra,
                                onValueChange = { dataCompra = it },
                                label = { Text("Data (AAAA-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = valorTotalTexto,
                                onValueChange = { valorTotalTexto = it },
                                label = { Text("Valor Total (R$)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { lancarEmFinancas = !lancarEmFinancas }
                        ) {
                            Checkbox(
                                checked = lancarEmFinancas,
                                onCheckedChange = { lancarEmFinancas = it }
                            )
                            Text(
                                text = "Lançar valor total como despesa em Finanças",
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Card para adicionar item na lista
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "+ Adicionar Item à Nota",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                OutlinedTextField(
                                    value = novoNome,
                                    onValueChange = { novoNome = it },
                                    label = { Text("Nome do Produto") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = novaQtd,
                                        onValueChange = { novaQtd = it },
                                        label = { Text("Qtd") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                    OutlinedTextField(
                                        value = novoValorUnit,
                                        onValueChange = { novoValorUnit = it },
                                        label = { Text("Valor Unit. (R$)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    Button(
                                        onClick = {
                                            if (novoNome.isNotBlank()) {
                                                val q = novaQtd.replace(",", ".").toDoubleOrNull() ?: 1.0
                                                val v = novoValorUnit.replace(",", ".").toDoubleOrNull() ?: 0.0
                                                listaItens.add(
                                                    ItemCompraExtraido(
                                                        id = UUID.randomUUID().toString(),
                                                        reciboId = "",
                                                        nomeProduto = novoNome.trim(),
                                                        quantidade = q,
                                                        valorUnitario = v,
                                                        categoriaSugerida = novaCategoria
                                                    )
                                                )
                                                novoNome = ""
                                                novaQtd = "1"
                                                novoValorUnit = ""
                                                val soma = listaItens.sumOf { it.quantidade * it.valorUnitario }
                                                if (soma > 0.0) {
                                                    valorTotalTexto = String.format(java.util.Locale.US, "%.2f", soma)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CorCompras),
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    ) {
                                        Text("Incluir")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Itens da Nota (${listaItens.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    items(listaItens.size) { idx ->
                        val item = listaItens[idx]
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.nomeProduto,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${item.quantidade}x ${formatarMoeda(item.valorUnitario)} • ${item.categoriaSugerida ?: "Geral"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatarMoeda(item.quantidade * item.valorUnitario),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = VerdeEsmeralda
                                )
                                IconButton(
                                    onClick = { listaItens.removeAt(idx) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Remover",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = aoCancelar,
                        enabled = !salvando,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val somaItens = listaItens.sumOf { it.quantidade * it.valorUnitario }
                            val totalFinal = valorTotalTexto.replace(",", ".").toDoubleOrNull()
                                ?.takeIf { it > 0.0 } ?: somaItens
                            aoConfirmar(
                                fornecedor.ifBlank { "Supermercado" },
                                dataCompra,
                                totalFinal,
                                listaItens.toList(),
                                lancarEmFinancas
                            )
                        },
                        enabled = !salvando && (fornecedor.isNotBlank() || listaItens.isNotEmpty()),
                        colors = ButtonDefaults.buttonColors(containerColor = CorCompras),
                        modifier = Modifier.weight(1.6f)
                    ) {
                        Text(
                            text = if (salvando) "Salvando..." else "Salvar Nota (${listaItens.size} itens)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoAdicionarItemAvulso(
    nomeFornecedor: String,
    aoFechar: () -> Unit,
    aoSalvar: (nomeProduto: String, quantidade: Double, valorUnitario: Double, categoria: String) -> Unit
) {
    var nomeProduto by remember { mutableStateOf("") }
    var quantidadeTexto by remember { mutableStateOf("1") }
    var valorUnitarioTexto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Alimentação") }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Adicionar Item • $nomeFornecedor", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nomeProduto,
                    onValueChange = { nomeProduto = it },
                    label = { Text("Nome do Produto") },
                    placeholder = { Text("Ex: Arroz 5kg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantidadeTexto,
                        onValueChange = { quantidadeTexto = it },
                        label = { Text("Quantidade") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = valorUnitarioTexto,
                        onValueChange = { valorUnitarioTexto = it },
                        label = { Text("Valor Unit. (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoria") },
                    placeholder = { Text("Ex: Alimentação, Limpeza") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nomeProduto.isNotBlank()) {
                        val qtd = quantidadeTexto.replace(",", ".").toDoubleOrNull() ?: 1.0
                        val valUnit = valorUnitarioTexto.replace(",", ".").toDoubleOrNull() ?: 0.0
                        aoSalvar(nomeProduto.trim(), qtd, valUnit, categoria.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CorCompras)
            ) {
                Text("Salvar Item")
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) {
                Text("Cancelar")
            }
        }
    )
}
