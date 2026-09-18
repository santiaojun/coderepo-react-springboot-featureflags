# Flagpost

A feature flag management app for teams that ship behind flags: create a flag, target the
people who should see it, ramp it to a percentage of traffic, and roll it back when the
graphs turn the wrong way — with a full audit trail of who changed what, in which
environment.

Built on React 19 and Spring Boot 3.4 with MongoDB. Every feature works end to end: a
screen, a real API call, a backend handler, and stored data.

## Technology stack

| Layer | Choice |
|---|---|
| Frontend | React 19.2.4 |
| Frontend build tool | Vite 8.2.2 with `@vitejs/plugin-react` 6.1.1 |
| Frontend state | React hooks only — no router and no state library |
| HTTP client | Native `fetch`, wrapped once in `frontend/src/shared/api/client.js` |
| Backend | Spring Boot 3.4.1 (`spring-boot-starter-web`) |
| Backend language and runtime | Java 21 |
| Backend build tool | Gradle 9.7.1 via the committed wrapper |
| Database | MongoDB |
| Data access | `spring-boot-starter-data-mongodb`, used through `MongoTemplate` in hand-written repository classes |
| Package manager | Bun 1.4.2, with `frontend` as a workspace |
| Process runner | `concurrently` 9.2.1 |
| Validation | Static `*Validator` classes that parse raw request maps into records |
| Authentication | Two-stage JWT (`com.auth0:java-jwt` 4.4.0) with BCrypt password hashing (`at.favre.lib:bcrypt` 0.10.2) |
| Typography | `@fontsource/roboto` 5.3.0, served locally |

### Validation

Every write route parses its request body through a static validator before any service
runs, so controllers stay thin and services never touch raw maps. Failures raise a
`ValidationException` that `GlobalExceptionHandler` renders as a single envelope:

```json
{ "error": { "code": "VALIDATION_ERROR", "message": "...", "details": { "fieldErrors": {}, "formErrors": [] } } }
```

Successful responses use the matching `{ "data": ... }` envelope. Conflicts return 409
(duplicate key, stale version, deleting a flag or segment still in use) and missing
resources return 404.

### Authentication

Signing in is two steps, which is what lets one account act as any member and makes the
audit trail meaningful:

1. `POST /auth/login` verifies the BCrypt hash and returns a **workspace** token.
2. `POST /auth/switch-profile` exchanges it for a **profile** token carrying `profileId`.

Both are HMAC256 JWTs whose lifetime comes from `JWT_EXPIRES_IN` (default 24h). The
profile token identifies the actor recorded on every change. `ApiAuthFilter` protects all
routes except `GET /api/v1/health` and `POST /api/v1/auth/login`; requests without a valid
token get 401.

## Seeded access

Five accounts are seeded, all sharing one password. Any account can open any of the five
member profiles, so you can switch actors to exercise the audit trail.

| Email | Role | Team |
|---|---|---|
| `amara.okafor@flagpost.dev` | admin | Platform engineering lead |
| `priya.raghunathan@flagpost.dev` | engineer | Checkout squad |
| `diego.salazar@flagpost.dev` | engineer | Growth experiments |
| `lin.zhao@flagpost.dev` | engineer | Mobile platform |
| `noor.haddad@flagpost.dev` | viewer | Support operations |

**Password (all five): `flagpost2026`**

These are development seed credentials for a local demo database, defined in
`backend/src/main/java/com/featureflags/seed/SeedData.java`. They are not secrets and are
not used anywhere outside the seeded local environment.

### What else the seed creates

- **3 environments** — Development, Staging, Production, each with independent flag state
- **12 flags** per environment — 11 active plus 1 retired, across 14 tags, boolean and multivariate
- **3 segments** per environment — enterprise accounts, European customers, beta testers
- **40 end users** — across 3 plans and 10 countries, with attributes to target on
- **60 change records** — a replayed history so rollback has something real to roll back to

The seed declares configuration history as an ordered list of states and replays them,
writing one version and one audit entry per step. Seeded history is therefore produced the
same way a live change is, and the final state always matches the last record's snapshot.

## Prerequisites

- **Bun** 1.4+ (package manager and task runner)
- **Java** 21 (the Gradle wrapper is committed; no separate Gradle install needed)
- **MongoDB** reachable on `127.0.0.1:27017`

`setup.sh` starts MongoDB if it can find a local `mongod`; otherwise start it yourself
(a Docker container publishing 27017 works) before running install.

## Run it

