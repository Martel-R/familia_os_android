package br.com.raevan.familialm.dados.repositorios

import br.com.raevan.familialm.dominio.modelos.*
import br.com.raevan.familialm.dominio.repositorios.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.UUID

class RepositorioUsuariosMemoria : RepositorioUsuarios {
    private val estadoUsuarioLogado = MutableStateFlow(
        Usuario(
            id = "usr-pai-01",
            nome = "Lucas Monteiro",
            perfil = TipoPerfil.PAI_ADMIN,
            saldoMoedas = 120,
            avatarUrl = null,
            criadoEm = "2026-01-10"
        )
    )

    private val estadoMembros = MutableStateFlow(
        listOf(
            Usuario("usr-pai-01", "Lucas Monteiro", TipoPerfil.PAI_ADMIN, 120),
            Usuario("usr-mae-02", "Renata Monteiro", TipoPerfil.PAI_ADMIN, 95),
            Usuario("usr-filho-03", "Theo Monteiro", TipoPerfil.FILHO_MEMBRO, 45),
            Usuario("usr-filha-04", "Maya Monteiro", TipoPerfil.FILHO_MEMBRO, 30),
            Usuario("usr-apoio-05", "Dona Cida", TipoPerfil.CUIDADOR, 0)
        )
    )

    override fun obterUsuarioLogado(): Flow<Usuario> = estadoUsuarioLogado.asStateFlow()

    override fun listarMembrosFamilia(): Flow<List<Usuario>> = estadoMembros.asStateFlow()

    override suspend fun atualizarSaldoMoedas(usuarioId: String, deltaMoedas: Int) = withContext(Dispatchers.IO) {
        estadoUsuarioLogado.update { if (it.id == usuarioId) it.copy(saldoMoedas = it.saldoMoedas + deltaMoedas) else it }
        estadoMembros.update { lista ->
            lista.map { if (it.id == usuarioId) it.copy(saldoMoedas = it.saldoMoedas + deltaMoedas) else it }
        }
    }
}

class RepositorioFinancasMemoria : RepositorioFinancas {
    private val estadoTransacoes = MutableStateFlow(
        listOf(
            TransacaoFinanceira("tx-01", "usr-pai-01", "cat-01", "Trabalho", TipoTransacao.RECEITA, "Salário Mensal - Lucas", 14500.00, "2026-11-05", "2026-11-05", StatusPagamento.PAGO, null),
            TransacaoFinanceira("tx-02", "usr-mae-02", "cat-01", "Consultoria", TipoTransacao.RECEITA, "Honorários Consultoria - Renata", 6200.00, "2026-11-10", null, StatusPagamento.PENDENTE, null),
            TransacaoFinanceira("tx-03", null, "cat-02", "Habitação", TipoTransacao.DESPESA_FIXA, "Financiamento Imobiliário", 3850.00, "2026-11-12", null, StatusPagamento.PENDENTE, "Débito em conta"),
            TransacaoFinanceira("tx-04", null, "cat-03", "Educação", TipoTransacao.DESPESA_FIXA, "Escola das Crianças (Theo e Maya)", 2900.00, "2026-11-10", "2026-11-08", StatusPagamento.PAGO, null),
            TransacaoFinanceira("tx-05", null, "cat-04", "Assinaturas", TipoTransacao.ASSINATURA, "Netflix + Spotify Familiar + iCloud", 139.90, "2026-11-15", null, StatusPagamento.PENDENTE, null),
            TransacaoFinanceira("tx-06", null, "cat-05", "Serviços", TipoTransacao.DESPESA_FIXA, "Energia Elétrica (Equatorial)", 465.30, "2026-11-09", null, StatusPagamento.PENDENTE, "Vence amanhã"),
            TransacaoFinanceira("tx-07", null, "cat-05", "Serviços", TipoTransacao.DESPESA_FIXA, "Internet Fibra 1Gb", 169.90, "2026-11-07", "2026-11-06", StatusPagamento.PAGO, null)
        )
    )

    private val estadoCofrinhos = MutableStateFlow(
        listOf(
            Cofrinho("cof-01", "Viagem Férias de Julho", 15000.00, 9850.00, "2026-07-01", "airplane"),
            Cofrinho("cof-02", "Reserva de Emergência da Casa", 25000.00, 21400.00, null, "shield"),
            Cofrinho("cof-03", "Pintura Externa e Jardim", 4500.00, 1800.00, "2026-12-15", "home")
        )
    )

