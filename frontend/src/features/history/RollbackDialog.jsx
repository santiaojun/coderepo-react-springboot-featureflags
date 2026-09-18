import { useId, useState } from "react";
import { createPortal } from "react-dom";
import { Modal } from "../../shared/components/Modal.jsx";
import { ChangeDiff } from "./ChangeDiff.jsx";

export function RollbackDialog({ environmentName, error = "", onCancel, onConfirm, record }) {
    const titleId = `rollback-${useId().replace(/:/g, "")}`;
    const [comment, setComment] = useState("");
    const [busy, setBusy] = useState(false);
    const confirm = async () => {
        setBusy(true);
        const completed = await onConfirm(record.version, comment.trim());
        if (!completed) setBusy(false);
    };
    return createPortal(<Modal className="rollback-modal" labelledBy={titleId} onClose={() => { if (!busy) onCancel(); }}>
        <section className="rollback-dialog">
            <h2 id={titleId}>Restore version {record.version}?</h2>
            <p>
                This writes version {record.version} of <strong>{record.flagKey}</strong> back to {environmentName} as a
                new version. Nothing in the history is removed, so you can roll the restore back too.
            </p>
            <div className="rollback-summary">
                <span className="distribution-label">That version</span>
                <strong>{record.summary}</strong>
                <ChangeDiff changes={record.changes} />
            </div>
            <label className="field">
                <span>Comment for the audit trail</span>
                <input
                    data-autofocus
                    disabled={busy}
                    maxLength={280}
                    placeholder="Why are you rolling back?"
                    value={comment}
                    onChange={(event) => setComment(event.target.value)}
                />
            </label>
            {error && <p className="form-error" role="alert">{error}</p>}
            <footer>
                <button disabled={busy} type="button" onClick={onCancel}>Cancel</button>
                <button className="confirmation-confirm" disabled={busy} type="button" onClick={confirm}>
                    {busy ? "Restoring…" : `Restore version ${record.version}`}
                </button>
            </footer>
        </section>
    </Modal>, document.body);
}
