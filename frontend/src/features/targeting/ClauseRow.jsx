import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { ATTRIBUTE_OPTIONS, OPERATORS, formatClauseValues, parseClauseValues } from "./clause-operators.js";

export function ClauseRow({ clause, disabled = false, error = "", onChange, onRemove }) {
    return <div className="clause-row">
        <div className="clause-field">
            <label className="sr-only" htmlFor={`clause-attribute-${clause.localId}`}>Attribute</label>
            <SelectMenu
                ariaLabel="Attribute"
                disabled={disabled}
                options={ATTRIBUTE_OPTIONS}
                value={clause.attribute}
                onChange={(attribute) => onChange({ ...clause, attribute })}
            />
        </div>
        <div className="clause-field clause-operator">
            <SelectMenu
                ariaLabel="Operator"
                disabled={disabled}
                options={OPERATORS}
                value={clause.op}
                onChange={(op) => onChange({ ...clause, op })}
            />
        </div>
        <div className="clause-field clause-values">
            <label className="sr-only" htmlFor={`clause-values-${clause.localId}`}>Values</label>
            <input
                disabled={disabled}
                id={`clause-values-${clause.localId}`}
                placeholder="Comma separated, for example DE, FR"
                value={formatClauseValues(clause.values)}
                onChange={(event) => onChange({ ...clause, values: parseClauseValues(event.target.value) })}
            />
        </div>
        <label className="clause-negate">
            <input
                checked={clause.negate}
                disabled={disabled}
                type="checkbox"
                onChange={(event) => onChange({ ...clause, negate: event.target.checked })}
            />
            <span>Invert</span>
        </label>
        <button aria-label="Remove condition" className="icon-button" disabled={disabled} type="button" onClick={onRemove}>
            <MaterialIcon size={18}>close</MaterialIcon>
        </button>
        {error && <small className="field-error clause-error" role="alert">{error}</small>}
    </div>;
}