    override fun obterResumoFinanceiro(): Flow<ResumoFinanceiro> = estadoTransacoes.map { transacoes ->
        val totalReceitas = transacoes.filter { it.tipo == TipoTransacao.RECEITA }.sumOf { it.valor }
        val despesasPagas = transacoes.filter { it.tipo != TipoTransacao.RECEITA && it.status == StatusPagamento.PAGO }.sumOf { it.valor }
        val despesasPendentes = transacoes.filter { it.tipo != TipoTransacao.RECEITA && it.status != StatusPagamento.PAGO }.sumOf { it.valor }
        val totalCofrinhos = estadoCofrinhos.value.sumOf { it.saldoAtual }
        val saldoLivre = totalReceitas - despesasPagas - despesasPendentes

        ResumoFinanceiro(
            totalReceitas = totalReceitas,
            totalDespesasPagas = despesasPagas,
            totalDespesasPendentes = despesasPendentes,
            saldoLivreAlocacao = saldoLivre,
            totalCofrinhos = totalCofrinhos
        )
    }

    override fun listarTransacoes(): Flow<List<TransacaoFinanceira>> = estadoTransacoes.asStateFlow()

    override fun listarCofrinhos(): Flow<List<Cofrinho>> = estadoCofrinhos.asStateFlow()

    override suspend fun alternarStatusPagamento(transacaoId: String) = withContext(Dispatchers.IO) {
        estadoTransacoes.update { lista ->
            lista.map { tx ->
                if (tx.id == transacaoId) {
                    val novoStatus = if (tx.status == StatusPagamento.PAGO) StatusPagamento.PENDENTE else StatusPagamento.PAGO
                    val novaData = if (novoStatus == StatusPagamento.PAGO) "2026-10-25" else null
                    tx.copy(status = novoStatus, dataPagamento = novaData)
                } else tx
            }
        }
    }

    override suspend fun adicionarTransacao(transacao: TransacaoFinanceira) = withContext(Dispatchers.IO) {
        estadoTransacoes.update { listOf(transacao) + it }
    }

    override suspend fun depositarCofrinho(cofrinhoId: String, valor: Double) = withContext(Dispatchers.IO) {
        estadoCofrinhos.update { lista ->
            lista.map { cof ->
                if (cof.id == cofrinhoId) cof.copy(saldoAtual = cof.saldoAtual + valor) else cof
            }
        }
    }
}

class RepositorioEventosMemoria : RepositorioEventos {
    private val estadoEventos = MutableStateFlow(
        listOf(
            Evento(
                id = "evt-01",
                nome = "Aniversário de 8 Anos do Theo",
                dataEvento = "15 de Novembro de 2026 às 16:00",
                descricao = "Festa temática de Minecraft no salão do condomínio. Haverá oficina de slime.",
                orcamentoEstimado = 3500.00,
                totalGastoReal = 1890.00
            ),
            Evento(
                id = "evt-02",
                nome = "Ceia de Natal da Família Monteiro",
                dataEvento = "24 de Dezembro de 2026 às 20:00",
                descricao = "Ceia compartilhada na nossa casa. Cada núcleo traz um prato e bebida.",
                orcamentoEstimado = 1800.00,
                totalGastoReal = 350.00
            )
        )
    )

    private val estadoConvidados = MutableStateFlow(
        listOf(
            Convidado("cnv-01", "evt-01", "Tio Carlos e Família", "(91) 98111-2233", StatusRsvp.CONFIRMADO, null),
            Convidado("cnv-02", "evt-01", "Vovó Marlene", "(91) 98444-5566", StatusRsvp.CONFIRMADO, "Sem lactose"),
            Convidado("cnv-03", "evt-01", "Colega Arthur (Escola)", "(91) 98888-9900", StatusRsvp.PENDENTE, "Alergia a amendoim"),
            Convidado("cnv-04", "evt-01", "Colega Sofia (Natação)", "(91) 99111-7788", StatusRsvp.CONFIRMADO, null)
        )
    )

