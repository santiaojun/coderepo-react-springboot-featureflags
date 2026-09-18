import { useCallback, useEffect, useState } from "react";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { pluralize } from "../../shared/utils/format.js";
import { ChangeTimeline } from "./ChangeTimeline.jsx";
import { historyApi } from "./history.api.js";

const ACTIONS = [
    { value: "", label: "Every action" },
    { value: "flag_created", label: "Flag created" },
    { value: "flag_updated", label: "Flag updated" },
    { value: "toggled", label: "Turned on or off" },
    { value: "targeting_updated", label: "Targeting updated" },
    { value: "rollout_updated", label: "Rollout updated" },
    { value: "rolled_back", label: "Rolled back" },
    { value: "flag_retired", label: "Flag retired" },
    { value: "flag_restored", label: "Flag restored" },
];

export function HistoryPage({ environment, members }) {
    const [action, setAction] = useState("");
    const [actorId, setActorId] = useState("");
    const [records, setRecords] = useState([]);
    const [status, setStatus] = useState({ loading: true, error: "" });

    const load = useCallback(async () => {
        setStatus({ loading: true, error: "" });
        try {
            setRecords(await historyApi.list({ environment: environment.key, action, actorId, limit: 100 }));
            setStatus({ loading: false, error: "" });
        } catch (error) {
            setStatus({ loading: false, error: error.message });
            setRecords([]);
        }
    }, [environment.key, action, actorId]);

    useEffect(() => { load(); }, [load]);

    const actorOptions = [
        { value: "", label: "Everyone" },
        ...members.map((member) => ({ value: member._id, label: member.name })),
    ];

    return <section className="history-page" aria-labelledby="history-page-title">
        <header className="flag-page-header">
            <div className="flag-page-heading">
                <h1 id="history-page-title">Change history</h1>
                <p>
                    {status.loading
                        ? "Loading changes…"
                        : `${pluralize(records.length, "change")} in ${environment.name}, newest first`}
                </p>
            </div>
        </header>
        <div className="flag-filters">
            <SelectMenu ariaLabel="Filter by action" className="flag-filter-menu" options={ACTIONS} value={action} onChange={setAction} />
            <SelectMenu ariaLabel="Filter by member" className="flag-filter-menu" options={actorOptions} value={actorId} onChange={setActorId} />
        </div>
        <ChangeTimeline
            emptyMessage="No changes match these filters yet."
            error={status.error}
            loading={status.loading}
            records={records}
            showFlag
            onRetry={load}
        />
    </section>;
}
