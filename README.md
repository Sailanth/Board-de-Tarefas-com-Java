# 📋 Board — Gerenciador de Tarefas via Terminal

Aplicação de linha de comando (CLI) para gerenciamento de boards no estilo Kanban, desenvolvida em Java puro com JDBC, MySQL e Liquibase para migrations.

---

## 📌 O que o projeto faz

O **Board** permite criar e gerenciar boards de tarefas diretamente pelo terminal. Cada board é composto por colunas tipadas, e os cards (tarefas) percorrem essas colunas em sequência — similar a um fluxo Kanban simplificado.

### Funcionalidades principais

- **Gerenciamento de boards**: criar, selecionar e excluir boards
- **Colunas tipadas**: cada board possui obrigatoriamente uma coluna inicial, uma final e uma de cancelamento, além de colunas intermediárias (pendentes) opcionais
- **Ciclo de vida dos cards**:
  - Criar cards na coluna inicial
  - Avançar um card para a próxima coluna
  - Cancelar um card (move para a coluna de cancelamento)
  - Bloquear e desbloquear cards com registro de motivo e histórico
- **Visualização**: ver detalhes do board, de uma coluna específica ou de um card individual
- **Migrations automáticas**: ao iniciar, o banco é criado/atualizado automaticamente via Liquibase

---

## 🏗️ Arquitetura

```
br.com.dio
├── Main.java                        # Ponto de entrada da aplicação
├── dto/                             # Data Transfer Objects (records Java)
│   ├── BoardColumnDTO
│   ├── BoardColumnInfoDTO
│   ├── BoardDetailsDTO
│   └── CardDetailsDTO
├── exception/                       # Exceções de domínio
│   ├── CardBlockedException
│   ├── CardFinishedException
│   └── EntityNotFoundException
├── persistence/
│   ├── config/ConnectionConfig      # Configuração de conexão JDBC
│   ├── converter/OffsetDateTimeConverter
│   ├── dao/                         # Acesso a dados (JDBC puro)
│   │   ├── BlockDAO
│   │   ├── BoardColumnDAO
│   │   ├── BoardDAO
│   │   └── CardDAO
│   ├── entity/                      # Entidades de domínio
│   │   ├── BlockEntity
│   │   ├── BoardColumnEntity
│   │   ├── BoardColumnKindEnum      # INITIAL | PENDING | FINAL | CANCEL
│   │   ├── BoardEntity
│   │   └── CardEntity
│   └── migration/MigrationStrategy  # Execução do Liquibase
├── service/                         # Regras de negócio
│   ├── BoardColumnQueryService
│   ├── BoardQueryService
│   ├── BoardService
│   ├── CardQueryService
│   └── CardService
└── ui/                              # Interface de terminal (menus)
    ├── MainMenu
    └── BoardMenu
```

### Modelo de dados

```
BOARDS
  └─< BOARDS_COLUMNS  (board_id FK, ON DELETE CASCADE)
          └─< CARDS   (board_column_id FK, ON DELETE CASCADE)
                └─< BLOCKS (card_id FK, ON DELETE CASCADE)
```

---

## 🚀 Como executar

### Pré-requisitos

| Ferramenta | Versão mínima |
|---|---|
| Java | 17+ |
| MySQL | 8.0+ |
| Gradle | via wrapper (`./gradlew`) |

### 1. Configurar o banco de dados

```sql
CREATE DATABASE board;
CREATE USER 'board'@'localhost' IDENTIFIED BY 'board';
GRANT ALL PRIVILEGES ON board.* TO 'board'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configurar conexão (variáveis de ambiente — opcional)

Por padrão, a aplicação usa as credenciais acima. Para sobrescrever, exporte as variáveis antes de executar:

```bash
export DB_URL=jdbc:mysql://localhost/board
export DB_USER=board
export DB_PASSWORD=board
```

### 3. Compilar e executar

```bash
./gradlew build
./gradlew run
```

Ou, após gerar o JAR:

```bash
java -jar build/libs/board-1.0-SNAPSHOT.jar
```

> As migrations são executadas automaticamente na primeira inicialização.

---

## 🔄 Fluxo de um card

```
[INITIAL] → [PENDING...] → [FINAL]
               ↓
           [CANCEL]
```

- Um card só pode avançar se **não estiver bloqueado**
- Um card em `FINAL` não pode ser movido nem bloqueado
- Um card pode ser bloqueado em qualquer coluna do tipo `INITIAL` ou `PENDING`
- O histórico de bloqueios é mantido na tabela `BLOCKS`

---

## 🛠️ Tecnologias utilizadas

| Tecnologia | Função |
|---|---|
| Java 17+ | Linguagem principal |
| JDBC | Acesso direto ao banco de dados |
| MySQL 8 | Banco de dados relacional |
| Liquibase 4.29 | Migrations e versionamento do schema |
| Lombok 1.18 | Redução de boilerplate (Data, AllArgsConstructor, etc.) |
| Gradle (Kotlin DSL) | Build e gerenciamento de dependências |

---

## 🔧 Melhorias realizadas neste fork

As seguintes melhorias foram aplicadas em relação ao código original:

### Correções de bugs

- **`MigrationStrategy`**: removida abertura redundante de uma segunda conexão JDBC dentro do método `executeMigration()`. A conexão já era injetada via construtor e deve ser reaproveitada — abrir uma nova conexão desperdiçava recursos e ignorava a instância fornecida.

- **`CardService.cancel()`**: removido código morto — a busca pela "próxima coluna" dentro do método `cancel()` não tinha efeito algum (o resultado era descartado imediatamente). Substituído por uma validação explícita que impede cancelar um card que já está na coluna de cancelamento.

### Melhorias de robustez

- **`ConnectionConfig`**: credenciais de banco de dados não são mais hardcoded. A classe agora lê as variáveis de ambiente `DB_URL`, `DB_USER` e `DB_PASSWORD`, com fallback para os valores padrões de desenvolvimento.

- **`BoardColumnKindEnum.findByName()`**: substituído `.orElseThrow()` sem argumento por `.orElseThrow(() -> new IllegalArgumentException(...))` com mensagem descritiva, facilitando o diagnóstico quando um valor inesperado chega do banco.

- **`MigrationStrategy.executeMigration()`**: erros de Liquibase e de I/O que antes eram silenciados com `e.printStackTrace()` agora são relançados como `SQLException` e `RuntimeException`, evitando que a aplicação continue em estado inconsistente após uma falha de migração.

### Correções de texto (UI e mensagens de erro)

- Corrigidos os typos nas mensagens exibidas ao usuário:
  - `"necesário"` → `"necessário"`
  - `"desbloquea-lo"` → `"desbloqueá-lo"`
  - `"baord"` → `"board"` (em `MainMenu`)
- Corrigido o label da opção 9 no `BoardMenu`: era `"Voltar para o menu anterior um card"` — texto sem sentido — e passou a ser `"Voltar para o menu anterior"`.

---

## 📄 Licença

Projeto de uso educacional, sem licença definida.

---

> Baseado no código de [José Luiz Abreu Cardoso Junior](https://github.com/juniorjrjl).
