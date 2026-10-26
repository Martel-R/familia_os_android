package br.com.raevan.familialm

import br.com.raevan.familialm.dados.repositorios.RepositorioRotinasMemoria
import br.com.raevan.familialm.dados.repositorios.RepositorioUsuariosMemoria
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RotinasGamificacaoTest {

    @Test
    fun deveIncrementarMoedasAoConcluirTarefa() = runBlocking {
        val repoUsuarios = RepositorioUsuariosMemoria()
        val repoRotinas = RepositorioRotinasMemoria(repoUsuarios)

        val usuarioInicial = repoUsuarios.obterUsuarioLogado().first()
        val saldoInicial = usuarioInicial.saldoMoedas

        // Encontra uma tarefa não concluída atribuída ao usuário ou a outro membro
        val tarefas = repoRotinas.listarTarefas().first()
        val tarefaPendente = tarefas.first { !it.estaConcluida }

        val moedasGanhas = repoRotinas.alternarConclusaoTarefa(tarefaPendente.id)

        assertTrue(moedasGanhas > 0)
        assertEquals(tarefaPendente.moedasRecompensa, moedasGanhas)

        val tarefasAtualizadas = repoRotinas.listarTarefas().first()
        val tarefaConcluida = tarefasAtualizadas.first { it.id == tarefaPendente.id }
        assertTrue(tarefaConcluida.estaConcluida)
    }

    @Test
    fun devePermitirResgateDePrivilegioComSaldoSuficiente() = runBlocking {
        val repoUsuarios = RepositorioUsuariosMemoria()
        val repoRotinas = RepositorioRotinasMemoria(repoUsuarios)

        val usuario = repoUsuarios.obterUsuarioLogado().first()
        val saldoInicial = usuario.saldoMoedas // Lucas tem 120 moedas inicialmente

        val itemLoja = repoRotinas.listarItensLoja().first().first() // custo: 25 moedas

        val sucesso = repoRotinas.resgatarItemLoja(usuario.id, itemLoja.id)

        assertTrue(sucesso)
        val usuarioAtualizado = repoUsuarios.obterUsuarioLogado().first()
        assertEquals(saldoInicial - itemLoja.custoMoedas, usuarioAtualizado.saldoMoedas)
    }
}