    private val estadoCotacoes = MutableStateFlow(
        listOf(
            Cotacao("cot-01", "evt-01", "Bolo Personalizado e Doces", "Confeitaria Doce Arte", 480.00, StatusCotacao.APROVADA, null, "Sabor ninho com morango"),
            Cotacao("cot-02", "evt-01", "Decoração Temática Completa", "Festas & Magia", 1200.00, StatusCotacao.APROVADA, null, "Balões e painel"),
            Cotacao("cot-03", "evt-01", "Recreador Infantil (3 Horas)", "Tio Pipoca Entretenimento", 450.00, StatusCotacao.EM_ANALISE, null, "Inclui oficina de slime")
        )
    )

    override fun listarEventos(): Flow<List<Evento>> = estadoEventos.asStateFlow()

    override fun listarConvidados(eventoId: String): Flow<List<Convidado>> = estadoConvidados.map { lista ->
        lista.filter { it.eventoId == eventoId }
    }

    override fun listarCotacoes(eventoId: String): Flow<List<Cotacao>> = estadoCotacoes.map { lista ->
        lista.filter { it.eventoId == eventoId }
    }

    override suspend fun atualizarStatusRsvp(convidadoId: String, novoStatus: StatusRsvp) = withContext(Dispatchers.IO) {
        estadoConvidados.update { lista ->
            lista.map { if (it.id == convidadoId) it.copy(status = novoStatus) else it }
        }
    }

    override suspend fun adicionarCotacao(cotacao: Cotacao) = withContext(Dispatchers.IO) {
        estadoCotacoes.update { listOf(cotacao) + it }
    }
}

class RepositorioCofreMemoria : RepositorioCofre {
    private val estadoCredenciais = MutableStateFlow(
        listOf(
            CredencialCofre("crd-01", "Banco Principal (Conta Conjunta)", NivelCofre.MASTER, "lucas.monteiro", "••••••••••••", "https://banco.exemplo.com", "Chave Pix e recuperação"),
            CredencialCofre("crd-02", "E-mail Administrativo da Casa", NivelCofre.MASTER, "casamonteiro@gmail.com", "••••••••••••", "https://accounts.google.com", "Senhas de roteadores e IoT"),
            CredencialCofre("crd-03", "Netflix Familiar", NivelCofre.COMPARTILHADO, "familia.monteiro@stream.com", "CinemaEmCasa2026!", "https://netflix.com", "Perfil Crianças sem pin"),
            CredencialCofre("crd-04", "Disney+ e Max", NivelCofre.COMPARTILHADO, "familia.monteiro@stream.com", "MagiaMonteiro#88", "https://disneyplus.com", null),
            CredencialCofre("crd-05", "Robô Aspirador e Câmeras", NivelCofre.COMPARTILHADO, "homehub@casa.local", "HubRoboSeguro2026", null, "Acesso no app local")
        )
    )

    private val estadoRedeWifi = MutableStateFlow(
        RedeWifiFamiliar(
            nomeRedeSsid = "Monteiro_5G_Visitantes",
            senhaRede = "BemVindoCasa2026!",
            tipoSeguranca = "WPA2/WPA3",
            ehRedeVisitantes = true
        )
    )

    override fun listarCredenciais(incluirMaster: Boolean): Flow<List<CredencialCofre>> = estadoCredenciais.map { lista ->
        if (incluirMaster) lista else lista.filter { it.nivel == NivelCofre.COMPARTILHADO }
    }

    override fun obterRedeWifiFamiliar(): Flow<RedeWifiFamiliar> = estadoRedeWifi.asStateFlow()

    override suspend fun adicionarCredencial(credencial: CredencialCofre) = withContext(Dispatchers.IO) {
        estadoCredenciais.update { listOf(credencial) + it }
    }
}

