import { useId, useState } from "react";
import { createPortal } from "react-dom";
import { Modal } from "../../shared/components/Modal.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { fieldMessage, formMessage } from "../../shared/api/errors.js";
import { UserPicker } from "../audience/UserPicker.jsx";
import { ClauseRow } from "../targeting/ClauseRow.jsx";

let clauseSequence = 0;
const nextClause = () => ({ localId: `segment-clause-${clauseSequence += 1}`, attribute: "plan", op: "in", values: [], negate: false });

const slugify = (value) => value.toLowerCase().trim().replace(/[^a-z0-9]+/g, "-").replace(/^-+/, "").replace(/-+$/, "").slice(0, 50);

function KeyList({ disabled, excludeKeys, keys, label, onAdd, onRemove }) {
    return <div className="segment-key-list">
        <span className="field-label">{label}</span>
        {keys.length > 0 && <ul className="target-keys">
            {keys.map((key) => <li key={key}>
                <code>{key}</code>
                <button aria-label={`Remove ${key}`} className="icon-button" disabled={disabled} type="button" onClick={() => onRemove(key)}>
                    <MaterialIcon size={16}>close</MaterialIcon>
                </button>
            </li>)}
        </ul>}
        {!disabled && <UserPicker excludeKeys={excludeKeys} label={label} placeholder="Add a member" onSelect={(user) => onAdd(user.key)} />}
    </div>;
}

