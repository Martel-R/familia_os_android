package br.com.raevan.familialm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.raevan.familialm.apresentacao.navegacao.EstruturaNavegacaoPrincipal
import br.com.raevan.familialm.nucleo.tema.TemaHomeHub

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaHomeHub {
                EstruturaNavegacaoPrincipal()
            }
        }
    }
}
