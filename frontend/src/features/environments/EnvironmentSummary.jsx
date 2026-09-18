import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";

function SummaryStat({ label, value }) {
    return <div className="environment-stat"><strong>{value}</strong><span>{label}</span></div>;
}

export function EnvironmentSummary({ error = "", loading = false, onRetry, summary }) {
    if (loading) {
        return <section className="environment-summary" aria-busy="true">
            <h2>This environment</h2>
            <div className="environment-stats">
                {["", "", "", ""].map((_, index) => <div className="environment-stat skeleton" key={index} />)}
            </div>
        </section>;
    }
    if (error) {
        return <section className="environment-summary">
            <h2>This environment</h2>
            <div className="sidebar-error" role="alert">
                <span>{error}</span>
                <button type="button" onClick={onRetry}>Retry</button>
            </div>
        </section>;
    }
    if (!summary) return null;
    return <section className="environment-summary">
        <h2>This environment</h2>
        <div className="environment-stats">
            <SummaryStat label="Flags on" value={summary.enabledCount} />
            <SummaryStat label="Flags off" value={summary.disabledCount} />
            <SummaryStat label="Rolling out" value={summary.rollingOutCount} />
            <SummaryStat label="With rules" value={summary.targetedCount} />
        </div>
        <p className="environment-summary-note">
            <MaterialIcon size={15}>group</MaterialIcon>
            {summary.segmentCount} segments defined here
        </p>
        {summary.retiredCount > 0 && <p className="environment-summary-note">
            <MaterialIcon size={15}>history</MaterialIcon>
            {summary.retiredCount} retired {summary.retiredCount === 1 ? "flag" : "flags"} kept for audit
        </p>}
    </section>;
}
