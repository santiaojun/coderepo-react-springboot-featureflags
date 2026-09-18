import { useCallback, useEffect, useState } from "react";
import { ConfirmationDialog } from "../../shared/components/ConfirmationDialog.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { relativeTime } from "../../shared/utils/format.js";
import { ChangeTimeline } from "../history/ChangeTimeline.jsx";
import { RollbackDialog } from "../history/RollbackDialog.jsx";
import { historyApi } from "../history/history.api.js";
import { RolloutPanel } from "../rollouts/RolloutPanel.jsx";
import { TargetingPanel } from "../targeting/TargetingPanel.jsx";
import { targetingApi } from "../targeting/targeting.api.js";
import { FlagSettings } from "./FlagSettings.jsx";
import { FlagToggle } from "./FlagToggle.jsx";
import { flagApi } from "./flag.api.js";
import { servingLabel } from "./flag-format.js";

const TABS = [
    { value: "targeting", label: "Targeting" },
    { value: "rollout", label: "Rollout" },
    { value: "history", label: "History" },
    { value: "settings", label: "Settings" },
];

function DetailSkeleton() {
    return <div className="flag-detail-loading" role="status" aria-busy="true">
        <span /><span /><span />
    </div>;
}

export function FlagDetail({ environment, flagKey, members, onBack, onChanged, segments }) {
    const [detail, setDetail] = useState(null);
    const [status, setStatus] = useState({ loading: true, error: "" });
    const [tab, setTab] = useState("targeting");
    const [history, setHistory] = useState([]);
    const [historyStatus, setHistoryStatus] = useState({ loading: true, error: "" });
    const [distribution, setDistribution] = useState(null);
    const [togglePending, setTogglePending] = useState(false);
    const [toggleError, setToggleError] = useState("");
    const [pendingToggle, setPendingToggle] = useState(null);
    const [restoring, setRestoring] = useState(null);
    const [restoreError, setRestoreError] = useState("");

    const load = useCallback(async () => {
        setStatus({ loading: true, error: "" });
        try {
            setDetail(await flagApi.get(flagKey, environment.key));
            setStatus({ loading: false, error: "" });
        } catch (error) {
            setStatus({ loading: false, error: error.message });
            setDetail(null);
        }
    }, [flagKey, environment.key]);

    const loadHistory = useCallback(async () => {
        setHistoryStatus({ loading: true, error: "" });
        try {
            setHistory(await historyApi.list({ flagKey, environment: environment.key, limit: 100 }));
            setHistoryStatus({ loading: false, error: "" });
        } catch (error) {
            setHistoryStatus({ loading: false, error: error.message });
        }
    }, [flagKey, environment.key]);

    const loadDistribution = useCallback(async () => {
        try {
            setDistribution(await targetingApi.distribution(flagKey, environment.key));
        } catch {
            setDistribution(null);
        }
    }, [flagKey, environment.key]);

    useEffect(() => { load(); }, [load]);
    useEffect(() => { loadHistory(); loadDistribution(); }, [loadHistory, loadDistribution]);

    const refresh = async () => {
        await Promise.all([load(), loadHistory(), loadDistribution()]);
        onChanged?.();
    };

    const applyToggle = async (next) => {
        setTogglePending(true);
        setToggleError("");
        try {
            await flagApi.toggle(flagKey, environment.key, { enabled: next, version: detail.configuration.version });
            setPendingToggle(null);
            await refresh();
        } catch (error) {
            setToggleError(error.message);
        } finally {
            setTogglePending(false);
        }
    };

    const requestToggle = (next) => {
        if (environment.critical) setPendingToggle(next);
        else applyToggle(next);
    };

    const restore = async (version, comment) => {
        setRestoreError("");
        try {
            await targetingApi.restore(flagKey, environment.key, version, comment);
            setRestoring(null);
            await refresh();
            return true;
        } catch (error) {
            setRestoreError(error.message);
            return false;
        }
    };

    if (status.loading) return <section className="flag-detail">{<DetailSkeleton />}</section>;

    if (status.error) {
        return <section className="flag-detail">
            <button className="text-button" type="button" onClick={onBack}>
                <MaterialIcon size={17}>arrow_back</MaterialIcon>All flags
            </button>
            <div className="empty-state">
                <h2>This flag could not be loaded</h2>
                <p>{status.error}</p>
                <button type="button" onClick={load}>Retry</button>
            </div>
        </section>;
    }

    const { flag, configuration } = detail;

    return <section className="flag-detail" aria-labelledby="flag-detail-title">
        <button className="text-button flag-detail-back" type="button" onClick={onBack}>
            <MaterialIcon size={17}>arrow_back</MaterialIcon>All flags
        </button>

        <header className="flag-detail-header">
            <div className="flag-detail-identity">
                <h1 id="flag-detail-title">{flag.name}</h1>
                <code>{flag.key}</code>
                <div className="flag-detail-tags">
                    <span className="flag-tag">{flag.kind === "boolean" ? "Boolean" : "Multivariate"}</span>
                    {flag.status === "retired" && <span className="flag-tag flag-tag-retired">Retired</span>}
                    {!flag.temporary && <span className="flag-tag">Permanent</span>}
                    {flag.tags.map((tag) => <span className="flag-tag" key={tag}>{tag}</span>)}
                </div>
                {flag.description && <p className="flag-detail-description">{flag.description}</p>}
            </div>
            <div className="flag-detail-state">
                <span className="flag-detail-serving">
                    In {environment.name} serving <strong>{servingLabel(flag, configuration)}</strong>
                </span>
                <FlagToggle
                    disabled={flag.status === "retired"}
                    enabled={configuration.enabled}
                    flagName={flag.name}
                    pending={togglePending}
                    onChange={requestToggle}
                />
                <span className="flag-detail-version">
                    v{configuration.version} · {configuration.lastModifiedByName} {relativeTime(configuration.lastModifiedAt)}
                </span>
            </div>
        </header>

        {toggleError && <div className="service-error inline" role="alert">
            <span>{toggleError}</span>
            <button type="button" onClick={refresh}>Reload</button>
        </div>}

        <ul className="environment-chips">
            {detail.environments.map((entry) => <li className={entry.key === environment.key ? "current" : ""} key={entry.key}>
                <span className="environment-dot" style={{ "--environment-color": entry.color }} />
                <strong>{entry.name}</strong>
                <span className={`environment-chip-state ${entry.enabled ? "on" : "off"}`}>{entry.enabled ? "On" : "Off"}</span>
                <small>{entry.serving}</small>
            </li>)}
        </ul>

        <nav className="detail-tabs" aria-label="Flag sections">
            {TABS.map((entry) => <button
                aria-current={tab === entry.value ? "page" : undefined}
                className={tab === entry.value ? "active" : ""}
                key={entry.value}
                type="button"
                onClick={() => setTab(entry.value)}
            >{entry.label}</button>)}
        </nav>

        <div className="detail-body">
            {tab === "targeting" && <TargetingPanel
                configuration={configuration}
                environment={environment}
                flag={flag}
                segments={segments}
                onGoToRollout={() => setTab("rollout")}
                onSave={async (payload) => { await targetingApi.update(flagKey, environment.key, payload); await refresh(); }}
            />}
            {tab === "rollout" && <RolloutPanel
                configuration={configuration}
                distribution={distribution}
                environment={environment}
                flag={flag}
                onSaved={refresh}
            />}
            {tab === "history" && <ChangeTimeline
                currentVersion={configuration.version}
                emptyMessage={`No changes recorded for ${flag.key} in ${environment.name}.`}
                error={historyStatus.error}
                loading={historyStatus.loading}
                records={history}
                onRestore={(record) => { setRestoring(record); setRestoreError(""); }}
                onRetry={loadHistory}
            />}
            {tab === "settings" && <FlagSettings
                flag={flag}
                members={members}
                onDelete={async () => { await flagApi.remove(flagKey); onChanged?.(); onBack(); }}
                onRestore={async () => { await flagApi.restore(flagKey); await refresh(); }}
                onRetire={async () => { await flagApi.retire(flagKey); await refresh(); }}
                onSave={async (payload) => { await flagApi.update(flagKey, payload); await refresh(); }}
            />}
        </div>

        {pendingToggle !== null && <ConfirmationDialog
            busy={togglePending}
            confirmLabel={pendingToggle ? `Turn on in ${environment.name}` : `Turn off in ${environment.name}`}
            error={toggleError}
            message={pendingToggle
                ? `${flag.key} will start serving ${servingLabel(flag, configuration)} to live traffic in ${environment.name}.`
                : `${flag.key} will serve its off variation to everybody in ${environment.name}.`}
            title={`${pendingToggle ? "Turn on" : "Turn off"} in ${environment.name}?`}
            onCancel={() => { setPendingToggle(null); setToggleError(""); }}
            onConfirm={() => applyToggle(pendingToggle)}
        />}

        {restoring && <RollbackDialog
            environmentName={environment.name}
            error={restoreError}
            record={restoring}
            onCancel={() => setRestoring(null)}
            onConfirm={restore}
        />}
    </section>;
}
