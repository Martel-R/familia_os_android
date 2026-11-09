package br.com.raevan.familialm.nucleo.wifi

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import br.com.raevan.familialm.dominio.modelos.RedeWifiFamiliar
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.util.UUID

object UtilitarioWifiAndroid {

    /**
     * Lê um arquivo de imagem (ex: print da tela "Compartilhar Wi-Fi" do Android ou foto de QR Code)
     * e extrai automaticamente o SSID, Senha e Tipo de Segurança do QR Code Wi-Fi.
     */
    fun decodificarWifiDeUri(context: Context, uri: Uri): RedeWifiFamiliar? {
        return try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val textoQr = lerTextoQrCodeDeBitmap(bitmap) ?: return null
            interpretarTextoQrWifi(textoQr)
        } catch (_: Exception) {
            null
        }
    }

    private fun lerTextoQrCodeDeBitmap(bitmapOriginal: Bitmap): String? {
        // Redimensiona caso seja um print muito grande de alta resolução
        val maxDim = 1600
        val bitmap = if (bitmapOriginal.width > maxDim || bitmapOriginal.height > maxDim) {
            val escala = maxDim.toFloat() / maxOf(bitmapOriginal.width, bitmapOriginal.height)
            Bitmap.createScaledBitmap(
                bitmapOriginal,
                (bitmapOriginal.width * escala).toInt().coerceAtLeast(100),
                (bitmapOriginal.height * escala).toInt().coerceAtLeast(100),
                true
            )
        } else {
            bitmapOriginal
        }

        val candidatos = mutableListOf<Bitmap>()
        candidatos.add(bitmap)

        // Recortes centrais (onde o Android exibe o QR Code na tela "Compartilhar Wi-Fi")
        val w = bitmap.width
        val h = bitmap.height
        if (w > 200 && h > 200) {
            // Recorte central 70%
            runCatching {
                candidatos.add(
                    Bitmap.createBitmap(
                        bitmap,
                        (w * 0.10f).toInt(),
                        (h * 0.15f).toInt(),
                        (w * 0.80f).toInt(),
                        (h * 0.65f).toInt()
                    )
                )
            }
            // Recorte quadrado central (metade superior/central)
            runCatching {
                val lado = (minOf(w, h) * 0.75f).toInt()
                val x = ((w - lado) / 2).coerceAtLeast(0)
                val y = ((h - lado) / 2 - (h * 0.08f).toInt()).coerceAtLeast(0)
                if (x + lado <= w && y + lado <= h) {
                    candidatos.add(Bitmap.createBitmap(bitmap, x, y, lado, lado))
                }
            }
        }

        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        val reader = MultiFormatReader().apply { setHints(hints) }

        for (bmp in candidatos) {
            val largura = bmp.width
            val altura = bmp.height
            val pixels = IntArray(largura * altura)
            bmp.getPixels(pixels, 0, largura, 0, 0, largura, altura)

            val fonteNormal = RGBLuminanceSource(largura, altura, pixels)
            val fonteInvertida = fonteNormal.invert() // Suporte a prints em Modo Escuro (Dark Mode)

            val binarizers = listOf(
                HybridBinarizer(fonteNormal),
                GlobalHistogramBinarizer(fonteNormal),
                HybridBinarizer(fonteInvertida),
                GlobalHistogramBinarizer(fonteInvertida)
            )

            for (bin in binarizers) {
                try {
                    val resultado = reader.decodeWithState(BinaryBitmap(bin))
                    if (!resultado.text.isNullOrBlank()) {
                        return resultado.text
                    }
                } catch (_: Exception) {
                    reader.reset()
                }
            }
        }
        return null
    }

    /**
     * Interpreta o formato oficial de QR Code de Wi-Fi:
     * WIFI:S:<SSID>;T:<WPA|WEP|nopass>;P:<SENHA>;H:<true|false>;;
     */
    fun interpretarTextoQrWifi(textoBruto: String): RedeWifiFamiliar? {
        val texto = textoBruto.trim()
        if (texto.startsWith("WIFI:", ignoreCase = true)) {
            val corpo = texto.substring(5)
            val ssid = extrairCampoWifiQr(corpo, "S") ?: return null
            val senha = extrairCampoWifiQr(corpo, "P").orEmpty()
            val tipoBruto = extrairCampoWifiQr(corpo, "T")?.uppercase().orEmpty()
            val seguranca = when {
                tipoBruto.contains("WEP") -> "WEP"
                tipoBruto.contains("NOPASS") || senha.isEmpty() -> "Aberta"
                else -> "WPA2/WPA3"
            }
            return RedeWifiFamiliar(
                id = UUID.randomUUID().toString(),
                apelido = ssid,
                nomeRedeSsid = ssid,
                senhaRede = senha,
                tipoSeguranca = seguranca,
                ehRedeVisitantes = ssid.contains("visit", ignoreCase = true) || ssid.contains("guest", ignoreCase = true)
            )
        }
        return null
    }

    private fun extrairCampoWifiQr(corpo: String, chave: String): String? {
        val prefixo = "$chave:"
        var idx = 0
        while (idx < corpo.length) {
            val pos = corpo.indexOf(prefixo, idx, ignoreCase = true)
            if (pos == -1) return null
            if (pos == 0 || corpo[pos - 1] == ';') {
                val inicioValor = pos + prefixo.length
                val sb = StringBuilder()
                var i = inicioValor
                while (i < corpo.length) {
                    val c = corpo[i]
                    if (c == '\\' && i + 1 < corpo.length) {
                        sb.append(corpo[i + 1])
                        i += 2
                    } else if (c == ';') {
                        break
                    } else {
                        sb.append(c)
                        i++
                    }
                }
                return sb.toString()
            }
            idx = pos + prefixo.length
        }
        return null
    }

    /**
     * Gera um Bitmap contendo o QR Code oficial da rede Wi-Fi para qualquer membro ou visitante escanear.
     */
    fun gerarBitmapQrCodeWifi(rede: RedeWifiFamiliar, tamanhoPx: Int = 560): Bitmap? {
        return try {
            val tipo = when {
                rede.senhaRede.isBlank() || rede.tipoSeguranca.equals("Aberta", ignoreCase = true) -> "nopass"
                rede.tipoSeguranca.contains("WEP", ignoreCase = true) -> "WEP"
                else -> "WPA"
            }
            val ssidEsc = escaparValorQr(rede.nomeRedeSsid)
            val senhaEsc = escaparValorQr(rede.senhaRede)
            val payload = "WIFI:S:$ssidEsc;T:$tipo;P:$senhaEsc;;"

            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val matriz = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, tamanhoPx, tamanhoPx, hints)
            val pixels = IntArray(tamanhoPx * tamanhoPx)
            for (y in 0 until tamanhoPx) {
                val offset = y * tamanhoPx
                for (x in 0 until tamanhoPx) {
                    pixels[offset + x] = if (matriz.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(tamanhoPx, tamanhoPx, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, tamanhoPx, 0, 0, tamanhoPx, tamanhoPx)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun escaparValorQr(valor: String): String {
        return valor
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
            .replace("\"", "\\\"")
    }

    /**
     * Obtém o SSID da rede Wi-Fi em que o aparelho está conectado atualmente (requer permissão de localização no Android).
     */
    @SuppressLint("MissingPermission")
    fun obterSsidConectadoAtual(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wifiManager?.connectionInfo
            val bruto = info?.ssid?.trim()?.removeSurrounding("\"")
            if (bruto.isNullOrBlank() || bruto.equals("<unknown ssid>", ignoreCase = true)) {
                null
            } else {
                bruto
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Lista os SSIDs das redes Wi-Fi próximas detectadas pelo aparelho.
     */
    @SuppressLint("MissingPermission")
    fun listarSsidsProximos(context: Context): List<String> {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val resultados = wifiManager?.scanResults.orEmpty()
            resultados
                .mapNotNull { it.SSID?.trim()?.removeSurrounding("\"") }
                .filter { it.isNotBlank() && !it.equals("<unknown ssid>", ignoreCase = true) }
                .distinct()
                .sorted()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Copia a senha da rede Wi-Fi para a área de transferência do celular.
     */
    fun copiarSenhaWifi(context: Context, rede: RedeWifiFamiliar) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Senha Wi-Fi ${rede.nomeRedeSsid}", rede.senhaRede)
            clipboard?.setPrimaryClip(clip)
        } catch (_: Exception) {}
    }

    /**
     * Abre o diálogo nativo do Android 11+ (Settings.ACTION_WIFI_ADD_NETWORKS) para salvar 1 ou várias
     * redes Wi-Fi diretamente nas Redes Salvas do aparelho com 1 toque!
     */
    fun salvarRedesNoSistemaAndroid(context: Context, redes: List<RedeWifiFamiliar>): String {
        val redesValidas = redes.filter { it.nomeRedeSsid.isNotBlank() }
        if (redesValidas.isEmpty()) return "Nenhuma rede Wi-Fi válida para adicionar."

        // Copia a senha da primeira rede como conveniência extra
        copiarSenhaWifi(context, redesValidas.first())

        return try {
            val sugestoes = ArrayList<WifiNetworkSuggestion>()
            for (r in redesValidas.take(5)) {
                val builder = WifiNetworkSuggestion.Builder()
                    .setSsid(r.nomeRedeSsid.trim())
                if (r.senhaRede.length >= 8 && !r.tipoSeguranca.equals("Aberta", ignoreCase = true)) {
                    builder.setWpa2Passphrase(r.senhaRede.trim())
                }
                sugestoes.add(builder.build())
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
                    val bundle = Bundle()
                    bundle.putParcelableArrayList(Settings.EXTRA_WIFI_NETWORK_LIST, sugestoes)
                    putExtras(bundle)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Confirme na janela do Android para salvar ${sugestoes.size} rede(s) Wi-Fi neste celular!"
            } else {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wifiManager?.addNetworkSuggestions(sugestoes)
                val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Sugestão de Wi-Fi enviada ao Android e senha copiada!"
            }
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
            "Senha copiada! Conecte-se à rede '${redesValidas.first().nomeRedeSsid}'."
        }
    }
}
