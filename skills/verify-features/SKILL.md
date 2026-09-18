---
name: verify-features
description: Verify Flagpost's five core features through the running UI - sign in with a seeded account, create or update through each feature's screens, reload to prove the change persisted to MongoDB, and confirm the change history recorded an audit entry. Use before submission, after touching flags, targeting, rollouts, environments, or history, or whenever a change needs proving in the real app rather than in tests.
---

# Verify Flagpost Features

Prove that each of the five core features works end to end **through the interface a
reviewer will actually use**, not through the API alone. Every feature is checked three
ways: the change is made in the UI, it survives a page reload, and it appears in the
change history with the right actor.

This skill verifies; it does not fix. Record what fails and report it.

## Why the UI matters here

The API can pass while the product is broken. A control can be unreachable at a narrow
width, a form can post without wiring its response back into view, or a write can land in
component state and never reach the server. Only a UI pass plus a reload catches those.

Read `references/feature-checks.md` for the per-feature step tables before starting.

## The one trap that invalidates a whole run

**Never restart the application to test persistence.** `bun start` runs `prestart` →
`setup.sh --start` → `bun run seed`, and the seed clears all eight collections first.
A restart wipes exactly the data you are trying to prove persisted, so a real write looks
like a failed one.

To prove persistence, **reload the page** (F5 / `Ctrl+R`) or re-navigate to the screen.
The browser refetches from the backend, which reads from MongoDB.

Restarting is only correct when you are deliberately testing the seed reset itself.

## Preflight

1. Confirm MongoDB is reachable on `127.0.0.1:27017`.
2. Confirm the app is already running: `GET http://localhost:8000/api/v1/health` returns
   200 with `"database": "connected"`, and `http://localhost:3000/` returns 200.
   - If it is not running, start it with `bun start` and accept that the database is now
     freshly seeded. Start it *before* the run, never during.
3. Note the starting baseline so you can tell your own writes apart from seed data:
   12 flags per environment (11 active, 1 retired), 3 segments per environment,
   40 audience members, 60 change records.
4. Decide which environment you are working in. **Development** is the safe default;
   Production is marked critical in the UI and asks for a second confirmation.

## Sign in

Use a seeded account. Any of the five works and each can open any of the five member
profiles, which is what makes the audit trail meaningful.

| Field | Value |
|---|---|
| Email | `amara.okafor@flagpost.dev` (admin) |
| Password | `flagpost2026` |

Sign-in is two steps by design: the password yields a workspace token, then selecting a
member profile yields the profile token that identifies the actor recorded on every
change. **Note which member you selected** — every audit entry you create should name that
person, and an entry attributed to someone else is a finding.

Useful selectors if you are driving a browser:

| Purpose | Selector |
|---|---|
| Email / password field | `[data-testid="email-input"]`, `[data-testid="password-input"]` |
| Submit | `.workspace-login-actions button` |
| Section nav | `.sidebar-nav button` (`Feature flags`, `Segments`, `Audience`, `Change history`) |
| Nav toggle (drawer below 900px) | `.header-menu` |
| Environment switcher | `.environment-trigger`, then `.environment-option` |
| Flag row / detail tabs | `.flag-row-main`, `.detail-tabs button` |

Below 900px the sidebar is an overlay drawer: open it with `.header-menu` before clicking
a nav item, and expect it to close itself after the selection.

## The loop, once per feature

For each of the five features, run the same four beats. `references/feature-checks.md`
gives the exact screens, controls, and expected values.

1. **Act.** Create or update through the UI. Prefer a uniquely named artifact such as
   `verify-features-probe` so it is unmistakably yours.
2. **Observe.** The screen should reflect the change immediately, with no error banner and
   no stuck loading state.
3. **Reload.** Refresh the page, navigate back to the screen, and confirm the value is
   still there. This is the persistence proof.
4. **Audit.** Open **Change history**, filter to the flag you touched, and confirm a new
   entry exists with the expected action, the member you selected as actor, a version one
   higher than before, and a field-level diff that matches what you changed.

A feature passes only when all four beats hold. Partial credit is a fail.

## What counts as a failure

Record a failure when any of these appear. The first two are the ones this skill exists to
catch.

- **Persistence failure.** The change is visible before the reload and gone after it, or
  the reloaded value disagrees with what the screen showed.
- **Audit failure.** No new change record, the wrong actor, a version that did not advance,
  an empty or wrong diff, or a summary that misdescribes the change.
- The control is unreachable, clipped, or covered at the width being tested.
- The screen shows stale data until a manual reload that a user would not know to perform.
- An error banner appears for a valid action, or a rejected action reports success.
- A write succeeds where it should be refused — editing a variation value, toggling a
  retired flag, or deleting a segment still in use.

## Cleanup

Remove every artifact you created, through the UI, in reverse order. Flags must be retired
before they can be deleted. Then reload and confirm the baseline counts from Preflight are
back.

Leave seeded data alone. If you cannot remove something, say so in the report rather than
reseeding, because reseeding destroys the evidence of what happened.

## Report

Lead with the verdict, then one row per feature. Keep evidence concrete: what you changed,
what the reload showed, and what the audit entry said.

```text
Feature verification - <environment> - <date>
Signed in as: <member name>

| Feature                        | UI action | Persisted after reload | Audit entry | Verdict |
|--------------------------------|-----------|------------------------|-------------|---------|
| Feature flag management        |           |                        |             |         |
| Environment-based config       |           |                        |             |         |
| User and segment targeting     |           |                        |             |         |
| Progressive rollouts           |           |                        |             |         |
| Change history and rollbacks   |           |                        |             |         |

Failures: <feature, what broke, exact steps to reproduce, expected vs observed>
Cleanup: <artifacts removed; baseline counts restored>
```

Use `PASS` only for a step you performed and observed. If the app was unreachable or a
screen could not be exercised, mark it `BLOCKED` with the reason and the exact steps to
resume — never infer a pass from the API, from the code, or from a previous run.
