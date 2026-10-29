package br.com.raevan.familialm.nucleo.notificacoes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReceptorAlarmeFamilia : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tipo = intent.getStringExtra(GerenciadorNotificacoes.EXTRA_TIPO) ?: GerenciadorNotificacoes.TIPO_ROTINA
        val titulo = intent.getStringExtra(GerenciadorNotificacoes.EXTRA_TITULO) ?: "Lembrete Familia OS"
        val mensagem = intent.getStringExtra(GerenciadorNotificacoes.EXTRA_MENSAGEM) ?: "Você tem um novo compromisso da casa."
        val idNotificacao = intent.getIntExtra(GerenciadorNotificacoes.EXTRA_ID_NOTIFICACAO, System.currentTimeMillis().toInt())

        // Dispara a notificação com som e canal apropriado
        GerenciadorNotificacoes.dispararNotificacaoImediata(
            context = context,
            titulo = titulo,
            mensagem = mensagem,
            tipo = tipo,
            idNotificacao = idNotificacao
        )

        // Se for rotina diária, reagenda automaticamente para o dia seguinte no mesmo horário
        if (tipo == GerenciadorNotificacoes.TIPO_ROTINA && idNotificacao == 99991) {
            GerenciadorNotificacoes.agendarLembreteRotinasDiarias(context, 8, 30)
        }
    }
}