```bash
bun install && bash setup.sh --seed   # install, build, and seed the database
bun start                             # start the backend and frontend together
```

Then open **http://localhost:3000** and sign in with any email above.

These are the exact commands in `hackerrank.yml`, and they work from a clean checkout.
`bun start` runs `setup.sh --start` first, which copies `.env` files from their
`.example` templates, verifies MongoDB, builds the backend if needed, and reseeds.

| Command | What it does |
|---|---|
| `bun start` | Backend on :8000 and frontend on :3000, concurrently |
| `bun run seed` | Reset the database to the seeded baseline |
| `bun run dev:backend` | Backend only (`gradlew bootRun`) |
| `bun run dev:frontend` | Frontend only (Vite dev server) |
| `cd frontend && bun run build` | Production build into `frontend/dist` |

The Vite dev server proxies `/api` to `http://localhost:8000`, so the frontend is
same-origin in development and needs no CORS configuration.

### Configuration

Read from environment variables only, with the defaults in `backend/.env.example`:

| Variable | Default |
|---|---|
| `PORT` | `8000` |
| `MONGODB_URI` | `mongodb://localhost:27017/featureflags_db` |
| `JWT_SECRET` | `featureflags-secret-jwt-key-2026` |
| `JWT_EXPIRES_IN` | `24h` |

Override `JWT_SECRET` for any deployment that is not a local demo.

## MongoDB behavior

### Connection

The backend expects MongoDB on `mongodb://localhost:27017/featureflags_db`, overridable
with `MONGODB_URI`. Nothing else is required — collections and indexes are created on
first write, so there is no migration step.

`setup.sh` checks `127.0.0.1:27017` before doing anything. If nothing answers it tries to
start a local `mongod` (using `/etc/mongod.conf` when present, otherwise a `.mongodb/`
data directory). If it still cannot connect it stops with an error rather than letting the
app start against a dead database.

`GET /api/v1/health` reports the live connection: `200` with `"database": "connected"`, or
`503` with `"disconnected"`. It pings the server on every call, so it never reports stale
state.

### Seeding and reset

> **The database is reset on every start.** `bun start` runs `setup.sh --start`, which
> reseeds before the servers come up. Anything you created in a previous session —
> flags, segments, targeting rules, change history — is deleted. This is a demo
> environment, not a durable store.

