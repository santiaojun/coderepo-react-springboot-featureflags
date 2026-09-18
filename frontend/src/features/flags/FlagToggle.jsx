export function FlagToggle({ enabled, flagName, onChange, pending = false, disabled = false }) {
    return <button
        aria-busy={pending || undefined}
        aria-checked={enabled}
        aria-label={`${flagName} in this environment`}
        className="flag-toggle"
        disabled={disabled || pending}
        role="switch"
        type="button"
        onClick={(event) => { event.stopPropagation(); onChange(!enabled); }}
    >
        <span className="flag-toggle-track"><span className="flag-toggle-knob" /></span>
        <span className="flag-toggle-label">{enabled ? "On" : "Off"}</span>
    </button>;
}
