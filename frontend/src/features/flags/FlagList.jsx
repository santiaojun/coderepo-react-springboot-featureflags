import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { pluralize } from "../../shared/utils/format.js";
import { FlagListItem } from "./FlagListItem.jsx";
import { FLAG_STATE_FILTERS, FLAG_STATUS_FILTERS } from "./flag-format.js";

function FlagListSkeleton() {
    return <ul className="flag-list" aria-busy="true">
        {[0, 1, 2, 3, 4].map((index) => <li className="flag-row skeleton" key={index}>
            <span /><span /><span />
        </li>)}
    </ul>;
}

export function FlagList({
    environment, error, filters, flags, loading, onCreate, onFiltersChange, onOpen, onRetry, onToggle,
    pendingKey, tags,
}) {
    const filtered = filters.search || filters.tag || filters.state !== "all" || filters.status !== "active";
    const tagOptions = [{ value: "", label: "Any tag" }, ...tags.map((tag) => ({ value: tag, label: tag }))];
    return <section className="flag-page" aria-labelledby="flag-page-title">
        <header className="flag-page-header">
            <div className="flag-page-heading">
                <h1 id="flag-page-title">Feature flags</h1>
                <p>{loading ? "Loading flags…" : `${pluralize(flags.length, "flag")} in ${environment?.name || "this environment"}`}</p>
            </div>
            <button className="primary-button flag-create-button" type="button" onClick={onCreate}>
                <MaterialIcon size={18}>add</MaterialIcon>New flag
            </button>
        </header>
        <div className="flag-filters">
            <div className="flag-search">
                <MaterialIcon size={19}>search</MaterialIcon>
                <label className="sr-only" htmlFor="flag-search-input">Search flags</label>
                <input
                    id="flag-search-input"
                    placeholder="Search by key, name, or description"
                    type="search"
                    value={filters.search}
                    onChange={(event) => onFiltersChange({ ...filters, search: event.target.value })}
                />
            </div>
            <SelectMenu
                ariaLabel="Filter by state"
                className="flag-filter-menu"
                options={FLAG_STATE_FILTERS}
                value={filters.state}
                onChange={(state) => onFiltersChange({ ...filters, state })}
            />
            <SelectMenu
                ariaLabel="Filter by tag"
                className="flag-filter-menu"
                options={tagOptions}
                value={filters.tag}
                onChange={(tag) => onFiltersChange({ ...filters, tag })}
            />
            <SelectMenu
                ariaLabel="Filter by status"
                className="flag-filter-menu"
                options={FLAG_STATUS_FILTERS}
                value={filters.status}
                onChange={(status) => onFiltersChange({ ...filters, status })}
            />
        </div>
        {error && <div className="service-error inline" role="alert">
            <span>{error}</span>
            <button type="button" onClick={onRetry}>Retry</button>
        </div>}
        {loading && <FlagListSkeleton />}
        {!loading && !error && flags.length === 0 && <div className="empty-state">
            <h2>{filtered ? "No flags match these filters" : "No flags yet"}</h2>
            <p>{filtered
                ? "Try a different search term, tag, or state."
                : `Create the first flag for ${environment?.name || "this environment"} and it will appear in every environment.`}</p>
            {filtered
                ? <button type="button" onClick={() => onFiltersChange({ search: "", tag: "", state: "all", status: "active" })}>Clear filters</button>
                : <button type="button" onClick={onCreate}>New flag</button>}
        </div>}
        {!loading && flags.length > 0 && <ul className="flag-list">
            {flags.map((flag) => <FlagListItem
                flag={flag}
                key={flag.key}
                pending={pendingKey === flag.key}
                onOpen={onOpen}
                onToggle={onToggle}
            />)}
        </ul>}
    </section>;
}