class RepositorioRotinasMemoria(
    private val repositorioUsuarios: RepositorioUsuarios
) : RepositorioRotinas {
    private val estadoTarefas = MutableStateFlow(
        listOf(
            TarefaRotina("trf-01", "Arrumar a cama ao acordar", "usr-filho-03", "Theo", RecorrenciaTarefa.DIARIA, 5, "09:00", true),
            TarefaRotina("trf-02", "Guardar os brinquedos na caixa", "usr-filho-03", "Theo", RecorrenciaTarefa.DIARIA, 5, "18:00", false),
            TarefaRotina("trf-03", "Fazer o dever de casa de Matemática", "usr-filho-03", "Theo", RecorrenciaTarefa.DIARIA, 10, "16:00", false),
            TarefaRotina("trf-04", "Alimentar os peixes e trocar a água", "usr-filha-04", "Maya", RecorrenciaTarefa.DIARIA, 5, "08:30", true),
            TarefaRotina("trf-05", "Organizar a mochila da escola", "usr-filha-04", "Maya", RecorrenciaTarefa.DIARIA, 5, "20:00", false),
            TarefaRotina("trf-06", "Colocar o lixo reciclável para fora", "usr-pai-01", "Lucas", RecorrenciaTarefa.SEMANAL, 10, "21:00", false)
        )
    )

    private val estadoLoja = MutableStateFlow(
        listOf(
            ItemLojaPrivilegio("loj-01", "1 Hora Extra de Videogame", 25, "sports_esports", null, "Pode ser usado no sábado ou domingo"),
            ItemLojaPrivilegio("loj-02", "Escolher o Sabor da Pizza de Sexta", 30, "local_pizza", null, "Direito a escolher meia pizza"),
            ItemLojaPrivilegio("loj-03", "Noite do Cinema com Pipoca e Doce", 40, "movie", null, "Escolhe o filme em família"),
            ItemLojaPrivilegio("loj-04", "Passeio no Parque no Final de Semana", 50, "park", null, "Andar de patinete com os pais")
        )
    )

    override fun listarTarefas(): Flow<List<TarefaRotina>> = estadoTarefas.asStateFlow()

    override fun listarItensLoja(): Flow<List<ItemLojaPrivilegio>> = estadoLoja.asStateFlow()

    override suspend fun alternarConclusaoTarefa(tarefaId: String): Int = withContext(Dispatchers.IO) {
        var moedasGanhas = 0
        estadoTarefas.update { lista ->
            lista.map { tarefa ->
                if (tarefa.id == tarefaId) {
                    val novaConclusao = !tarefa.estaConcluida
                    if (novaConclusao) {
                        moedasGanhas = tarefa.moedasRecompensa
                        tarefa.responsavelId?.let { respId ->
                            repositorioUsuarios.atualizarSaldoMoedas(respId, tarefa.moedasRecompensa)
                        }
                    } else {
                        moedasGanhas = -tarefa.moedasRecompensa
                        tarefa.responsavelId?.let { respId ->
                            repositorioUsuarios.atualizarSaldoMoedas(respId, -tarefa.moedasRecompensa)
                        }
                    }
                    tarefa.copy(estaConcluida = novaConclusao)
                } else tarefa
            }
        }
        moedasGanhas
    }

    override suspend fun resgatarItemLoja(usuarioId: String, itemId: String): Boolean = withContext(Dispatchers.IO) {
        val item = estadoLoja.value.find { it.id == itemId } ?: return@withContext false
        repositorioUsuarios.atualizarSaldoMoedas(usuarioId, -item.custoMoedas)
        true
    }
}

class RepositorioSaudeMemoria : RepositorioSaude {
    private val estadoCheckins = MutableStateFlow(
        listOf(
            CheckinSaude("chk-01", "usr-pai-01", "Lucas", 4, "Dia produtivo no trabalho, caminhada leve no final da tarde.", "Hoje às 18:30"),
            CheckinSaude("chk-02", "usr-mae-02", "Renata", 5, "Família reunida e projetos da casa em ordem!", "Hoje às 19:15"),
            CheckinSaude("chk-03", "usr-filho-03", "Theo", 5, "Tirou 10 no teste de ciências!", "Hoje às 17:00")
        )
    )

