import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { fieldMessage } from "../../shared/api/errors.js";

const MAX_VARIATIONS = 8;

export function VariationEditor({ disabled = false, error, kind, onChange, variations }) {
    const boolean = kind === "boolean";
    const update = (index, patch) =>
        onChange(variations.map((variation, current) => (current === index ? { ...variation, ...patch } : variation)));
    const add = () => onChange([...variations, { name: "", value: "" }]);
    const remove = (index) => onChange(variations.filter((_, current) => current !== index));
    return <fieldset className="variation-editor">
        <legend>Variations</legend>
        <p className="field-hint">
            {boolean
                ? "A boolean flag always serves true first and false second. Name them so the audit trail reads clearly."
                : "Give every variation a distinct value. The value is what your application receives."}
        </p>
        {variations.map((variation, index) => <div className="variation-row" key={index}>
            <span className="variation-index">v{index + 1}</span>
            <label className="field">
                <span>Name</span>
                <input
                    disabled={disabled}
                    maxLength={60}
                    value={variation.name}
                    onChange={(event) => update(index, { name: event.target.value })}
                />
                {fieldMessage(error, `variations.${index}.name`) && <small className="field-error" role="alert">{fieldMessage(error, `variations.${index}.name`)}</small>}
            </label>
            <label className="field">
                <span>Value</span>
                {boolean
                    ? <input disabled readOnly value={index === 0 ? "true" : "false"} />
                    : <input
                        disabled={disabled}
                        maxLength={200}
                        placeholder="e.g. compact"
                        value={variation.value}
                        onChange={(event) => update(index, { value: event.target.value })}
                    />}
                {fieldMessage(error, `variations.${index}.value`) && <small className="field-error" role="alert">{fieldMessage(error, `variations.${index}.value`)}</small>}
            </label>
            {!boolean && variations.length > 2 && <button
                aria-label={`Remove variation ${index + 1}`}
                className="icon-button"
                disabled={disabled}
                type="button"
                onClick={() => remove(index)}
            ><MaterialIcon size={18}>delete</MaterialIcon></button>}
        </div>)}
        {fieldMessage(error, "variations") && <small className="field-error" role="alert">{fieldMessage(error, "variations")}</small>}
        {!boolean && variations.length < MAX_VARIATIONS && <button className="text-button" disabled={disabled} type="button" onClick={add}>
            <MaterialIcon size={17}>add</MaterialIcon>Add variation
        </button>}
    </fieldset>;
}
