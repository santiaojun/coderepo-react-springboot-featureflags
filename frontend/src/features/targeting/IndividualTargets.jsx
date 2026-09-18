import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { fieldMessage } from "../../shared/api/errors.js";
import { UserPicker } from "../audience/UserPicker.jsx";

export function IndividualTargets({ disabled = false, error, flag, onChange, targets }) {
    const targetFor = (variationId) => targets.find((target) => target.variationId === variationId);
    const allKeys = targets.flatMap((target) => target.userKeys);

    const addUser = (variationId, userKey) => {
        const existing = targetFor(variationId);
        const cleaned = targets
            .map((target) => ({ ...target, userKeys: target.userKeys.filter((key) => key !== userKey) }))
            .filter((target) => target.variationId !== variationId);
        onChange([...cleaned, { variationId, userKeys: [...(existing?.userKeys || []).filter((key) => key !== userKey), userKey] }]);
    };

    const removeUser = (variationId, userKey) => onChange(targets
        .map((target) => (target.variationId === variationId
            ? { ...target, userKeys: target.userKeys.filter((key) => key !== userKey) }
            : target))
        .filter((target) => target.userKeys.length > 0));

    return <section className="individual-targets">
        <header className="panel-heading">
            <div>
                <h3>Individual targets</h3>
                <p>Named members always receive the variation listed here, ahead of every rule and rollout.</p>
            </div>
        </header>
        {fieldMessage(error, "targets") && <small className="field-error" role="alert">{fieldMessage(error, "targets")}</small>}
        <div className="target-groups">
            {flag.variations.map((variation, index) => {
                const target = targetFor(variation.id);
                const keys = target?.userKeys || [];
                return <div className="target-group" key={variation.id}>
                    <header>
                        <span className="variation-index">v{index + 1}</span>
                        <strong>{variation.name}</strong>
                        <span className="target-count">{keys.length ? `${keys.length} targeted` : "None targeted"}</span>
                    </header>
                    {keys.length > 0 && <ul className="target-keys">
                        {keys.map((key) => <li key={key}>
                            <code>{key}</code>
                            <button aria-label={`Remove ${key} from ${variation.name}`} className="icon-button" disabled={disabled} type="button" onClick={() => removeUser(variation.id, key)}>
                                <MaterialIcon size={16}>close</MaterialIcon>
                            </button>
                        </li>)}
                    </ul>}
                    {!disabled && <UserPicker
                        excludeKeys={allKeys}
                        label={`Target a member with ${variation.name}`}
                        placeholder="Add a member"
                        onSelect={(user) => addUser(variation.id, user.key)}
                    />}
                    {fieldMessage(error, `targets.${targets.indexOf(target)}.userKeys`) && <small className="field-error" role="alert">
                        {fieldMessage(error, `targets.${targets.indexOf(target)}.userKeys`)}
                    </small>}
                </div>;
            })}
        </div>
    </section>;
}
