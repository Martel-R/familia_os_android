package br.com.raevan.familialm.dados.repositorios

import br.com.raevan.familialm.dados.rede.ClienteSupabase
import br.com.raevan.familialm.dados.remoto.dto.*
import br.com.raevan.familialm.dominio.modelos.*
import br.com.raevan.familialm.dominio.repositorios.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

class RepositorioUsuariosSupabase : RepositorioUsuarios {
    override fun obterUsuarioLogado(): Flow<Usuario> = flow {
        val dtos = ClienteSupabase.client.from("usuarios")
            .select {
                filter { eq("ativo", true) }
                limit(1)
            }
            .decodeList<UsuarioDto>()

        val primeiro = dtos.firstOrNull() ?: UsuarioDto(
            id = "usr-padrao",
            nome = "Usuário",
            perfil = "pai_admin",
            saldoMoedas = 0
        )

        emit(primeiro.paraDominio())
    }.flowOn(Dispatchers.IO)

    override fun listarMembrosFamilia(): Flow<List<Usuario>> = flow {
        val dtos = ClienteSupabase.client.from("usuarios")
            .select {
                filter { eq("ativo", true) }
                order("nome", Order.ASCENDING)
            }
            .decodeList<UsuarioDto>()

        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override suspend fun atualizarSaldoMoedas(usuarioId: String, deltaMoedas: Int) = withContext(Dispatchers.IO) {
        val atual = ClienteSupabase.client.from("usuarios")
            .select {
                filter { eq("id", usuarioId) }
            }
            .decodeSingleOrNull<UsuarioDto>()

        if (atual != null) {
            val novoSaldo = maxOf(0, atual.saldoMoedas + deltaMoedas)
            ClienteSupabase.client.from("usuarios")
                .update({ set("saldo_moedas", novoSaldo) }) {
                    filter { eq("id", usuarioId) }
                }
        }
    }

    private fun UsuarioDto.paraDominio() = Usuario(
        id = id,
        nome = nome,
        perfil = when (perfil) {
            "pai_admin" -> TipoPerfil.PAI_ADMIN
            "filho_membro" -> TipoPerfil.FILHO_MEMBRO
            else -> TipoPerfil.CUIDADOR
        },
        saldoMoedas = saldoMoedas,
        avatarUrl = avatarUrl,
        criadoEm = criadoEm
    )
}

class RepositorioFinancasSupabase : RepositorioFinancas {
    override fun obterResumoFinanceiro(): Flow<ResumoFinanceiro> = flow {
        val transacoes = ClienteSupabase.client.from("transacoes_financeiras")
            .select()
            .decodeList<TransacaoDto>()

        val cofrinhos = ClienteSupabase.client.from("cofrinhos")
            .select()
            .decodeList<CofrinhoDto>()

        val totalReceitas = transacoes.filter { it.tipo == "receita" }.sumOf { it.valor }
        val despesasPagas = transacoes.filter { it.tipo != "receita" && it.status == "pago" }.sumOf { it.valor }
        val despesasPendentes = transacoes.filter { it.tipo != "receita" && it.status != "pago" }.sumOf { it.valor }
        val totalCofrinhos = cofrinhos.sumOf { it.saldoAtual }
        val saldoLivre = totalReceitas - despesasPagas - despesasPendentes

        emit(
            ResumoFinanceiro(
                totalReceitas = totalReceitas,
                totalDespesasPagas = despesasPagas,
                totalDespesasPendentes = despesasPendentes,
                saldoLivreAlocacao = saldoLivre,
                totalCofrinhos = totalCofrinhos
            )
        )
    }.flowOn(Dispatchers.IO)

