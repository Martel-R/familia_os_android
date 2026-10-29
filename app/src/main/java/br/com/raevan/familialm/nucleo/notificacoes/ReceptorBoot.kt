package br.com.raevan.familialm.nucleo.notificacoes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReceptorBoot : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // Inicializa canais e reagenda alarmes das rotinas da casa
            GerenciadorNotificacoes.inicializarCanais(context)
            GerenciadorNotificacoes.agendarLembreteRotinasDiarias(context, 8, 30)
        }
    }
}
