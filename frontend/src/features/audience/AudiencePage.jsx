import { useCallback, useEffect, useState } from "react";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { attributeLabel, pluralize, relativeTime, variationValueLabel } from "../../shared/utils/format.js";
import { UserFlagMatrix } from "./UserFlagMatrix.jsx";
import { audienceApi } from "./audience.api.js";

const PAGE_LIMIT = 40;

export function AudiencePage({ environment }) {
    const [search, setSearch] = useState("");
    const [plan, setPlan] = useState("");
    const [country, setCountry] = useState("");
    const [data, setData] = useState(null);
    const [status, setStatus] = useState({ loading: true, error: "" });
    const [inspecting, setInspecting] = useState(null);

    const load = useCallback(async () => {
        setStatus({ loading: true, error: "" });
        try {
            setData(await audienceApi.list({ search: search.trim(), plan, country, limit: PAGE_LIMIT }));
            setStatus({ loading: false, error: "" });
        } catch (error) {
            setStatus({ loading: false, error: error.message });
            setData(null);
        }
    }, [search, plan, country]);

    useEffect(() => {
        const timer = setTimeout(load, 200);
        return () => clearTimeout(timer);
    }, [load]);

    const planOptions = [{ value: "", label: "Any plan" }, ...(data?.plans || []).map((value) => ({ value, label: attributeLabel(value) }))];
    const countryOptions = [{ value: "", label: "Any country" }, ...(data?.countries || []).map((value) => ({ value, label: value }))];
    const filtered = Boolean(search.trim() || plan || country);

    return <section className="audience-page" aria-labelledby="audience-page-title">
        <header className="flag-page-header">
            <div className="flag-page-heading">
                <h1 id="audience-page-title">Audience</h1>
                <p>
                    {status.loading
                        ? "Loading the audience…"
                        : `${pluralize(data?.users.length || 0, "member")} shown of ${data?.total || 0}. Open anybody to see every flag they receive in ${environment.name}.`}
                </p>
            </div>
        </header>
        <div className="flag-filters">
            <div className="flag-search">
                <MaterialIcon size={19}>search</MaterialIcon>
                <label className="sr-only" htmlFor="audience-search">Search the audience</label>
                <input
                    id="audience-search"
                    placeholder="Search by name, email, or key"
                    type="search"
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                />
            </div>
            <SelectMenu ariaLabel="Filter by plan" className="flag-filter-menu" options={planOptions} value={plan} onChange={setPlan} />
            <SelectMenu ariaLabel="Filter by country" className="flag-filter-menu" options={countryOptions} value={country} onChange={setCountry} />
        </div>

        {status.error && <div className="service-error inline" role="alert">
            <span>{status.error}</span>
            <button type="button" onClick={load}>Retry</button>
        </div>}

        {status.loading && <ul className="audience-list" aria-busy="true">
            {[0, 1, 2, 3, 4].map((index) => <li className="audience-row skeleton" key={index}><span /><span /></li>)}
        </ul>}

        {!status.loading && data?.users.length === 0 && <div className="empty-state">
            <h2>No audience members match</h2>
            <p>{filtered ? "Try a different search term or clear the filters." : "The audience is empty."}</p>
            {filtered && <button type="button" onClick={() => { setSearch(""); setPlan(""); setCountry(""); }}>Clear filters</button>}
        </div>}

        {!status.loading && data?.users.length > 0 && <ul className="audience-list">
            {data.users.map((user) => <li className="audience-row" key={user.key}>
                <button type="button" onClick={() => setInspecting(user.key)}>
                    <span className="audience-identity">
                        <strong>{user.name}</strong>
                        <small>{user.email}</small>
                        <code>{user.key}</code>
                    </span>
                    <span className="audience-attributes">
                        {Object.entries(user.attributes).map(([attribute, value]) => <span key={attribute}>
                            <small>{attributeLabel(attribute)}</small>
                            <strong>{variationValueLabel(value)}</strong>
                        </span>)}
                    </span>
                    <span className="audience-seen">Last seen {relativeTime(user.lastSeenAt)}</span>
                </button>
            </li>)}
        </ul>}

        {inspecting && <UserFlagMatrix environmentKey={environment.key} userKey={inspecting} onClose={() => setInspecting(null)} />}
    </section>;
}
