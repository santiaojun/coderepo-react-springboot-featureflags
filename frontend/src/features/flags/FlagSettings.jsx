import { useEffect, useMemo, useState } from "react";
import { ConfirmationDialog } from "../../shared/components/ConfirmationDialog.jsx";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { fieldMessage, formMessage } from "../../shared/api/errors.js";
import { variationValueLabel } from "../../shared/utils/format.js";

const draftOf = (flag) => ({
    name: flag.name,
    description: flag.description,
    tags: flag.tags.join(", "),
    temporary: flag.temporary,
    maintainerId: flag.maintainerId || "",
    variations: flag.variations.map((variation) => ({ id: variation.id, name: variation.name, description: variation.description })),
});

export function FlagSettings({ flag, members, onDelete, onRestore, onRetire, onSave }) {
    const [draft, setDraft] = useState(() => draftOf(flag));
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);
    const [confirming, setConfirming] = useState("");
    const [lifecycleBusy, setLifecycleBusy] = useState(false);
    const [lifecycleError, setLifecycleError] = useState("");

    useEffect(() => { setDraft(draftOf(flag)); setError(null); }, [flag]);

    const dirty = useMemo(() => JSON.stringify(draft) !== JSON.stringify(draftOf(flag)), [draft, flag]);
    const memberOptions = members.map((member) => ({ value: member._id, label: `${member.name} · ${member.title}` }));

    const submit = async (event) => {
        event.preventDefault();
        setSaving(true);
        setError(null);
        const original = draftOf(flag);
        const payload = {};
        if (draft.name !== original.name) payload.name = draft.name.trim();
        if (draft.description !== original.description) payload.description = draft.description.trim();
        if (draft.tags !== original.tags) payload.tags = draft.tags.split(",").map((tag) => tag.trim()).filter(Boolean);
        if (draft.temporary !== original.temporary) payload.temporary = draft.temporary;
        if (draft.maintainerId !== original.maintainerId) payload.maintainerId = draft.maintainerId;
        const renamed = draft.variations.filter((variation, index) =>
            variation.name !== original.variations[index].name || variation.description !== original.variations[index].description);
        if (renamed.length) payload.variationNames = renamed;
        try {
            await onSave(payload);
        } catch (saveError) {
            setError(saveError);
        } finally {
            setSaving(false);
        }
    };

    const runLifecycle = async (action) => {
        setLifecycleBusy(true);
        setLifecycleError("");
        try {
            if (action === "retire") await onRetire();
            if (action === "restore") await onRestore();
            if (action === "delete") await onDelete();
            setConfirming("");
        } catch (lifecycleFailure) {
            setLifecycleError(lifecycleFailure.message);
        } finally {
            setLifecycleBusy(false);
        }
    };

    return <div className="flag-settings">
        <form className="settings-form" onSubmit={submit}>
            <header className="panel-heading">
                <div>
                    <h3>Flag details</h3>
                    <p>These apply to every environment. The key and the variation values are fixed once a flag exists.</p>
                </div>
            </header>
            {formMessage(error) && <p className="form-error" role="alert">{formMessage(error)}</p>}
            <label className="field">
                <span>Name</span>
                <input disabled={saving} maxLength={80} value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} />
                {fieldMessage(error, "name") && <small className="field-error" role="alert">{fieldMessage(error, "name")}</small>}
            </label>
            <label className="field">
                <span>Description</span>
                <textarea disabled={saving} maxLength={1000} rows={2} value={draft.description} onChange={(event) => setDraft({ ...draft, description: event.target.value })} />
            </label>
            <div className="field-row">
                <label className="field">
                    <span>Tags</span>
                    <input disabled={saving} value={draft.tags} onChange={(event) => setDraft({ ...draft, tags: event.target.value })} />
                    {fieldMessage(error, "tags") && <small className="field-error" role="alert">{fieldMessage(error, "tags")}</small>}
                </label>
                <div className="field">
                    <span>Maintainer</span>
                    <SelectMenu ariaLabel="Flag maintainer" disabled={saving} options={memberOptions} value={draft.maintainerId} onChange={(maintainerId) => setDraft({ ...draft, maintainerId })} />
                </div>
            </div>
            <label className="checkbox-field">
                <input checked={draft.temporary} disabled={saving} type="checkbox" onChange={(event) => setDraft({ ...draft, temporary: event.target.checked })} />
                <span>Temporary flag — expected to be removed after the rollout finishes</span>
            </label>

            <fieldset className="variation-editor">
                <legend>Variations</legend>
                <p className="field-hint">
                    Display names and descriptions can change at any time. The identifier and the value are fixed, because
                    stored versions and rollback targets reference them.
                </p>
                {draft.variations.map((variation, index) => <div className="variation-row settings-variation" key={variation.id}>
                    <span className="variation-index">{variation.id}</span>
                    <label className="field">
                        <span>Display name</span>
                        <input
                            disabled={saving}
                            maxLength={60}
                            value={variation.name}
                            onChange={(event) => setDraft({
                                ...draft,
                                variations: draft.variations.map((entry, current) => (current === index ? { ...entry, name: event.target.value } : entry)),
                            })}
                        />
                    </label>
                    <label className="field">
                        <span>Value</span>
                        <input disabled readOnly value={variationValueLabel(flag.variations[index].value)} />
                        <small className="field-hint">Fixed</small>
                    </label>
                    <label className="field settings-variation-description">
                        <span>Description</span>
                        <input
                            disabled={saving}
                            maxLength={280}
                            placeholder="What this variation does"
                            value={variation.description}
                            onChange={(event) => setDraft({
                                ...draft,
                                variations: draft.variations.map((entry, current) => (current === index ? { ...entry, description: event.target.value } : entry)),
                            })}
                        />
                    </label>
                </div>)}
            </fieldset>

            <footer className="settings-actions">
                <button disabled={saving || !dirty} type="button" onClick={() => { setDraft(draftOf(flag)); setError(null); }}>Discard</button>
                <button className="primary-button" disabled={saving || !dirty} type="submit">{saving ? "Saving…" : "Save details"}</button>
            </footer>
        </form>

        <section className="lifecycle-panel">
            <header className="panel-heading">
                <div>
                    <h3>Lifecycle</h3>
                    <p>
                        Retiring hides a flag from the active list but keeps its configuration and its history. Only a
                        retired flag can be deleted, and deletion never removes its audit trail.
                    </p>
                </div>
            </header>
            {lifecycleError && <p className="form-error" role="alert">{lifecycleError}</p>}
            <div className="lifecycle-actions">
                {flag.status === "active"
                    ? <button className="danger-button" type="button" onClick={() => { setConfirming("retire"); setLifecycleError(""); }}>Retire flag</button>
                    : <>
                        <button className="primary-button" type="button" onClick={() => { setConfirming("restore"); setLifecycleError(""); }}>Restore flag</button>
                        <button className="danger-button" type="button" onClick={() => { setConfirming("delete"); setLifecycleError(""); }}>Delete permanently</button>
                    </>}
            </div>
        </section>

        {confirming === "retire" && <ConfirmationDialog
            busy={lifecycleBusy}
            confirmLabel="Retire flag"
            error={lifecycleError}
            message={`${flag.key} stays configured in every environment and keeps serving its current variation, but it moves out of the active list.`}
            title={`Retire ${flag.name}?`}
            onCancel={() => setConfirming("")}
            onConfirm={() => runLifecycle("retire")}
        />}
        {confirming === "restore" && <ConfirmationDialog
            busy={lifecycleBusy}
            confirmLabel="Restore flag"
            error={lifecycleError}
            message={`${flag.key} returns to the active list with its configuration unchanged.`}
            title={`Restore ${flag.name}?`}
            onCancel={() => setConfirming("")}
            onConfirm={() => runLifecycle("restore")}
        />}
        {confirming === "delete" && <ConfirmationDialog
            busy={lifecycleBusy}
            confirmLabel="Delete permanently"
            error={lifecycleError}
            message={`This removes ${flag.key} and its configuration from all three environments. Its change history is kept for audit.`}
            title={`Delete ${flag.name}?`}
            onCancel={() => setConfirming("")}
            onConfirm={() => runLifecycle("delete")}
        />}
    </div>;
}