export function SegmentEditor({ environment, onClose, onSave, segment }) {
    const titleId = `segment-editor-${useId().replace(/:/g, "")}`;
    const editing = Boolean(segment);
    const [name, setName] = useState(segment?.name || "");
    const [key, setKey] = useState(segment?.key || "");
    const [keyEdited, setKeyEdited] = useState(editing);
    const [description, setDescription] = useState(segment?.description || "");
    const [included, setIncluded] = useState(segment?.includedUserKeys || []);
    const [excluded, setExcluded] = useState(segment?.excludedUserKeys || []);
    const [rules, setRules] = useState(() => (segment?.rules || []).map((rule, index) => ({
        description: rule.description,
        clauses: rule.clauses.map((clause, clauseIndex) => ({ ...clause, localId: `segment-clause-${index}-${clauseIndex}` })),
    })));
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    const submit = async (event) => {
        event.preventDefault();
        setSaving(true);
        setError(null);
        const payload = {
            name: name.trim(),
            description: description.trim(),
            includedUserKeys: included,
            excludedUserKeys: excluded,
            rules: rules.map((rule) => ({
                description: rule.description,
                clauses: rule.clauses.map((clause) => ({
                    attribute: clause.attribute, op: clause.op, values: clause.values, negate: clause.negate,
                })),
            })),
        };
        try {
            await onSave(editing ? payload : { ...payload, key: keyEdited ? key.trim() : slugify(name) });
        } catch (saveError) {
            setError(saveError);
            setSaving(false);
        }
    };

    return createPortal(<Modal className="segment-editor-modal" labelledBy={titleId} onClose={() => { if (!saving) onClose(); }}>
        <form className="flag-editor" onSubmit={submit}>
            <header className="modal-header">
                <h2 id={titleId}>{editing ? `Edit ${segment.key}` : "New segment"}</h2>
                <button aria-label="Close" className="icon-button" disabled={saving} type="button" onClick={onClose}>
                    <MaterialIcon size={20}>close</MaterialIcon>
                </button>
            </header>
            <div className="flag-editor-body">
                {formMessage(error) && <p className="form-error" role="alert">{formMessage(error)}</p>}
                <p className="field-hint">
                    Segments belong to {environment.name}. Editing this one cannot change targeting in another environment.
                </p>
                <label className="field">
                    <span>Name</span>
                    <input
                        data-autofocus
                        disabled={saving}
                        maxLength={80}
                        required
                        value={name}
                        onChange={(event) => { setName(event.target.value); if (!keyEdited) setKey(slugify(event.target.value)); }}
                    />
                    {fieldMessage(error, "name") && <small className="field-error" role="alert">{fieldMessage(error, "name")}</small>}
                </label>
                {!editing && <label className="field">
                    <span>Key</span>
                    <input
                        className="mono-input"
                        disabled={saving}
                        required
                        value={keyEdited ? key : slugify(name)}
                        onChange={(event) => { setKeyEdited(true); setKey(event.target.value); }}
                    />
                    {fieldMessage(error, "key") && <small className="field-error" role="alert">{fieldMessage(error, "key")}</small>}
                </label>}
                <label className="field">
                    <span>Description</span>
                    <textarea
                        disabled={saving}
                        maxLength={1000}
                        rows={2}
                        value={description}
                        onChange={(event) => setDescription(event.target.value)}
                    />
                </label>

                <fieldset className="variation-editor">
                    <legend>Rules</legend>
                    <p className="field-hint">A member joins the segment when they meet every condition in any one rule.</p>
                    {rules.map((rule, index) => <div className="segment-rule" key={index}>
                        <header className="rule-card-header">
                            <span className="rule-order">Rule {index + 1}</span>
                            <button aria-label={`Delete rule ${index + 1}`} className="icon-button danger" disabled={saving} type="button" onClick={() => setRules(rules.filter((_, current) => current !== index))}>
                                <MaterialIcon size={18}>delete</MaterialIcon>
                            </button>
                        </header>
                        <label className="field">
                            <span>Description</span>
                            <input
                                disabled={saving}
                                maxLength={120}
                                value={rule.description}
                                onChange={(event) => setRules(rules.map((item, current) => (current === index ? { ...item, description: event.target.value } : item)))}
                            />
                        </label>
                        {rule.clauses.map((clause, clauseIndex) => <ClauseRow
                            clause={clause}
                            disabled={saving}
                            error={fieldMessage(error, `rules.${index}.clauses.${clauseIndex}.values`)}
                            key={clause.localId}
                            onChange={(next) => setRules(rules.map((item, current) => (current === index
                                ? { ...item, clauses: item.clauses.map((entry, position) => (position === clauseIndex ? next : entry)) }
                                : item)))}
                            onRemove={() => setRules(rules.map((item, current) => (current === index
                                ? { ...item, clauses: item.clauses.filter((_, position) => position !== clauseIndex) }
                                : item)))}
                        />)}
                        {fieldMessage(error, `rules.${index}.clauses`) && <small className="field-error" role="alert">{fieldMessage(error, `rules.${index}.clauses`)}</small>}
                        <button className="text-button" disabled={saving} type="button" onClick={() => setRules(rules.map((item, current) => (current === index ? { ...item, clauses: [...item.clauses, nextClause()] } : item)))}>
                            <MaterialIcon size={17}>add</MaterialIcon>Add condition
                        </button>
                    </div>)}
                    <button className="text-button" disabled={saving} type="button" onClick={() => setRules([...rules, { description: "", clauses: [nextClause()] }])}>
                        <MaterialIcon size={17}>add</MaterialIcon>Add rule
                    </button>
                </fieldset>

                <div className="field-row">
                    <KeyList
                        disabled={saving}
                        excludeKeys={[...included, ...excluded]}
                        keys={included}
                        label="Always included"
                        onAdd={(userKey) => setIncluded([...included, userKey])}
                        onRemove={(userKey) => setIncluded(included.filter((entry) => entry !== userKey))}
                    />
                    <KeyList
                        disabled={saving}
                        excludeKeys={[...included, ...excluded]}
                        keys={excluded}
                        label="Always excluded"
                        onAdd={(userKey) => setExcluded([...excluded, userKey])}
                        onRemove={(userKey) => setExcluded(excluded.filter((entry) => entry !== userKey))}
                    />
                </div>
                {fieldMessage(error, "includedUserKeys") && <small className="field-error" role="alert">{fieldMessage(error, "includedUserKeys")}</small>}
                {fieldMessage(error, "excludedUserKeys") && <small className="field-error" role="alert">{fieldMessage(error, "excludedUserKeys")}</small>}
                <p className="field-hint">Exclusions win over inclusions, and both win over rules.</p>
            </div>
            <footer className="modal-actions">
                <button disabled={saving} type="button" onClick={onClose}>Cancel</button>
                <button className="primary-button" disabled={saving} type="submit">
                    {saving ? "Saving…" : editing ? "Save segment" : "Create segment"}
                </button>
            </footer>
        </form>
    </Modal>, document.body);
}
