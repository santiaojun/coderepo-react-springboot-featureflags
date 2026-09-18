import { useEffect, useMemo, useState } from "react";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { fieldMessage, formMessage } from "../../shared/api/errors.js";
import { describeServe } from "../flags/flag-format.js";
import { IndividualTargets } from "./IndividualTargets.jsx";
import { RuleBuilder } from "./RuleBuilder.jsx";
import { EvaluationTester } from "./EvaluationTester.jsx";

const draftOf = (configuration) => ({
    offVariationId: configuration.offVariationId,
    targets: configuration.targets.map((target) => ({ ...target, userKeys: [...target.userKeys] })),
    rules: configuration.rules.map((rule, index) => ({
        description: rule.description,
        segmentIds: [...rule.segmentIds],
        clauses: rule.clauses.map((clause, clauseIndex) => ({ ...clause, localId: `clause-${index}-${clauseIndex}` })),
        serve: rule.serve,
    })),
    fallthrough: configuration.fallthrough,
});

export function TargetingPanel({ configuration, environment, flag, onGoToRollout, onSave, segments }) {
    const [draft, setDraft] = useState(() => draftOf(configuration));
    const [comment, setComment] = useState("");
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => { setDraft(draftOf(configuration)); setComment(""); setError(null); }, [configuration]);

    const variationOptions = flag.variations.map((variation) => ({ value: variation.id, label: variation.name }));
    const dirty = useMemo(() => JSON.stringify(draft) !== JSON.stringify(draftOf(configuration)), [draft, configuration]);
    const fallthroughIsRollout = draft.fallthrough?.kind === "rollout";

    const submit = async (event) => {
        event.preventDefault();
        setSaving(true);
        setError(null);
        try {
            await onSave({
                offVariationId: draft.offVariationId,
                targets: draft.targets
                    .filter((target) => target.userKeys.length > 0)
                    .map((target) => ({ variationId: target.variationId, userKeys: target.userKeys })),
                rules: draft.rules.map((rule) => ({
                    description: rule.description,
                    segmentIds: rule.segmentIds,
                    clauses: rule.clauses.map((clause) => ({
                        attribute: clause.attribute, op: clause.op, values: clause.values, negate: clause.negate,
                    })),
                    serve: rule.serve?.kind === "rollout"
                        ? { kind: "rollout", rollout: rule.serve.rollout }
                        : { kind: "variation", variationId: rule.serve.variationId },
                })),
                fallthrough: fallthroughIsRollout
                    ? { kind: "rollout", rollout: draft.fallthrough.rollout }
                    : { kind: "variation", variationId: draft.fallthrough.variationId },
                version: configuration.version,
                comment: comment.trim(),
            });
        } catch (saveError) {
            setError(saveError);
        } finally {
            setSaving(false);
        }
    };

    return <form className="targeting-panel" onSubmit={submit}>
        {formMessage(error) && <p className="form-error" role="alert">{formMessage(error)}</p>}

        <IndividualTargets
            disabled={saving}
            error={error}
            flag={flag}
            targets={draft.targets}
            onChange={(targets) => setDraft((current) => ({ ...current, targets }))}
        />

        <RuleBuilder
            disabled={saving}
            error={error}
            flag={flag}
            rules={draft.rules}
            segments={segments}
            onChange={(rules) => setDraft((current) => ({ ...current, rules }))}
        />

        <section className="fallthrough-panel">
            <header className="panel-heading">
                <div>
                    <h3>Everyone else</h3>
                    <p>What members receive when no individual target and no rule applies.</p>
                </div>
            </header>
            {fallthroughIsRollout
                ? <p className="rule-serve-rollout">
                    <MaterialIcon size={15}>insights</MaterialIcon>
                    {describeServe(flag, draft.fallthrough)}
                    <button className="text-button" type="button" onClick={onGoToRollout}>Edit in Rollout</button>
                </p>
                : <div className="field">
                    <span>Serve</span>
                    <SelectMenu
                        ariaLabel="Variation served to everyone else"
                        disabled={saving}
                        options={variationOptions}
                        value={draft.fallthrough.variationId}
                        onChange={(variationId) => setDraft((current) => ({ ...current, fallthrough: { kind: "variation", variationId, rollout: null } }))}
                    />
                    {fieldMessage(error, "fallthrough.variationId") && <small className="field-error" role="alert">{fieldMessage(error, "fallthrough.variationId")}</small>}
                </div>}
            <div className="field">
                <span>Serve when this flag is off</span>
                <SelectMenu
                    ariaLabel="Variation served when the flag is off"
                    disabled={saving}
                    options={variationOptions}
                    value={draft.offVariationId}
                    onChange={(offVariationId) => setDraft((current) => ({ ...current, offVariationId }))}
                />
                {fieldMessage(error, "offVariationId") && <small className="field-error" role="alert">{fieldMessage(error, "offVariationId")}</small>}
            </div>
        </section>

        <footer className={`targeting-actions ${dirty ? "visible" : ""}`}>
            <label className="field targeting-comment">
                <span>Comment for the audit trail</span>
                <input
                    disabled={saving}
                    maxLength={280}
                    placeholder={`Why are you changing targeting in ${environment.name}?`}
                    value={comment}
                    onChange={(event) => setComment(event.target.value)}
                />
                {fieldMessage(error, "comment") && <small className="field-error" role="alert">{fieldMessage(error, "comment")}</small>}
            </label>
            <div className="targeting-buttons">
                <button disabled={saving || !dirty} type="button" onClick={() => { setDraft(draftOf(configuration)); setComment(""); setError(null); }}>
                    Discard
                </button>
                <button className="primary-button" disabled={saving || !dirty} type="submit">
                    {saving ? "Saving…" : "Save targeting"}
                </button>
            </div>
        </footer>

        <EvaluationTester dirty={dirty} environmentKey={environment.key} flagKey={flag.key} />
    </form>;
}
