package br.com.raevan.familialm

import android.app.Application
import br.com.raevan.familialm.dados.rede.ClienteSupabase
import br.com.raevan.familialm.dados.sessao.GerenciadorSessao
import br.com.raevan.familialm.nucleo.notificacoes.GerenciadorNotificacoes

class FamiliaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Inicializar Persistência de Sessão
        GerenciadorSessao.inicializar(this)

        // 2. Inicializar Cliente Supabase com SessionManager persistente
        ClienteSupabase.inicializar(this)

        // 3. Inicializar Canais de Notificação e Alarmes
        GerenciadorNotificacoes.inicializarCanais(this)
        GerenciadorNotificacoes.agendarLembreteRotinasDiarias(this, 8, 30)
    }
}
