# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repo layout

Two-service monorepo: `backend/` (Spring Boot) and `frontend/` (Vue 3). Bruno API tests live in `bruno/`. Local dev DB compose file is at `backend/local/docker-compose.yaml`.

## Common commands

### Local database
```bash
docker compose -f backend/local/docker-compose.yaml up -d   # MariaDB on :3306, db=main, user/password
```
The backend reads `DATABASE_URL` / `DATABASE_HOST` / `DATABASE_PORT` / `DATABASE_NAME` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` env vars (defaults match the compose file).

### Backend (`cd backend`)
```bash
./gradlew bootRun                          # run the app
./gradlew test                             # run all tests
./gradlew test --tests FullyQualifiedClass # run a single test class
./gradlew test --tests Class.methodName    # run a single test method
./gradlew generateOpenApiDocs              # writes ../api/openapi.json (used by frontend client gen)
./gradlew build                            # compile + test + package
```
Gradle is configured for Java 25 toolchain (`build.gradle.kts:13`). The OpenAPI plugin runs `bootRun` with `--spring.profiles.active=openapi` to dump the spec.

### Frontend (`cd frontend`)
```bash
npm install
npm run dev                # vite dev server
npm run build              # type-check + bundle
npm run type-check         # vue-tsc --build
npm run lint               # oxlint then eslint, both with --fix
npm run format             # prettier
npx orval                  # regenerate ./src/api from ../api/openapi.json
```
There is no frontend test runner configured.

### Regenerating the typed API client
The frontend's `src/api/` is generated. After any backend API change:
1. `cd backend && ./gradlew generateOpenApiDocs` (writes `api/openapi.json` at repo root)
2. `cd frontend && npx orval` (regenerates `frontend/src/api/`)
3. `npx vue-tsc --noEmit -p tsconfig.app.json` to confirm no type drift

CI runs exactly this sequence on every push (`.github/workflows/validate-api.yaml`) and fails on TS errors — that's the contract gate between the two services.

## Architecture

### Backend feature-package layout (enforced by README)
Code under `backend/src/main/java/dev/six_seven_quiz/` is organised **per feature, not per layer**. Each feature owns its own `controller/`, `service/`, `repository/`, `model/`, `dto/`, `component/mapper/`, `exception/`, `validator/`. Top-level features: `authentication/`, `authorization/`, `user/`, `quiz/`, plus a `shared/` package for cross-cutting concerns (`SecurityConfiguration`, `SessionConfiguration`, `GlobalExceptionHandler`, utilities). When adding code, extend an existing feature package or add a new sibling — do **not** create top-level `controller/` or `service/` directories.

### DTO conventions
Inside `quiz/dto/` requests live under `request/` and responses are split by use case under `response/authoring/` (quiz creator's view, includes correct-answer flags) vs `response/viewing/` (attempt-taker's view, redacts answer correctness until the attempt is finished). Pick the right subpackage based on which actor sees the payload.

### Persistence & sessions
- JPA entities use plain UUID primary keys (an earlier embedded-composite-key pattern was removed). Ordered `@OneToMany` collections (`Quiz.questions`, `Attempt.questions`) are persisted via junction tables with a `position` column (`quiz_questions`, `attempt_questions`) — order is part of the schema, not derived.
- Spring Session uses **JDBC store** with `initialize-schema: never` (`application.yaml:7`). The session schema is provisioned by Liquibase, not Spring Session's bootstrapper.
- `spring.jpa.open-in-view: false` — lazy associations must be initialised inside the service/transaction layer, not in controllers or mappers run after the response is built.
- Error responses strip stack traces, messages, binding errors, and exception class (`application.yaml:11-15`). Don't rely on Spring's default error body; throw domain exceptions handled by `shared/GlobalExceptionHandler`.

### Schema migrations
Add a new file under `backend/src/main/resources/db/changelog/` and **include it from `main.yaml`** — Liquibase only loads what `main.yaml` references. Existing changesets are immutable; never edit a merged one, write a follow-up changeset instead.

### Frontend → backend coupling
The frontend MUST call the backend through the generated client in `frontend/src/api/` (per README rule). Don't write hand-rolled axios calls to backend endpoints — add/modify the backend endpoint, regenerate, then consume the generated hook. The shared axios instance in `frontend/src/lib/axios.ts` is wired in via Orval's `mutator` override (`orval.config.ts`), so auth cookies and base URL flow through one place.

### Workflow rules from README
- No direct pushes to `main` (branch protection); open PRs.
- Use the generated API client; don't bypass it.
- DB changes go through Liquibase changelogs registered in `main.yaml`.
