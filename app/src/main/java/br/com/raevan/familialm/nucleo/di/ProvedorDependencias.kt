package br.com.raevan.familialm.nucleo.di

import br.com.raevan.familialm.dados.repositorios.*
import br.com.raevan.familialm.dominio.repositorios.*

object ProvedorDependencias {
    val repositorioUsuarios: RepositorioUsuarios by lazy {
        RepositorioUsuariosMemoria()
    }

    val repositorioFinancas: RepositorioFinancas by lazy {
        RepositorioFinancasMemoria()
    }

    val repositorioEventos: RepositorioEventos by lazy {
        RepositorioEventosMemoria()
    }

    val repositorioCofre: RepositorioCofre by lazy {
        RepositorioCofreMemoria()
    }

    val repositorioRotinas: RepositorioRotinas by lazy {
        RepositorioRotinasMemoria(repositorioUsuarios)
    }

    val repositorioSaude: RepositorioSaude by lazy {
        RepositorioSaudeMemoria()
    }

    val repositorioInventario: RepositorioInventario by lazy {
        RepositorioInventarioMemoria()
    }

    val repositorioMural: RepositorioMural by lazy {
        RepositorioMuralMemoria()
    }

    val repositorioAutomacao: RepositorioAutomacao by lazy {
        RepositorioAutomacaoMemoria()
    }

    val repositorioCompras: RepositorioCompras by lazy {
        RepositorioComprasMemoria()
    }
}
