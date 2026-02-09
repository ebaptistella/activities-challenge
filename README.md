# Challenge - Planned and Executed Activities Management System

## What

This project is a **monolithic web application** for managing and comparing planned and executed activities. The system allows:

- **CSV file upload** with planned activities (`*_planned.csv`) and executed activities (`*_executed.csv`)
- **Persistent storage** of activities in PostgreSQL database
- **Query and filtering** of activities by date, activity name, and activity type
- **Comparative visualization** between what was planned and what was actually executed
- **Modern web interface** for data interaction

The system was developed as a technical challenge, focusing on clean architecture, extensibility, and development best practices.

**Architecture**: Monolith with clear separation between backend (Clojure/Pedestal) and frontend (ClojureScript/Reagent) within `src/challenge/`. Both are managed by a single `project.clj` file.

## How

### Architecture

The system follows a **Hexagonal Architecture** inspired by Nubank practices, with clear separation between backend and frontend:

#### Backend Architecture

The backend is organized into two main layers:

1. **Domain Layer**
   - `logic/`: Pure business logic (pure functions, no side effects, no I/O)
   - `models/`: Data models and validation schemas (strict schemas)
   - `controllers/`: Flow orchestration (Logic Sandwich pattern: Queries → Logic → Effects)

2. **External Layer**
   - `infrastructure/`: Concrete implementations (database, HTTP server handlers)
   - `interceptors/`: Pedestal interceptors (validation, logging, component injection)
   - `adapters/`: Data transformation between wire schemas and models
   - `wire/`: Schemas for external communication (in/out/persistency)

#### Frontend Architecture

The frontend is a **ClojureScript/Reagent** application organized under `src/challenge/frontend/challenge/ui/`:

- **Components**: Reagent components organized by feature
- **State Management**: Reactive state with `reagent/atom`
- **HTTP Client**: Communication with backend API via `fetch`
- **Models & Logic**: Frontend-specific data models and business logic

The `challenge/ui/` path under the frontend source root ensures the namespace `challenge.ui.core` (and `challenge.ui.*`) matches the ClojureScript compiler’s file-to-namespace convention.

### Data Flow

#### CSV Import
```
Upload CSV → Parser → Validation → Database
```

1. User uploads CSV file via web interface
2. Backend receives the file and identifies the type (planned/executed)
3. CSV is parsed and validated
4. Data is batch inserted into PostgreSQL
5. Import summary is returned (valid/invalid/errors)

#### Activity Query
```
Frontend Filters → HTTP Request → Backend Query → DB Query → Enrichment → JSON Response → Frontend Display
```

1. User applies filters (date, activity, type) in the web interface
2. Frontend sends HTTP request to backend API
3. Backend queries database with filters
4. Activities are enriched with `kind` calculation (planned/executed)
5. When both values are present, "executed" takes priority
6. JSON response is formatted and sent to frontend
7. Frontend displays results in the UI

### Frontend

The frontend is built with **ClojureScript** and **Reagent** (React wrapper), located in `src/challenge/frontend/challenge/ui/`:

- **Reactive State**: Managed with `reagent/atom` for component state
- **Functional Components**: Organized by responsibility (activities, filters, upload)
- **HTTP Communication**: Via `fetch` API to backend REST endpoints
- **Modern UI**: Styled with Tailwind CSS
- **Build System**: Compiled with `lein-cljsbuild` to `resources/public/js/app.js` and `resources/public/js/out/`
- **Dependencies**: React 18 (cljsjs/react, cljsjs/react-dom) for Reagent

## Core Concepts

### 1. Hexagonal Architecture

The architecture clearly separates business logic from technical implementations:

- **Domain Layer**: 
  - `logic/`: Pure, testable functions, no external dependencies or I/O
  - `models/`: Strict schemas for domain entities
  - `controllers/`: Orchestration following the Logic Sandwich pattern
  
- **External Layer**:
  - `infrastructure/`: Concrete implementations (database, HTTP handlers)
  - `interceptors/`: Pedestal interceptors for validation, logging, and component injection
  - `adapters/`: Transformation between wire schemas (loose/strict) and models
  - `wire/`: Schemas for external communication (in: loose, out: strict, persistency: strict)

