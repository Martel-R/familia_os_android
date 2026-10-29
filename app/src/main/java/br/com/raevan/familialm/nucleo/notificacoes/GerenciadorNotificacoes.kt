package br.com.raevan.familialm.nucleo.notificacoes

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import br.com.raevan.familialm.MainActivity
import java.util.Calendar

object GerenciadorNotificacoes {

    const val CANAL_MEDICAMENTOS_ID = "familia_medicamentos_canal"
    const val CANAL_ROTINAS_ID = "familia_rotinas_canal"

    const val EXTRA_TIPO = "extra_tipo_notificacao"
    const val EXTRA_TITULO = "extra_titulo"
    const val EXTRA_MENSAGEM = "extra_mensagem"
    const val EXTRA_ID_NOTIFICACAO = "extra_id_notificacao"

    const val TIPO_MEDICAMENTO = "medicamento"
    const val TIPO_ROTINA = "rotina"

    fun inicializarCanais(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal 1: Medicamentos (Importância Máxima com Alerta Sonoro)
            val canalMedicamentos = NotificationChannel(
                CANAL_MEDICAMENTOS_ID,
                "Lembretes de Medicamentos 💊",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarmes de horários exatos para tomar remédios da família"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            // Canal 2: Rotinas e Gamificação (Importância Padrão)
            val canalRotinas = NotificationChannel(
                CANAL_ROTINAS_ID,
                "Rotinas & Hábitos da Casa ⭐",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lembretes de tarefas do dia e moedas a conquistar"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(canalMedicamentos)
            notificationManager.createNotificationChannel(canalRotinas)
        }
    }

    fun agendarAlarmeMedicamento(
        context: Context,
        idMedicamento: String,
        nomeRemedio: String,
        dosagem: String,
        horario: String, // formato "HH:mm" ou "08:00"
        membroNome: String
    ) {
        val partes = horario.trim().split(":")
        val hora = partes.getOrNull(0)?.toIntOrNull() ?: 8
        val minuto = partes.getOrNull(1)?.toIntOrNull() ?: 0

        val calendario = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Se o horário de hoje já passou, agenda para amanhã no mesmo horário
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        val intent = Intent(context, ReceptorAlarmeFamilia::class.java).apply {
            putExtra(EXTRA_TIPO, TIPO_MEDICAMENTO)
            putExtra(EXTRA_TITULO, "💊 Hora do Remédio: $nomeRemedio ($dosagem)")
            putExtra(EXTRA_MENSAGEM, "Lembrete para $membroNome tomar $nomeRemedio às $horario.")
            putExtra(EXTRA_ID_NOTIFICACAO, idMedicamento.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            idMedicamento.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendario.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendario.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Em caso de restrição do sistema operacional, agenda alarme padrão
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendario.timeInMillis,
                pendingIntent
            )
        }
    }

    fun agendarLembreteRotinasDiarias(context: Context, hora: Int = 8, minuto: Int = 30) {
        val calendario = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        val idUnico = 99991 // ID fixo para rotinas diárias
        val intent = Intent(context, ReceptorAlarmeFamilia::class.java).apply {
            putExtra(EXTRA_TIPO, TIPO_ROTINA)
            putExtra(EXTRA_TITULO, "⭐ Rotinas de Hoje Prontas!")
            putExtra(EXTRA_MENSAGEM, "Bom dia! Veja as tarefas da casa de hoje e junte moedas para a loja de privilégios.")
            putExtra(EXTRA_ID_NOTIFICACAO, idUnico)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            idUnico,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendario.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendario.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {}
    }

    fun dispararNotificacaoImediata(
        context: Context,
        titulo: String,
        mensagem: String,
        tipo: String = TIPO_ROTINA,
        idNotificacao: Int = System.currentTimeMillis().toInt()
    ) {
        val canalId = if (tipo == TIPO_MEDICAMENTO) CANAL_MEDICAMENTOS_ID else CANAL_ROTINAS_ID

        val intentApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntentApp = PendingIntent.getActivity(
            context,
            idNotificacao,
            intentApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val construtor = NotificationCompat.Builder(context, canalId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensagem))
            .setPriority(if (tipo == TIPO_MEDICAMENTO) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntentApp)

        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        )

        notificationManager?.notify(idNotificacao, construtor.build())
    }
}
