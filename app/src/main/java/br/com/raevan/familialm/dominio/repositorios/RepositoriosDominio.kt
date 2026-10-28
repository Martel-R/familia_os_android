package br.com.raevan.familialm.dominio.repositorios

import br.com.raevan.familialm.dominio.modelos.*
import kotlinx.coroutines.flow.Flow

interface RepositorioUsuarios {
    fun obterUsuarioLogado(): Flow<Usuario>
    fun listarMembrosFamilia(): Flow<List<Usuario>>
    suspend fun atualizarSaldoMoedas(usuarioId: String, deltaMoedas: Int)
}

interface RepositorioFinancas {
    fun obterResumoFinanceiro(): Flow<ResumoFinanceiro>
    fun listarTransacoes(): Flow<List<TransacaoFinanceira>>
    fun listarCofrinhos(): Flow<List<Cofrinho>>
    suspend fun alternarStatusPagamento(transacaoId: String)
    suspend fun adicionarTransacao(transacao: TransacaoFinanceira)
    suspend fun depositarCofrinho(cofrinhoId: String, valor: Double)
}

interface RepositorioEventos {
    fun listarEventos(): Flow<List<Evento>>
    fun listarConvidados(eventoId: String): Flow<List<Convidado>>
    fun listarCotacoes(eventoId: String): Flow<List<Cotacao>>
    suspend fun atualizarStatusRsvp(convidadoId: String, novoStatus: StatusRsvp)
    suspend fun adicionarCotacao(cotacao: Cotacao)
}

interface RepositorioCofre {
    fun listarCredenciais(incluirMaster: Boolean): Flow<List<CredencialCofre>>
    fun obterRedeWifiFamiliar(): Flow<RedeWifiFamiliar>
    suspend fun adicionarCredencial(credencial: CredencialCofre)
}

interface RepositorioRotinas {
    fun listarTarefas(): Flow<List<TarefaRotina>>
    fun listarItensLoja(): Flow<List<ItemLojaPrivilegio>>
    suspend fun alternarConclusaoTarefa(tarefaId: String): Int // retorna moedas ganhas
    suspend fun resgatarItemLoja(usuarioId: String, itemId: String): Boolean
}

interface RepositorioSaude {
    fun listarCheckinsHumor(): Flow<List<CheckinSaude>>
    fun listarFichasMedicas(): Flow<List<FichaMedica>>
    fun listarLembretesMedicamento(): Flow<List<MedicamentoLembrete>>
    suspend fun registrarCheckin(checkin: CheckinSaude)
    suspend fun alternarMedicamentoTomado(medicamentoId: String)
}

interface RepositorioInventario {
    fun listarItensInventario(): Flow<List<ItemInventario>>
    fun buscarItens(termo: String): Flow<List<ItemInventario>>
    fun listarContatos(): Flow<List<ContatoAgenda>>
    fun listarHistoricoManutencoes(): Flow<List<HistoricoManutencao>>
    suspend fun sincronizarContatos(contatos: List<ContatoAgenda>): Int
}

interface RepositorioMural {
    fun listarConquistas(): Flow<List<ConquistaMural>>
    fun listarFotosAlbum(): Flow<List<FotoAlbum>>
    suspend fun adicionarReacao(conquistaId: String)
}

interface RepositorioAutomacao {
    fun listarDispositivos(): Flow<List<DispositivoAutomacao>>
    fun listarCameras(): Flow<List<CameraFrigate>>
    suspend fun alternarDispositivo(dispositivoId: String)
}

interface RepositorioCompras {
    fun listarRecibos(): Flow<List<ReciboCompra>>
    suspend fun simularOcrCupomFiscal(nomeSupermercado: String, valorTotal: Double)
}
