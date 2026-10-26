# 📱 HomeHub OS - Android App

O aplicativo móvel nativo do **HomeHub OS**, a plataforma inteligente e centralizada para gestão da rotina, finanças, segurança e memórias da família.

## 🎯 Objetivo
Prover uma experiência nativa, fluida e com acesso offline rápido para os moradores da casa. O app mobile é a principal interface de uso diário, utilizando os sensores do aparelho (câmera, biometria) e integrações nativas (Notificações Push, Autofill, Calendário) para facilitar a vida familiar.

## 🛠 Stack Tecnológica
*   **Linguagem:** Kotlin
*   **Interface (UI):** Jetpack Compose (Material Design 3)
*   **Arquitetura:** Clean Architecture + MVVM (Model-View-ViewModel)
*   **Injeção de Dependências:** Hilt (ou Koin)
*   **Backend as a Service (BaaS):** Supabase (via Supabase Kotlin SDK)
*   **Assincronismo:** Kotlin Coroutines & Flow
*   **Navegação:** Jetpack Navigation Compose
*   **Push Notifications:** Firebase Cloud Messaging (FCM)

## 🔗 Ecossistema e Integrações
Este app faz parte de um ecossistema maior, conectando-se a:
1.  **Supabase:** Banco PostgreSQL, Auth, Storage e Edge Functions.
2.  **Rede Local:** Home Assistant (IoT) e Frigate (Câmeras via WebRTC) com acesso remoto via Cloudflare Tunnel.
3.  **Inteligência Artificial:** OpenHands AI para buscas no inventário e Gemini/GPT para processamento de cupons fiscais.
4.  **Mensageria:** Evolution API (WhatsApp) acionada pelo backend.

## 🚀 Como Iniciar

### Pré-requisitos
*   Android Studio Ladybug / Meerkat (ou superior)
*   JDK 21 ou JDK 22
*   Android SDK 35/37 (compilado com compileSdk 37, minSdk 29)

### Comandos de Build e Testes
```bash
# Compilar o código Kotlin
./gradlew compileDebugKotlin

# Executar a suíte de testes unitários
./gradlew test

# Gerar o APK de Debug
./gradlew assembleDebug
```

## 🏗 Estrutura do Código (Padrão pt-BR)
O projeto segue estritamente a nomenclatura em Português do Brasil (pt-BR) e Clean Architecture:

```
app/src/main/java/br/com/raevan/familialm/
├── nucleo/
│   ├── tema/             # TemaHomeHub, Paleta de Cores e Estilos M3
│   └── di/               # Injeção de dependências e localizador de repositórios
├── dominio/
│   ├── modelos/          # Modelos de domínio imutáveis fiéis ao DDL Supabase
│   └── repositorios/     # Interfaces de repositório reativas com Kotlin Flow
├── dados/
│   └── repositorios/     # Implementações com Coroutines e Dispatchers.IO
└── apresentacao/
    ├── componentes/      # Componentes Compose reutilizáveis (cards, badges, topbar)
    ├── navegacao/        # Destinos e Scaffold com Jetpack Navigation Compose
    ├── painel/           # Dashboard 360º com Balanço Livre, Tarefas do Dia e Wi-Fi
    ├── financas/         # Contas fixas, receitas, assinaturas e cofrinhos com progresso
    ├── rotinas/          # Checklist com gamificação, moedas e Loja de Privilégios
    ├── saude/            # Ficha médica de emergência, check-in de humor e medicamentos
    ├── cofre/            # Senhas compartilhadas e Cofre Master com biometria/PIN
    ├── eventos/          # Planejamento de aniversários, RSVP e cotações
    ├── inventario/       # Busca por cômodo, garantias e contatos de emergência
    ├── mural/            # Mural de vitórias com reações e álbum privado
    ├── automacao/        # Dispositivos IoT e monitoramento de câmeras Frigate NVR
    └── compras/          # Compras inteligentes e simulação de OCR de cupons
```