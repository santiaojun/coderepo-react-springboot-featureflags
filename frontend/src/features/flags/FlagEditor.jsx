import { useId, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { Modal } from "../../shared/components/Modal.jsx";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { fieldMessage, formMessage } from "../../shared/api/errors.js";
import { VariationEditor } from "./VariationEditor.jsx";

const KINDS = [
    { value: "boolean", label: "Boolean — on or off" },
    { value: "multivariate", label: "Multivariate — two or more values" },
];

const BOOLEAN_VARIATIONS = [{ name: "Available", value: true }, { name: "Unavailable", value: false }];
const MULTIVARIATE_VARIATIONS = [{ name: "", value: "" }, { name: "", value: "" }];

const slugify = (value) => value.toLowerCase().trim().replace(/[^a-z0-9]+/g, "-").replace(/^-+/, "").replace(/-+$/, "").slice(0, 50);

export function FlagEditor({ onClose, onSave }) {
    const titleId = `flag-editor-${useId().replace(/:/g, "")}`;
    const [name, setName] = useState("");
    const [key, setKey] = useState("");
    const [keyEdited, setKeyEdited] = useState(false);
    const [description, setDescription] = useState("");
    const [kind, setKind] = useState("boolean");
    const [tags, setTags] = useState("");
    const [temporary, setTemporary] = useState(true);
    const [variations, setVariations] = useState(BOOLEAN_VARIATIONS);
    const [onIndex, setOnIndex] = useState(0);
    const [offIndex, setOffIndex] = useState(1);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    const variationOptions = useMemo(() => variations.map((variation, index) => ({
        value: String(index),
        label: variation.name.trim() || `Variation v${index + 1}`,
    })), [variations]);

    const changeKind = (nextKind) => {
        setKind(nextKind);
        const next = nextKind === "boolean" ? BOOLEAN_VARIATIONS : MULTIVARIATE_VARIATIONS;
        setVariations(next);
        setOnIndex(0);
        setOffIndex(next.length - 1);
    };

    const changeVariations = (next) => {
        setVariations(next);
        if (onIndex >= next.length) setOnIndex(0);
        if (offIndex >= next.length) setOffIndex(next.length - 1);
    };

    const submit = async (event) => {
        event.preventDefault();
        setSaving(true);
        setError(null);
        try {
            await onSave({
                key: keyEdited ? key.trim() : slugify(name),
                name: name.trim(),
                description: description.trim(),
                kind,
                tags: tags.split(",").map((tag) => tag.trim()).filter(Boolean),
                temporary,
                variations: variations.map((variation, index) => ({
                    name: variation.name.trim(),
                    value: kind === "boolean" ? index === 0 : variation.value,
                })),
                defaultOnVariationIndex: Number(onIndex),
                defaultOffVariationIndex: Number(offIndex),
            });
        } catch (saveError) {
            setError(saveError);
            setSaving(false);
        }
    };

    return createPortal(<Modal className="flag-editor-modal" labelledBy={titleId} onClose={() => { if (!saving) onClose(); }}>
        <form className="flag-editor" onSubmit={submit}>
            <header className="modal-header">
                <h2 id={titleId}>New feature flag</h2>
                <button aria-label="Close" className="icon-button" disabled={saving} type="button" onClick={onClose}>
                    <MaterialIcon size={20}>close</MaterialIcon>
                </button>
            </header>
            <div className="flag-editor-body">
                {formMessage(error) && !fieldMessage(error, "key") && <p className="form-error" role="alert">{formMessage(error)}</p>}
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
                <label className="field">
                    <span>Key</span>
                    <input
                        className="mono-input"
                        disabled={saving}
                        required
                        value={keyEdited ? key : slugify(name)}
                        onChange={(event) => { setKeyEdited(true); setKey(event.target.value); }}
                    />
                    <small className="field-hint">Your application references this key. It cannot be changed later.</small>
                    {fieldMessage(error, "key") && <small className="field-error" role="alert">{fieldMessage(error, "key")}</small>}
                </label>
                <label className="field">
                    <span>Description</span>
                    <textarea
                        disabled={saving}
                        maxLength={1000}
                        rows={2}
                        value={description}
                        onChange={(event) => setDescription(event.target.value)}
                    />
                    {fieldMessage(error, "description") && <small className="field-error" role="alert">{fieldMessage(error, "description")}</small>}
                </label>
                <div className="field-row">
                    <div className="field">
                        <span>Type</span>
                        <SelectMenu ariaLabel="Flag type" disabled={saving} options={KINDS} value={kind} onChange={changeKind} />
                    </div>
                    <label className="field">
                        <span>Tags</span>
                        <input
                            disabled={saving}
                            placeholder="checkout, payments"
                            value={tags}
                            onChange={(event) => setTags(event.target.value)}
                        />
                        {fieldMessage(error, "tags") && <small className="field-error" role="alert">{fieldMessage(error, "tags")}</small>}
                    </label>
                </div>
                <VariationEditor disabled={saving} error={error} kind={kind} variations={variations} onChange={changeVariations} />
                <div className="field-row">
                    <div className="field">
                        <span>Serve when on</span>
                        <SelectMenu ariaLabel="Variation served when the flag is on" disabled={saving} options={variationOptions} value={String(onIndex)} onChange={(value) => setOnIndex(Number(value))} />
                    </div>
                    <div className="field">
                        <span>Serve when off</span>
                        <SelectMenu ariaLabel="Variation served when the flag is off" disabled={saving} options={variationOptions} value={String(offIndex)} onChange={(value) => setOffIndex(Number(value))} />
                    </div>
                </div>
                <label className="checkbox-field">
                    <input checked={temporary} disabled={saving} type="checkbox" onChange={(event) => setTemporary(event.target.checked)} />
                    <span>Temporary flag — expected to be removed after the rollout finishes</span>
                </label>
                <p className="field-hint">
                    New flags start on in Development and off in Staging and Production, so nothing reaches customers until you turn it on.
                </p>
            </div>
            <footer className="modal-actions">
                <button disabled={saving} type="button" onClick={onClose}>Cancel</button>
                <button className="primary-button" disabled={saving} type="submit">{saving ? "Creating…" : "Create flag"}</button>
            </footer>
        </form>
    </Modal>, document.body);
}
