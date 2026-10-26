# Documentação de Arquitetura e Estrutura

## 1. Estrutura de Pastas Sugerida (Android)

```
app/src/main/java/com/homehub/os/
├── core/               # Extensões, utilitários, temas (Compose), constantes.
├── di/                 # Módulos de injeção de dependência (Hilt).
├── domain/             # (Regras de negócio)
│   ├── models/         # Data classes do domínio (ex: Transacao.kt).
│   ├── repository/     # Interfaces dos repositórios.
│   └── usecases/       # Casos de uso (ex: ObterSaldoFamiliarUseCase.kt).
├── data/               # (Dados)
│   ├── local/          # Room DB, DataStore, DAOs.
│   ├── remote/         # Supabase Client, chamadas de API.
│   └── repository/     # Implementações do repository do domínio.
└── presentation/       # (UI)
    ├── auth/           # Telas de login/magic link.
    ├── dashboard/      # Tela inicial principal.
    ├── financas/       # Telas do módulo financeiro.
    ├── cofre/          # Telas do cofre de senhas (exige biometria).
    └── componentes/    # Composables reutilizáveis (botões, cards).

```

## 2. Modelagem Básica (Supabase/PostgreSQL Schema Overview)

*(O app deve consumir essas tabelas usando PostgREST via Supabase Kotlin SDK. Todas as tabelas e colunas devem ser em pt-BR)*

* **`usuarios`**: Perfis dos usuários, vinculados ao Supabase Auth.

* **`transacoes_financeiras`**: Receitas, despesas fixas, assinaturas e status de pagamento.

* **`eventos`**: Tabela de organização de eventos e lista de convidados.

* **`cofre_credenciais`**: Senhas armazenadas (dados sensíveis devem estar criptografados E2E ou usando Vault do PG).

* **`tarefas_rotinas`**: Tarefas diárias e pontuação (gamificação).

* **`inventario_itens`**: Itens mapeados na casa, locais, links de notas fiscais e manuais.

* **`saude_checkins`**: Registros de humor e anotações.

## 3. Fluxo de Autenticação

* O app utiliza o Supabase Auth.

* Fluxo principal: Login sem senha (Magic Link via Email) ou Login Social (Google/Apple).

* Sessões mantidas ativamente via refresh tokens do Supabase SDK.