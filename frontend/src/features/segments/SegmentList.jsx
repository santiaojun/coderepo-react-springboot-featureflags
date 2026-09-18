import { useCallback, useEffect, useState } from "react";
import { ConfirmationDialog } from "../../shared/components/ConfirmationDialog.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { pluralize, relativeTime } from "../../shared/utils/format.js";
import { clauseSummary } from "../targeting/clause-operators.js";
import { SegmentEditor } from "./SegmentEditor.jsx";
import { SegmentMembersDrawer } from "./SegmentMembersDrawer.jsx";
import { segmentApi } from "./segment.api.js";

export function SegmentList({ environment, onSegmentsChanged }) {
    const [data, setData] = useState(null);
    const [status, setStatus] = useState({ loading: true, error: "" });
    const [editing, setEditing] = useState(null);
    const [creating, setCreating] = useState(false);
    const [viewing, setViewing] = useState(null);
    const [deleting, setDeleting] = useState(null);
    const [deleteError, setDeleteError] = useState("");
    const [deleteBusy, setDeleteBusy] = useState(false);

    const load = useCallback(async () => {
        setStatus({ loading: true, error: "" });
        try {
            setData(await segmentApi.list(environment.key));
            setStatus({ loading: false, error: "" });
        } catch (error) {
            setStatus({ loading: false, error: error.message });
            setData(null);
        }
    }, [environment.key]);

    useEffect(() => { load(); }, [load]);

    const finish = async () => {
        setCreating(false);
        setEditing(null);
        await load();
        onSegmentsChanged?.();
    };

    const remove = async () => {
        setDeleteBusy(true);
        setDeleteError("");
        try {
            await segmentApi.remove(deleting.key, environment.key);
            setDeleting(null);
            await load();
            onSegmentsChanged?.();
        } catch (error) {
            const blocked = error.details?.flagKeys;
            setDeleteError(blocked ? `${error.message} Used by: ${blocked.join(", ")}.` : error.message);
        } finally {
            setDeleteBusy(false);
        }
    };

    return <section className="segment-page" aria-labelledby="segment-page-title">
        <header className="flag-page-header">
            <div className="flag-page-heading">
                <h1 id="segment-page-title">Segments</h1>
                <p>
                    {status.loading
                        ? "Loading segments…"
                        : `${pluralize(data?.segments.length || 0, "segment")} in ${environment.name}, matched against ${data?.audienceSize || 0} audience members`}
                </p>
            </div>
            <button className="primary-button flag-create-button" type="button" onClick={() => setCreating(true)}>
                <MaterialIcon size={18}>add</MaterialIcon>New segment
            </button>
        </header>

        {status.error && <div className="service-error inline" role="alert">
            <span>{status.error}</span>
            <button type="button" onClick={load}>Retry</button>
        </div>}

        {status.loading && <ul className="segment-grid" aria-busy="true">
            {[0, 1, 2].map((index) => <li className="segment-card skeleton" key={index}><span /><span /></li>)}
        </ul>}

        {!status.loading && data?.segments.length === 0 && <div className="empty-state">
            <h2>No segments in {environment.name}</h2>
            <p>Group your audience by plan, country, or any attribute, then serve a variation to the whole group.</p>
            <button type="button" onClick={() => setCreating(true)}>New segment</button>
        </div>}

        {!status.loading && data?.segments.length > 0 && <ul className="segment-grid">
            {data.segments.map((segment) => <li className="segment-card" key={segment._id}>
                <header>
                    <div>
                        <strong>{segment.name}</strong>
                        <code>{segment.key}</code>
                    </div>
                    <span className="segment-member-count">{pluralize(segment.memberCount, "member")}</span>
                </header>
                {segment.description && <p className="segment-description">{segment.description}</p>}
                <ul className="segment-rules">
                    {segment.rules.map((rule) => <li key={rule.id}>
                        {rule.clauses.map((clause, index) => <span className="clause-pill" key={index}>{clauseSummary(clause)}</span>)}
                    </li>)}
                    {segment.includedUserKeys.length > 0 && <li>
                        <span className="clause-pill included">{pluralize(segment.includedUserKeys.length, "member")} always included</span>
                    </li>}
                    {segment.excludedUserKeys.length > 0 && <li>
                        <span className="clause-pill excluded">{pluralize(segment.excludedUserKeys.length, "member")} always excluded</span>
                    </li>}
                </ul>
                {segment.usedByFlagKeys.length > 0 && <p className="segment-usage">
                    <MaterialIcon size={14}>subject</MaterialIcon>
                    Used by {segment.usedByFlagKeys.join(", ")}
                </p>}
                <footer className="segment-card-footer">
                    <span className="segment-modified">
                        {segment.lastModifiedByName ? `${segment.lastModifiedByName} · ` : ""}{relativeTime(segment.updatedAt)}
                    </span>
                    <div>
                        <button className="text-button" type="button" onClick={() => setViewing(segment.key)}>View members</button>
                        <button className="text-button" type="button" onClick={() => setEditing(segment)}>Edit</button>
                        <button className="text-button danger" type="button" onClick={() => { setDeleting(segment); setDeleteError(""); }}>Delete</button>
                    </div>
                </footer>
            </li>)}
        </ul>}

        {creating && <SegmentEditor
            environment={environment}
            onClose={() => setCreating(false)}
            onSave={async (payload) => { await segmentApi.create(environment.key, payload); await finish(); }}
        />}
        {editing && <SegmentEditor
            environment={environment}
            segment={editing}
            onClose={() => setEditing(null)}
            onSave={async (payload) => { await segmentApi.update(editing.key, environment.key, payload); await finish(); }}
        />}
        {viewing && <SegmentMembersDrawer environmentKey={environment.key} segmentKey={viewing} onClose={() => setViewing(null)} />}
        {deleting && <ConfirmationDialog
            busy={deleteBusy}
            confirmLabel="Delete segment"
            error={deleteError}
            message={`Deleting ${deleting.key} removes it from ${environment.name} only. Flags that reference it must be updated first.`}
            title={`Delete ${deleting.name}?`}
            onCancel={() => setDeleting(null)}
            onConfirm={remove}
        />}
    </section>;
}
