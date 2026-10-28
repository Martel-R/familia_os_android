package br.com.raevan.familialm.nucleo.di

import br.com.raevan.familialm.dados.repositorios.*
import br.com.raevan.familialm.dominio.repositorios.*

object ProvedorDependencias {
    val repositorioUsuarios: RepositorioUsuarios by lazy {
        RepositorioUsuariosSupabase()
    }

    val repositorioFinancas: RepositorioFinancas by lazy {
        RepositorioFinancasSupabase()
    }

    val repositorioEventos: RepositorioEventos by lazy {
        RepositorioEventosSupabase()
    }

    val repositorioCofre: RepositorioCofre by lazy {
        RepositorioCofreSupabase()
    }

    val repositorioRotinas: RepositorioRotinas by lazy {
        RepositorioRotinasSupabase(repositorioUsuarios)
    }

    val repositorioSaude: RepositorioSaude by lazy {
        RepositorioSaudeSupabase()
    }

    val repositorioInventario: RepositorioInventario by lazy {
        RepositorioInventarioSupabase()
    }

    val repositorioMural: RepositorioMural by lazy {
        RepositorioMuralSupabase()
    }

    val repositorioAutomacao: RepositorioAutomacao by lazy {
        RepositorioAutomacaoMemoria() // Módulo local/Frigate
    }

    val repositorioCompras: RepositorioCompras by lazy {
        RepositorioComprasSupabase()
    }
}
