package br.com.raevan.familialm.dados.remoto.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsuarioDto(
    val id: String,
    val nome: String,
    val perfil: String,
    @SerialName("saldo_moedas") val saldoMoedas: Int = 0,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val ativo: Boolean = true,
    @SerialName("criado_em") val criadoEm: String = ""
)

@Serializable
data class TransacaoDto(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String? = null,
    @SerialName("categoria_id") val categoriaId: String? = null,
    val tipo: String,
    val descricao: String,
    val valor: Double,
    @SerialName("data_vencimento") val dataVencimento: String,
    @SerialName("data_pagamento") val dataPagamento: String? = null,
    val status: String = "pendente",
    val observacoes: String? = null,
    @SerialName("criado_em") val criadoEm: String = ""
)

@Serializable
data class CofrinhoDto(
    val id: String,
    val nome: String,
    @SerialName("meta_valor") val metaValor: Double,
    @SerialName("saldo_atual") val saldoAtual: Double = 0.0,
    @SerialName("data_limite") val dataLimite: String? = null,
    val icone: String? = null
)

@Serializable
data class TarefaRotinaDto(
    val id: String,
    val titulo: String,
    @SerialName("responsavel_id") val responsavelId: String? = null,
    val recorrencia: String = "diaria",
    @SerialName("dias_semana") val diasSemana: String? = null,
    @SerialName("dia_mes") val diaMes: Int? = null,
    @SerialName("moedas_recompensa") val moedasRecompensa: Int = 5,
    @SerialName("esta_concluida") val estaConcluida: Boolean = false,
    @SerialName("ultima_conclusao_data") val ultimaConclusaoData: String? = null,
    @SerialName("aberta_para_todos") val abertaParaTodos: Boolean = false,
    @SerialName("atualizado_em") val atualizadoEm: String = ""
)

@Serializable
data class ItemLojaDto(
    val id: String,
    val titulo: String,
    @SerialName("custo_moedas") val custoMoedas: Int,
    val icone: String? = null,
    @SerialName("estoque_disponivel") val estoqueDisponivel: Int? = null,
    val descricao: String? = null,
    val ativo: Boolean = true
)

@Serializable
data class EventoDto(
    val id: String,
    val nome: String,
    @SerialName("data_evento") val dataEvento: String,
    @SerialName("data_fim") val dataFim: String? = null,
    val tipo: String? = "outro",
    val local: String? = null,
    val destino: String? = null,
    val descricao: String? = null,
    @SerialName("orcamento_estimado") val orcamentoEstimado: Double? = null,
    @SerialName("criado_em") val criadoEm: String = ""
)

@Serializable
data class ConvidadoDto(
    val id: String,
    @SerialName("evento_id") val eventoId: String,
    val nome: String,
    val telefone: String? = null,
    val status: String = "pendente",
    val adultos: Int = 1,
    val criancas: Int = 0,
    @SerialName("restricoes_alimentares") val restricoesAlimentares: String? = null,
    @SerialName("itens_que_leva") val itensQueLeva: String? = null
)

@Serializable
data class CotacaoDto(
    val id: String,
    @SerialName("evento_id") val eventoId: String? = null,
    @SerialName("item_ou_servico") val itemOuServico: String,
    val fornecedor: String,
    val valor: Double,
    val status: String = "em_analise",
    val pago: Boolean = false,
    val categoria: String? = "geral",
    @SerialName("link_anexo") val linkAnexo: String? = null,
    val observacoes: String? = null
)

@Serializable
data class ContatoDto(
    val id: String,
    val nome: String,
    val especialidade: String? = null,
    val telefone: String,
    val email: String? = null,
    @SerialName("foto_uri") val fotoUri: String? = null,
    @SerialName("eh_emergencia") val ehEmergencia: Boolean = false,
    val origem: String? = "agenda_telefone",
    val observacoes: String? = null
)

@Serializable
data class ItemInventarioDto(
    val id: String,
    val nome: String,
    val localizacao: String,
    val quantidade: Int = 1,
    @SerialName("data_compra") val dataCompra: String? = null,
    @SerialName("data_fim_garantia") val dataFimGarantia: String? = null,
    @SerialName("caminho_nota_fiscal") val caminhoNotaFiscal: String? = null,
    @SerialName("caminho_manual") val caminhoManual: String? = null
)

@Serializable
data class HistoricoManutencaoDto(
    val id: String,
    @SerialName("item_id") val itemId: String? = null,
    @SerialName("prestador_id") val prestadorId: String? = null,
    val descricao: String,
    @SerialName("data_servico") val dataServico: String,
    val custo: Double? = null,
    @SerialName("data_proxima_manutencao") val dataProximaManutencao: String? = null
)

