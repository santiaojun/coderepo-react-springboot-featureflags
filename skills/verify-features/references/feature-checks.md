# Per-Feature Verification Steps

Concrete steps for the four-beat loop in `SKILL.md`: act, observe, reload, audit. Work in
**Development** unless a step says otherwise.

Seeded anchors you can rely on:

| Anchor | Value |
|---|---|
| Multivariate flag with rollout history | `search-ranking-v2` (Baseline / Semantic / Hybrid) |
| Flag with 6 production versions incl. a rollback | `checkout-express-pay` |
| Simple boolean flag | `dark-mode` |
| Retired flag | `legacy-report-builder` |
| Segments | `enterprise-accounts` (13), `eu-customers` (10), `beta-testers` (14) |
| Audience keys | `user-1001` … `user-1040` |

Change-history actions to expect: `flag_created`, `flag_updated`, `toggled`,
`targeting_updated`, `rollout_updated`, `rolled_back`.

---

## 1. Feature flag management

**Screens:** Feature flags list → `New flag` modal → flag detail → Settings tab.

| Beat | Steps |
|---|---|
| Act | `New flag`. Key `verify-features-probe`, a name, kind **multivariate**, three variations (`Alpha`/`alpha`, `Beta`/`beta`, `Off`/`off`). Create. Then open it, go to **Settings**, change the name and add a tag. Save. Return to the list and flip the row toggle on. |
| Observe | The flag appears in the list; the detail header shows the new name; the toggle shows the on state with its served variation. No error banner. |
| Reload | Refresh. The flag is still listed with the new name, the tag, and the toggle still on. |
| Audit | Change history filtered to `verify-features-probe` shows `flag_created` at v1, `flag_updated`, and `toggled` — each naming your member, versions ascending, diffs matching the fields you changed. |

Also confirm these are **refused**, each with a clear message and no state change:

- Reusing an existing key such as `dark-mode` (conflict).
- A malformed key with spaces or capitals.
- Fewer than two variations on a multivariate flag.
- Editing a **variation value** in Settings — values are immutable once created because
  history and rollback depend on them. Name and description must still be editable.
- Toggling `legacy-report-builder` while it is retired.
- Deleting the probe while it is still active — it must be retired first.

## 2. Environment-based configuration

**Screens:** header environment switcher → Feature flags list → flag detail.

| Beat | Steps |
|---|---|
| Act | In **Development**, toggle `dark-mode` on. Switch to **Staging** via the header pill and look at the same flag. |
| Observe | Staging shows its own state, unchanged by what you just did in Development. The switcher makes the active environment obvious, and the summary panel counts refresh per environment. |
| Reload | Refresh while in Staging: still Staging, still its own state. Switch back to Development: your toggle is still on. |
| Audit | The Development entry exists and names Development in its summary. **No** matching entry appears under Staging or Production. |

Independence is the whole point of this feature, so check all three:

- Version counters are per environment — v1 in Development is a different configuration
  from v1 in Production.
- A rollback in one environment must not alter another. Restore `checkout-express-pay`
  to an earlier version in Development, then confirm Staging and Production sit at their
  original versions.
- Production is flagged critical in the UI and should ask for a second confirmation.

## 3. User and segment targeting

**Screens:** Segments → `New segment` / Edit → `View members`; flag detail → Targeting tab.

| Beat | Steps |
|---|---|
| Act | Create segment `verify-features-segment` with a rule on a user attribute (for example country in `JP`). Open `View members`. Then on your probe flag's **Targeting** tab, add an individual target for `user-1001`, add a rule serving a variation to `enterprise-accounts`, set the fallthrough, and save. |
| Observe | The member drawer lists a non-zero, plausible match count. The targeting panel shows the target, the rule in order, and the fallthrough. The Evaluation tester for `user-1001` returns the targeted variation with reason `targetMatch`. |
| Reload | Refresh, reopen the Targeting tab: target, rule, and fallthrough all still present and in the same order. |
| Audit | A `targeting_updated` entry exists with a diff naming the individual target, the rule, and the fallthrough change. |

Confirm these are refused:

- A rule that serves an unknown variation.
- An individual target for an unknown user key such as `user-9999`.
- A rule with neither a segment nor a clause.
- A segment whose rules match nobody.
- Deleting a segment that a flag still uses — the message should name the blocking flags.

## 4. Progressive rollouts

**Screens:** flag detail → Rollout tab (stepper, slider, split bar, preview, distribution).

| Beat | Steps |
|---|---|
| Act | On the probe flag, use the stepper to set 25%, check the preview, then commit. Raise it to 50% and commit again. |
| Observe | The split bar matches the requested weights. The preview shows projected versus current split *before* committing. The distribution block reports actual per-variation counts over the 40 seeded members. |
| Reload | Refresh, reopen the Rollout tab: the committed split is still 50%. |
| Audit | Two `rollout_updated` entries, one per commit, with ascending versions and summaries naming the percentages. |

Rollout-specific properties worth confirming, because they are easy to get wrong:

- **Stability.** Raising 25% → 50% must not move users who were already in the variation;
  only newly included ones change. Spot-check one user with the Evaluation tester before
  and after.
- Weights that do not total 100 are refused.
- A user's variation is stable across reloads — bucketing is deterministic, not random.

## 5. Change history and rollbacks

**Screens:** Change history page (timeline, diffs, filters) → rollback dialog; flag detail → History tab.

| Beat | Steps |
|---|---|
| Act | Open Change history. Filter by action and by member. Then pick `checkout-express-pay` in Development and restore an earlier version through the rollback dialog. |
| Observe | The timeline reads newest first with actor, environment, summary, and a field-level diff per entry. Filters narrow the list. The rollback dialog previews what will change. |
| Reload | Refresh: the configuration is at the restored values and the new entry is still in the timeline. |
| Audit | A `rolled_back` entry exists that records **which version it restored from**, and it is a **new version on top** — earlier entries must still be present, because restore is additive and never rewrites history. |

Also confirm:

- Restoring a version that does not exist is refused.
- Change records for a **deleted** flag survive the deletion — history outlives the flag.
- The seeded `checkout-express-pay` timeline in Production reads v1 `flag_created` →
  v2 `toggled` → v3/v4 `rollout_updated` → v5 `rolled_back` (from v3) → v6 `rollout_updated`.

---

## Cleanup order

Reverse of creation, all through the UI:

1. Clear targeting rules and individual targets from the probe flag.
2. Retire the probe flag, then delete it.
3. Delete `verify-features-segment`.
4. Undo any seeded-flag changes you made — the `dark-mode` toggle and the
   `checkout-express-pay` rollback are both visible in the timeline, so restore them to
   their prior versions rather than leaving them changed.
5. Reload and confirm the Preflight baseline: 12 flags per environment, 3 segments per
   environment, 40 audience members.

Change records you generated remain in history by design. Note the count you added in the
report so the next run can tell seed data from run residue.