### 2. Component System

Uses `com.stuartsierra/component` for lifecycle management:

- **Dependency Injection**: Components receive dependencies via `using`
- **Lifecycle Management**: `start` and `stop` for initialization and cleanup
- **Testability**: Allows injection of mocked components in tests

### 3. Logic Sandwich Pattern

Pattern used in controllers:

```
Query (Infrastructure) → Logic (Domain) → Effect (Infrastructure)
```

Example:
```clojure
;; Query: fetch data from database
(raw-activities (database/query-activities-raw ds filters))

;; Logic: process and enrich
(enriched (logic/filter-activities-by-kind raw-activities type-filter))

;; Effect: format response
(response (adapters/model->wire-response result))
```

### 4. Activity Kind Calculation

The system calculates the activity `kind` based on values:

- **executed**: When `amount_executed` is present (priority)
- **planned**: When only `amount_planned` is present
- **nil**: When both are `nil`

**Important note**: When both values are present, the activity is classified as `executed` (the `both` type no longer exists).

### 5. Pure Functions

The `logic/` layer contains only pure functions:

- No side effects
- Deterministic
- Easy to test
- Reusable

### 6. Schema Validation

The system uses **Prismatic Schema** with automatic validation:

- **Models**: Strict schemas (all fields validated)
- **Wire.in**: Loose schemas (tolerant to extra fields for forward compatibility)
- **Wire.out**: Strict schemas (explicit control of what is sent)
- **Wire.persistency**: Strict schemas with namespaced keywords

### 7. Test Strategy

- **Unit Tests**: Test pure functions in isolation with `clojure.test`
- **Integration Tests**: Test complete flows with `state-flow` and mocked components
- **Schema Validation**: Automatically enabled in tests via `schema.test/validate-schemas`
- **Mock Components**: Mocked components for persistency in integration tests
- **Auto-initialization**: Test dependencies are automatically loaded when loading namespaces

## Useful Commands

### Development

```bash
# Install dependencies (includes ClojureScript and Reagent)
lein deps

# Run application (backend only - compile frontend separately)
lein run-dev

# For full development (backend + frontend):
# Terminal 1: Backend (REPL or server)
lein repl :repl-auto
# or
lein run-dev

# Terminal 2: Frontend watch mode (auto-recompiles on file changes)
lein cljs-watch
# or: lein cljsbuild auto app

# Compile ClojureScript once (without watch)
lein cljs-once
# or: lein cljsbuild once app

# Run application locally (after compiling frontend)
lein run

# Start REPL
lein repl

# Run REPL with dev profile (recommended)
lein repl :dev

# Run REPL with auto-start system
lein repl :repl-auto
```

### Tests

```bash
# Run all tests
lein test

# Run unit tests only
lein test challenge.logic.activity-test

# Run integration tests
lein test challenge.integration

# Run a specific test
lein test challenge.integration.activity-fake-test
```

### Tests in REPL

Tests can be executed directly in the REPL. Dependencies are automatically initialized:

```clojure
;; Load test namespace
(require 'challenge.integration.activity-fake-test :reload)

;; Run tests from namespace
(require 'clojure.test)
(clojure.test/run-tests 'challenge.integration.activity-fake-test)

;; Run a specific test
(clojure.test/test-var #'challenge.integration.activity-fake-test/activity-fake-test)

;; Run all tests
(clojure.test/run-all-tests)
```

### Linting and Formatting

```bash
# Run full lint (clean-ns, format, diagnostics, cljfmt, nsorg, kondo)
lein lint

# Fix namespace organization
lein clean-ns-fix

# Fix formatting
lein format-fix

# Check formatting (dry run)
lein format

# Check namespace organization (dry run)
lein nsorg-check

# Static analysis (clj-kondo)
lein kondo
```

### Build and Deploy

