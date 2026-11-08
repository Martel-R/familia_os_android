package br.com.raevan.familialm.nucleo.atualizacao

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import br.com.raevan.familialm.nucleo.tema.AzulPrimario
import br.com.raevan.familialm.nucleo.tema.VerdeSucesso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class InfoVersaoRemota(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val fallbackApkUrl: String,
    val releaseNotes: String,
    val publishedAt: String
)

object GerenciadorAtualizacaoApp {
    private val URLS_MANIFESTO = listOf(
        "https://github.com/Martel-R/familia_os_android/releases/download/latest-apk/version.json",
        "https://familia.martel.page/api/download/apk/version"
    )

    private const val PREFS_UPDATE = "familia_os_auto_update_prefs"
    private const val KEY_VERSAO_ADIADA = "versao_adiada_temporariamente"

    fun obterVersaoInstalada(context: Context): Pair<Int, String> {
        return try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = PackageInfoCompat.getLongVersionCode(pkgInfo).toInt()
            val name = pkgInfo.versionName ?: "2.0"
            code to name
        } catch (_: Throwable) {
            1 to "2.0"
        }
    }

    fun marcarVersaoAdiadaSessao(context: Context, versionCode: Int) {
        try {
            context.getSharedPreferences(PREFS_UPDATE, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_VERSAO_ADIADA, versionCode)
                .apply()
        } catch (_: Throwable) {}
    }

    private fun abrirConexaoSeguindoRedirects(urlInicial: String, timeoutMs: Int = 15000): HttpURLConnection {
        var urlAtual = urlInicial
        var tentativas = 0
        while (tentativas < 6) {
            val conn = (URL(urlAtual).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                useCaches = false
                setRequestProperty("User-Agent", "FamiliaOS-Android-Updater/2.6")
                setRequestProperty("Cache-Control", "no-cache")
            }
            val status = conn.responseCode
            if (status in listOf(
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307,
                    308
                )
            ) {
                val novaLocation = conn.getHeaderField("Location")
                conn.disconnect()
                if (novaLocation.isNullOrBlank()) break
                urlAtual = novaLocation
                tentativas++
            } else {
                return conn
            }
        }
        return URL(urlAtual).openConnection() as HttpURLConnection
    }

    suspend fun consultarVersaoRemota(): InfoVersaoRemota? = withContext(Dispatchers.IO) {
        val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
        for (urlBase in URLS_MANIFESTO) {
            try {
                val urlComTs = "$urlBase?t=${System.currentTimeMillis()}"
                val conn = abrirConexaoSeguindoRedirects(urlComTs, 12000)
                if (conn.responseCode in 200..299) {
                    val texto = conn.inputStream.bufferedReader().use { it.readText() }
                    val obj = jsonParser.parseToJsonElement(texto).jsonObject
                    val code = obj["versionCode"]?.jsonPrimitive?.intOrNull ?: continue
                    val name = obj["versionName"]?.jsonPrimitive?.content ?: "2.$code"
                    val apkUrl = obj["apkUrl"]?.jsonPrimitive?.content
                        ?: "https://github.com/Martel-R/familia_os_android/releases/download/latest-apk/familia-os.apk"
                    val fallbackUrl = obj["fallbackApkUrl"]?.jsonPrimitive?.content
                        ?: "https://familia.martel.page/api/download/apk"
                    val notes = obj["releaseNotes"]?.jsonPrimitive?.content ?: "Melhorias e correções do sistema."
                    val pub = obj["publishedAt"]?.jsonPrimitive?.content ?: ""
                    return@withContext InfoVersaoRemota(
                        versionCode = code,
                        versionName = name,
                        apkUrl = apkUrl,
                        fallbackApkUrl = fallbackUrl,
                        releaseNotes = notes,
                        publishedAt = pub
                    )
                }
            } catch (_: Throwable) {}
        }
        null
    }

    suspend fun verificarAtualizacaoDisponivel(context: Context): InfoVersaoRemota? {
        val (instaladoCode, _) = obterVersaoInstalada(context)
        val remoto = consultarVersaoRemota() ?: return null
        return if (remoto.versionCode > instaladoCode) remoto else null
    }

    suspend fun baixarApkAtualizacao(
        context: Context,
        info: InfoVersaoRemota,
        aoProgresso: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val pastaUpdate = File(context.cacheDir, "atualizacao").apply { mkdirs() }
        val arquivoDestino = File(pastaUpdate, "familia-os-v${info.versionCode}.apk")
        if (arquivoDestino.exists()) {
            arquivoDestino.delete()
        }

        val urlsDownload = listOf(info.apkUrl, info.fallbackApkUrl).distinct()
        var ultimoErro: String = "Falha no download do APK"

        for (urlDownload in urlsDownload) {
            try {
                val conn = abrirConexaoSeguindoRedirects("$urlDownload?t=${System.currentTimeMillis()}", 30000)
                if (conn.responseCode !in 200..299) {
                    ultimoErro = "HTTP ${conn.responseCode}"
                    continue
                }
                val tamanhoTotal = conn.contentLengthLong.takeIf { it > 0 } ?: 14_500_000L
                var bytesBaixados = 0L

                conn.inputStream.use { entrada ->
                    FileOutputStream(arquivoDestino).use { saida ->
                        val buffer = ByteArray(32 * 1024)
                        var lidos: Int
                        while (entrada.read(buffer).also { lidos = it } != -1) {
                            saida.write(buffer, 0, lidos)
                            bytesBaixados += lidos
                            val pct = ((bytesBaixados * 100L) / tamanhoTotal).toInt().coerceIn(1, 99)
                            withContext(Dispatchers.Main) {
                                aoProgresso(pct)
                            }
                        }
                        saida.flush()
                    }
                }

                // Valida integridade mínima (> 5 MB e cabeçalho ZIP 'PK')
                if (arquivoDestino.exists() && arquivoDestino.length() > 5_000_000L) {
                    val cabecalho = ByteArray(2)
                    arquivoDestino.inputStream().use { it.read(cabecalho) }
                    if (cabecalho[0] == 0x50.toByte() && cabecalho[1] == 0x4B.toByte()) {
                        withContext(Dispatchers.Main) { aoProgresso(100) }
                        return@withContext arquivoDestino
                    }
                }
                ultimoErro = "Arquivo APK baixado está incompleto (${arquivoDestino.length()} bytes)"
            } catch (e: Throwable) {
                ultimoErro = e.message ?: "Erro de conexão ao baixar atualização"
            }
        }

        throw IllegalStateException(ultimoErro)
    }

    fun podeInstalarPacotes(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                context.packageManager.canRequestPackageInstalls()
            } catch (_: Throwable) {
                true
            }
        } else {
            true
        }
    }

    fun abrirConfiguracaoFontesDesconhecidas(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {}
        }
    }

    fun iniciarInstalacaoApk(context: Context, arquivoApk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            arquivoApk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
fun DialogoAtualizacaoApp(
    infoRemota: InfoVersaoRemota,
    versaoInstaladaNome: String,
    versaoInstaladaCodigo: Int,
    aoFechar: () -> Unit
) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()

    var baixando by remember { mutableStateOf(false) }
    var progresso by remember { mutableStateOf(0) }
    var arquivoBaixado by remember { mutableStateOf<File?>(null) }
    var aguardandoPermissaoInstalacao by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (!baixando) aoFechar()
        },
        icon = {
            Icon(
                imageVector = if (arquivoBaixado != null) Icons.Default.CheckCircle else Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = if (arquivoBaixado != null) VerdeSucesso else AzulPrimario,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = if (arquivoBaixado != null) "Atualização Pronta!" else "Nova Atualização Disponível",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Uma nova versão do Família OS está disponível para instalação:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AzulPrimario.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Versão atual: v$versaoInstaladaNome (#$versaoInstaladaCodigo)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Nova versão: v${infoRemota.versionName} (#${infoRemota.versionCode})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrimario
                        )
                        if (infoRemota.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Novidades: ${infoRemota.releaseNotes}",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (baixando) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Baixando atualização...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("$progresso%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AzulPrimario)
                        }
                        LinearProgressIndicator(
                            progress = { progresso / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = AzulPrimario
                        )
                    }
                }

                if (aguardandoPermissaoInstalacao) {
                    Text(
                        text = "⚠️ Autorize a opção \"Permitir desta fonte\" nas configurações do Android que se abriram e depois toque em \"Instalar Agora\" abaixo.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (mensagemErro != null) {
                    Text(
                        text = "Erro: $mensagemErro",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !baixando,
                onClick = {
                    val arqExistente = arquivoBaixado
                    if (arqExistente != null && arqExistente.exists()) {
                        if (!GerenciadorAtualizacaoApp.podeInstalarPacotes(contexto)) {
                            aguardandoPermissaoInstalacao = true
                            GerenciadorAtualizacaoApp.abrirConfiguracaoFontesDesconhecidas(contexto)
                        } else {
                            aguardandoPermissaoInstalacao = false
                            GerenciadorAtualizacaoApp.iniciarInstalacaoApk(contexto, arqExistente)
                        }
                    } else {
                        baixando = true
                        progresso = 1
                        mensagemErro = null
                        escopo.launch {
                            try {
                                val arq = GerenciadorAtualizacaoApp.baixarApkAtualizacao(
                                    context = contexto,
                                    info = infoRemota,
                                    aoProgresso = { pct -> progresso = pct }
                                )
                                baixando = false
                                arquivoBaixado = arq
                                if (!GerenciadorAtualizacaoApp.podeInstalarPacotes(contexto)) {
                                    aguardandoPermissaoInstalacao = true
                                    GerenciadorAtualizacaoApp.abrirConfiguracaoFontesDesconhecidas(contexto)
                                } else {
                                    aguardandoPermissaoInstalacao = false
                                    GerenciadorAtualizacaoApp.iniciarInstalacaoApk(contexto, arq)
                                }
                            } catch (e: Throwable) {
                                baixando = false
                                mensagemErro = e.message ?: "Não foi possível baixar o APK."
                            }
                        }
                    }
                }
            ) {
                Text(
                    text = when {
                        baixando -> "Baixando..."
                        arquivoBaixado != null -> "Instalar Agora"
                        else -> "Baixar e Atualizar"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (!baixando) {
                TextButton(onClick = aoFechar) {
                    Text("Depois")
                }
            }
        }
    )
}
