# Jira clone

A small Jira-style issue tracker. Users create projects, track issues on a list and a Kanban board, group them into sprints, comment, and see the history of every change. Project owners manage their projects; users assigned to a project's issues can view it and comment.

It is a server-rendered **modular monolith**: Spring Boot 4 and Java 25, Spring Modulith, Thymeleaf with htmx (no SPA and no Node build), and PostgreSQL with Flyway.

## Features

| Area | What you can do | Spec |
| --- | --- | --- |
| Accounts | Register, log in and log out (form login, CSRF-protected) | [authentication](specs/authentication-spec.md) |
| Dashboard | See your accessible projects with issue counts per status | [project access](specs/project-access-spec.md) |
| Projects | Create (unique key such as `PROJ`), search and page through them; the owner can edit and delete | [projects](specs/projects-spec.md), [project access](specs/project-access-spec.md) |
| Issues | Create; filter, search and page the list; edit fields inline; manage several assignees; delete | [issues](specs/issues-spec.md), [project access](specs/project-access-spec.md) |
| Board | Drag cards between To Do, In Progress and Done; filter by sprint | [board](specs/board-spec.md) |
| Sprints | Plan, start and complete sprints; move issues in and out | [sprints](specs/sprints-spec.md) |
| Comments and history | Add plain-text comments; every change is logged on the issue | [comments](specs/comments-spec.md), [activity history](specs/activity-history-spec.md) |

## Quick start

Prerequisites: JDK 25 and Docker.

```sh
docker compose up -d postgres      # PostgreSQL 17 on localhost:5432
./mvnw spring-boot:run             # app on http://localhost:8080
```

Log in as a seeded user or register at `/register`.

| Username | Password |
| --- | --- |
| `alice`, `bob`, `carol` | `password` |

Flyway seeds these users for local development only. Usernames are case-insensitive. `/` shows a public landing page for guests and the dashboard for signed-in users. Business pages require a login; `/login` and `/register` are public.

Alternatively, run `TestJiraCloneApplication` (under `src/test`) from your IDE. It starts the app against a throwaway Testcontainers PostgreSQL, so you do not need compose.

### Configuration

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/jiraclone` |
| `DB_USERNAME` | `jiraclone` |
| `DB_PASSWORD` | `jiraclone` |

The defaults match `docker-compose.yml`. Flyway owns the schema (migrations in `src/main/resources/db/migration`), and Hibernate only validates it.

## Test

```sh
./mvnw verify                      # needs a running Docker daemon (Testcontainers)
```

The suite includes MockMvc integration tests against a real PostgreSQL and architecture checks: Spring Modulith `ApplicationModules.verify()` in `JiraCloneApplicationTests`, and ArchUnit rules in `ArchitectureTests`. CI runs the same command on every push to `main` and on every pull request ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)).

## Docker image

```sh
docker build -t jira-clone .
docker compose --profile app up --build   # app + Postgres on http://localhost:8080
```

### Production with remote PostgreSQL

Use `docker-compose.prd.yml` **on its own**, not as an override of the development
file. It runs only the app, with no database service or database volume.

Supply `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` through the deployment environment
or a protected `.env.prd` file (ignored by Git). All three are required; no local
database credentials are used as fallbacks. Use a PostgreSQL JDBC URL such as
`jdbc:postgresql://db.example.com:5432/jiraclone?sslmode=verify-full` and configure
the trusted CA as required by your database provider.

After approving the deployment and preparing the remote schema:

```sh
docker compose --env-file .env.prd -f docker-compose.prd.yml up -d --build
```

The app binds to `127.0.0.1:8080` on the host (set `APP_PORT` to change the port).
Put an HTTPS reverse proxy in front of it; the Compose file does not expose it
publicly or provision TLS.

Automatic Flyway migrations are disabled in this file. Apply reviewed migrations
through your approved database migration process before starting the app;
Hibernate still validates the schema. **The bundled `V2__seed_users.sql` migration
creates demo accounts with known passwords, including on a remote database.**
Remove or secure those accounts through an approved database operation before
allowing production access. Do not enable automatic migrations against production
without reviewing this seed migration.

## Observability

[`observability/`](observability/README.md) contains a standalone Docker Compose
stack with Grafana, Prometheus and a provisioned application/JVM dashboard.
It targets `homelab-server-1:8085`. Enable the application's `observability`
profile and configure the dedicated scrape credential before starting the stack;
see the guide for secrets, private connectivity and verification.

## Project layout

```text
src/main/java/com/jiraclone/
  identity/  projects/  issues/  board/  sprints/  comments/  history/   one package per module
  common/                                                               security and error handling
src/main/resources/
  templates/<module>/   Thymeleaf pages and htmx fragments
  db/migration/         Flyway migrations
  static/               app.css, vendored htmx and SortableJS
src/test/java/          integration tests by module, plus architecture tests
docs/                   architecture, data model, HTTP API, UI design
specs/                  feature requirements and implementation plans
```

Each module exposes a public API (`*Api`, events, records) in its root package and keeps `core/` (domain, persistence) and `web/` (controllers) internal. See [architecture](docs/architecture.md#2-modules) for the dependency rules.

## Documentation

| Where | What |
| --- | --- |
| [`specs/`](specs/README.md) | What each feature must do: requirements with numbered acceptance criteria, plus the plan used to build it. This is the source of truth for behaviour. |
| [`docs/architecture.md`](docs/architecture.md) | Modules, dependencies, htmx conventions, key flows, architecture decisions |
| [`docs/database.md`](docs/database.md) | Data model, ER diagram, constraints and migrations |
| [`docs/api/openapi.yaml`](docs/api/openapi.yaml) | HTTP endpoints (HTML and htmx, not JSON) |
| [`docs/ui/design.md`](docs/ui/design.md) | Screens, navigation, board wireframe |
| [`AGENTS.md`](AGENTS.md) | Engineering rules for contributors and coding agents |

## Contributing

Spec first: update or add the spec in `specs/`, then implement with tests, then update the affected docs in the same change (see [specs/README.md](specs/README.md#adding-or-changing-a-feature)). Follow [`AGENTS.md`](AGENTS.md). Before you push, `./mvnw verify` must pass.