Seeding is deterministic: `SeedRunner` first clears all eight application collections,
then rebuilds the baseline described under [Seeded access](#seeded-access). The same
input always produces the same database, so a reviewer sees exactly what you see.

Configuration history is not inserted as finished records. It is declared as an ordered
list of states and replayed, writing one version and one audit entry per step, so seeded
history is produced by the same code path a live change uses and the final state always
matches the last record's snapshot. Change timestamps are relative to the day you seed.

| To do this | Run |
|---|---|
| Reset to the baseline without restarting | `bun run seed` |
| Reset and start both servers | `bun start` |
| Keep your data across restarts | Start the servers directly: `bun run dev:backend` and `bun run dev:frontend` (neither triggers `prestart`) |

## Features

Five features, each complete through the UI, the API, and MongoDB.

**Feature flag management.** Create boolean and multivariate flags with a key,
description, tags, maintainer, and default variations. Toggle per environment, edit
metadata, retire, and hard-delete. Variation keys and values are immutable once created —
history and rollback depend on them — while display names and descriptions stay editable.
Deletion requires retiring first; change records outlive the flag.

**Environment-based configuration.** Development, Staging, and Production each hold their
own flag state, version counter, and timeline. Switching environments in the header
rescopes the whole workspace. Version 1 means a different configuration in each
environment, and restoring in one never touches another.

**User and segment targeting.** Build audience segments from user attributes (plan,
country, beta opt-in, and more) with include and exclude lists. Serve a variation to named
individuals or to a whole segment, with rules evaluated in order. A built-in evaluator
explains which rule produced a given user's variation.

**Progressive rollouts.** Split traffic across variations by percentage, preview the split
before committing, and ramp it in steps. Bucketing is deterministic SHA-1 over the
bucketing attribute, so a user keeps their variation as the percentage rises and only the
newly included ones move.

**Change history and rollbacks.** Every change records the actor, the environment, a
human-readable summary, and a field-level diff. Restore any earlier version in one click;
the restore is additive — it writes a new version from the stored snapshot rather than
rewriting history.

## Structure

```text
.
├── backend/                  # Spring Boot API
│   └── src/main/java/com/featureflags/
│       ├── shared/           # envelopes, errors, validation, Mongo config, request context
│       ├── auth/             # login, JWT, profile switching, request filter
│       ├── members/          # workspace members (change actors)
│       ├── environments/     # environments and per-environment summaries
│       ├── flags/            # flags, configurations, variations, clause matching
│       ├── targeting/        # the evaluator, individual targets, rules
│       ├── rollouts/         # percentage rollouts and previews
│       ├── segments/         # environment-scoped audience segments
│       ├── audience/         # seeded end users and their flag matrix
│       ├── history/          # change records and diffs
│       └── seed/             # deterministic baseline, replayed as real changes
│   ├── src/main/resources/
│   │   └── application.properties   # port, Mongo URI, JWT config (all env-overridable)
│   ├── .env.example          # backend environment template
│   └── build.gradle          # Java 21, Spring Boot, seed task
├── frontend/                 # React app (Vite)
│   ├── src/
│   │   ├── features/        # one folder per feature, each with a co-located *.api.js
│   │   └── shared/          # API client, layout, reusable controls, utils
│   ├── .env.example          # frontend environment template
│   └── vite.config.js        # port 3000 and the /api proxy to :8000
├── .vscode/launch.json       # backend debugger configuration
├── skills/validate/          # submission verifier
├── transcripts/              # exported AI transcripts (added at submission)
├── AGENTS.md                 # agent operating rules and transcript logging
├── README.md                 # this file
├── hackerrank.yml            # install and run configuration
├── package.json              # Bun workspace and root commands
└── setup.sh                  # MongoDB readiness, backend build, seed reset
```

Each backend feature keeps controller, service, repository, and validator separate.
Validators are static and return records, so controllers stay thin and services never
parse raw request maps. Clause matching lives once in `flags` and is shared by `targeting`
and `segments`; rollout parsing lives once in `rollouts`.

The frontend has no router — the workspace is a single shell with view state — and adds no
dependency beyond React and the Roboto font.

### MongoDB collections

`environments`, `members`, `workspaceaccounts`, `flags`, `flagconfigurations`, `segments`,
`endusers`, `changerecords`.

Flag identity is separate from flag state: `flags` holds the key, description, and
variations, while `flagconfigurations` holds one document per `(flag, environment)` with
its own version counter. That is what keeps the three environments independent.

### API

30 routes under `/api/v1`. All except `/health` and `/auth/login` require a bearer token.

| Area | Routes |
|---|---|
| Health | `GET /health` |
| Auth | `POST /auth/login`, `GET /auth/session`, `POST /auth/switch-profile`, `POST /auth/logout` |
| Members | `GET /members` |
| Environments | `GET /environments`, `GET /environments/{env}/summary` |
| Flags | `GET /flags`, `POST /flags`, `GET /flags/{key}`, `PATCH /flags/{key}`, `DELETE /flags/{key}`, `POST /flags/{key}/retire`, `POST /flags/{key}/restore` |
| Toggle | `PATCH /flags/{key}/environments/{env}/toggle` |
| Targeting | `PUT /flags/{key}/environments/{env}/targeting`, `POST .../evaluate`, `GET .../distribution` |
| Rollouts | `PATCH .../rollout`, `POST .../rollout/preview` |
| Rollback | `POST /flags/{key}/environments/{env}/restore` |
| Segments | `GET /segments`, `POST /segments`, `PATCH /segments/{key}`, `DELETE /segments/{key}`, `GET /segments/{key}/members` |
| Audience | `GET /audience/users`, `GET /audience/users/{userKey}/flags` |
| History | `GET /change-history` |

Responses are wrapped as `{"data": ...}`. Validation failures return 400 with field-level
errors, conflicts return 409 (duplicate key, stale version, deleting a flag or segment
still in use), and missing resources return 404.

Writes to a flag configuration accept an optional `version`. If it does not match the
current version the write is rejected with 409, so two people editing the same flag cannot
silently overwrite each other.

## Accessibility and responsiveness

Responsive at 375, 768, 1024, and 1440 px with no horizontal page overflow: the sidebar
becomes an overlay drawer below 900 px and follows the breakpoint on resize, menus anchored
to narrow triggers pin to the viewport instead of spilling off-screen, and the collapsed
drawer leaves the tab order. Dialogs are native `<dialog>` elements, menus implement
combobox roles and keyboard interaction, and every control has a visible focus ring. Light
and dark themes are both supported. Loading, empty, validation, and error states are
implemented per screen.
