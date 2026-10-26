package br.com.raevan.familialm.apresentacao.financas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.raevan.familialm.dominio.modelos.*
import br.com.raevan.familialm.nucleo.di.ProvedorDependencias
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class EstadoFinancas(
    val carregando: Boolean = false,
    val resumo: ResumoFinanceiro = ResumoFinanceiro(0.0, 0.0, 0.0, 0.0, 0.0),
    val transacoes: List<TransacaoFinanceira> = emptyList(),
    val cofrinhos: List<Cofrinho> = emptyList(),
    val abaSelecionada: Int = 0 // 0: Todas / Contas Fixas, 1: Assinaturas, 2: Cofrinhos
)

class FinancasViewModel : ViewModel() {
    private val repositorioFinancas = ProvedorDependencias.repositorioFinancas

    private val _estado = MutableStateFlow(EstadoFinancas(carregando = true))
    val estado: StateFlow<EstadoFinancas> = _estado.asStateFlow()

    init {
        observarDados()
    }

    private fun observarDados() {
        viewModelScope.launch {
            combine(
                repositorioFinancas.obterResumoFinanceiro(),
                repositorioFinancas.listarTransacoes(),
                repositorioFinancas.listarCofrinhos()
            ) { resumo, transacoes, cofrinhos ->
                _estado.value.copy(
                    carregando = false,
                    resumo = resumo,
                    transacoes = transacoes,
                    cofrinhos = cofrinhos
                )
            }.collect { novoEstado ->
                _estado.value = novoEstado
            }
        }
    }

    fun selecionarAba(indice: Int) {
        _estado.update { it.copy(abaSelecionada = indice) }
    }

    fun alternarStatusPagamento(transacaoId: String) {
        viewModelScope.launch {
            repositorioFinancas.alternarStatusPagamento(transacaoId)
        }
    }

    fun adicionarTransacao(
        descricao: String,
        valor: Double,
        tipo: TipoTransacao,
        vencimento: String
    ) {
        viewModelScope.launch {
            val novaTx = TransacaoFinanceira(
                id = UUID.randomUUID().toString(),
                descricao = descricao,
                valor = valor,
                tipo = tipo,
                dataVencimento = vencimento,
                status = StatusPagamento.PENDENTE
            )
            repositorioFinancas.adicionarTransacao(novaTx)
        }
    }

    fun depositarNoCofrinho(cofrinhoId: String, valor: Double) {
        viewModelScope.launch {
            repositorioFinancas.depositarCofrinho(cofrinhoId, valor)
        }
    }
}
