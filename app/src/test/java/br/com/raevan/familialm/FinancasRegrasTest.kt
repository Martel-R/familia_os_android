package br.com.raevan.familialm

import br.com.raevan.familialm.dados.repositorios.RepositorioFinancasMemoria
import br.com.raevan.familialm.dominio.modelos.Cofrinho
import br.com.raevan.familialm.dominio.modelos.StatusPagamento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FinancasRegrasTest {

    @Test
    fun deveCalcularProgressoDoCofrinhoCorretamente() {
        val cofrinho = Cofrinho(
            id = "cof-teste",
            nome = "Viagem",
            metaValor = 10000.0,
            saldoAtual = 2500.0
        )

        assertEquals(0.25f, cofrinho.progressoPercentual, 0.001f)
    }

    @Test
    fun deveLimitarProgressoMaximoEmCemPorCento() {
        val cofrinho = Cofrinho(
            id = "cof-teste-2",
            nome = "Reserva",
            metaValor = 1000.0,
            saldoAtual = 1500.0
        )

        assertEquals(1.0f, cofrinho.progressoPercentual, 0.001f)
    }

    @Test
    fun deveAlternarStatusPagamentoDePendenteParaPago() = runBlocking {
        val repositorio = RepositorioFinancasMemoria()
        val transacoesIniciais = repositorio.listarTransacoes().first()
        val transacaoPendente = transacoesIniciais.first { it.status == StatusPagamento.PENDENTE }

        repositorio.alternarStatusPagamento(transacaoPendente.id)

        val transacoesAtualizadas = repositorio.listarTransacoes().first()
        val transacaoModificada = transacoesAtualizadas.first { it.id == transacaoPendente.id }

        assertEquals(StatusPagamento.PAGO, transacaoModificada.status)
        assertNotNull(transacaoModificada.dataPagamento)
    }

    @Test
    fun deveAtualizarSaldoAoFazerAporteNoCofrinho() = runBlocking {
        val repositorio = RepositorioFinancasMemoria()
        val cofrinhoInicial = repositorio.listarCofrinhos().first().first()
        val saldoAnterior = cofrinhoInicial.saldoAtual

        repositorio.depositarCofrinho(cofrinhoInicial.id, 200.0)

        val cofrinhoAtualizado = repositorio.listarCofrinhos().first().first { it.id == cofrinhoInicial.id }
        assertEquals(saldoAnterior + 200.0, cofrinhoAtualizado.saldoAtual, 0.001)
    }
}
