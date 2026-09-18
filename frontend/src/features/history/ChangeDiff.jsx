function DiffValue({ tone, value }) {
    if (Array.isArray(value)) {
        if (value.length === 0) return <span className={`diff-value ${tone} empty`}>none</span>;
        return <ul className={`diff-value ${tone} diff-list`}>
            {value.map((entry, index) => <li key={index}>{String(entry)}</li>)}
        </ul>;
    }
    if (value === null || value === undefined || value === "") {
        return <span className={`diff-value ${tone} empty`}>none</span>;
    }
    return <span className={`diff-value ${tone}`}>{String(value)}</span>;
}

export function ChangeDiff({ changes = [] }) {
    if (!changes.length) return null;
    return <dl className="change-diff">
        {changes.map((change, index) => <div className="change-diff-row" key={`${change.field}-${index}`}>
            <dt>{change.label || change.field}</dt>
            <dd>
                <DiffValue tone="before" value={change.from} />
                <span aria-hidden="true" className="diff-arrow">→</span>
                <DiffValue tone="after" value={change.to} />
            </dd>
        </div>)}
    </dl>;
}
