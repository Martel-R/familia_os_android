package br.com.raevan.familialm.nucleo.tema

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val EsquemaCoresClaro = lightColorScheme(
    primary = AzulPrimario,
    onPrimary = SuperficieClara,
    primaryContainer = AzulPrimarioClaro,
    onPrimaryContainer = AzulPrimario,
    secondary = VerdeEsmeralda,
    onSecondary = SuperficieClara,
    secondaryContainer = VerdeEsmeraldaClaro,
    onSecondaryContainer = VerdeEsmeralda,
    background = FundoClaro,
    onBackground = TextoPrimarioClaro,
    surface = SuperficieClara,
    onSurface = TextoPrimarioClaro,
    surfaceVariant = SuperficieVarianteClara,
    onSurfaceVariant = TextoSecundarioClaro,
    outline = BordaClara
)

private val EsquemaCoresEscuro = darkColorScheme(
    primary = AzulPrimarioEscuro,
    onPrimary = FundoEscuro,
    primaryContainer = AzulPrimario,
    onPrimaryContainer = AzulPrimarioClaro,
    secondary = VerdeEsmeraldaEscuro,
    onSecondary = FundoEscuro,
    secondaryContainer = VerdeEsmeralda,
    onSecondaryContainer = VerdeEsmeraldaClaro,
    background = FundoEscuro,
    onBackground = TextoPrimarioEscuro,
    surface = SuperficieEscura,
    onSurface = TextoPrimarioEscuro,
    surfaceVariant = SuperficieVarianteEscura,
    onSurfaceVariant = TextoSecundarioEscuro,
    outline = BordaEscura
)

@Composable
fun TemaHomeHub(
    modoEscuro: Boolean = isSystemInDarkTheme(),
    conteudo: @Composable () -> Unit
) {
    val esquemaCores = if (modoEscuro) EsquemaCoresEscuro else EsquemaCoresClaro
    val visualizacao = LocalView.current

    if (!visualizacao.isInEditMode) {
        SideEffect {
            val janela = (visualizacao.context as? Activity)?.window
            if (janela != null) {
                janela.statusBarColor = esquemaCores.background.toArgb()
                janela.navigationBarColor = esquemaCores.background.toArgb()
                WindowCompat.getInsetsController(janela, visualizacao).isAppearanceLightStatusBars = !modoEscuro
                WindowCompat.getInsetsController(janela, visualizacao).isAppearanceLightNavigationBars = !modoEscuro
            }
        }
    }

    MaterialTheme(
        colorScheme = esquemaCores,
        content = conteudo
    )
}
