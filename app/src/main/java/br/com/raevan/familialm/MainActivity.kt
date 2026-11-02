package br.com.raevan.familialm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import br.com.raevan.familialm.apresentacao.navegacao.EstruturaNavegacaoPrincipal
import br.com.raevan.familialm.nucleo.notificacoes.GerenciadorNotificacoes
import br.com.raevan.familialm.nucleo.tema.TemaHomeHub

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (_: Throwable) {}

        try {
            GerenciadorNotificacoes.inicializarCanais(this)
            GerenciadorNotificacoes.agendarLembreteRotinasDiarias(this, 8, 30)
        } catch (_: Throwable) {}

        setContent {
            TemaHomeHub {
                // Solicitar permissão de notificação no Android 13+ (API 33+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val solicitadorNotificacao = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { }

                    LaunchedEffect(Unit) {
                        try {
                            val temPermissao = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!temPermissao) {
                                solicitadorNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } catch (_: Throwable) {}
                    }
                }

                EstruturaNavegacaoPrincipal()
            }
        }
    }
}
