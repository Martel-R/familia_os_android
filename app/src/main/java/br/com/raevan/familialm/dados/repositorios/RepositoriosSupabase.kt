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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private fun obterFamiliaAtivaSegura(): String {
    return try {
        GerenciadorSessao.obterInstancia().obterFamiliaIdAtiva()
            .takeIf { it.isNotBlank() } ?: GerenciadorSessao.DEFAULT_FAMILIA_ID
    } catch (_: Exception) {
        GerenciadorSessao.DEFAULT_FAMILIA_ID
    }
}

private fun normalizarAvatar(avatar: String?): String? {
    val limpo = avatar?.trim()
    return if (limpo.isNullOrBlank() || limpo.contains("?")) null else limpo
}

class RepositorioUsuariosSupabase : RepositorioUsuarios {
    override fun obterUsuarioLogado(): Flow<Usuario> = flow {
        val todosAtivos = try {
            ClienteSupabase.client.from("usuarios")
                .select {
                    filter { eq("ativo", true) }
                }
                .decodeList<UsuarioDto>()
        } catch (_: Exception) {
            emptyList()
        }

        val authUser = try { ClienteSupabase.client.auth.currentUserOrNull() } catch (_: Exception) { null }
        val ultimoEmail = try { GerenciadorSessao.obterInstancia().obterUltimoEmail() } catch (_: Exception) { "" }
        val familiaSalva = obterFamiliaAtivaSegura()

        val usuarioResolvido = todosAtivos.firstOrNull { dto ->
            (authUser?.id != null && (dto.authUserId == authUser.id || dto.id == authUser.id)) ||
                (!authUser?.email.isNullOrBlank() && dto.email.equals(authUser?.email, ignoreCase = true)) ||
                (ultimoEmail.isNotBlank() && dto.email.equals(ultimoEmail, ignoreCase = true))
        } ?: todosAtivos.firstOrNull { dto ->
            dto.familiaId == familiaSalva
        } ?: todosAtivos.firstOrNull() ?: UsuarioDto(
            id = "usr-padrao",
            familiaId = familiaSalva,
            nome = ultimoEmail.substringBefore("@").ifBlank { "Usuário" },
            perfil = "pai_admin",
            saldoMoedas = 0
        )

        usuarioResolvido.familiaId?.takeIf { it.isNotBlank() }?.let { famId ->
            try {
                val famDto = ClienteSupabase.client.from("familias")
                    .select {
                        filter { eq("id", famId) }
                    }
                    .decodeSingleOrNull<FamiliaDto>()
                if (famDto != null) {
                    GerenciadorSessao.obterInstancia().salvarFamiliaAtiva(
                        familiaId = famId,
                        nomeFamilia = famDto.nome,
                        codigoConvite = famDto.codigoConvite ?: ""
                    )
                }
            } catch (_: Exception) {}
        }

        emit(usuarioResolvido.paraDominio())
    }.catch {
        emit(
            Usuario(
                id = "usr-padrao",
                nome = "Membro da Família",
                perfil = TipoPerfil.PAI_ADMIN,
                saldoMoedas = 0,
                familiaId = obterFamiliaAtivaSegura()
            )
        )
    }.flowOn(Dispatchers.IO)

