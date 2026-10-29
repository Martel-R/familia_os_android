package br.com.raevan.familialm.apresentacao.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import br.com.raevan.familialm.dados.sessao.GerenciadorSessao
import br.com.raevan.familialm.nucleo.seguranca.GerenciadorBiometria
import br.com.raevan.familialm.nucleo.tema.AzulPrimario

@Composable
fun TelaDesbloqueioBiometrico(
    aoDesbloquearComSucesso: () -> Unit,
    aoEntrarComSenha: () -> Unit
) {
    val contexto = LocalContext.current
    val activity = contexto as? FragmentActivity
    var mensagemErro by remember { mutableStateOf<String?>(null) }

    val ultimoEmail = remember {
        try {
            GerenciadorSessao.obterInstancia().obterUltimoEmail()
        } catch (_: Exception) {
            ""
        }
    }

    val dispararBiometria = {
        if (activity != null && GerenciadorBiometria.podeAutenticarComBiometria(contexto)) {
            mensagemErro = null
            GerenciadorBiometria.solicitarAutenticacao(
                activity = activity,
                titulo = "Desbloquear Familia OS",
                subtitulo = "Confirme sua digital ou reconhecimento facial",
                aoSucesso = {
                    aoDesbloquearComSucesso()
                },
                aoErro = { erro ->
                    mensagemErro = erro
                }
            )
        } else {
            // Se o aparelho não possui biometria cadastrada, desbloqueia direto com a sessão salva
            aoDesbloquearComSucesso()
        }
    }

    // Dispara a biometria automaticamente ao abrir o app
    LaunchedEffect(Unit) {
        dispararBiometria()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Ícone do App
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(AzulPrimario.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometria",
                    tint = AzulPrimario,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Familia OS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sua sessão está ativa e protegida",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (ultimoEmail.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "👤 $ultimoEmail",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Botão Principal: Tentar Biometria Novamente
            Button(
                onClick = { dispararBiometria() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Desbloquear com Biometria",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão Secundário: Entrar com Senha / Outra Conta
            OutlinedButton(
                onClick = { aoEntrarComSenha() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Entrar com Senha / Trocar Conta",
                    fontSize = 14.sp
                )
            }

            if (mensagemErro != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = mensagemErro ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