```bash
# Full build (compiles ClojureScript + creates uberjar)
lein build

# Production build (optimized ClojureScript with advanced optimizations)
lein build-prod

# Manual build steps:
# 1. Compile ClojureScript for development
lein cljs-once
# or: lein cljsbuild once app

# 2. Compile ClojureScript for production (optimized)
lein cljsbuild once prod

# 3. Create uberjar (includes compiled frontend assets and all resources)
lein uberjar

# Resources (config/, migrations/, public/, logback.xml) are copied to target/classes
# before packaging via the lein-resource plugin, so they are always included in the JAR.

# Clean build artifacts (removes target/ and resources/public/js/)
lein clean

# Useful ClojureScript aliases:
lein cljs-watch    # Watch mode (auto-recompile on file changes)
lein cljs-once     # Compile once (development, :optimizations :none)
```

### Docker

The Docker build process automatically compiles ClojureScript before creating the uberjar:

```bash
# Build and start services (PostgreSQL + App)
cd docker
docker-compose up --build

# Run in background
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f app

# Run database only
docker-compose up postgres
```

**Note**: The Dockerfile compiles ClojureScript (`lein cljsbuild once app`) before creating the uberjar, ensuring the frontend is included in the final artifact.

### Database

```bash
# Run migrations
lein migratus migrate

# Rollback last migration
lein migratus rollback

# Check migration status
lein migratus pending
```

### Development Environment

```bash
# Required environment variables (optional, has defaults)
export DATABASE_URL="postgresql://postgres:postgres@localhost:5432/challenge"
export DB_HOST="localhost"
export DB_PORT="5432"
export DB_NAME="challenge"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
```

### Integration Tests

Integration tests **do not require** a running PostgreSQL database. They use mocked components:

- **Mock Persistency**: In-memory storage for tests
- **Random Port**: Server uses port 0 (random) to avoid conflicts
- **Auto-initialization**: Dependencies are automatically loaded when loading test namespaces
- **State-flow**: Framework for integration tests with state management

```bash
# Run integration tests (no database needed)
lein test challenge.integration

# In REPL, tests work automatically:
(require 'challenge.integration.activity-fake-test :reload)
(require 'clojure.test)
(clojure.test/run-tests 'challenge.integration.activity-fake-test)
```

### Directory Structure

```
activities-challenge/
├── src/
│   └── challenge/
│       ├── backend/          # Backend: Clojure code (.clj files)
│       │   └── challenge/    # Backend code (namespaces challenge.*)
│       │       ├── adapters/     # Wire ↔ model transformation
│       │       ├── components/   # System components (Pedestal, Logger, DB, etc)
│       │       ├── config/       # Configuration reading
│       │       ├── controllers/  # Orchestration (Logic Sandwich)
│       │       ├── handlers/     # HTTP route definitions
│       │       ├── infrastructure/ # External implementations
│       │       │   ├── http_server/ # HTTP handlers 
│       │       │   └── persistency/ # Database operations
│       │       ├── interceptors/  # Pedestal interceptors (validation, logging)
│       │       ├── interface/    # HTTP interfaces (response helpers)
│       │       │   └── http/
│       │       ├── logic/        # Pure business logic (domain layer)
│       │       ├── models/       # Domain models (strict schemas)
│       │       ├── schema/       # Schema creation helpers
│       │       ├── wire/         # External communication schemas
│       │       │   ├── in/       # Input schemas (loose)
│       │       │   ├── out/      # Output schemas (strict)
│       │       │   └── persistency/ # Database schemas (strict, namespaced)
│       │       ├── main.clj      # Application entry point
│       │       ├── migrate.clj   # Migration utilities
│       │       ├── repl.clj      # REPL development utilities
│       │       └── system.clj   # Component system definition
│       └── frontend/         # Frontend: ClojureScript/Reagent application
│           └── challenge/    # Namespace prefix (challenge.ui.*)
│               └── ui/
│                   ├── adapters.cljs
│                   ├── components/   # Reagent components
│                   │   ├── activities.cljs
│                   │   ├── filters.cljs
│                   │   └── upload.cljs
│                   ├── core.cljs     # Main application entry (challenge.ui.core)
│                   ├── http_client.cljs
│                   ├── logic.cljs
│                   └── models.cljs
├── test/
│   ├── integration/          # Integration tests
│   │   └── challenge/
│   │       └── integration/
│   │           ├── aux/      # Test helpers and setup
│   │           │   ├── init.clj        # Automatic dependency setup
│   │           │   ├── http-helpers.clj # HTTP request helpers
│   │           │   └── mock-persistency.clj # Persistency mock
│   │           └── *_test.clj # Integration tests
│   └── unit/                 # Unit tests
│       └── challenge/
│           └── *_test.clj    # Pure function tests
├── resources/
│   ├── migrations/           # SQL migrations
│   ├── config/               # Application configuration
│   │   └── application.edn
│   └── public/              # Static assets
│       ├── index.html
│       ├── swagger-ui.html
│       └── js/               # Compiled ClojureScript (lein cljs-once / cljs-watch)
│           ├── app.js        # Bootstrap (loads from /js/out/)
│           └── out/           # Development build output (:optimizations :none)
├── project.clj               # Single project.clj (monolith)
└── docker/                   # Docker configuration
```

