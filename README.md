# Challenge - Sistema de Gestão de Atividades Planejadas e Executadas

## What (O que é)

Este projeto é um sistema web para gerenciar e comparar atividades planejadas e executadas. O sistema permite:

- **Upload de arquivos CSV** com atividades planejadas (`*_planned.csv`) e executadas (`*_executed.csv`)
- **Armazenamento persistente** das atividades em banco de dados PostgreSQL
- **Consulta e filtragem** de atividades por data, nome da atividade e tipo de atividade
- **Visualização comparativa** entre o que foi planejado e o que foi realmente executado
- **Interface web moderna** para interação com os dados

O sistema foi desenvolvido como um desafio técnico, focando em arquitetura limpa, extensibilidade e boas práticas de desenvolvimento.

## How (Como funciona)

### Arquitetura

O sistema segue uma **Arquitetura Hexagonal** simplificada, organizada em duas camadas principais:

1. **Domain Layer** (Camada de Domínio)
   - `logic/`: Lógica de negócio pura (funções puras, sem efeitos colaterais)
   - `models/`: Modelos de dados e schemas de validação
   - `adapters/`: Transformação de dados entre camadas

2. **External Layer** (Camada Externa)
   - `infrastructure/`: Implementações concretas (banco de dados, HTTP server, CSV parser)
   - `controllers/`: Orquestração de fluxos (padrão Logic Sandwich: Queries → Logic → Effects)

### Fluxo de Dados

#### Importação de CSV
```
Upload CSV → Parser → Validação → Banco de Dados
```

1. Usuário faz upload de arquivo CSV via interface web
2. Backend recebe o arquivo e identifica o tipo (planned/executed)
3. CSV é parseado e validado
4. Dados são inseridos em lote no PostgreSQL
5. Resumo da importação é retornado (válidos/inválidos/erros)

#### Consulta de Atividades
```
Filtros → Query DB → Enriquecimento → Resposta JSON
```

1. Usuário aplica filtros (data, atividade, tipo)
2. Backend consulta banco de dados com filtros
3. Atividades são enriquecidas com cálculo de `kind` (planned/executed)
4. Quando ambos os valores estão presentes, prioriza-se "executed"
5. Resposta JSON é formatada e enviada ao frontend

### Frontend

O frontend é construído com **ClojureScript** e **Reagent** (wrapper React):

- **Estado reativo** gerenciado com `reagent/atom`
- **Componentes funcionais** organizados por responsabilidade
- **Comunicação HTTP** via `fetch` API
- **UI moderna** com Tailwind CSS

## Core Concepts (Conceitos Principais)

### 1. Arquitetura Hexagonal

A arquitetura separa claramente a lógica de negócio das implementações técnicas:

- **Domain**: Funções puras, testáveis, sem dependências externas
- **External**: Implementações concretas que podem ser trocadas (ex: banco de dados, HTTP)

### 2. Component System

Uso de `com.stuartsierra/component` para gerenciamento de ciclo de vida:

- **Dependency Injection**: Componentes recebem dependências via `using`
- **Lifecycle Management**: `start` e `stop` para inicialização e limpeza
- **Testability**: Permite injeção de componentes mockados em testes

### 3. Logic Sandwich Pattern

Padrão usado nos controllers:

```
Query (Infrastructure) → Logic (Domain) → Effect (Infrastructure)
```

Exemplo:
```clojure
;; Query: busca dados do banco
(raw-activities (database/query-activities-raw ds filters))

;; Logic: processa e enriquece
(enriched (logic/filter-activities-by-kind raw-activities type-filter))

;; Effect: formata resposta
(response (adapters/model->wire-response result))
```

### 4. Activity Kind Calculation

O sistema calcula o tipo (`kind`) de uma atividade baseado nos valores:

- **executed**: Quando `amount_executed` está presente (prioridade)
- **planned**: Quando apenas `amount_planned` está presente
- **nil**: Quando ambos são `nil`

**Nota importante**: Quando ambos os valores estão presentes, a atividade é classificada como `executed` (não existe mais o tipo `both`).

### 5. Pure Functions

A camada `logic/` contém apenas funções puras:

- Sem efeitos colaterais
- Determinísticas
- Fáceis de testar
- Reutilizáveis

### 7. Test Strategy