    override fun listarTransacoes(): Flow<List<TransacaoFinanceira>> = flow {
        val dtos = ClienteSupabase.client.from("transacoes_financeiras")
            .select {
                order("data_vencimento", Order.DESCENDING)
            }
            .decodeList<TransacaoDto>()

        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun listarCofrinhos(): Flow<List<Cofrinho>> = flow {
        val dtos = ClienteSupabase.client.from("cofrinhos")
            .select {
                order("nome", Order.ASCENDING)
            }
            .decodeList<CofrinhoDto>()

        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override suspend fun alternarStatusPagamento(transacaoId: String) = withContext(Dispatchers.IO) {
        val atual = ClienteSupabase.client.from("transacoes_financeiras")
            .select { filter { eq("id", transacaoId) } }
            .decodeSingleOrNull<TransacaoDto>()

        if (atual != null) {
            val novoStatus = if (atual.status == "pago") "pendente" else "pago"
            val novaData = if (novoStatus == "pago") java.time.LocalDate.now().toString() else null
            ClienteSupabase.client.from("transacoes_financeiras")
                .update({
                    set("status", novoStatus)
                    set("data_pagamento", novaData)
                }) {
                    filter { eq("id", transacaoId) }
                }
        }
    }

    override suspend fun adicionarTransacao(transacao: TransacaoFinanceira) = withContext(Dispatchers.IO) {
        val dto = TransacaoDto(
            id = transacao.id.ifBlank { UUID.randomUUID().toString() },
            usuarioId = transacao.usuarioId,
            categoriaId = transacao.categoriaId,
            tipo = when (transacao.tipo) {
                TipoTransacao.RECEITA -> "receita"
                TipoTransacao.DESPESA_FIXA -> "despesa_fixa"
                TipoTransacao.DESPESA_VARIAVEL -> "despesa_variavel"
                TipoTransacao.ASSINATURA -> "assinatura"
            },
            descricao = transacao.descricao,
            valor = transacao.valor,
            dataVencimento = transacao.dataVencimento,
            dataPagamento = transacao.dataPagamento,
            status = transacao.status.name.lowercase(),
            observacoes = transacao.observacoes
        )
        ClienteSupabase.client.from("transacoes_financeiras").insert(dto)
        Unit
    }

    override suspend fun depositarCofrinho(cofrinhoId: String, valor: Double) = withContext(Dispatchers.IO) {
        val atual = ClienteSupabase.client.from("cofrinhos")
            .select { filter { eq("id", cofrinhoId) } }
            .decodeSingleOrNull<CofrinhoDto>()

        if (atual != null) {
            val novoSaldo = atual.saldoAtual + valor
            ClienteSupabase.client.from("cofrinhos")
                .update({ set("saldo_atual", novoSaldo) }) {
                    filter { eq("id", cofrinhoId) }
                }
        }
    }

    private fun TransacaoDto.paraDominio() = TransacaoFinanceira(
        id = id,
        usuarioId = usuarioId,
        categoriaId = categoriaId,
        tipo = when (tipo) {
            "receita" -> TipoTransacao.RECEITA
            "despesa_fixa" -> TipoTransacao.DESPESA_FIXA
            "assinatura" -> TipoTransacao.ASSINATURA
            else -> TipoTransacao.DESPESA_VARIAVEL
        },
        descricao = descricao,
        valor = valor,
        dataVencimento = dataVencimento,
        dataPagamento = dataPagamento,
        status = when (status) {
            "pago" -> StatusPagamento.PAGO
            "atrasado" -> StatusPagamento.ATRASADO
            else -> StatusPagamento.PENDENTE
        },
        observacoes = observacoes,
        criadoEm = criadoEm
    )

    private fun CofrinhoDto.paraDominio() = Cofrinho(
        id = id,
        nome = nome,
        metaValor = metaValor,
        saldoAtual = saldoAtual,
        dataLimite = dataLimite,
        icone = icone
    )
}

class RepositorioRotinasSupabase(
    private val repositorioUsuarios: RepositorioUsuarios
) : RepositorioRotinas {
    override fun listarTarefas(): Flow<List<TarefaRotina>> = flow {
        val dtos = ClienteSupabase.client.from("tarefas_rotinas")
            .select {
                order("esta_concluida", Order.ASCENDING)
                order("atualizado_em", Order.DESCENDING)
            }
            .decodeList<TarefaRotinaDto>()

        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun listarItensLoja(): Flow<List<ItemLojaPrivilegio>> = flow {
        val dtos = ClienteSupabase.client.from("loja_privilegios")
            .select {
                filter { eq("ativo", true) }
                order("custo_moedas", Order.ASCENDING)
            }
            .decodeList<ItemLojaDto>()

        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override suspend fun alternarConclusaoTarefa(tarefaId: String): Int = withContext(Dispatchers.IO) {
        val atual = ClienteSupabase.client.from("tarefas_rotinas")
            .select { filter { eq("id", tarefaId) } }
            .decodeSingleOrNull<TarefaRotinaDto>() ?: return@withContext 0

        val novaConclusao = !atual.estaConcluida
        val hoje = java.time.LocalDate.now().toString()

        ClienteSupabase.client.from("tarefas_rotinas")
            .update({
                set("esta_concluida", novaConclusao)
                set("ultima_conclusao_data", if (novaConclusao) hoje else null)
            }) {
                filter { eq("id", tarefaId) }
            }

        val delta = if (novaConclusao) atual.moedasRecompensa else -atual.moedasRecompensa
        atual.responsavelId?.let { respId ->
            repositorioUsuarios.atualizarSaldoMoedas(respId, delta)
        }
        delta
    }

    override suspend fun resgatarItemLoja(usuarioId: String, itemId: String): Boolean = withContext(Dispatchers.IO) {
        val item = ClienteSupabase.client.from("loja_privilegios")
            .select { filter { eq("id", itemId) } }
            .decodeSingleOrNull<ItemLojaDto>() ?: return@withContext false

        repositorioUsuarios.atualizarSaldoMoedas(usuarioId, -item.custoMoedas)

        // Registrar resgate
        ClienteSupabase.client.from("resgates_privilegios")
            .insert(mapOf(
                "privilegio_id" to itemId,
                "usuario_id" to usuarioId,
                "moedas_gastas" to item.custoMoedas,
                "status" to "aprovado"
            ))
        true
    }

    private fun TarefaRotinaDto.paraDominio() = TarefaRotina(
        id = id,
        titulo = titulo,
        responsavelId = responsavelId,
        recorrencia = when (recorrencia) {
            "semanal" -> RecorrenciaTarefa.SEMANAL
            "mensal" -> RecorrenciaTarefa.MENSAL
            "unica" -> RecorrenciaTarefa.UNICA
            else -> RecorrenciaTarefa.DIARIA
        },
        moedasRecompensa = moedasRecompensa,
        estaConcluida = estaConcluida,
        atualizadoEm = atualizadoEm,
        diasSemana = diasSemana,
        diaMes = diaMes,
        abertaParaTodos = abertaParaTodos || responsavelId == null,
        ultimaConclusaoData = ultimaConclusaoData
    )

    private fun ItemLojaDto.paraDominio() = ItemLojaPrivilegio(
        id = id,
        titulo = titulo,
        custoMoedas = custoMoedas,
        icone = icone,
        estoqueDisponivel = estoqueDisponivel,
        descricao = descricao ?: ""
    )
}

class RepositorioEventosSupabase : RepositorioEventos {
    override fun listarEventos(): Flow<List<Evento>> = flow {
        val dtos = ClienteSupabase.client.from("eventos")
            .select {
                order("data_evento", Order.ASCENDING)
            }
            .decodeList<EventoDto>()

        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun listarConvidados(eventoId: String): Flow<List<Convidado>> = flow {
        val dtos = ClienteSupabase.client.from("convidados")
            .select {
                filter { eq("evento_id", eventoId) }
                order("nome", Order.ASCENDING)
            }
            .decodeList<ConvidadoDto>()

        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun listarCotacoes(eventoId: String): Flow<List<Cotacao>> = flow {
        val dtos = ClienteSupabase.client.from("cotacoes")
            .select {
                filter { eq("evento_id", eventoId) }
                order("valor", Order.DESCENDING)
            }
            .decodeList<CotacaoDto>()

        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override suspend fun atualizarStatusRsvp(convidadoId: String, novoStatus: StatusRsvp) = withContext(Dispatchers.IO) {
        val statusString = novoStatus.name.lowercase()
        ClienteSupabase.client.from("convidados")
            .update({ set("status", statusString) }) {
                filter { eq("id", convidadoId) }
            }
        Unit
    }

    override suspend fun adicionarCotacao(cotacao: Cotacao) = withContext(Dispatchers.IO) {
        val dto = CotacaoDto(
            id = cotacao.id.ifBlank { UUID.randomUUID().toString() },
            eventoId = cotacao.eventoId,
            itemOuServico = cotacao.itemOuServico,
            fornecedor = cotacao.fornecedor,
            valor = cotacao.valor,
            status = cotacao.status.name.lowercase(),
            linkAnexo = cotacao.linkAnexo,
            observacoes = cotacao.observacoes
        )
        ClienteSupabase.client.from("cotacoes").insert(dto)
        Unit
    }

    private fun EventoDto.paraDominio() = Evento(
        id = id,
        nome = nome,
        dataEvento = dataEvento,
        descricao = descricao,
        orcamentoEstimado = orcamentoEstimado,
        criadoEm = criadoEm
    )

    private fun ConvidadoDto.paraDominio() = Convidado(
        id = id,
        eventoId = eventoId,
        nome = nome,
        telefone = telefone,
        status = when (status) {
            "confirmado" -> StatusRsvp.CONFIRMADO
            "recusado" -> StatusRsvp.RECUSADO
            else -> StatusRsvp.PENDENTE
        },
        restricoesAlimentares = restricoesAlimentares
    )

    private fun CotacaoDto.paraDominio() = Cotacao(
        id = id,
        eventoId = eventoId,
        itemOuServico = itemOuServico,
        fornecedor = fornecedor,
        valor = valor,
        status = when (status) {
            "aprovada" -> StatusCotacao.APROVADA
            "rejeitada" -> StatusCotacao.REJEITADA
            else -> StatusCotacao.EM_ANALISE
        },
        linkAnexo = linkAnexo,
        observacoes = observacoes
    )
}

class RepositorioInventarioSupabase : RepositorioInventario {
    override fun listarItensInventario(): Flow<List<ItemInventario>> = flow {
        val dtos = ClienteSupabase.client.from("inventario_itens")
            .select { order("nome", Order.ASCENDING) }
            .decodeList<ItemInventarioDto>()
        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun buscarItens(termo: String): Flow<List<ItemInventario>> = flow {
        val dtos = ClienteSupabase.client.from("inventario_itens")
            .select {
                order("nome", Order.ASCENDING)
            }
            .decodeList<ItemInventarioDto>()

        val filtrados = if (termo.isBlank()) dtos else dtos.filter {
            it.nome.contains(termo, ignoreCase = true) || it.localizacao.contains(termo, ignoreCase = true)
        }
        emit(filtrados.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun listarContatos(): Flow<List<ContatoAgenda>> = flow {
        val dtos = ClienteSupabase.client.from("agenda_contatos")
            .select {
                order("eh_emergencia", Order.DESCENDING)
                order("nome", Order.ASCENDING)
            }
            .decodeList<ContatoDto>()

        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun listarHistoricoManutencoes(): Flow<List<HistoricoManutencao>> = flow {
        val dtos = ClienteSupabase.client.from("historico_manutencoes")
            .select { order("data_servico", Order.DESCENDING) }
            .decodeList<HistoricoManutencaoDto>()

        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override suspend fun sincronizarContatos(contatos: List<ContatoAgenda>): Int = withContext(Dispatchers.IO) {
        val dtos = contatos.map {
            ContatoDto(
                id = it.id.ifBlank { UUID.randomUUID().toString() },
                nome = it.nome,
                especialidade = it.especialidade,
                telefone = it.telefone,
                email = it.email,
                ehEmergencia = it.ehEmergencia,
                observacoes = it.observacoes
            )
        }

        ClienteSupabase.client.from("agenda_contatos").upsert(dtos) {
            onConflict = "telefone"
        }
        dtos.size
    }

    private fun ItemInventarioDto.paraDominio() = ItemInventario(
        id = id,
        nome = nome,
        localizacao = localizacao,
        quantidade = quantidade,
        dataCompra = dataCompra,
        dataFimGarantia = dataFimGarantia,
        caminhoNotaFiscal = caminhoNotaFiscal,
        caminhoManual = caminhoManual
    )

    private fun ContatoDto.paraDominio() = ContatoAgenda(
        id = id,
        nome = nome,
        especialidade = especialidade,
        telefone = telefone,
        email = email,
        ehEmergencia = ehEmergencia,
        observacoes = observacoes
    )

    private fun HistoricoManutencaoDto.paraDominio() = HistoricoManutencao(
        id = id,
        itemId = itemId,
        prestadorId = prestadorId,
        descricao = descricao,
        dataServico = dataServico,
        custo = custo,
        dataProximaManutencao = dataProximaManutencao
    )
}

class RepositorioSaudeSupabase : RepositorioSaude {
    override fun listarCheckinsHumor(): Flow<List<CheckinSaude>> = flow {
        val dtos = ClienteSupabase.client.from("saude_checkins")
            .select { order("criado_em", Order.DESCENDING) }
            .decodeList<CheckinSaudeDto>()
        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun listarFichasMedicas(): Flow<List<FichaMedica>> = flow {
        val dtos = ClienteSupabase.client.from("saude_fichas_medicas")
            .select()
            .decodeList<FichaMedicaDto>()
        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun listarLembretesMedicamento(): Flow<List<MedicamentoLembrete>> = flow {
        emit(emptyList()) // Sem tabela de medicamentos no schema remoto por enquanto
    }

    override suspend fun registrarCheckin(checkin: CheckinSaude) = withContext(Dispatchers.IO) {
        val dto = CheckinSaudeDto(
            id = checkin.id.ifBlank { UUID.randomUUID().toString() },
            usuarioId = checkin.usuarioId,
            nivelHumor = checkin.nivelHumor,
            notasDiario = checkin.notasDiario
        )
        ClienteSupabase.client.from("saude_checkins").insert(dto)
        Unit
    }

    override suspend fun alternarMedicamentoTomado(medicamentoId: String) {}

    private fun CheckinSaudeDto.paraDominio() = CheckinSaude(
        id = id,
        usuarioId = usuarioId,
        nivelHumor = nivelHumor,
        notasDiario = notasDiario,
        criadoEm = criadoEm
    )

    private fun FichaMedicaDto.paraDominio() = FichaMedica(
        id = id,
        usuarioId = usuarioId,
        nomeUsuario = "Membro da Família",
        tipoSanguineo = tipoSanguineo,
        alergias = alergias,
        medicamentosUsoContinuo = medicamentosUsoContinuo,
        numeroPlanoSaude = numeroPlanoSaude
    )
}

class RepositorioCofreSupabase : RepositorioCofre {
    override fun listarCredenciais(incluirMaster: Boolean): Flow<List<CredencialCofre>> = flow {
        val dtos = ClienteSupabase.client.from("cofre_credenciais")
            .select {
                if (!incluirMaster) filter { eq("nivel", "compartilhado") }
                order("titulo", Order.ASCENDING)
            }
            .decodeList<CredencialCofreDto>()
        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun obterRedeWifiFamiliar(): Flow<RedeWifiFamiliar> = flow {
        emit(
            RedeWifiFamiliar(
                nomeRedeSsid = "Familia_OS_5G",
                senhaRede = "FamiliaSegura2026",
                tipoSeguranca = "WPA2/WPA3",
                ehRedeVisitantes = false
            )
        )
    }

    override suspend fun adicionarCredencial(credencial: CredencialCofre) = withContext(Dispatchers.IO) {
        val dto = CredencialCofreDto(
            id = credencial.id.ifBlank { UUID.randomUUID().toString() },
            titulo = credencial.titulo,
            nivel = credencial.nivel.name.lowercase(),
            loginUsuario = credencial.loginUsuario,
            senhaCriptografada = credencial.senhaCriptografada,
            urlAcesso = credencial.urlAcesso,
            notasSeguras = credencial.notasSeguras,
            criadoPor = credencial.criadoPor
        )
        ClienteSupabase.client.from("cofre_credenciais").insert(dto)
        Unit
    }

    private fun CredencialCofreDto.paraDominio() = CredencialCofre(
        id = id,
        titulo = titulo,
        nivel = if (nivel == "master") NivelCofre.MASTER else NivelCofre.COMPARTILHADO,
        loginUsuario = loginUsuario,
        senhaCriptografada = senhaCriptografada,
        urlAcesso = urlAcesso,
        notasSeguras = notasSeguras,
        criadoPor = criadoPor
    )
}

class RepositorioMuralSupabase : RepositorioMural {
    override fun listarConquistas(): Flow<List<ConquistaMural>> = flow {
        val dtos = ClienteSupabase.client.from("mural_conquistas")
            .select { order("data_conquista", Order.DESCENDING) }
            .decodeList<ConquistaMuralDto>()
        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun listarFotosAlbum(): Flow<List<FotoAlbum>> = flow {
        emit(emptyList())
    }

    override suspend fun adicionarReacao(conquistaId: String) {}

    private fun ConquistaMuralDto.paraDominio() = ConquistaMural(
        id = id,
        titulo = titulo,
        descricao = descricao,
        usuarioId = usuarioId,
        dataConquista = dataConquista,
        caminhoFoto = caminhoFoto
    )
}

class RepositorioComprasSupabase : RepositorioCompras {
    override fun listarRecibos(): Flow<List<ReciboCompra>> = flow {
        val dtos = ClienteSupabase.client.from("compras_recibos")
            .select { order("criado_em", Order.DESCENDING) }
            .decodeList<ReciboCompraDto>()
        emit(dtos.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override suspend fun simularOcrCupomFiscal(nomeSupermercado: String, valorTotal: Double) = withContext(Dispatchers.IO) {
        val dto = ReciboCompraDto(
            id = UUID.randomUUID().toString(),
            fornecedor = nomeSupermercado,
            dataCompra = java.time.LocalDate.now().toString(),
            valorTotal = valorTotal
        )
        ClienteSupabase.client.from("compras_recibos").insert(dto)
        Unit
    }

    private fun ReciboCompraDto.paraDominio() = ReciboCompra(
        id = id,
        fornecedor = fornecedor,
        dataCompra = dataCompra,
        valorTotal = valorTotal,
        caminhoImagemRecibo = caminhoImagemRecibo,
        criadoEm = criadoEm
    )
}