@Serializable
data class CheckinSaudeDto(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("nivel_humor") val nivelHumor: Int,
    @SerialName("notas_diario") val notasDiario: String? = null,
    @SerialName("criado_em") val criadoEm: String = ""
)

@Serializable
data class FichaMedicaDto(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("tipo_sanguineo") val tipoSanguineo: String? = null,
    val alergias: String? = null,
    @SerialName("medicamentos_uso_continuo") val medicamentosUsoContinuo: String? = null,
    @SerialName("numero_plano_saude") val numeroPlanoSaude: String? = null,
    @SerialName("carteirinha_plano_url") val carteirinhaPlanoUrl: String? = null,
    @SerialName("numero_plano_odontologico") val numeroPlanoOdontologico: String? = null,
    @SerialName("carteirinha_odonto_url") val carteirinhaOdontoUrl: String? = null,
    @SerialName("resumo_clinico_ia") val resumoClinicoIa: String? = null,
    @SerialName("orientacoes_medico_ia") val orientacoesMedicoIa: String? = null,
    @SerialName("atualizado_ia_em") val atualizadoIaEm: String? = null
)

@Serializable
data class MedicamentoDto(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("nome_remedio") val nomeRemedio: String,
    val dosagem: String,
    val horario: String,
    @SerialName("foi_tomado_hoje") val foiTomadoHoje: Boolean = false,
    @SerialName("criado_em") val criadoEm: String = ""
)

@Serializable
data class VacinaDto(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("nome_vacina") val nomeVacina: String,
    val dose: String = "1ª Dose",
    @SerialName("data_aplicacao") val dataAplicacao: String,
    @SerialName("data_proxima_dose") val dataProximaDose: String? = null,
    val lote: String? = null,
    @SerialName("local_aplicacao") val localAplicacao: String? = null,
    val observacoes: String? = null
)

@Serializable
data class ConsultaExameDto(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String,
    val tipo: String,
    val titulo: String,
    @SerialName("especialista_ou_clinica") val especialistaOuClinica: String? = null,
    @SerialName("data_agendada") val dataAgendada: String? = null,
    @SerialName("data_retorno") val dataRetorno: String? = null,
    val observacoes: String? = null,
    val resultado: String? = null,
    val status: String? = "agendado",
    @SerialName("contato_id") val contatoId: String? = null,
    @SerialName("evento_id") val eventoId: String? = null,
    val concluido: Boolean = false
)

@Serializable
data class MetricaVitalDto(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("tipo_metrica") val tipoMetrica: String,
    @SerialName("valor_principal") val valorPrincipal: Double,
    @SerialName("valor_secundario") val valorSecundario: Double? = null,
    val unidade: String,
    val observacao: String? = null,
    @SerialName("data_medicao") val dataMedicao: String
)

@Serializable
data class CredencialCofreDto(
    val id: String,
    val titulo: String,
    val nivel: String,
    @SerialName("login_usuario") val loginUsuario: String? = null,
    @SerialName("senha_criptografada") val senhaCriptografada: String,
    @SerialName("url_acesso") val urlAcesso: String? = null,
    @SerialName("notas_seguras") val notasSeguras: String? = null,
    @SerialName("criado_por") val criadoPor: String? = null
)

@Serializable
data class ConquistaMuralDto(
    val id: String,
    val titulo: String,
    val descricao: String?,
    @SerialName("usuario_id") val usuarioId: String? = null,
    @SerialName("data_conquista") val dataConquista: String,
    @SerialName("caminho_foto") val caminhoFoto: String? = null
)

@Serializable
data class ReciboCompraDto(
    val id: String,
    val fornecedor: String? = null,
    @SerialName("data_compra") val dataCompra: String? = null,
    @SerialName("valor_total") val valorTotal: Double = 0.0,
    @SerialName("caminho_imagem_recibo") val caminhoImagemRecibo: String? = null,
    @SerialName("criado_em") val criadoEm: String = ""
)

@Serializable
data class ItemCompraExtraidoDto(
    val id: String,
    @SerialName("recibo_id") val reciboId: String,
    @SerialName("nome_produto") val nomeProduto: String,
    val quantidade: Double = 1.0,
    @SerialName("valor_unitario") val valorUnitario: Double,
    @SerialName("categoria_sugerida") val categoriaSugerida: String? = null
)
