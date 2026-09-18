import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { fieldMessage } from "../../shared/api/errors.js";
import { describeServe } from "../flags/flag-format.js";
import { ClauseRow } from "./ClauseRow.jsx";

let clauseSequence = 0;
const nextClause = () => ({ localId: `clause-${clauseSequence += 1}`, attribute: "plan", op: "in", values: [], negate: false });

export function RuleBuilder({ disabled = false, error, flag, onChange, rules, segments }) {
    const variationOptions = flag.variations.map((variation) => ({ value: variation.id, label: variation.name }));
    const update = (index, patch) => onChange(rules.map((rule, current) => (current === index ? { ...rule, ...patch } : rule)));
    const move = (index, offset) => {
        const target = index + offset;
        if (target < 0 || target >= rules.length) return;
        const next = [...rules];
        [next[index], next[target]] = [next[target], next[index]];
        onChange(next);
    };
    const addRule = () => onChange([...rules, {
        description: "",
        segmentIds: [],
        clauses: [nextClause()],
        serve: { kind: "variation", variationId: flag.variations[0].id, rollout: null },
    }]);

    return <section className="rule-builder">
        <header className="panel-heading">
            <div>
                <h3>Targeting rules</h3>
                <p>Rules are evaluated in order. The first match wins, so put the most specific rule first.</p>
            </div>
            <button className="text-button" disabled={disabled} type="button" onClick={addRule}>
                <MaterialIcon size={17}>add</MaterialIcon>Add rule
            </button>
        </header>
        {rules.length === 0 && <p className="panel-empty">No rules yet. Everyone falls through to the default below.</p>}
        <ol className="rule-list">
            {rules.map((rule, index) => {
                const servesRollout = rule.serve?.kind === "rollout";
                return <li className="rule-card" key={index}>
                    <header className="rule-card-header">
                        <span className="rule-order">Rule {index + 1}</span>
                        <div className="rule-card-controls">
                            <button aria-label={`Move rule ${index + 1} earlier`} className="icon-button" disabled={disabled || index === 0} type="button" onClick={() => move(index, -1)}>
                                <MaterialIcon size={18}>expand_less</MaterialIcon>
                            </button>
                            <button aria-label={`Move rule ${index + 1} later`} className="icon-button" disabled={disabled || index === rules.length - 1} type="button" onClick={() => move(index, 1)}>
                                <MaterialIcon size={18}>expand_more</MaterialIcon>
                            </button>
                            <button aria-label={`Delete rule ${index + 1}`} className="icon-button danger" disabled={disabled} type="button" onClick={() => onChange(rules.filter((_, current) => current !== index))}>
                                <MaterialIcon size={18}>delete</MaterialIcon>
                            </button>
                        </div>
                    </header>
                    <label className="field">
                        <span>Description</span>
                        <input
                            disabled={disabled}
                            maxLength={120}
                            placeholder="Why this rule exists"
                            value={rule.description}
                            onChange={(event) => update(index, { description: event.target.value })}
                        />
                    </label>
                    <div className="rule-segments">
                        <span className="field-label">Segments</span>
                        {segments.length === 0
                            ? <p className="field-hint">No segments exist in this environment yet.</p>
                            : <div className="segment-chips">
                                {segments.map((segment) => {
                                    const selected = rule.segmentIds.includes(segment._id);
                                    return <label className={`segment-chip ${selected ? "selected" : ""}`} key={segment._id}>
                                        <input
                                            checked={selected}
                                            disabled={disabled}
                                            type="checkbox"
                                            onChange={(event) => update(index, {
                                                segmentIds: event.target.checked
                                                    ? [...rule.segmentIds, segment._id]
                                                    : rule.segmentIds.filter((id) => id !== segment._id),
                                            })}
                                        />
                                        <span>{segment.name}</span>
                                        <small>{segment.memberCount} members</small>
                                    </label>;
                                })}
                            </div>}
                        <p className="field-hint">A member matches when they are in any selected segment and meet every condition.</p>
                    </div>
                    <div className="rule-clauses">
                        <span className="field-label">Conditions</span>
                        {rule.clauses.map((clause, clauseIndex) => <ClauseRow
                            clause={clause.localId ? clause : { ...clause, localId: `clause-${index}-${clauseIndex}` }}
                            disabled={disabled}
                            error={fieldMessage(error, `rules.${index}.clauses.${clauseIndex}.values`) || fieldMessage(error, `rules.${index}.clauses.${clauseIndex}.attribute`)}
                            key={clause.localId || clauseIndex}
                            onChange={(next) => update(index, { clauses: rule.clauses.map((item, current) => (current === clauseIndex ? next : item)) })}
                            onRemove={() => update(index, { clauses: rule.clauses.filter((_, current) => current !== clauseIndex) })}
                        />)}
                        <button className="text-button" disabled={disabled} type="button" onClick={() => update(index, { clauses: [...rule.clauses, nextClause()] })}>
                            <MaterialIcon size={17}>add</MaterialIcon>Add condition
                        </button>
                    </div>
                    <div className="rule-serve">
                        <span className="field-label">Then serve</span>
                        {servesRollout
                            ? <p className="rule-serve-rollout">
                                <MaterialIcon size={15}>insights</MaterialIcon>
                                {describeServe(flag, rule.serve)}
                                <small>This rule serves a percentage split. Saving keeps it unchanged.</small>
                            </p>
                            : <SelectMenu
                                ariaLabel={`Variation served by rule ${index + 1}`}
                                className="rule-serve-menu"
                                disabled={disabled}
                                options={variationOptions}
                                value={rule.serve?.variationId || flag.variations[0].id}
                                onChange={(variationId) => update(index, { serve: { kind: "variation", variationId, rollout: null } })}
                            />}
                    </div>
                    {fieldMessage(error, `rules.${index}`) && <small className="field-error" role="alert">{fieldMessage(error, `rules.${index}`)}</small>}
                    {fieldMessage(error, `rules.${index}.segmentIds`) && <small className="field-error" role="alert">{fieldMessage(error, `rules.${index}.segmentIds`)}</small>}
                </li>;
            })}
        </ol>
    </section>;
}