    override fun listarMembrosFamilia(): Flow<List<Usuario>> = flow {
        val familiaAtivaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("usuarios")
                .select {
                    filter {
                        eq("ativo", true)
                        eq("familia_id", familiaAtivaId)
                    }
                    order("nome", Order.ASCENDING)
                }
                .decodeList<UsuarioDto>()
        } catch (_: Exception) {
            emptyList()
        }

        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun listarTodosMembrosFamilia(incluirInativos: Boolean): Flow<List<Usuario>> = flow {
        val familiaAtivaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("usuarios")
                .select {
                    filter {
                        if (!incluirInativos) eq("ativo", true)
                        eq("familia_id", familiaAtivaId)
                    }
                    order("nome", Order.ASCENDING)
                }
                .decodeList<UsuarioDto>()
        } catch (_: Exception) {
            emptyList()
        }

        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun obterFamiliaAtiva(): Flow<Familia> = flow {
        val sessao = try { GerenciadorSessao.obterInstancia() } catch (_: Exception) { null }
        val familiaIdSalva = obterFamiliaAtivaSegura()

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
    }.catch {
        emit(
            Familia(
                id = obterFamiliaAtivaSegura(),
                nome = "Minha Família",
                codigoConvite = "MARTEL"
            )
        )
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
                    setRequestProperty("User-Agent", ClienteSupabase.USER_AGENT_PADRAO)
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                val payload = JSONObject().apply {
                    put("acao", "atualizar_familia")
                    put("familiaId", familiaId)
                    put("nome", limpo)
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
                setRequestProperty("User-Agent", ClienteSupabase.USER_AGENT_PADRAO)
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
        val familiaId = obterFamiliaAtivaSegura()
        val perfilBanco = when (perfil) {
            TipoPerfil.PAI_ADMIN -> "pai_admin"
            TipoPerfil.FILHO_MEMBRO -> "filho_membro"
            TipoPerfil.CUIDADOR -> "cuidador"
        }

        try {
            val conn = (URL("https://familia.martel.page/api/admin/users").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", ClienteSupabase.USER_AGENT_PADRAO)
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", senha)
                put("nome", nome.trim())
                put("perfil", perfilBanco)
                put("avatar_url", avatarEmoji ?: "👤")
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
        try {
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
        } catch (_: Exception) {}
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
        avatarUrl = normalizarAvatar(avatarUrl),
        criadoEm = criadoEm ?: "",
        familiaId = familiaId,
        email = email,
        ativo = ativo
    )
}

class RepositorioFinancasSupabase : RepositorioFinancas {
    override fun obterResumoFinanceiro(): Flow<ResumoFinanceiro> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val transacoes = try {
            ClienteSupabase.client.from("transacoes_financeiras")
                .select { filter { eq("familia_id", familiaId) } }
                .decodeList<TransacaoDto>()
        } catch (_: Exception) {
            emptyList()
        }

        val cofrinhos = try {
            ClienteSupabase.client.from("cofrinhos")
                .select { filter { eq("familia_id", familiaId) } }
                .decodeList<CofrinhoDto>()
        } catch (_: Exception) {
            emptyList()
        }

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
    }.catch {
        emit(ResumoFinanceiro(0.0, 0.0, 0.0, 0.0, 0.0))
    }.flowOn(Dispatchers.IO)

    override fun listarTransacoes(): Flow<List<TransacaoFinanceira>> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("transacoes_financeiras")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("data_vencimento", Order.DESCENDING)
                }
                .decodeList<TransacaoDto>()
        } catch (_: Exception) {
            emptyList()
        }

        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun listarCofrinhos(): Flow<List<Cofrinho>> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("cofrinhos")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("nome", Order.ASCENDING)
                }
                .decodeList<CofrinhoDto>()
        } catch (_: Exception) {
            emptyList()
        }

        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override suspend fun alternarStatusPagamento(transacaoId: String) = withContext(Dispatchers.IO) {
        try {
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
        } catch (_: Exception) {}
    }

    override suspend fun adicionarTransacao(transacao: TransacaoFinanceira) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = TransacaoDto(
                id = transacao.id.ifBlank { UUID.randomUUID().toString() },
                familiaId = familiaId,
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
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun depositarCofrinho(cofrinhoId: String, valor: Double) = withContext(Dispatchers.IO) {
        try {
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
        } catch (_: Exception) {}
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
        criadoEm = criadoEm ?: ""
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
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("tarefas_rotinas")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("esta_concluida", Order.ASCENDING)
                    order("atualizado_em", Order.DESCENDING)
                }
                .decodeList<TarefaRotinaDto>()
        } catch (_: Exception) {
            emptyList()
        }

        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun listarItensLoja(): Flow<List<ItemLojaPrivilegio>> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("loja_privilegios")
                .select {
                    filter {
                        eq("ativo", true)
                        eq("familia_id", familiaId)
                    }
                    order("custo_moedas", Order.ASCENDING)
                }
                .decodeList<ItemLojaDto>()
        } catch (_: Exception) {
            emptyList()
        }

        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override suspend fun alternarConclusaoTarefa(tarefaId: String): Int = withContext(Dispatchers.IO) {
        try {
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
        } catch (_: Exception) {
            0
        }
    }

    override suspend fun resgatarItemLoja(usuarioId: String, itemId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val item = ClienteSupabase.client.from("loja_privilegios")
                .select { filter { eq("id", itemId) } }
                .decodeSingleOrNull<ItemLojaDto>() ?: return@withContext false

            repositorioUsuarios.atualizarSaldoMoedas(usuarioId, -item.custoMoedas)

            ClienteSupabase.client.from("resgates_privilegios")
                .insert(
                    buildJsonObject {
                        put("privilegio_id", itemId)
                        put("usuario_id", usuarioId)
                        put("familia_id", familiaId)
                        put("moedas_gastas", item.custoMoedas)
                        put("status", "aprovado")
                    }
                )
            true
        } catch (_: Exception) {
            false
        }
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
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = TarefaRotinaDto(
                id = UUID.randomUUID().toString(),
                familiaId = familiaId,
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
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun adicionarItemLoja(
        titulo: String,
        descricao: String,
        custoMoedas: Int,
        icone: String?
    ) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = ItemLojaDto(
                id = UUID.randomUUID().toString(),
                familiaId = familiaId,
                titulo = titulo.trim(),
                descricao = descricao.trim(),
                custoMoedas = custoMoedas,
                icone = icone ?: "🎁",
                estoqueDisponivel = -1,
                ativo = true
            )
            ClienteSupabase.client.from("loja_privilegios").insert(dto)
        } catch (_: Exception) {}
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
        atualizadoEm = atualizadoEm ?: "",
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
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("eventos")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("data_evento", Order.ASCENDING)
                }
                .decodeList<EventoDto>()
        } catch (_: Exception) {
            emptyList()
        }

        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun listarConvidados(eventoId: String): Flow<List<Convidado>> = flow {
        val dtos = try {
            ClienteSupabase.client.from("convidados")
                .select {
                    filter { eq("evento_id", eventoId) }
                    order("nome", Order.ASCENDING)
                }
                .decodeList<ConvidadoDto>()
        } catch (_: Exception) {
            emptyList()
        }

        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun listarCotacoes(eventoId: String): Flow<List<Cotacao>> = flow {
        val dtos = try {
            ClienteSupabase.client.from("cotacoes")
                .select {
                    filter { eq("evento_id", eventoId) }
                    order("valor", Order.DESCENDING)
                }
                .decodeList<CotacaoDto>()
        } catch (_: Exception) {
            emptyList()
        }

        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override suspend fun atualizarStatusRsvp(convidadoId: String, novoStatus: StatusRsvp) = withContext(Dispatchers.IO) {
        try {
            val statusString = novoStatus.name.lowercase()
            ClienteSupabase.client.from("convidados")
                .update({ set("status", statusString) }) {
                    filter { eq("id", convidadoId) }
                }
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun adicionarCotacao(cotacao: Cotacao) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = CotacaoDto(
                id = cotacao.id.ifBlank { UUID.randomUUID().toString() },
                familiaId = familiaId,
                eventoId = cotacao.eventoId,
                itemOuServico = cotacao.itemOuServico,
                fornecedor = cotacao.fornecedor,
                valor = cotacao.valor,
                status = cotacao.status.name.lowercase(),
                linkAnexo = cotacao.linkAnexo,
                observacoes = cotacao.observacoes
            )
            ClienteSupabase.client.from("cotacoes").insert(dto)
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun adicionarEvento(
        nome: String,
        dataEvento: String,
        descricao: String?,
        orcamentoEstimado: Double
    ) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = EventoDto(
                id = UUID.randomUUID().toString(),
                familiaId = familiaId,
                nome = nome.trim(),
                dataEvento = dataEvento,
                descricao = descricao?.takeIf { it.isNotBlank() },
                orcamentoEstimado = orcamentoEstimado
            )
            ClienteSupabase.client.from("eventos").insert(dto)
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun adicionarConvidado(
        eventoId: String,
        nome: String,
        telefone: String?,
        restricoesAlimentares: String?
    ) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = ConvidadoDto(
                id = UUID.randomUUID().toString(),
                familiaId = familiaId,
                eventoId = eventoId,
                nome = nome.trim(),
                telefone = telefone?.takeIf { it.isNotBlank() },
                status = "pendente",
                restricoesAlimentares = restricoesAlimentares?.takeIf { it.isNotBlank() }
            )
            ClienteSupabase.client.from("convidados").insert(dto)
        } catch (_: Exception) {}
        Unit
    }

    private fun EventoDto.paraDominio() = Evento(
        id = id,
        nome = nome,
        dataEvento = dataEvento,
        descricao = descricao,
        orcamentoEstimado = orcamentoEstimado,
        criadoEm = criadoEm ?: ""
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
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("inventario_itens")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("nome", Order.ASCENDING)
                }
                .decodeList<ItemInventarioDto>()
        } catch (_: Exception) {
            emptyList()
        }
        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun buscarItens(termo: String): Flow<List<ItemInventario>> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("inventario_itens")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("nome", Order.ASCENDING)
                }
                .decodeList<ItemInventarioDto>()
        } catch (_: Exception) {
            emptyList()
        }

        val filtrados = if (termo.isBlank()) dtos else dtos.filter {
            it.nome.contains(termo, ignoreCase = true) || it.localizacao.contains(termo, ignoreCase = true)
        }
        emit(filtrados.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun listarContatos(): Flow<List<ContatoAgenda>> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("agenda_contatos")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("eh_emergencia", Order.DESCENDING)
                    order("nome", Order.ASCENDING)
                }
                .decodeList<ContatoDto>()
        } catch (_: Exception) {
            emptyList()
        }

        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun listarHistoricoManutencoes(): Flow<List<HistoricoManutencao>> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("historico_manutencoes")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("data_servico", Order.DESCENDING)
                }
                .decodeList<HistoricoManutencaoDto>()
        } catch (_: Exception) {
            emptyList()
        }

        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override suspend fun sincronizarContatos(contatos: List<ContatoAgenda>): Int = withContext(Dispatchers.IO) {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = contatos.map {
            ContatoDto(
                id = it.id.ifBlank { UUID.randomUUID().toString() },
                familiaId = familiaId,
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
    private suspend fun obterMapaUsuariosFamilia(familiaId: String): Map<String, UsuarioDto> {
        return try {
            ClienteSupabase.client.from("usuarios")
                .select { filter { eq("familia_id", familiaId) } }
                .decodeList<UsuarioDto>()
                .associateBy { it.id }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override fun listarCheckinsHumor(): Flow<List<CheckinSaude>> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("saude_checkins")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("criado_em", Order.DESCENDING)
                }
                .decodeList<CheckinSaudeDto>()
        } catch (_: Exception) {
            emptyList()
        }
        val usuarios = obterMapaUsuariosFamilia(familiaId)
        emit(dtos.map { dto ->
            CheckinSaude(
                id = dto.id,
                usuarioId = dto.usuarioId,
                nomeUsuario = usuarios[dto.usuarioId]?.nome?.split(" ")?.firstOrNull() ?: "Membro",
                nivelHumor = dto.nivelHumor,
                notasDiario = dto.notasDiario,
                criadoEm = dto.criadoEm ?: ""
            )
        })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun listarFichasMedicas(): Flow<List<FichaMedica>> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("saude_fichas_medicas")
                .select { filter { eq("familia_id", familiaId) } }
                .decodeList<FichaMedicaDto>()
        } catch (_: Exception) {
            emptyList()
        }
        val usuarios = obterMapaUsuariosFamilia(familiaId)
        emit(dtos.map { dto ->
            dto.paraDominio(usuarios[dto.usuarioId]?.nome ?: "Membro da Família")
        })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun listarLembretesMedicamento(): Flow<List<MedicamentoLembrete>> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("saude_medicamentos")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("horario", Order.ASCENDING)
                }
                .decodeList<MedicamentoDto>()
        } catch (_: Exception) {
            emptyList()
        }

        val usuarios = obterMapaUsuariosFamilia(familiaId)

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
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override suspend fun registrarCheckin(checkin: CheckinSaude) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = CheckinSaudeDto(
                id = checkin.id.ifBlank { UUID.randomUUID().toString() },
                familiaId = familiaId,
                usuarioId = checkin.usuarioId,
                nivelHumor = checkin.nivelHumor,
                notasDiario = checkin.notasDiario
            )
            ClienteSupabase.client.from("saude_checkins").insert(dto)
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun alternarMedicamentoTomado(medicamentoId: String) = withContext(Dispatchers.IO) {
        try {
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
        } catch (_: Exception) {}
        Unit
    }

    override fun listarVacinas(): Flow<List<VacinaRegistro>> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("saude_vacinacao")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("data_aplicacao", Order.DESCENDING)
                }
                .decodeList<VacinaDto>()
        } catch (_: Exception) {
            emptyList()
        }

        val usuarios = obterMapaUsuariosFamilia(familiaId)

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
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun listarConsultasExames(): Flow<List<ConsultaExameRegistro>> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("saude_consultas_exames")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("data_agendada", Order.ASCENDING)
                }
                .decodeList<ConsultaExameDto>()
        } catch (_: Exception) {
            emptyList()
        }

        val usuarios = obterMapaUsuariosFamilia(familiaId)

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
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun listarMetricasVitais(): Flow<List<MetricaVitalRegistro>> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("saude_metricas_vitais")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("data_medicao", Order.DESCENDING)
                }
                .decodeList<MetricaVitalDto>()
        } catch (_: Exception) {
            emptyList()
        }

        val usuarios = obterMapaUsuariosFamilia(familiaId)

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
                dataMedicao = dto.dataMedicao ?: ""
            )
        })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override suspend fun adicionarVacina(vacina: VacinaRegistro) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = VacinaDto(
                id = vacina.id.ifBlank { UUID.randomUUID().toString() },
                familiaId = familiaId,
                usuarioId = vacina.usuarioId,
                nomeVacina = vacina.nomeVacina,
                dose = vacina.dose,
                dataAplicacao = vacina.dataAplicacao,
                dataProximaDose = vacina.dataProximaDose,
                lote = vacina.lote,
                localAplicacao = vacina.localAplicacao
            )
            ClienteSupabase.client.from("saude_vacinacao").insert(dto)
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun adicionarConsultaExame(consulta: ConsultaExameRegistro) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = ConsultaExameDto(
                id = consulta.id.ifBlank { UUID.randomUUID().toString() },
                familiaId = familiaId,
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
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun alternarConsultaConcluida(id: String) = withContext(Dispatchers.IO) {
        try {
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
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun atualizarConsultaExame(consulta: ConsultaExameRegistro) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = ConsultaExameDto(
                id = consulta.id,
                familiaId = familiaId,
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
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun excluirConsultaExame(id: String) = withContext(Dispatchers.IO) {
        try {
            ClienteSupabase.client.from("saude_consultas_exames").delete {
                filter { eq("id", id) }
            }
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun adicionarMetricaVital(metrica: MetricaVitalRegistro) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = MetricaVitalDto(
                id = metrica.id.ifBlank { UUID.randomUUID().toString() },
                familiaId = familiaId,
                usuarioId = metrica.usuarioId,
                tipoMetrica = metrica.tipoMetrica,
                valorPrincipal = metrica.valorPrincipal,
                valorSecundario = metrica.valorSecundario,
                unidade = metrica.unidade,
                observacao = metrica.observacao,
                dataMedicao = metrica.dataMedicao.takeIf { it.matches(Regex("^\\d{4}-\\d{2}-\\d{2}.*")) }
            )
            ClienteSupabase.client.from("saude_metricas_vitais").insert(dto)
        } catch (_: Exception) {}
        Unit
    }

    private fun FichaMedicaDto.paraDominio(nomeMembro: String) = FichaMedica(
        id = id,
        usuarioId = usuarioId,
        nomeUsuario = nomeMembro,
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
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("cofre_credenciais")
                .select {
                    filter {
                        eq("familia_id", familiaId)
                        if (!incluirMaster) eq("nivel", "compartilhado")
                    }
                    order("titulo", Order.ASCENDING)
                }
                .decodeList<CredencialCofreDto>()
        } catch (_: Exception) {
            emptyList()
        }
        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun obterRedeWifiFamiliar(): Flow<RedeWifiFamiliar> = flow {
        val familiaId = obterFamiliaAtivaSegura()
        val chaveTenant = if (familiaId == GerenciadorSessao.DEFAULT_FAMILIA_ID) {
            "WIFI_PRINCIPAL"
        } else {
            "WIFI_PRINCIPAL:$familiaId"
        }

        val config = try {
            val todas = ClienteSupabase.client.from("configuracoes_casa")
                .select()
                .decodeList<ConfiguracaoCasaDto>()
            todas.firstOrNull { it.id == chaveTenant }
                ?: todas.firstOrNull { it.id == "WIFI_PRINCIPAL" && (it.familiaId == null || it.familiaId == familiaId) }
        } catch (_: Exception) {
            null
        }

        var ssid = "Familia_OS_5G"
        var senha = "FamiliaSegura2026"
        var seguranca = "WPA2/WPA3"
        var visitante = false

        val elemento = config?.valor
        if (elemento is JsonObject) {
            ssid = elemento["nomeRedeSsid"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: ssid
            senha = elemento["senhaRede"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: senha
            seguranca = elemento["tipoSeguranca"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: seguranca
            visitante = elemento["ehRedeVisitantes"]?.jsonPrimitive?.booleanOrNull ?: false
        } else if (elemento is JsonPrimitive) {
            val partes = elemento.contentOrNull?.split("|") ?: emptyList()
            ssid = partes.getOrNull(0)?.takeIf { it.isNotBlank() } ?: ssid
            senha = partes.getOrNull(1)?.takeIf { it.isNotBlank() } ?: senha
            seguranca = partes.getOrNull(2)?.takeIf { it.isNotBlank() } ?: seguranca
        }

        emit(
            RedeWifiFamiliar(
                nomeRedeSsid = ssid,
                senhaRede = senha,
                tipoSeguranca = seguranca,
                ehRedeVisitantes = visitante
            )
        )
    }.catch {
        emit(
            RedeWifiFamiliar(
                nomeRedeSsid = "Familia_OS_5G",
                senhaRede = "FamiliaSegura2026",
                tipoSeguranca = "WPA2/WPA3",
                ehRedeVisitantes = false
            )
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun salvarRedeWifiFamiliar(wifi: RedeWifiFamiliar) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val chaveTenant = if (familiaId == GerenciadorSessao.DEFAULT_FAMILIA_ID) {
                "WIFI_PRINCIPAL"
            } else {
                "WIFI_PRINCIPAL:$familiaId"
            }
            val valorJson = buildJsonObject {
                put("nomeRedeSsid", wifi.nomeRedeSsid.trim())
                put("senhaRede", wifi.senhaRede.trim())
                put("tipoSeguranca", wifi.tipoSeguranca.trim())
                put("ehRedeVisitantes", wifi.ehRedeVisitantes)
            }

            val dto = ConfiguracaoCasaDto(
                id = chaveTenant,
                valor = valorJson,
                familiaId = familiaId
            )
            ClienteSupabase.client.from("configuracoes_casa").upsert(dto) {
                onConflict = "id"
            }
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun excluirCredencial(credencialId: String) = withContext(Dispatchers.IO) {
        try {
            ClienteSupabase.client.from("cofre_credenciais").delete {
                filter { eq("id", credencialId) }
            }
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun adicionarCredencial(credencial: CredencialCofre) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = CredencialCofreDto(
                id = credencial.id.ifBlank { UUID.randomUUID().toString() },
                familiaId = familiaId,
                titulo = credencial.titulo,
                nivel = credencial.nivel.name.lowercase(),
                loginUsuario = credencial.loginUsuario,
                senhaCriptografada = credencial.senhaCriptografada,
                urlAcesso = credencial.urlAcesso,
                notasSeguras = credencial.notasSeguras,
                criadoPor = credencial.criadoPor
            )
            ClienteSupabase.client.from("cofre_credenciais").insert(dto)
        } catch (_: Exception) {}
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
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("mural_conquistas")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("data_conquista", Order.DESCENDING)
                }
                .decodeList<ConquistaMuralDto>()
        } catch (_: Exception) {
            emptyList()
        }
        emit(dtos.map { it.paraDominio() })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override fun listarFotosAlbum(): Flow<List<FotoAlbum>> = flow {
        emit(emptyList())
    }

    override suspend fun adicionarReacao(conquistaId: String) {}

    override suspend fun adicionarConquista(
        titulo: String,
        descricao: String?,
        usuarioId: String?
    ) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = ConquistaMuralDto(
                id = UUID.randomUUID().toString(),
                familiaId = familiaId,
                titulo = titulo.trim(),
                descricao = descricao?.takeIf { it.isNotBlank() },
                usuarioId = usuarioId,
                dataConquista = java.time.LocalDate.now().toString()
            )
            ClienteSupabase.client.from("mural_conquistas").insert(dto)
        } catch (_: Exception) {}
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
        val familiaId = obterFamiliaAtivaSegura()
        val dtos = try {
            ClienteSupabase.client.from("compras_recibos")
                .select {
                    filter { eq("familia_id", familiaId) }
                    order("criado_em", Order.DESCENDING)
                }
                .decodeList<ReciboCompraDto>()
        } catch (_: Exception) {
            emptyList()
        }

        val itensDtos = try {
            ClienteSupabase.client.from("compras_itens_extraidos")
                .select {
                    filter { eq("familia_id", familiaId) }
                }
                .decodeList<ItemCompraExtraidoDto>()
        } catch (_: Exception) {
            emptyList()
        }

        val itensPorRecibo = itensDtos
            .map { it.paraDominio() }
            .groupBy { it.reciboId }

        emit(dtos.map { it.paraDominio(itensPorRecibo[it.id] ?: emptyList()) })
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

    override suspend fun simularOcrCupomFiscal(nomeSupermercado: String, valorTotal: Double) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = ReciboCompraDto(
                id = UUID.randomUUID().toString(),
                familiaId = familiaId,
                fornecedor = nomeSupermercado,
                dataCompra = java.time.LocalDate.now().toString(),
                valorTotal = valorTotal
            )
            ClienteSupabase.client.from("compras_recibos").insert(dto)
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun extrairNotaComIa(
        bytesArquivo: ByteArray,
        mimeType: String,
        nomeArquivo: String
    ): ReciboCompra = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://ai-familia.martel.page/api/ocr/receipt",
            "https://familia.martel.page/api/ocr/receipt"
        )
        var ultimoErro = "Não foi possível comunicar com o serviço de IA."

        for (urlStr in urls) {
            try {
                val boundary = "----FamiliaOSBoundary" + System.currentTimeMillis()
                val conn = (java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    doInput = true
                    useCaches = false
                    connectTimeout = 25000
                    readTimeout = 60000
                    setRequestProperty("User-Agent", ClienteSupabase.USER_AGENT_PADRAO)
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                }

                conn.outputStream.use { out ->
                    val writer = out.writer(Charsets.UTF_8)
                    writer.append("--$boundary\r\n")
                    writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"$nomeArquivo\"\r\n")
                    writer.append("Content-Type: $mimeType\r\n\r\n")
                    writer.flush()
                    out.write(bytesArquivo)
                    out.flush()
                    writer.append("\r\n--$boundary--\r\n")
                    writer.flush()
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    val resposta = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val json = org.json.JSONObject(resposta)
                    val fornecedor = json.optString("fornecedor", "").takeIf { it.isNotBlank() && it != "null" }
                        ?: "Estabelecimento Comercial"
                    val dataCompra = json.optString("data_compra", "").takeIf { it.isNotBlank() && it != "null" }
                        ?: java.time.LocalDate.now().toString()
                    val itensJson = json.optJSONArray("itens")
                    val itensExtraidos = mutableListOf<ItemCompraExtraido>()
                    if (itensJson != null) {
                        for (i in 0 until itensJson.length()) {
                            val obj = itensJson.optJSONObject(i) ?: continue
                            val nomeProd = obj.optString("nome_produto", "").trim()
                            if (nomeProd.isNotEmpty()) {
                                val qtd = obj.optDouble("quantidade", 1.0).let { if (it.isNaN() || it <= 0.0) 1.0 else it }
                                val valUnit = obj.optDouble("valor_unitario", 0.0).let { if (it.isNaN() || it < 0.0) 0.0 else it }
                                val cat = obj.optString("categoria_sugerida", "Alimentação").takeIf { it.isNotBlank() && it != "null" }
                                    ?: "Alimentação"
                                itensExtraidos.add(
                                    ItemCompraExtraido(
                                        id = UUID.randomUUID().toString(),
                                        reciboId = "",
                                        nomeProduto = nomeProd,
                                        quantidade = qtd,
                                        valorUnitario = valUnit,
                                        categoriaSugerida = cat
                                    )
                                )
                            }
                        }
                    }
                    val somaItens = itensExtraidos.sumOf { it.quantidade * it.valorUnitario }
                    val valorTotal = json.optDouble("valor_total", 0.0).let {
                        if (it.isNaN() || it <= 0.0) somaItens else it
                    }
                    return@withContext ReciboCompra(
                        id = UUID.randomUUID().toString(),
                        fornecedor = fornecedor,
                        dataCompra = dataCompra,
                        valorTotal = valorTotal,
                        itens = itensExtraidos
                    )
                } else {
                    val errTxt = try {
                        conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "HTTP $code"
                    } catch (_: Exception) {
                        "HTTP $code"
                    }
                    ultimoErro = errTxt
                }
            } catch (e: Exception) {
                ultimoErro = e.message ?: "Erro de conexão com a IA"
            }
        }

        throw IllegalStateException(ultimoErro)
    }

    private fun fazerUploadImagemRecibo(
        familiaId: String,
        bytesImagem: ByteArray?,
        mimeTypeImagem: String?
    ): String? {
        if (bytesImagem == null || bytesImagem.isEmpty()) return null
        return try {
            val ext = if (mimeTypeImagem == "application/pdf") "pdf" else "jpg"
            val caminho = "recibos/$familiaId/${System.currentTimeMillis()}_cupom.$ext"
            val urlUpload = "${ClienteSupabase.SUPABASE_URL}/storage/v1/object/familia-os-recibos/$caminho"
            val conn = (java.net.URL(urlUpload).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 25000
                setRequestProperty("apikey", ClienteSupabase.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${ClienteSupabase.SUPABASE_ANON_KEY}")
                setRequestProperty("User-Agent", ClienteSupabase.USER_AGENT_PADRAO)
                setRequestProperty("Content-Type", mimeTypeImagem ?: "image/jpeg")
                setRequestProperty("x-upsert", "true")
            }
            conn.outputStream.use { it.write(bytesImagem) }
            if (conn.responseCode in 200..299) {
                "${ClienteSupabase.SUPABASE_URL}/storage/v1/object/public/familia-os-recibos/$caminho"
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizarDataCompraIso(dataBruta: String?): String {
        val limpa = dataBruta?.trim().orEmpty()
        if (limpa.isEmpty() || limpa == "null") {
            return java.time.LocalDate.now().toString()
        }
        // Formato YYYY-MM-DD (ou YYYY-MM-DDTHH:mm:ss)
        val regexIso = Regex("^(\\d{4}-\\d{2}-\\d{2}).*")
        regexIso.matchEntire(limpa)?.let { return it.groupValues[1] }

        // Formato DD/MM/YYYY ou DD-MM-YYYY
        val regexBr = Regex("^(\\d{2})[/\\-](\\d{2})[/\\-](\\d{4}).*")
        regexBr.matchEntire(limpa)?.let {
            return "${it.groupValues[3]}-${it.groupValues[2]}-${it.groupValues[1]}"
        }

        return java.time.LocalDate.now().toString()
    }

    override suspend fun salvarReciboComItens(
        fornecedor: String,
        dataCompra: String,
        valorTotal: Double,
        itens: List<ItemCompraExtraido>,
        lancarEmFinancas: Boolean,
        bytesImagem: ByteArray?,
        mimeTypeImagem: String?
    ) = withContext(Dispatchers.IO) {
        val familiaId = obterFamiliaAtivaSegura()
        val reciboId = UUID.randomUUID().toString()
        val dataFormatada = normalizarDataCompraIso(dataCompra)
        val urlImagem = fazerUploadImagemRecibo(familiaId, bytesImagem, mimeTypeImagem)

        val reciboDto = ReciboCompraDto(
            id = reciboId,
            familiaId = familiaId,
            fornecedor = fornecedor.trim().ifBlank { "Supermercado" },
            dataCompra = dataFormatada,
            valorTotal = valorTotal,
            caminhoImagemRecibo = urlImagem
        )
        ClienteSupabase.client.from("compras_recibos").insert(reciboDto)

        val itensValidos = itens.filter { it.nomeProduto.isNotBlank() }
        if (itensValidos.isNotEmpty()) {
            val dtosItens = itensValidos.map { item ->
                ItemCompraExtraidoDto(
                    id = UUID.randomUUID().toString(),
                    familiaId = familiaId,
                    reciboId = reciboId,
                    nomeProduto = item.nomeProduto.trim(),
                    quantidade = if (item.quantidade > 0.0) item.quantidade else 1.0,
                    valorUnitario = item.valorUnitario,
                    categoriaSugerida = item.categoriaSugerida ?: "Alimentação"
                )
            }
            ClienteSupabase.client.from("compras_itens_extraidos").insert(dtosItens)
        }

        if (lancarEmFinancas && valorTotal > 0.0) {
            try {
                val transacaoDto = TransacaoDto(
                    id = UUID.randomUUID().toString(),
                    familiaId = familiaId,
                    descricao = "Compra (${itensValidos.size} itens) - ${fornecedor.trim()}",
                    tipo = "despesa_variavel",
                    valor = valorTotal,
                    dataVencimento = dataFormatada,
                    status = "pago"
                )
                ClienteSupabase.client.from("transacoes_financeiras").insert(transacaoDto)
            } catch (_: Exception) {}
        }
        Unit
    }

    override suspend fun adicionarItemAoRecibo(
        reciboId: String,
        nomeProduto: String,
        quantidade: Double,
        valorUnitario: Double,
        categoriaSugerida: String
    ) = withContext(Dispatchers.IO) {
        try {
            val familiaId = obterFamiliaAtivaSegura()
            val dto = ItemCompraExtraidoDto(
                id = UUID.randomUUID().toString(),
                familiaId = familiaId,
                reciboId = reciboId,
                nomeProduto = nomeProduto.trim(),
                quantidade = if (quantidade > 0.0) quantidade else 1.0,
                valorUnitario = valorUnitario,
                categoriaSugerida = categoriaSugerida.ifBlank { "Geral" }
            )
            ClienteSupabase.client.from("compras_itens_extraidos").insert(dto)
            recalcularValorTotalRecibo(reciboId)
        } catch (_: Exception) {}
        Unit
    }

    private suspend fun recalcularValorTotalRecibo(reciboId: String) {
        try {
            val itensAtuais = ClienteSupabase.client.from("compras_itens_extraidos")
                .select { filter { eq("recibo_id", reciboId) } }
                .decodeList<ItemCompraExtraidoDto>()
            if (itensAtuais.isNotEmpty()) {
                val novoTotal = itensAtuais.sumOf { it.quantidade * it.valorUnitario }
                ClienteSupabase.client.from("compras_recibos").update({
                    set("valor_total", novoTotal)
                }) {
                    filter { eq("id", reciboId) }
                }
            }
        } catch (_: Exception) {}
    }

    override suspend fun atualizarItemRecibo(
        itemId: String,
        reciboId: String,
        nomeProduto: String,
        quantidade: Double,
        valorUnitario: Double,
        categoriaSugerida: String
    ) = withContext(Dispatchers.IO) {
        val qtdFinal = if (quantidade > 0.0) quantidade else 1.0
        ClienteSupabase.client.from("compras_itens_extraidos").update({
            set("nome_produto", nomeProduto.trim())
            set("quantidade", qtdFinal)
            set("valor_unitario", valorUnitario)
            set("categoria_sugerida", categoriaSugerida.ifBlank { "Geral" })
        }) {
            filter { eq("id", itemId) }
        }
        recalcularValorTotalRecibo(reciboId)
        Unit
    }

    override suspend fun atualizarCabecalhoRecibo(
        reciboId: String,
        fornecedor: String,
        dataCompra: String,
        valorTotal: Double
    ) = withContext(Dispatchers.IO) {
        val dataFormatada = normalizarDataCompraIso(dataCompra)
        ClienteSupabase.client.from("compras_recibos").update({
            set("fornecedor", fornecedor.trim().ifBlank { "Supermercado" })
            set("data_compra", dataFormatada)
            set("valor_total", valorTotal)
        }) {
            filter { eq("id", reciboId) }
        }
        Unit
    }

    override suspend fun excluirRecibo(reciboId: String) = withContext(Dispatchers.IO) {
        try {
            ClienteSupabase.client.from("compras_itens_extraidos").delete {
                filter { eq("recibo_id", reciboId) }
            }
            ClienteSupabase.client.from("compras_recibos").delete {
                filter { eq("id", reciboId) }
            }
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun excluirItemRecibo(itemId: String) = withContext(Dispatchers.IO) {
        try {
            ClienteSupabase.client.from("compras_itens_extraidos").delete {
                filter { eq("id", itemId) }
            }
        } catch (_: Exception) {}
        Unit
    }

    private fun ItemCompraExtraidoDto.paraDominio() = ItemCompraExtraido(
        id = id,
        reciboId = reciboId,
        nomeProduto = nomeProduto,
        quantidade = quantidade,
        valorUnitario = valorUnitario,
        categoriaSugerida = categoriaSugerida
    )

    private fun ReciboCompraDto.paraDominio(itens: List<ItemCompraExtraido> = emptyList()) = ReciboCompra(
        id = id,
        fornecedor = fornecedor,
        dataCompra = dataCompra,
        valorTotal = valorTotal,
        caminhoImagemRecibo = caminhoImagemRecibo,
        itens = itens,
        criadoEm = criadoEm ?: ""
    )
}
