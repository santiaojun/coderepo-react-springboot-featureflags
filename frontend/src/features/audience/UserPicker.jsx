import { useEffect, useId, useRef, useState } from "react";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { audienceApi } from "./audience.api.js";

const RESULT_LIMIT = 8;

export function UserPicker({ excludeKeys = [], label = "Add an audience member", onSelect, placeholder = "Search by name, email, or key" }) {
    const listId = `user-picker-${useId().replace(/:/g, "")}`;
    const [term, setTerm] = useState("");
    const [results, setResults] = useState([]);
    const [status, setStatus] = useState({ loading: false, error: "" });
    const [open, setOpen] = useState(false);
    const anchor = useRef(null);

    useEffect(() => {
        if (!open) return undefined;
        let active = true;
        const timer = setTimeout(async () => {
            setStatus({ loading: true, error: "" });
            try {
                const data = await audienceApi.list({ search: term.trim(), limit: RESULT_LIMIT });
                if (!active) return;
                setResults(data.users.filter((user) => !excludeKeys.includes(user.key)));
                setStatus({ loading: false, error: "" });
            } catch (error) {
                if (active) setStatus({ loading: false, error: error.message });
            }
        }, 180);
        return () => { active = false; clearTimeout(timer); };
    }, [term, open, excludeKeys]);

    useEffect(() => {
        if (!open) return undefined;
        const closeOutside = (event) => { if (!anchor.current?.contains(event.target)) setOpen(false); };
        document.addEventListener("pointerdown", closeOutside);
        return () => document.removeEventListener("pointerdown", closeOutside);
    }, [open]);

    const choose = (user) => {
        onSelect(user);
        setTerm("");
        setOpen(false);
    };

    return <div className="user-picker" ref={anchor}>
        <div className={`user-picker-field ${open ? "active" : ""}`}>
            <MaterialIcon size={18}>search</MaterialIcon>
            <label className="sr-only" htmlFor={`${listId}-input`}>{label}</label>
            <input
                aria-controls={listId}
                aria-expanded={open}
                autoComplete="off"
                id={`${listId}-input`}
                placeholder={placeholder}
                role="combobox"
                type="text"
                value={term}
                onChange={(event) => { setTerm(event.target.value); setOpen(true); }}
                onFocus={() => setOpen(true)}
                onKeyDown={(event) => { if (event.key === "Escape") { setOpen(false); event.stopPropagation(); } }}
            />
        </div>
        {open && <div className="user-picker-results" id={listId} role="listbox" aria-label={label}>
            {status.loading && <p className="user-picker-state">Searching…</p>}
            {!status.loading && status.error && <p className="user-picker-state error" role="alert">{status.error}</p>}
            {!status.loading && !status.error && results.length === 0 && <p className="user-picker-state">No audience members match.</p>}
            {!status.loading && results.map((user) => <button key={user.key} role="option" aria-selected="false" type="button" onClick={() => choose(user)}>
                <span className="user-picker-name">
                    <strong>{user.name}</strong>
                    <small>{user.email}</small>
                </span>
                <span className="user-picker-attributes">
                    <code>{user.key}</code>
                    <span>{user.attributes.plan} · {user.attributes.country}</span>
                </span>
            </button>)}
        </div>}
    </div>;
}
