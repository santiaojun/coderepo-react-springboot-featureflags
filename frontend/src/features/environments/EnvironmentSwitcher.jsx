import { useEffect, useRef, useState } from "react";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";

export function EnvironmentSwitcher({ environments = [], onChange, value }) {
    const [open, setOpen] = useState(false);
    const anchor = useRef(null);
    const active = environments.find((environment) => environment.key === value) || environments[0];
    useEffect(() => {
        if (!open) return undefined;
        const closeOutside = (event) => { if (!anchor.current?.contains(event.target)) setOpen(false); };
        const closeOnEscape = (event) => { if (event.key === "Escape") setOpen(false); };
        document.addEventListener("pointerdown", closeOutside);
        document.addEventListener("keydown", closeOnEscape);
        return () => { document.removeEventListener("pointerdown", closeOutside); document.removeEventListener("keydown", closeOnEscape); };
    }, [open]);
    if (!active) return null;
    return <div className="environment-switcher" ref={anchor}>
        <button aria-expanded={open} aria-haspopup="listbox" aria-label={`Environment: ${active.name}`} className={`environment-trigger ${open ? "active" : ""}`} type="button" onClick={() => setOpen((current) => !current)}>
            <span className="environment-dot" style={{ "--environment-color": active.color }} />
            <span className="environment-trigger-text">
                <small>Environment</small>
                <strong>{active.name}</strong>
            </span>
            <MaterialIcon size={19}>{open ? "arrow_drop_up" : "arrow_drop_down"}</MaterialIcon>
        </button>
        {open && <div aria-label="Environment" className="environment-menu" role="listbox">
            {environments.map((environment) => <button
                aria-selected={environment.key === active.key}
                className="environment-option"
                key={environment.key}
                role="option"
                type="button"
                onClick={() => { onChange(environment.key); setOpen(false); }}
            >
                <span className="environment-dot" style={{ "--environment-color": environment.color }} />
                <span>
                    <strong>{environment.name}</strong>
                    <small>{environment.description}</small>
                </span>
                {environment.critical && <span className="environment-critical-tag">Live</span>}
            </button>)}
        </div>}
    </div>;
}
