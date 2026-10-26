package br.com.raevan.familialm.apresentacao.inventario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.raevan.familialm.apresentacao.componentes.formatarMoeda
import br.com.raevan.familialm.dominio.modelos.ContatoAgenda
import br.com.raevan.familialm.dominio.modelos.HistoricoManutencao
import br.com.raevan.familialm.dominio.modelos.ItemInventario
import br.com.raevan.familialm.nucleo.tema.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaInventario(
    viewModel: InventarioViewModel,
    aoVoltar: () -> Unit = {}
) {
    val estado by viewModel.estado.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Inventário & Manutenções",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Localização de itens, garantias e prestadores",
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
        }
    ) { preenchimento ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(preenchimento)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Barra de Busca
            item {
                OutlinedTextField(
                    value = estado.termoBusca,
                    onValueChange = { viewModel.atualizarBusca(it) },
                    placeholder = { Text("Buscar furadeira, lâmpadas, manuais...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    trailingIcon = {
                        if (estado.termoBusca.isNotBlank()) {
                            IconButton(onClick = { viewModel.atualizarBusca("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Abas
            item {
                TabRow(
                    selectedTabIndex = estado.abaSelecionada,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = estado.abaSelecionada == 0,
                        onClick = { viewModel.selecionarAba(0) },
                        text = { Text("Itens (${estado.itens.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 1,
                        onClick = { viewModel.selecionarAba(1) },
                        text = { Text("Agenda de Contatos", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = estado.abaSelecionada == 2,
                        onClick = { viewModel.selecionarAba(2) },
                        text = { Text("Manutenções", fontSize = 12.sp) }
                    )
                }
            }

            when (estado.abaSelecionada) {
                0 -> { // Itens da Casa
                    items(estado.itens.size) { indice ->
                        val item = estado.itens[indice]
                        ItemCardInventario(item = item)
                    }
                }

                1 -> { // Contatos Úteis
                    items(estado.contatos.size) { indice ->
                        val contato = estado.contatos[indice]
                        ItemCardContato(contato = contato)
                    }
                }

                2 -> { // Histórico e Preventivas
                    items(estado.manutencoes.size) { indice ->
                        val mnt = estado.manutencoes[indice]
                        ItemCardManutencao(manutencao = mnt)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ItemCardInventario(item: ItemInventario) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.nome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AzulPrimarioClaro
                ) {
                    Text(
                        text = "Qtd: ${item.quantidade}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = CorInventario, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.localizacao,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (item.dataFimGarantia != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Garantia até: ${item.dataFimGarantia}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ItemCardContato(contato: ContatoAgenda) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contato.nome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (contato.ehEmergencia) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = "EMERGÊNCIA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = VermelhoAlerta,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (contato.especialidade != null) {
                    Text(
                        text = contato.especialidade,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "📞 ${contato.telefone}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AzulPrimario
                )
            }
        }
    }
}

@Composable
fun ItemCardManutencao(manutencao: HistoricoManutencao) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = manutencao.nomeItem ?: manutencao.descricao,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (manutencao.custo != null) {
                    Text(
                        text = formatarMoeda(manutencao.custo),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = VerdeEsmeralda
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Serviço: ${manutencao.descricao}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (manutencao.nomePrestador != null) {
                Text(
                    text = "Prestador: ${manutencao.nomePrestador}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Realizado em: ${manutencao.dataServico}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (manutencao.dataProximaManutencao != null) {
                    Text(
                        text = "Próxima: ${manutencao.dataProximaManutencao}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }
    }
}
