package br.com.raevan.familialm.dados.repositorios

import br.com.raevan.familialm.dados.rede.ClienteSupabase
import br.com.raevan.familialm.dados.remoto.dto.*
import br.com.raevan.familialm.dados.sessao.GerenciadorSessao
import br.com.raevan.familialm.dominio.modelos.*
import br.com.raevan.familialm.dominio.repositorios.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class RepositorioUsuariosSupabase : RepositorioUsuarios {
    override fun obterUsuarioLogado(): Flow<Usuario> = flow {
        val todosAtivos = ClienteSupabase.client.from("usuarios")
            .select {
                filter { eq("ativo", true) }
            }
            .decodeList<UsuarioDto>()

        val authUser = try { ClienteSupabase.client.auth.currentUserOrNull() } catch (_: Exception) { null }
        val ultimoEmail = try { GerenciadorSessao.obterInstancia().obterUltimoEmail() } catch (_: Exception) { "" }

        val usuarioResolvido = todosAtivos.firstOrNull { dto ->
            (authUser?.id != null && (dto.authUserId == authUser.id || dto.id == authUser.id)) ||
                (!authUser?.email.isNullOrBlank() && dto.email.equals(authUser?.email, ignoreCase = true)) ||
                (ultimoEmail.isNotBlank() && dto.email.equals(ultimoEmail, ignoreCase = true))
        } ?: todosAtivos.firstOrNull() ?: UsuarioDto(
            id = "usr-padrao",
            familiaId = GerenciadorSessao.DEFAULT_FAMILIA_ID,
            nome = "Usuário",
            perfil = "pai_admin",
            saldoMoedas = 0
        )

        usuarioResolvido.familiaId?.let { famId ->
            try {
                val famDto = ClienteSupabase.client.from("familias")
                    .select {
                        filter { eq("id", famId) }
                    }
                    .decodeSingleOrNull<FamiliaDto>()
                GerenciadorSessao.obterInstancia().salvarFamiliaAtiva(
                    familiaId = famId,
                    nomeFamilia = famDto?.nome ?: "Minha Família",
                    codigoConvite = famDto?.codigoConvite ?: ""
                )
            } catch (_: Exception) {}
        }

        emit(usuarioResolvido.paraDominio())
    }.flowOn(Dispatchers.IO)

    override fun listarMembrosFamilia(): Flow<List<Usuario>> = flow {
        val dtos = ClienteSupabase.client.from("usuarios")
            .select {
                filter { eq("ativo", true) }
                order("nome", Order.ASCENDING)
            }
            .decodeList<UsuarioDto>()

        val familiaAtivaId = try {
            GerenciadorSessao.obterInstancia().obterFamiliaIdAtiva()
        } catch (_: Exception) {
            GerenciadorSessao.DEFAULT_FAMILIA_ID
        }

        val filtrados = dtos.filter { dto ->
            dto.familiaId == null || dto.familiaId == familiaAtivaId
        }

        emit(filtrados.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun listarTodosMembrosFamilia(incluirInativos: Boolean): Flow<List<Usuario>> = flow {
        val dtos = ClienteSupabase.client.from("usuarios")
            .select {
                if (!incluirInativos) filter { eq("ativo", true) }
                order("nome", Order.ASCENDING)
            }
            .decodeList<UsuarioDto>()

        val familiaAtivaId = try {
            GerenciadorSessao.obterInstancia().obterFamiliaIdAtiva()
        } catch (_: Exception) {
            GerenciadorSessao.DEFAULT_FAMILIA_ID
        }

        val filtrados = dtos.filter { dto ->
            dto.familiaId == null || dto.familiaId == familiaAtivaId
        }

        emit(filtrados.map { it.paraDominio() })
    }.flowOn(Dispatchers.IO)

    override fun obterFamiliaAtiva(): Flow<Familia> = flow {
        val sessao = try { GerenciadorSessao.obterInstancia() } catch (_: Exception) { null }
        val familiaIdSalva = sessao?.obterFamiliaIdAtiva() ?: GerenciadorSessao.DEFAULT_FAMILIA_ID

        val dto = try {
            ClienteSupabase.client.from("familias")
                .select { filter { eq("id", familiaIdSalva) } }
                .decodeSingleOrNull<FamiliaDto>()
        } catch (_: Exception) {
            null
        }

        val familia = if (dto != null) {
            sessao?.salvarFamiliaAtiva(dto.id, dto.nome, dto.codigoConvite ?: "MARTEL")
            Familia(
                id = dto.id,
                nome = dto.nome,
                codigoConvite = dto.codigoConvite ?: "MARTEL",
                plano = dto.plano
            )
        } else {
            Familia(
                id = familiaIdSalva,
                nome = sessao?.obterNomeFamiliaAtiva() ?: "Família Martel",
                codigoConvite = sessao?.obterCodigoConviteAtivo() ?: "MARTEL"
            )
        }

        emit(familia)
    }.flowOn(Dispatchers.IO)

    override suspend fun atualizarNomeFamilia(familiaId: String, novoNome: String): Boolean = withContext(Dispatchers.IO) {
        val limpo = novoNome.trim()
        if (limpo.isBlank()) return@withContext false

        var atualizado = false
        try {
            ClienteSupabase.client.from("familias")
                .update({ set("nome", limpo) }) {
                    filter { eq("id", familiaId) }
                }
            atualizado = true
        } catch (_: Exception) {}

        if (!atualizado) {
            try {
                val conn = (URL("https://familia.martel.page/api/tenant").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                val payload = JSONObject().apply {
                    put("acao", "atualizar_familia")
                    put("familiaId", familiaId)
                    put("nomeFamilia", limpo)
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                atualizado = conn.responseCode in 200..299
            } catch (_: Exception) {}
        }

        try {
            val sessao = GerenciadorSessao.obterInstancia()
            sessao.salvarFamiliaAtiva(familiaId, limpo, sessao.obterCodigoConviteAtivo())
        } catch (_: Exception) {}

        atualizado
    }

    override suspend fun vincularPorCodigoConvite(usuarioId: String, codigoConvite: String): Familia? = withContext(Dispatchers.IO) {
        val codigo = codigoConvite.trim().uppercase()
        if (codigo.isBlank()) return@withContext null

        try {
            val famDto = ClienteSupabase.client.from("familias")
                .select { filter { eq("codigo_convite", codigo) } }
                .decodeSingleOrNull<FamiliaDto>()

            if (famDto != null) {
                ClienteSupabase.client.from("usuarios")
                    .update({ set("familia_id", famDto.id) }) {
                        filter { eq("id", usuarioId) }
                    }
                try {
                    GerenciadorSessao.obterInstancia().salvarFamiliaAtiva(
                        familiaId = famDto.id,
                        nomeFamilia = famDto.nome,
                        codigoConvite = famDto.codigoConvite ?: codigo
                    )
                } catch (_: Exception) {}
                return@withContext Familia(
                    id = famDto.id,
                    nome = famDto.nome,
                    codigoConvite = famDto.codigoConvite ?: codigo,
                    plano = famDto.plano
                )
            }
        } catch (_: Exception) {}

        try {
            val conn = (URL("https://familia.martel.page/api/tenant").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }
            val payload = JSONObject().apply {
                put("acao", "vincular_convite")
                put("usuarioId", usuarioId)
                put("codigoConvite", codigo)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val famObj = JSONObject(respStr).optJSONObject("familia")
                if (famObj != null) {
                    val fam = Familia(
                        id = famObj.optString("id"),
                        nome = famObj.optString("nome", "Minha Família"),
                        codigoConvite = famObj.optString("codigo_convite", codigo)
                    )
                    try {
                        GerenciadorSessao.obterInstancia().salvarFamiliaAtiva(fam.id, fam.nome, fam.codigoConvite ?: codigo)
                    } catch (_: Exception) {}
                    return@withContext fam
                }
            }
        } catch (_: Exception) {}

        null
    }

    override suspend fun criarMembroFamilia(
        nome: String,
        email: String,
        senha: String,
        perfil: TipoPerfil,
        avatarEmoji: String?,
        saldoMoedas: Int
    ): Result<Usuario> = withContext(Dispatchers.IO) {
        val familiaId = try {
            GerenciadorSessao.obterInstancia().obterFamiliaIdAtiva()
        } catch (_: Exception) {
            GerenciadorSessao.DEFAULT_FAMILIA_ID
        }
        val perfilBanco = when (perfil) {
            TipoPerfil.PAI_ADMIN -> "pai_admin"
            TipoPerfil.FILHO_MEMBRO -> "filho_membro"
            TipoPerfil.CUIDADOR -> "cuidador"
        }

        try {
            val conn = (URL("https://familia.martel.page/api/admin/users").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", senha)
                put("nome", nome.trim())
                put("perfil", perfilBanco)
                put("avatar_emoji", avatarEmoji ?: "👤")
                put("saldo_moedas", saldoMoedas)
                put("familia_id", familiaId)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = conn.responseCode
            val respTexto = (if (status in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: "{}"

            if (status in 200..299) {
                val jsonObj = JSONObject(respTexto).optJSONObject("user")
                val idCriado = jsonObj?.optString("id")?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                return@withContext Result.success(
                    Usuario(
                        id = idCriado,
                        nome = nome.trim(),
                        perfil = perfil,
                        saldoMoedas = saldoMoedas,
                        avatarUrl = avatarEmoji,
                        familiaId = familiaId,
                        email = email.trim(),
                        ativo = true
                    )
                )
            }
        } catch (_: Exception) {}

        return@withContext try {
            val novoId = UUID.randomUUID().toString()
            val dto = UsuarioDto(
                id = novoId,
                familiaId = familiaId,
                email = email.trim(),
                nome = nome.trim(),
                perfil = perfilBanco,
                saldoMoedas = saldoMoedas,
                avatarUrl = avatarEmoji,
                ativo = true
            )
            ClienteSupabase.client.from("usuarios").insert(dto)
            Result.success(dto.paraDominio())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun atualizarMembroFamilia(
        usuarioId: String,
        nome: String,
        perfil: TipoPerfil,
        avatarEmoji: String?,
        ativo: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val perfilBanco = when (perfil) {
            TipoPerfil.PAI_ADMIN -> "pai_admin"
            TipoPerfil.FILHO_MEMBRO -> "filho_membro"
            TipoPerfil.CUIDADOR -> "cuidador"
        }
        try {
            ClienteSupabase.client.from("usuarios")
                .update({
                    set("nome", nome.trim())
                    set("perfil", perfilBanco)
                    if (!avatarEmoji.isNullOrBlank()) set("avatar_url", avatarEmoji)
                    set("ativo", ativo)
                }) {
                    filter { eq("id", usuarioId) }
                }
            true
        } catch (_: Exception) {
            false
        }
    }

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
        criadoEm = criadoEm,
        familiaId = familiaId,
        email = email,
        ativo = ativo
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

    override suspend fun adicionarTarefa(
        titulo: String,
        responsavelId: String?,
        recorrencia: RecorrenciaTarefa,
        diasSemana: String?,
        diaMes: Int?,
        moedasRecompensa: Int,
        abertaParaTodos: Boolean
    ) = withContext(Dispatchers.IO) {
        val dto = TarefaRotinaDto(
            id = UUID.randomUUID().toString(),
            titulo = titulo.trim(),
            responsavelId = if (abertaParaTodos) null else responsavelId,
            recorrencia = when (recorrencia) {
                RecorrenciaTarefa.SEMANAL -> "semanal"
                RecorrenciaTarefa.MENSAL -> "mensal"
                RecorrenciaTarefa.UNICA -> "unica"
                RecorrenciaTarefa.DIARIA -> "diaria"
            },
            diasSemana = diasSemana,
            diaMes = diaMes,
            moedasRecompensa = moedasRecompensa,
            estaConcluida = false,
            abertaParaTodos = abertaParaTodos
        )
        ClienteSupabase.client.from("tarefas_rotinas").insert(dto)
        Unit
    }

    override suspend fun adicionarItemLoja(
        titulo: String,
        descricao: String,
        custoMoedas: Int,
        icone: String?
    ) = withContext(Dispatchers.IO) {
        val dto = ItemLojaDto(
            id = UUID.randomUUID().toString(),
            titulo = titulo.trim(),
            descricao = descricao.trim(),
            custoMoedas = custoMoedas,
            icone = icone ?: "🎁",
            estoqueDisponivel = -1,
            ativo = true
        )
        ClienteSupabase.client.from("loja_privilegios").insert(dto)
        Unit
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

    override suspend fun adicionarEvento(
        nome: String,
        dataEvento: String,
        descricao: String?,
        orcamentoEstimado: Double
    ) = withContext(Dispatchers.IO) {
        val dto = EventoDto(
            id = UUID.randomUUID().toString(),
            nome = nome.trim(),
            dataEvento = dataEvento,
            descricao = descricao?.takeIf { it.isNotBlank() },
            orcamentoEstimado = orcamentoEstimado
        )
        ClienteSupabase.client.from("eventos").insert(dto)
        Unit
    }

    override suspend fun adicionarConvidado(
        eventoId: String,
        nome: String,
        telefone: String?,
        restricoesAlimentares: String?
    ) = withContext(Dispatchers.IO) {
        val dto = ConvidadoDto(
            id = UUID.randomUUID().toString(),
            eventoId = eventoId,
            nome = nome.trim(),
            telefone = telefone?.takeIf { it.isNotBlank() },
            status = "pendente",
            restricoesAlimentares = restricoesAlimentares?.takeIf { it.isNotBlank() }
        )
        ClienteSupabase.client.from("convidados").insert(dto)
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
                categoria = it.categoria,
                especialidade = it.especialidade,
                membroId = it.membroId,
                grauParentesco = it.grauParentesco,
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
        categoria = categoria ?: "amigos",
        especialidade = especialidade,
        membroId = membroId,
        grauParentesco = grauParentesco,
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
        val dtos = ClienteSupabase.client.from("saude_medicamentos")
            .select { order("horario", Order.ASCENDING) }
            .decodeList<MedicamentoDto>()

        val usuarios = ClienteSupabase.client.from("usuarios")
            .select()
            .decodeList<UsuarioDto>()
            .associateBy { it.id }

        emit(dtos.map { dto ->
            MedicamentoLembrete(
                id = dto.id,
                nomeRemedio = dto.nomeRemedio,
                dosagem = dto.dosagem,
                horario = dto.horario,
                membroFamiliar = usuarios[dto.usuarioId]?.nome ?: "Membro",
                foiTomadoHoje = dto.foiTomadoHoje
            )
        })
    }.flowOn(Dispatchers.IO)

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

    override suspend fun alternarMedicamentoTomado(medicamentoId: String) = withContext(Dispatchers.IO) {
        val atual = ClienteSupabase.client.from("saude_medicamentos")
            .select { filter { eq("id", medicamentoId) } }
            .decodeSingleOrNull<MedicamentoDto>()

        if (atual != null) {
            val novoStatus = !atual.foiTomadoHoje
            ClienteSupabase.client.from("saude_medicamentos")
                .update({ set("foi_tomado_hoje", novoStatus) }) {
                    filter { eq("id", medicamentoId) }
                }
        }
        Unit
    }

    override fun listarVacinas(): Flow<List<VacinaRegistro>> = flow {
        val dtos = ClienteSupabase.client.from("saude_vacinacao")
            .select { order("data_aplicacao", Order.DESCENDING) }
            .decodeList<VacinaDto>()

        val usuarios = ClienteSupabase.client.from("usuarios")
            .select().decodeList<UsuarioDto>().associateBy { it.id }

        emit(dtos.map { dto ->
            VacinaRegistro(
                id = dto.id,
                usuarioId = dto.usuarioId,
                nomeMembro = usuarios[dto.usuarioId]?.nome ?: "Membro",
                nomeVacina = dto.nomeVacina,
                dose = dto.dose,
                dataAplicacao = dto.dataAplicacao,
                dataProximaDose = dto.dataProximaDose,
                lote = dto.lote,
                localAplicacao = dto.localAplicacao
            )
        })
    }.flowOn(Dispatchers.IO)

    override fun listarConsultasExames(): Flow<List<ConsultaExameRegistro>> = flow {
        val dtos = ClienteSupabase.client.from("saude_consultas_exames")
            .select { order("data_agendada", Order.ASCENDING) }
            .decodeList<ConsultaExameDto>()

        val usuarios = ClienteSupabase.client.from("usuarios")
            .select().decodeList<UsuarioDto>().associateBy { it.id }

        emit(dtos.map { dto ->
            ConsultaExameRegistro(
                id = dto.id,
                usuarioId = dto.usuarioId,
                nomeMembro = usuarios[dto.usuarioId]?.nome ?: "Membro",
                tipo = dto.tipo,
                titulo = dto.titulo,
                especialistaOuClinica = dto.especialistaOuClinica,
                dataAgendada = dto.dataAgendada,
                dataRetorno = dto.dataRetorno,
                observacoes = dto.observacoes,
                resultado = dto.resultado,
                status = dto.status ?: if (dto.concluido) "concluido" else "agendado",
                contatoId = dto.contatoId,
                eventoId = dto.eventoId,
                concluido = dto.concluido
            )
        })
    }.flowOn(Dispatchers.IO)

    override fun listarMetricasVitais(): Flow<List<MetricaVitalRegistro>> = flow {
        val dtos = ClienteSupabase.client.from("saude_metricas_vitais")
            .select { order("data_medicao", Order.DESCENDING) }
            .decodeList<MetricaVitalDto>()

        val usuarios = ClienteSupabase.client.from("usuarios")
            .select().decodeList<UsuarioDto>().associateBy { it.id }

        emit(dtos.map { dto ->
            MetricaVitalRegistro(
                id = dto.id,
                usuarioId = dto.usuarioId,
                nomeMembro = usuarios[dto.usuarioId]?.nome ?: "Membro",
                tipoMetrica = dto.tipoMetrica,
                valorPrincipal = dto.valorPrincipal,
                valorSecundario = dto.valorSecundario,
                unidade = dto.unidade,
                observacao = dto.observacao,
                dataMedicao = dto.dataMedicao
            )
        })
    }.flowOn(Dispatchers.IO)

    override suspend fun adicionarVacina(vacina: VacinaRegistro) = withContext(Dispatchers.IO) {
        val dto = VacinaDto(
            id = vacina.id.ifBlank { UUID.randomUUID().toString() },
            usuarioId = vacina.usuarioId,
            nomeVacina = vacina.nomeVacina,
            dose = vacina.dose,
            dataAplicacao = vacina.dataAplicacao,
            dataProximaDose = vacina.dataProximaDose,
            lote = vacina.lote,
            localAplicacao = vacina.localAplicacao
        )
        ClienteSupabase.client.from("saude_vacinacao").insert(dto)
        Unit
    }

    override suspend fun adicionarConsultaExame(consulta: ConsultaExameRegistro) = withContext(Dispatchers.IO) {
        val dto = ConsultaExameDto(
            id = consulta.id.ifBlank { UUID.randomUUID().toString() },
            usuarioId = consulta.usuarioId,
            tipo = consulta.tipo,
            titulo = consulta.titulo,
            especialistaOuClinica = consulta.especialistaOuClinica,
            dataAgendada = consulta.dataAgendada,
            dataRetorno = consulta.dataRetorno,
            observacoes = consulta.observacoes,
            resultado = consulta.resultado,
            status = consulta.status,
            contatoId = consulta.contatoId,
            eventoId = consulta.eventoId,
            concluido = consulta.concluido
        )
        ClienteSupabase.client.from("saude_consultas_exames").insert(dto)
        Unit
    }

    override suspend fun alternarConsultaConcluida(id: String) = withContext(Dispatchers.IO) {
        val atual = ClienteSupabase.client.from("saude_consultas_exames")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<ConsultaExameDto>()

        if (atual != null) {
            val novoConcluido = !atual.concluido
            val novoStatus = if (novoConcluido) "concluido" else "agendado"
            ClienteSupabase.client.from("saude_consultas_exames")
                .update({ 
                    set("concluido", novoConcluido)
                    set("status", novoStatus)
                }) {
                    filter { eq("id", id) }
                }
        }
        Unit
    }

    override suspend fun atualizarConsultaExame(consulta: ConsultaExameRegistro) = withContext(Dispatchers.IO) {
        val dto = ConsultaExameDto(
            id = consulta.id,
            usuarioId = consulta.usuarioId,
            tipo = consulta.tipo,
            titulo = consulta.titulo,
            especialistaOuClinica = consulta.especialistaOuClinica,
            dataAgendada = consulta.dataAgendada,
            dataRetorno = consulta.dataRetorno,
            observacoes = consulta.observacoes,
            resultado = consulta.resultado,
            status = consulta.status,
            contatoId = consulta.contatoId,
            eventoId = consulta.eventoId,
            concluido = consulta.concluido
        )
        ClienteSupabase.client.from("saude_consultas_exames").update(dto) {
            filter { eq("id", consulta.id) }
        }
        Unit
    }

    override suspend fun excluirConsultaExame(id: String) = withContext(Dispatchers.IO) {
        ClienteSupabase.client.from("saude_consultas_exames").delete {
            filter { eq("id", id) }
        }
        Unit
    }

    override suspend fun adicionarMetricaVital(metrica: MetricaVitalRegistro) = withContext(Dispatchers.IO) {
        val dto = MetricaVitalDto(
            id = metrica.id.ifBlank { UUID.randomUUID().toString() },
            usuarioId = metrica.usuarioId,
            tipoMetrica = metrica.tipoMetrica,
            valorPrincipal = metrica.valorPrincipal,
            valorSecundario = metrica.valorSecundario,
            unidade = metrica.unidade,
            observacao = metrica.observacao,
            dataMedicao = metrica.dataMedicao
        )
        ClienteSupabase.client.from("saude_metricas_vitais").insert(dto)
        Unit
    }

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
        numeroPlanoSaude = numeroPlanoSaude,
        carteirinhaPlanoUrl = carteirinhaPlanoUrl,
        numeroPlanoOdontologico = numeroPlanoOdontologico,
        carteirinhaOdontoUrl = carteirinhaOdontoUrl,
        resumoClinicoIa = resumoClinicoIa,
        orientacoesMedicoIa = orientacoesMedicoIa,
        atualizadoIaEm = atualizadoIaEm
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
        val familiaId = try {
            GerenciadorSessao.obterInstancia().obterFamiliaIdAtiva()
        } catch (_: Exception) {
            GerenciadorSessao.DEFAULT_FAMILIA_ID
        }
        val chaveTenant = "WIFI_PRINCIPAL:$familiaId"

        val config = try {
            val todas = ClienteSupabase.client.from("configuracoes_casa")
                .select()
                .decodeList<ConfiguracaoCasaDto>()
            todas.firstOrNull { it.chave == chaveTenant }
                ?: todas.firstOrNull { it.chave == "WIFI_PRINCIPAL" }
        } catch (_: Exception) {
            null
        }

        val partes = config?.valor?.split("|") ?: emptyList()
        val ssid = partes.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "Familia_OS_5G"
        val senha = partes.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "FamiliaSegura2026"
        val seguranca = partes.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "WPA2/WPA3"

        emit(
            RedeWifiFamiliar(
                nomeRedeSsid = ssid,
                senhaRede = senha,
                tipoSeguranca = seguranca,
                ehRedeVisitantes = false
            )
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun salvarRedeWifiFamiliar(wifi: RedeWifiFamiliar) = withContext(Dispatchers.IO) {
        val familiaId = try {
            GerenciadorSessao.obterInstancia().obterFamiliaIdAtiva()
        } catch (_: Exception) {
            GerenciadorSessao.DEFAULT_FAMILIA_ID
        }
        val chaveTenant = "WIFI_PRINCIPAL:$familiaId"
        val valorFormatado = "${wifi.nomeRedeSsid.trim()}|${wifi.senhaRede.trim()}|${wifi.tipoSeguranca.trim()}"

        val dto = ConfiguracaoCasaDto(
            chave = chaveTenant,
            valor = valorFormatado,
            descricao = "Configuração Wi-Fi da Família ($familiaId)",
            familiaId = familiaId
        )
        ClienteSupabase.client.from("configuracoes_casa").upsert(dto) {
            onConflict = "chave"
        }
        Unit
    }

    override suspend fun excluirCredencial(credencialId: String) = withContext(Dispatchers.IO) {
        ClienteSupabase.client.from("cofre_credenciais").delete {
            filter { eq("id", credencialId) }
        }
        Unit
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

    override suspend fun adicionarConquista(
        titulo: String,
        descricao: String?,
        usuarioId: String?
    ) = withContext(Dispatchers.IO) {
        val dto = ConquistaMuralDto(
            id = UUID.randomUUID().toString(),
            titulo = titulo.trim(),
            descricao = descricao?.takeIf { it.isNotBlank() },
            usuarioId = usuarioId,
            dataConquista = java.time.LocalDate.now().toString()
        )
        ClienteSupabase.client.from("mural_conquistas").insert(dto)
        Unit
    }

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
