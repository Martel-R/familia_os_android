package br.com.raevan.familialm.dominio.repositorios

import br.com.raevan.familialm.dominio.modelos.*
import kotlinx.coroutines.flow.Flow

interface RepositorioUsuarios {
    fun obterUsuarioLogado(): Flow<Usuario>
    fun listarMembrosFamilia(): Flow<List<Usuario>>
    fun listarTodosMembrosFamilia(incluirInativos: Boolean = true): Flow<List<Usuario>> = listarMembrosFamilia()
    fun obterFamiliaAtiva(): Flow<Familia> = kotlinx.coroutines.flow.flowOf(
        Familia(
            id = "00000000-0000-0000-0000-000000000001",
            nome = "Família Martel",
            codigoConvite = "MARTEL"
        )
    )
    suspend fun atualizarSaldoMoedas(usuarioId: String, deltaMoedas: Int)
    suspend fun atualizarNomeFamilia(familiaId: String, novoNome: String): Boolean = false
    suspend fun vincularPorCodigoConvite(usuarioId: String, codigoConvite: String): Familia? = null
    suspend fun criarMembroFamilia(
        nome: String,
        email: String,
        senha: String,
        perfil: TipoPerfil,
        avatarEmoji: String? = null,
        saldoMoedas: Int = 0
    ): Result<Usuario> = Result.failure(UnsupportedOperationException())
    suspend fun atualizarMembroFamilia(
        usuarioId: String,
        nome: String,
        perfil: TipoPerfil,
        avatarEmoji: String? = null,
        ativo: Boolean = true
    ): Boolean = false
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
    suspend fun adicionarEvento(nome: String, dataEvento: String, descricao: String?, orcamentoEstimado: Double) {}
    suspend fun adicionarConvidado(eventoId: String, nome: String, telefone: String?, restricoesAlimentares: String?) {}
}

interface RepositorioCofre {
    fun listarCredenciais(incluirMaster: Boolean): Flow<List<CredencialCofre>>
    fun obterRedeWifiFamiliar(): Flow<RedeWifiFamiliar>
    suspend fun adicionarCredencial(credencial: CredencialCofre)
    suspend fun excluirCredencial(credencialId: String) {}
    suspend fun salvarRedeWifiFamiliar(wifi: RedeWifiFamiliar) {}
}

interface RepositorioRotinas {
    fun listarTarefas(): Flow<List<TarefaRotina>>
    fun listarItensLoja(): Flow<List<ItemLojaPrivilegio>>
    suspend fun alternarConclusaoTarefa(tarefaId: String): Int // retorna moedas ganhas
    suspend fun resgatarItemLoja(usuarioId: String, itemId: String): Boolean
    suspend fun adicionarTarefa(
        titulo: String,
        responsavelId: String?,
        recorrencia: RecorrenciaTarefa,
        diasSemana: String?,
        diaMes: Int?,
        moedasRecompensa: Int,
        abertaParaTodos: Boolean
    ) {}
    suspend fun adicionarItemLoja(titulo: String, descricao: String, custoMoedas: Int, icone: String?) {}
}

interface RepositorioSaude {
    fun listarCheckinsHumor(): Flow<List<CheckinSaude>>
    fun listarFichasMedicas(): Flow<List<FichaMedica>>
    fun listarLembretesMedicamento(): Flow<List<MedicamentoLembrete>>
    fun listarVacinas(): Flow<List<VacinaRegistro>>
    fun listarConsultasExames(): Flow<List<ConsultaExameRegistro>>
    fun listarMetricasVitais(): Flow<List<MetricaVitalRegistro>>
    suspend fun registrarCheckin(checkin: CheckinSaude)
    suspend fun alternarMedicamentoTomado(medicamentoId: String)
    suspend fun adicionarVacina(vacina: VacinaRegistro)
    suspend fun adicionarConsultaExame(consulta: ConsultaExameRegistro)
    suspend fun atualizarConsultaExame(consulta: ConsultaExameRegistro)
    suspend fun alternarConsultaConcluida(id: String)
    suspend fun excluirConsultaExame(id: String)
    suspend fun adicionarMetricaVital(metrica: MetricaVitalRegistro)
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
    suspend fun adicionarConquista(titulo: String, descricao: String?, usuarioId: String?) {}
}

interface RepositorioAutomacao {
    fun listarDispositivos(): Flow<List<DispositivoAutomacao>>
    fun listarCameras(): Flow<List<CameraFrigate>>
    suspend fun alternarDispositivo(dispositivoId: String)
}

interface RepositorioCompras {
    fun listarRecibos(): Flow<List<ReciboCompra>>
    suspend fun simularOcrCupomFiscal(nomeSupermercado: String, valorTotal: Double)
    suspend fun extrairNotaComIa(bytesArquivo: ByteArray, mimeType: String, nomeArquivo: String): ReciboCompra
    suspend fun salvarReciboComItens(
        fornecedor: String,
        dataCompra: String,
        valorTotal: Double,
        itens: List<ItemCompraExtraido>,
        lancarEmFinancas: Boolean = true
    )
    suspend fun adicionarItemAoRecibo(
        reciboId: String,
        nomeProduto: String,
        quantidade: Double,
        valorUnitario: Double,
        categoriaSugerida: String
    )
    suspend fun excluirRecibo(reciboId: String)
    suspend fun excluirItemRecibo(itemId: String)
}
