package br.com.raevan.familialm.dados.sessao

import android.content.Context
import android.content.SharedPreferences
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

class GerenciadorSessao private constructor(context: Context) : SessionManager {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun saveSession(session: UserSession) {
        val jsonStr = json.encodeToString(UserSession.serializer(), session)
        prefs.edit().putString(KEY_SESSION, jsonStr).apply()
    }

    override suspend fun loadSession(): UserSession? {
        val jsonStr = prefs.getString(KEY_SESSION, null) ?: return null
        return try {
            json.decodeFromString(UserSession.serializer(), jsonStr)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    fun temSessaoSalva(): Boolean {
        val sessionStr = prefs.getString(KEY_SESSION, null)
        return !sessionStr.isNullOrBlank()
    }

    fun salvarUltimoEmail(email: String) {
        prefs.edit().putString(KEY_ULTIMO_EMAIL, email.trim()).apply()
    }

    fun obterUltimoEmail(): String {
        return prefs.getString(KEY_ULTIMO_EMAIL, "") ?: ""
    }

    fun isBiometriaAtivada(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIA_ATIVADA, true)
    }

    fun definirBiometriaAtivada(ativada: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIA_ATIVADA, ativada).apply()
    }

    companion object {
        private const val PREFS_NAME = "familia_os_sessao_prefs"
        private const val KEY_SESSION = "supabase_user_session"
        private const val KEY_ULTIMO_EMAIL = "ultimo_email_logado"
        private const val KEY_BIOMETRIA_ATIVADA = "biometria_ativada"

        @Volatile
        private var INSTANCIA: GerenciadorSessao? = null

        fun inicializar(context: Context): GerenciadorSessao {
            return INSTANCIA ?: synchronized(this) {
                INSTANCIA ?: GerenciadorSessao(context.applicationContext).also { INSTANCIA = it }
            }
        }

        fun obterInstancia(): GerenciadorSessao {
            return INSTANCIA ?: throw IllegalStateException("GerenciadorSessao não foi inicializado com Context!")
        }
    }
}