### REPL and Development

```clojure
;; In REPL, after loading challenge.repl:
(require 'challenge.repl)

;; Start system
(challenge.repl/start!)

;; Stop system
(challenge.repl/stop!)

;; Restart system
(challenge.repl/restart!)

;; Reload namespaces and restart
(challenge.repl/reload!)

;; Run tests in REPL
(require 'challenge.integration.activity-fake-test :reload)
(require 'clojure.test)
(clojure.test/run-tests 'challenge.integration.activity-fake-test)
```

### Troubleshooting

```bash
# Clear Leiningen cache
rm -rf ~/.m2/repository
rm -rf ~/.lein

# Reinstall dependencies
lein deps

# Check Java version (requires Java 21+)
java -version

# Check Leiningen version
lein version

# View application logs
tail -f logs/pedrepl-*.log

# Check if port 3000 is in use (may cause conflicts in tests)
lsof -i :3000

# Kill process on port 3000 (if needed)
kill -9 $(lsof -t -i:3000)
```

### Important Notes

- **Monolith Structure**: Single `project.clj` manages both backend (Clojure) and frontend (ClojureScript). Backend code is in `src/challenge/backend/challenge/` and frontend code is in `src/challenge/frontend/challenge/ui/`. The backend uses `:source-paths ["src/challenge/backend"]`; the frontend uses `:cljsbuild` with `:source-paths ["src/challenge/frontend"]` and `:main challenge.ui.core`, so the path `challenge/ui/core.cljs` under that root is required for the compiler to find the entry namespace.
- **Build Order**: For production builds, ClojureScript must be compiled before creating the uberjar. Use `lein build` or `lein build-prod` to handle this automatically.
- **Development Workflow**: For full development, run backend and frontend watch mode in separate terminals. Frontend watch mode (`lein cljs-watch` or `lein cljsbuild auto app`) automatically recompiles on file changes.
- **Port 0 in Tests**: Integration tests use port 0 (random) to avoid conflicts. The system preserves this configuration even when there is a configuration file.
- **Auto-initialization**: When loading test namespaces in the REPL, dependencies (such as `schema.test`) are automatically initialized.
- **Mock Components**: Integration tests use mocked components, do not require a real database.

## Main Technologies

### Backend
- **Language**: Clojure 1.12.2
- **HTTP Server**: Pedestal 0.5.8 (with Jetty)
- **Database**: PostgreSQL (via next.jdbc)
- **Component System**: Component (Stuart Sierra) 1.1.0
- **Schema Validation**: Prismatic Schema 1.4.1 + clj-schema 0.5.1
- **Migrations**: Migratus 1.4.5
- **JSON**: Cheshire 5.11.0
- **Logging**: Logback Classic 1.2.3

### Frontend
- **Language**: ClojureScript 1.11.60
- **UI Framework**: Reagent 1.2.0 (React wrapper)
- **React**: cljsjs/react 18.2.0-0, cljsjs/react-dom 18.2.0-0
- **Build Tool**: lein-cljsbuild 1.1.8
- **Compiler**: Google Closure Compiler (via ClojureScript)
- **Output**: `resources/public/js/app.js` (bootstrap) and `resources/public/js/out/` (dev build)

### Testing
- **Unit Tests**: `clojure.test`
- **Integration Tests**: `state-flow` 5.20.0
- **Assertions**: `matcher-combinators` 3.8.3
- **Mocking**: `mockfn` 0.7.0