    private val estadoFichas = MutableStateFlow(
        listOf(
            FichaMedica(
                id = "fch-01",
                usuarioId = "usr-filho-03",
                nomeUsuario = "Theo Monteiro (8 anos)",
                tipoSanguineo = "O+",
                alergias = "Amendoim e Dipirona",
                medicamentosUsoContinuo = "Aerolin em caso de crise asmática",
                numeroPlanoSaude = "Unimed Nacional: 0045.8921.332-9",
                contatoEmergencia = "Dr. Marcelo (Pediatra) - (91) 98765-4321"
            ),
            FichaMedica(
                id = "fch-02",
                usuarioId = "usr-filha-04",
                nomeUsuario = "Maya Monteiro (5 anos)",
                tipoSanguineo = "A+",
                alergias = "Nenhuma alergia conhecida",
                medicamentosUsoContinuo = "Suplementação Vitamina D (gotas)",
                numeroPlanoSaude = "Unimed Nacional: 0045.8921.333-7",
                contatoEmergencia = "Dra. Patrícia (Pediatra) - (91) 98112-9900"
            ),
            FichaMedica(
                id = "fch-03",
                usuarioId = "usr-pai-01",
                nomeUsuario = "Lucas Monteiro",
                tipoSanguineo = "O+",
                alergias = "Frutos do mar leves",
                medicamentosUsoContinuo = "Nenhum",
                numeroPlanoSaude = "Unimed Nacional: 0045.8921.330-2",
                contatoEmergencia = "Renata (Esposa) - (91) 98222-3344"
            )
        )
    )

    private val estadoMedicamentos = MutableStateFlow(
        listOf(
            MedicamentoLembrete("med-01", "Vitamina D 1000UI", "2 gotas pela manhã", "08:00", "Maya", true),
            MedicamentoLembrete("med-02", "Antialérgico Loratadina", "5ml à noite (3 dias)", "21:00", "Theo", false),
            MedicamentoLembrete("med-03", "Complexo Vitamínico B", "1 comprimido com água", "12:30", "Lucas", true)
        )
    )

    override fun listarCheckinsHumor(): Flow<List<CheckinSaude>> = estadoCheckins.asStateFlow()

    override fun listarFichasMedicas(): Flow<List<FichaMedica>> = estadoFichas.asStateFlow()

    override fun listarLembretesMedicamento(): Flow<List<MedicamentoLembrete>> = estadoMedicamentos.asStateFlow()

    override suspend fun registrarCheckin(checkin: CheckinSaude) = withContext(Dispatchers.IO) {
        estadoCheckins.update { listOf(checkin) + it }
    }

    override suspend fun alternarMedicamentoTomado(medicamentoId: String) = withContext(Dispatchers.IO) {
        estadoMedicamentos.update { lista ->
            lista.map { if (it.id == medicamentoId) it.copy(foiTomadoHoje = !it.foiTomadoHoje) else it }
        }
    }
}

class RepositorioInventarioMemoria : RepositorioInventario {
    private val estadoItens = MutableStateFlow(
        listOf(
            ItemInventario("inv-01", "Furadeira de Impacto Bosch 750W", "Oficina / Armário Superior Caixa 2", 1, "2024-03-15", "2025-03-15", null, null),
            ItemInventario("inv-02", "Árvore de Natal e Enfeites Dourados", "Sótão / Caixa Organizadora Grande 1", 1, "2023-11-20", null, null, null),
            ItemInventario("inv-03", "Lâmpadas LED Inteligentes (Reserva)", "Lavanderia / Gaveta de Utilitários", 4, "2026-02-10", "2027-02-10", null, null),
            ItemInventario("inv-04", "Manual e Controle Reserva TV Sala", "Sala de Estar / Gaveta do Rack", 1, "2025-08-14", "2026-08-14", null, null),
            ItemInventario("inv-05", "Maleta de Primeiros Socorros Completa", "Armário do Banheiro Social", 1, "2026-01-05", null, null, null)
        )
    )

    private val estadoContatos = MutableStateFlow(
        listOf(
            ContatoAgenda("cnt-01", "Sr. Valdir Eletricista", "Instalações e Quadro de Luz", "(91) 98455-1122", true, "Atende emergências no condomínio"),
            ContatoAgenda("cnt-02", "Marcos Encanador", "Caça Vazamentos e Válvulas", "(91) 99122-3344", true, "Já consertou a tubulação da cozinha"),
            ContatoAgenda("cnt-03", "Dr. Marcelo Pediatra", "Pediatria e Emergência Theo", "(91) 98765-4321", true, "Pediatra de confiança"),
            ContatoAgenda("cnt-04", "ClimaFrio Refrigeração", "Manutenção de Ar-Condicionado", "(91) 98877-6655", false, "Faz a higienização semestral"),
            ContatoAgenda("cnt-05", "Portaria do Condomínio", "Segurança e Correspondências", "(91) 3211-0000", true, "Ramal 101")
        )
    )

