package br.com.raevan.familialm.dados.rede

import android.content.Context
import br.com.raevan.familialm.dados.sessao.GerenciadorSessao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage

object ClienteSupabase {
    private const val SUPABASE_URL = "https://api-supabase.martel.page"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0"

    @Volatile
    private var clienteInstancia: SupabaseClient? = null

    fun inicializar(context: Context) {
        if (clienteInstancia == null) {
            val sessionManager = GerenciadorSessao.inicializar(context)
            clienteInstancia = createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_ANON_KEY
            ) {
                install(Postgrest) {
                    defaultSchema = "familialm"
                }
                install(Auth) {
                    this.sessionManager = sessionManager
                }
                install(Storage)
            }
        }
    }

    val client: SupabaseClient
        get() = clienteInstancia ?: synchronized(this) {
            clienteInstancia ?: criarPadrao().also { clienteInstancia = it }
        }

    private fun criarPadrao(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Postgrest) {
                defaultSchema = "familialm"
            }
            install(Auth)
            install(Storage)
        }
    }
}
