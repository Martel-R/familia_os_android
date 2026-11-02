package br.com.raevan.familialm

import android.app.Application
import android.os.Build
import android.util.Log
import br.com.raevan.familialm.dados.rede.ClienteSupabase
import br.com.raevan.familialm.dados.sessao.GerenciadorSessao
import br.com.raevan.familialm.nucleo.notificacoes.GerenciadorNotificacoes
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FamiliaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        configurarCapturaGlobalDeCrash()

        try {
            GerenciadorSessao.inicializar(this)
        } catch (t: Throwable) {
            Log.e("FamiliaApp", "Erro ao inicializar GerenciadorSessao", t)
        }

        try {
            ClienteSupabase.inicializar(this)
        } catch (t: Throwable) {
            Log.e("FamiliaApp", "Erro ao inicializar ClienteSupabase", t)
        }

        try {
            GerenciadorNotificacoes.inicializarCanais(this)
            GerenciadorNotificacoes.agendarLembreteRotinasDiarias(this, 8, 30)
        } catch (t: Throwable) {
            Log.e("FamiliaApp", "Erro ao inicializar canais e alarmes", t)
        }
    }

    private fun configurarCapturaGlobalDeCrash() {
        val handlerAnterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = Log.getStackTraceString(throwable)
                Log.e("FamiliaAppCrash", "Excecao fatal na thread ${thread.name}: $trace")
                val latch = CountDownLatch(1)
                Thread {
                    try {
                        val url = URL("${ClienteSupabase.SUPABASE_URL}/rest/v1/configuracoes_casa?on_conflict=id")
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            setRequestProperty("apikey", ClienteSupabase.SUPABASE_ANON_KEY)
                            setRequestProperty("Authorization", "Bearer ${ClienteSupabase.SUPABASE_ANON_KEY}")
                            setRequestProperty("Content-Profile", "familialm")
                            setRequestProperty("Content-Type", "application/json")
                            setRequestProperty("Prefer", "resolution=merge-duplicates")
                            setRequestProperty("User-Agent", ClienteSupabase.USER_AGENT_PADRAO)
                            connectTimeout = 3000
                            readTimeout = 3000
                            doOutput = true
                        }
                        val payload = JSONObject().apply {
                            put("id", "ultimo_crash_android")
                            put("valor", JSONObject().apply {
                                put("thread", thread.name)
                                put("exception", throwable.javaClass.name)
                                put("message", throwable.message ?: "")
                                put("stacktrace", trace.take(4000))
                                put("sdk", Build.VERSION.SDK_INT)
                                put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                            })
                        }
                        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                        conn.responseCode
                    } catch (_: Throwable) {
                    } finally {
                        latch.countDown()
                    }
                }.start()
                latch.await(2500, TimeUnit.MILLISECONDS)
            } catch (_: Throwable) {}
            handlerAnterior?.uncaughtException(thread, throwable)
        }
    }
}