    private val estadoManutencoes = MutableStateFlow(
        listOf(
            HistoricoManutencao(
                id = "mnt-01",
                itemId = "inv-04",
                nomeItem = "Ar-Condicionado Suíte Master",
                prestadorId = "cnt-04",
                nomePrestador = "ClimaFrio Refrigeração",
                descricao = "Higienização completa e troca do filtro",
                dataServico = "2026-08-20",
                custo = 180.00,
                dataProximaManutencao = "2027-02-20"
            ),
            HistoricoManutencao(
                id = "mnt-02",
                itemId = null,
                nomeItem = "Filtro Central de Água",
                prestadorId = "cnt-02",
                nomePrestador = "Marcos Encanador",
                descricao = "Troca do refil de carvão ativado",
                dataServico = "2026-09-10",
                custo = 120.00,
                dataProximaManutencao = "2027-03-10"
            )
        )
    )

    override fun listarItensInventario(): Flow<List<ItemInventario>> = estadoItens.asStateFlow()

    override fun buscarItens(termo: String): Flow<List<ItemInventario>> = estadoItens.map { lista ->
        if (termo.isBlank()) lista
        else lista.filter { it.nome.contains(termo, ignoreCase = true) || it.localizacao.contains(termo, ignoreCase = true) }
    }

    override fun listarContatos(): Flow<List<ContatoAgenda>> = estadoContatos.asStateFlow()

    override fun listarHistoricoManutencoes(): Flow<List<HistoricoManutencao>> = estadoManutencoes.asStateFlow()
}

class RepositorioMuralMemoria : RepositorioMural {
    private val estadoConquistas = MutableStateFlow(
        listOf(
            ConquistaMural("cq-01", "🏆 Theo conquistou a Faixa Amarela no Judô!", "Após meses de treino e dedicação, passou no exame da academia com louvor.", "usr-filho-03", "Theo", "18 de Outubro de 2026", null, 8),
            ConquistaMural("cq-02", "🚗 Quitação definitiva do Financiamento do Carro!", "Objetivo financeiro de 3 anos alcançado pela família Monteiro. Um passo rumo à liberdade financeira!", "usr-pai-01", "Família Monteiro", "05 de Outubro de 2026", null, 14),
            ConquistaMural("cq-03", "🎨 Maya teve seu desenho exposto na Feira de Artes!", "Quadro das borboletas coloridas escolhido pela professora para a entrada da escola.", "usr-filha-04", "Maya", "28 de Setembro de 2026", null, 11)
        )
    )

    private val estadoFotos = MutableStateFlow(
        listOf(
            FotoAlbum("ft-01", "Piquenique no Parque", "Lucas", "22/10/2026", false, ""),
            FotoAlbum("ft-02", "Passeio de Bicicleta", "Renata", "19/10/2026", false, ""),
            FotoAlbum("ft-03", "Domingo de Pizza", "Lucas", "12/10/2026", false, "")
        )
    )

    override fun listarConquistas(): Flow<List<ConquistaMural>> = estadoConquistas.asStateFlow()

    override fun listarFotosAlbum(): Flow<List<FotoAlbum>> = estadoFotos.asStateFlow()

    override suspend fun adicionarReacao(conquistaId: String) = withContext(Dispatchers.IO) {
        estadoConquistas.update { lista ->
            lista.map { if (it.id == conquistaId) it.copy(totalReacoes = it.totalReacoes + 1) else it }
        }
    }
}

