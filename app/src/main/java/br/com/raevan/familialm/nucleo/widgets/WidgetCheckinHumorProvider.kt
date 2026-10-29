package br.com.raevan.familialm.nucleo.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import br.com.raevan.familialm.MainActivity
import br.com.raevan.familialm.R
import br.com.raevan.familialm.dados.rede.ClienteSupabase
import br.com.raevan.familialm.dados.remoto.dto.CheckinSaudeDto
import br.com.raevan.familialm.dados.remoto.dto.UsuarioDto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class WidgetCheckinHumorProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REGISTRAR_HUMOR = "br.com.raevan.familialm.ACTION_REGISTRAR_HUMOR"
        const val EXTRA_NIVEL_HUMOR = "EXTRA_NIVEL_HUMOR"
        private const val PREFS_NAME = "widget_checkin_humor_prefs"
        private const val KEY_ULTIMO_NIVEL = "ultimo_nivel"
        private const val KEY_ULTIMO_HORARIO = "ultimo_horario"

        private val EMOJIS = mapOf(
            1 to ("😢" to "Muito Mal"),
            2 to ("😕" to "Para Baixo"),
            3 to ("😐" to "Neutro"),
            4 to ("🙂" to "Bem"),
            5 to ("😄" to "Excelente")
        )
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            atualizarVisualWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_REGISTRAR_HUMOR) {
            val nivel = intent.getIntExtra(EXTRA_NIVEL_HUMOR, 3)
            processarCheckin(context, nivel)
        }
    }

    private fun processarCheckin(context: Context, nivel: Int) {
        val horaAtual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_ULTIMO_NIVEL, nivel)
            .putString(KEY_ULTIMO_HORARIO, horaAtual)
            .apply()

        // Atualiza a visualização do widget imediatamente
        atualizarTodosWidgets(context)

        // Salva no Supabase em segundo plano
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ClienteSupabase.inicializar(context.applicationContext)
                var userId = try { ClienteSupabase.client.auth.currentUserOrNull()?.id } catch (_: Exception) { null }

                if (userId.isNullOrBlank()) {
                    val usuarios = try {
                        ClienteSupabase.client.from("usuarios").select {
                            filter { eq("ativo", true) }
                            limit(1)
                        }.decodeList<UsuarioDto>()
                    } catch (_: Exception) {
                        emptyList<UsuarioDto>()
                    }
                    if (usuarios.isNotEmpty()) {
                        userId = usuarios[0].id
                    }
                }

                if (!userId.isNullOrBlank()) {
                    val dto = CheckinSaudeDto(
                        id = UUID.randomUUID().toString(),
                        usuarioId = userId,
                        nivelHumor = nivel,
                        notasDiario = "Registrado via Widget da Tela Inicial"
                    )
                    ClienteSupabase.client.from("saude_checkins").insert(dto)
                }

                withContext(Dispatchers.Main) {
                    val msg = when (nivel) {
                        1 -> "😢 Registrado. Esperamos que seu dia melhore!"
                        2 -> "😕 Registrado. Força, um passo de cada vez!"
                        3 -> "😐 Registrado: Humor Neutro."
                        4 -> "🙂 Registrado: Que bom que você está bem!"
                        5 -> "😄 Registrado: Excelente! Tenha um dia incrível!"
                        else -> "Humor registrado com sucesso!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val info = EMOJIS[nivel] ?: ("🙂" to "Registrado")
                    Toast.makeText(context, "${info.first} Check-in salvo no aparelho!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun atualizarTodosWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, WidgetCheckinHumorProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        for (appWidgetId in appWidgetIds) {
            atualizarVisualWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun atualizarVisualWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_checkin_humor)

        // Configurar PendingIntents para cada emoji (1 a 5)
        views.setOnClickPendingIntent(R.id.btn_humor_1, criarPendingIntentHumor(context, 1, appWidgetId * 10 + 1))
        views.setOnClickPendingIntent(R.id.btn_humor_2, criarPendingIntentHumor(context, 2, appWidgetId * 10 + 2))
        views.setOnClickPendingIntent(R.id.btn_humor_3, criarPendingIntentHumor(context, 3, appWidgetId * 10 + 3))
        views.setOnClickPendingIntent(R.id.btn_humor_4, criarPendingIntentHumor(context, 4, appWidgetId * 10 + 4))
        views.setOnClickPendingIntent(R.id.btn_humor_5, criarPendingIntentHumor(context, 5, appWidgetId * 10 + 5))

        // PendingIntent para abrir o app no módulo de saúde ao clicar no rodapé ou cabeçalho
        val intentAbrir = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ABRIR_ABA", "saude")
        }
        val pendingIntentAbrir = PendingIntent.getActivity(
            context,
            appWidgetId * 10 + 9,
            intentAbrir,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.tv_widget_abrir_app, pendingIntentAbrir)
        views.setOnClickPendingIntent(R.id.widget_header, pendingIntentAbrir)

        // Exibir último humor registrado se houver
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ultimoNivel = prefs.getInt(KEY_ULTIMO_NIVEL, 0)
        val ultimoHorario = prefs.getString(KEY_ULTIMO_HORARIO, null)

        if (ultimoNivel in 1..5 && !ultimoHorario.isNullOrBlank()) {
            val info = EMOJIS[ultimoNivel]
            views.setTextViewText(R.id.tv_widget_humor_status, "Hoje: ${info?.first} ${info?.second}")
            views.setTextViewText(R.id.tv_widget_ultima_hora, "às $ultimoHorario")
        } else {
            views.setTextViewText(R.id.tv_widget_humor_status, "Como você está hoje?")
            views.setTextViewText(R.id.tv_widget_ultima_hora, "")
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun criarPendingIntentHumor(context: Context, nivel: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, WidgetCheckinHumorProvider::class.java).apply {
            action = ACTION_REGISTRAR_HUMOR
            putExtra(EXTRA_NIVEL_HUMOR, nivel)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
