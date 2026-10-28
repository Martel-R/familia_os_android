package br.com.raevan.familialm.apresentacao.navegacao

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.raevan.familialm.apresentacao.automacao.AutomacaoViewModel
import br.com.raevan.familialm.apresentacao.automacao.TelaAutomacaoSeguranca
import br.com.raevan.familialm.apresentacao.cofre.CofreViewModel
import br.com.raevan.familialm.apresentacao.cofre.TelaCofreSenhas
import br.com.raevan.familialm.apresentacao.compras.ComprasViewModel
import br.com.raevan.familialm.apresentacao.compras.TelaComprasInteligentes
import br.com.raevan.familialm.apresentacao.contatos.ContatosViewModel
import br.com.raevan.familialm.apresentacao.contatos.TelaContatos
import br.com.raevan.familialm.apresentacao.eventos.EventosViewModel
import br.com.raevan.familialm.apresentacao.eventos.TelaEventos
import br.com.raevan.familialm.apresentacao.financas.FinancasViewModel
import br.com.raevan.familialm.apresentacao.financas.TelaFinancas
import br.com.raevan.familialm.apresentacao.inventario.InventarioViewModel
import br.com.raevan.familialm.apresentacao.inventario.TelaInventario
import br.com.raevan.familialm.apresentacao.mural.MuralViewModel
import br.com.raevan.familialm.apresentacao.mural.TelaMuralConquistas
import br.com.raevan.familialm.apresentacao.painel.PainelViewModel
import br.com.raevan.familialm.apresentacao.painel.TelaMaisModulos
import br.com.raevan.familialm.apresentacao.painel.TelaPainelPrincipal
import br.com.raevan.familialm.apresentacao.rotinas.RotinasViewModel
import br.com.raevan.familialm.apresentacao.rotinas.TelaRotinas
import br.com.raevan.familialm.apresentacao.saude.SaudeViewModel
import br.com.raevan.familialm.apresentacao.saude.TelaSaude
import br.com.raevan.familialm.nucleo.tema.AzulPrimario

@Composable
fun EstruturaNavegacaoPrincipal() {
    val controladorNavegacao = rememberNavController()
    val entradaPilhaAtual by controladorNavegacao.currentBackStackEntryAsState()
    val rotaAtual = entradaPilhaAtual?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = NavigationBarDefaults.Elevation
            ) {
                DestinoNavegacao.itensBarraInferior.forEach { destino ->
                    val estaSelecionado = rotaAtual == destino.rota
                    NavigationBarItem(
                        selected = estaSelecionado,
                        onClick = {
                            if (rotaAtual != destino.rota) {
                                controladorNavegacao.navigate(destino.rota) {
                                    popUpTo(controladorNavegacao.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destino.icone,
                                contentDescription = destino.titulo
                            )
                        },
                        label = {
                            Text(
                                text = destino.titulo,
                                fontSize = 11.sp,
                                fontWeight = if (estaSelecionado) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AzulPrimario,
                            selectedTextColor = AzulPrimario,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { preenchimento ->
        NavHost(
            navController = controladorNavegacao,
            startDestination = DestinoNavegacao.Painel.rota,
            modifier = Modifier.padding(preenchimento)
        ) {
            composable(DestinoNavegacao.Painel.rota) {
                val viewModel: PainelViewModel = viewModel()
                TelaPainelPrincipal(
                    viewModel = viewModel,
                    aoNavegarParaFinancas = { controladorNavegacao.navigate(DestinoNavegacao.Financas.rota) },
                    aoNavegarParaRotinas = { controladorNavegacao.navigate(DestinoNavegacao.Rotinas.rota) },
                    aoNavegarParaSaude = { controladorNavegacao.navigate(DestinoNavegacao.Saude.rota) },
                    aoNavegarParaCofre = { controladorNavegacao.navigate(DestinoNavegacao.Cofre.rota) },
                    aoNavegarParaEventos = { controladorNavegacao.navigate(DestinoNavegacao.Eventos.rota) },
                    aoNavegarParaAutomacao = { controladorNavegacao.navigate(DestinoNavegacao.Automacao.rota) }
                )
            }

            composable(DestinoNavegacao.Financas.rota) {
                val viewModel: FinancasViewModel = viewModel()
                TelaFinancas(viewModel = viewModel)
            }

            composable(DestinoNavegacao.Rotinas.rota) {
                val viewModel: RotinasViewModel = viewModel()
                TelaRotinas(viewModel = viewModel)
            }

            composable(DestinoNavegacao.Saude.rota) {
                val viewModel: SaudeViewModel = viewModel()
                TelaSaude(viewModel = viewModel)
            }

            composable(DestinoNavegacao.Cofre.rota) {
                val viewModel: CofreViewModel = viewModel()
                TelaCofreSenhas(viewModel = viewModel)
            }

            composable(DestinoNavegacao.MaisModulos.rota) {
                TelaMaisModulos(
                    aoNavegarParaEventos = { controladorNavegacao.navigate(DestinoNavegacao.Eventos.rota) },
                    aoNavegarParaInventario = { controladorNavegacao.navigate(DestinoNavegacao.Inventario.rota) },
                    aoNavegarParaMural = { controladorNavegacao.navigate(DestinoNavegacao.Mural.rota) },
                    aoNavegarParaAutomacao = { controladorNavegacao.navigate(DestinoNavegacao.Automacao.rota) },
                    aoNavegarParaCompras = { controladorNavegacao.navigate(DestinoNavegacao.Compras.rota) },
                    aoNavegarParaContatos = { controladorNavegacao.navigate(DestinoNavegacao.Contatos.rota) }
                )
            }

            composable(DestinoNavegacao.Contatos.rota) {
                val viewModel: ContatosViewModel = viewModel()
                TelaContatos(viewModel = viewModel, aoVoltar = { controladorNavegacao.popBackStack() })
            }

            composable(DestinoNavegacao.Eventos.rota) {
                val viewModel: EventosViewModel = viewModel()
                TelaEventos(viewModel = viewModel, aoVoltar = { controladorNavegacao.popBackStack() })
            }

            composable(DestinoNavegacao.Inventario.rota) {
                val viewModel: InventarioViewModel = viewModel()
                TelaInventario(viewModel = viewModel, aoVoltar = { controladorNavegacao.popBackStack() })
            }

            composable(DestinoNavegacao.Mural.rota) {
                val viewModel: MuralViewModel = viewModel()
                TelaMuralConquistas(viewModel = viewModel, aoVoltar = { controladorNavegacao.popBackStack() })
            }

            composable(DestinoNavegacao.Automacao.rota) {
                val viewModel: AutomacaoViewModel = viewModel()
                TelaAutomacaoSeguranca(viewModel = viewModel, aoVoltar = { controladorNavegacao.popBackStack() })
            }

            composable(DestinoNavegacao.Compras.rota) {
                val viewModel: ComprasViewModel = viewModel()
                TelaComprasInteligentes(viewModel = viewModel, aoVoltar = { controladorNavegacao.popBackStack() })
            }
        }
    }
}