class RepositorioAutomacaoMemoria : RepositorioAutomacao {
    private val estadoDispositivos = MutableStateFlow(
        listOf(
            DispositivoAutomacao("aut-01", "Luz Principal da Sala", "Sala de Estar", TipoDispositivo.LUZ, true, "100% Brilho"),
            DispositivoAutomacao("aut-02", "Lustre da Mesa de Jantar", "Sala de Jantar", TipoDispositivo.LUZ, false, "Desligado"),
            DispositivoAutomacao("aut-03", "Ar-Condicionado Sala", "Sala de Estar", TipoDispositivo.CLIMATIZACAO, true, "23°C Refrigeração"),
            DispositivoAutomacao("aut-04", "Ar-Condicionado Quarto Theo", "Quarto Crianças", TipoDispositivo.CLIMATIZACAO, false, "24°C Desligado"),
            DispositivoAutomacao("aut-05", "Portão Eletrônico Principal", "Garagem", TipoDispositivo.PORTAO, false, "Fechado & Travado"),
            DispositivoAutomacao("aut-06", "Sensor de Presença Quintal", "Área Externa", TipoDispositivo.SENSOR, true, "Sem movimento detectado")
        )
    )

    private val estadoCameras = MutableStateFlow(
        listOf(
            CameraFrigate("cam-01", "Câmera Portão da Frente", "Entrada / Rua", true, "Pessoa detectada há 12 min"),
            CameraFrigate("cam-02", "Câmera Garagem Principal", "Garagem", true, "Veículo estacionado"),
            CameraFrigate("cam-03", "Câmera Quintal e Piscina", "Área de Lazer", true, "Movimento normal"),
            CameraFrigate("cam-04", "Câmera Hall dos Quartos", "Área Interna", true, "Monitorando")
        )
    )

    override fun listarDispositivos(): Flow<List<DispositivoAutomacao>> = estadoDispositivos.asStateFlow()

    override fun listarCameras(): Flow<List<CameraFrigate>> = estadoCameras.asStateFlow()

    override suspend fun alternarDispositivo(dispositivoId: String) = withContext(Dispatchers.IO) {
        estadoDispositivos.update { lista ->
            lista.map { if (it.id == dispositivoId) it.copy(estaLigado = !it.estaLigado) else it }
        }
    }
}

class RepositorioComprasMemoria : RepositorioCompras {
    private val estadoRecibos = MutableStateFlow(
        listOf(
            ReciboCompra(
                id = "rcb-01",
                fornecedor = "Supermercado Líder Doca",
                dataCompra = "24 de Outubro de 2026",
                valorTotal = 485.60,
                caminhoImagemRecibo = null,
                itens = listOf(
                    ItemCompraExtraido("item-01", "rcb-01", "Leite Integral Piracanjuba 1L (x12)", 12.0, 5.49, "Laticínios"),
                    ItemCompraExtraido("item-02", "rcb-01", "Café em Grãos Especial 500g", 2.0, 24.90, "Matinais"),
                    ItemCompraExtraido("item-03", "rcb-01", "Detergente Neutro Ypê (x6)", 6.0, 2.79, "Limpeza"),
                    ItemCompraExtraido("item-04", "rcb-01", "Filé de Salmão Fresco Kg", 1.8, 89.90, "Açougue / Peixaria")
                )
            ),
            ReciboCompra(
                id = "rcb-02",
                fornecedor = "Drogasil Farmácias",
                dataCompra = "21 de Outubro de 2026",
                valorTotal = 142.30,
                caminhoImagemRecibo = null,
                itens = listOf(
                    ItemCompraExtraido("item-05", "rcb-02", "Protetor Solar FPS 50 Infantil", 1.0, 78.50, "Higiene / Cuidado"),
                    ItemCompraExtraido("item-06", "rcb-02", "Curativos Adesivos Band-Aid (40un)", 2.0, 16.90, "Primeiros Socorros"),
                    ItemCompraExtraido("item-07", "rcb-02", "Lenços Umedecidos Huggies", 2.0, 15.00, "Bebê / Crianças")
                )
            )
        )
    )

    override fun listarRecibos(): Flow<List<ReciboCompra>> = estadoRecibos.asStateFlow()

    override suspend fun simularOcrCupomFiscal(nomeSupermercado: String, valorTotal: Double) = withContext(Dispatchers.IO) {
        val novoRecibo = ReciboCompra(
            id = UUID.randomUUID().toString(),
            fornecedor = nomeSupermercado,
            dataCompra = "Hoje",
            valorTotal = valorTotal,
            itens = listOf(
                ItemCompraExtraido(UUID.randomUUID().toString(), "temp", "Itens diversos da compra", 1.0, valorTotal, "Supermercado")
            )
        )
        estadoRecibos.update { listOf(novoRecibo) + it }
    }
}