- **Unit Tests**: Testam funções puras isoladamente
- **Integration Tests**: Testam fluxos completos com banco de dados real
- **Conditional Execution**: Testes de integração são pulados se o banco não estiver disponível

## Comandos Úteis

### Desenvolvimento

```bash
# Instalar dependências
lein deps

# Compilar ClojureScript
lein cljsbuild once app

# Executar aplicação localmente
lein run

# Iniciar REPL
lein repl

# Executar REPL com perfil dev
lein repl :dev
```

### Testes

```bash
# Executar todos os testes
lein test

# Executar testes de unidade apenas
lein test challenge.api.logic.logic-test

# Executar testes de integração
lein test challenge.integration

# Executar testes e linting
lein test-all
```

### Linting e Formatação

```bash
# Verificar linting
lein lint

# Copiar configurações de linting
lein lint-fix

# Verificar formatação
lein format

# Corrigir formatação automaticamente
lein format-fix

# Verificar tudo (formatação + linting)
lein check-all
```

### Build e Deploy

```bash
# Limpar artefatos
lein clean

# Limpar tudo (incluindo ClojureScript)
lein clean-all

# Compilar ClojureScript e gerar uberjar
lein uberjar-all

# Gerar uberjar standalone
lein uberjar
```

### Docker

```bash
# Construir e iniciar serviços (PostgreSQL + App)
cd docker
docker-compose up --build

# Executar em background
docker-compose up -d

# Parar serviços
docker-compose down

# Ver logs
docker-compose logs -f app

# Executar apenas o banco de dados
docker-compose up postgres
```

### Banco de Dados

```bash
# Executar migrações
lein migratus migrate

# Reverter última migração
lein migratus rollback

# Ver status das migrações
lein migratus pending
```

### Ambiente de Desenvolvimento

```bash
# Variáveis de ambiente necessárias (opcional, tem defaults)
export DATABASE_URL="postgresql://postgres:postgres@localhost:5432/challenge"
export DB_HOST="localhost"
export DB_PORT="5432"
export DB_NAME="challenge"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
```

### Testes de Integração

Os testes de integração requerem um banco PostgreSQL rodando:

```bash
# Iniciar PostgreSQL via Docker
cd docker
docker-compose up postgres -d

# Executar testes de integração
lein test challenge.integration

# Configurar variáveis de ambiente para testes
export DB_HOST="localhost"
export DB_PORT="5432"
export DB_NAME="challenge_test"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
```

### Estrutura de Diretórios

```
volis-challenge/
├── src/
│   └── challenge/
│       ├── api/          # Backend (Clojure)
│       │   ├── adapters/  # Transformação de dados
│       │   ├── config/    # Configuração
│       │   ├── controllers/ # Orquestração
│       │   ├── infrastructure/ # Implementações externas
│       │   ├── logic/     # Lógica de negócio pura
│       │   └── models/    # Modelos e schemas
│       └── ui/            # Frontend (ClojureScript)
│           ├── components/ # Componentes React/Reagent
│           ├── adapters.cljs
│           ├── core.cljs
│           ├── http_client.cljs
│           ├── logic.cljs
│           └── models.cljs
├── test/                  # Testes
│   └── challenge/
│       ├── api/          # Testes de unidade backend
│       ├── integration/  # Testes de integração
│       └── ui/           # Testes de unidade frontend
├── resources/
│   ├── migrations/       # Migrações SQL
│   ├── public/          # Assets estáticos
│   └── config.edn       # Configuração da aplicação
└── docker/              # Configuração Docker
```

### Troubleshooting

```bash
# Limpar cache do Leiningen
rm -rf ~/.m2/repository
rm -rf ~/.lein

# Reinstalar dependências
lein deps

# Verificar versão do Java (requer Java 21+)
java -version

# Verificar versão do Leiningen
lein version

# Ver logs da aplicação
tail -f logs/application.log
```

## Tecnologias Principais

- **Backend**: Clojure 1.12.2
- **Frontend**: ClojureScript 1.11.132 + Reagent 1.2.0
- **Banco de Dados**: PostgreSQL
- **HTTP Server**: Ring + Jetty
- **Routing**: Reitit
- **Component System**: Component (Stuart Sierra)
- **Migrations**: Migratus
- **Testing**: clojure.test
- **Styling**: Tailwind CSS

