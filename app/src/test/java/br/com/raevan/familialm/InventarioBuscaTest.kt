package br.com.raevan.familialm

import br.com.raevan.familialm.dados.repositorios.RepositorioInventarioMemoria
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class InventarioBuscaTest {

    @Test
    fun deveEncontrarItemPorNomeOuLocalizacao() = runBlocking {
        val repositorio = RepositorioInventarioMemoria()

        val resultadoFuradeira = repositorio.buscarItens("Furadeira").first()
        assertEquals(1, resultadoFuradeira.size)
        assertEquals("Furadeira de Impacto Bosch 750W", resultadoFuradeira.first().nome)

        val resultadoPorLocal = repositorio.buscarItens("Sótão").first()
        assertEquals(1, resultadoPorLocal.size)
        assertEquals("Árvore de Natal e Enfeites Dourados", resultadoPorLocal.first().nome)
    }

    @Test
    fun deveFiltrarContatosDeEmergencia() = runBlocking {
        val repositorio = RepositorioInventarioMemoria()
        val contatos = repositorio.listarContatos().first()

        val emergencias = contatos.filter { it.ehEmergencia }
        assertTrue(emergencias.isNotEmpty())
        assertTrue(emergencias.any { it.nome.contains("Eletricista") })
    }
}
