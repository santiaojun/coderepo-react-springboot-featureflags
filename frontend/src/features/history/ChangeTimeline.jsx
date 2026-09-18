import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { fullTime, relativeTime, titleCase } from "../../shared/utils/format.js";
import { ChangeDiff } from "./ChangeDiff.jsx";

const ACTION_ICONS = {
    flag_created: "add",
    flag_updated: "edit",
    toggled: "sync",
    targeting_updated: "group",
    rollout_updated: "insights",
    rolled_back: "history",
    flag_retired: "delete",
    flag_restored: "check_circle",
};

export function ChangeTimeline({
    currentVersion = null, emptyMessage = "No changes recorded yet.", error = "", loading = false,
    onRestore, onRetry, records = [], showFlag = false,
}) {
    if (loading) {
        return <ol className="change-timeline" aria-busy="true">
            {[0, 1, 2].map((index) => <li className="change-entry skeleton" key={index}><span /><span /></li>)}
        </ol>;
    }
    if (error) {
        return <div className="service-error inline" role="alert">
            <span>{error}</span>
            {onRetry && <button type="button" onClick={onRetry}>Retry</button>}
        </div>;
    }
    if (!records.length) {
        return <div className="empty-state compact"><h2>Nothing to show</h2><p>{emptyMessage}</p></div>;
    }
    return <ol className="change-timeline">
        {records.map((record) => {
            const restorable = Boolean(onRestore) && record.version > 0 && record.version !== currentVersion
                && record.environmentKey;
            return <li className="change-entry" key={record._id}>
                <span className={`change-icon action-${record.action}`} aria-hidden="true">
                    <MaterialIcon size={17}>{ACTION_ICONS[record.action] || "subject"}</MaterialIcon>
                </span>
                <div className="change-body">
                    <div className="change-headline">
                        <strong>{record.summary}</strong>
                        {record.version > 0 && <span className="change-version">v{record.version}</span>}
                        {record.environmentKey && <span className="change-environment">{titleCase(record.environmentKey)}</span>}
                        {record.version === currentVersion && record.environmentKey && <span className="change-current">Current</span>}
                    </div>
                    {showFlag && <div className="change-flag"><code>{record.flagKey}</code><span>{record.flagName}</span></div>}
                    <div className="change-meta">
                        <span>{record.actorName}</span>
                        <time dateTime={record.createdAt} title={fullTime(record.createdAt)}>{relativeTime(record.createdAt)}</time>
                        <span className="change-action">{titleCase(record.action)}</span>
                    </div>
                    {record.comment && <p className="change-comment">“{record.comment}”</p>}
                    {record.restoredFromVersion && <p className="change-restored">
                        <MaterialIcon size={14}>history</MaterialIcon>
                        Restored the configuration from version {record.restoredFromVersion}
                    </p>}
                    <ChangeDiff changes={record.changes} />
                    {restorable && <button className="text-button" type="button" onClick={() => onRestore(record)}>
                        <MaterialIcon size={16}>history</MaterialIcon>Restore version {record.version}
                    </button>}
                </div>
            </li>;
        })}
    </ol>;
}
