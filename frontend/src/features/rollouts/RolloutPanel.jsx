import { useCallback, useEffect, useMemo, useState } from "react";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { fieldMessage, formMessage } from "../../shared/api/errors.js";
import { pluralize } from "../../shared/utils/format.js";
import { RolloutSplitBar } from "./RolloutSplitBar.jsx";
import { RolloutStepper } from "./RolloutStepper.jsx";
import { buildRollout, treatmentOf, weightFor } from "./rollout-math.js";
import { rolloutApi } from "./rollout.api.js";

function DistributionList({ distribution, label }) {
    if (!distribution) return null;
    return <div className="distribution-block">
        <span className="distribution-label">{label}</span>
        <ul className="distribution-list">
            {distribution.variations.map((entry) => <li key={entry.variationId}>
                <span>{entry.variationName}</span>
                <strong>{entry.count}</strong>
                <small>{entry.percentage.toFixed(1)}%</small>
            </li>)}
        </ul>
    </div>;
}

export function RolloutPanel({ configuration, distribution, environment, flag, onSaved }) {
    const existingWeights = configuration.fallthrough?.kind === "rollout"
        ? configuration.fallthrough.rollout.weights
        : null;
    const initialTreatment = existingWeights
        ? treatmentOf(existingWeights.filter((weight) => weight.weight < 100))?.variationId || flag.variations[0].id
        : flag.variations[0].id;
    const initialControl = flag.variations.find((variation) => variation.id !== initialTreatment)?.id;

    const [treatmentId, setTreatmentId] = useState(initialTreatment);
    const [controlId, setControlId] = useState(initialControl);
    const [percent, setPercent] = useState(existingWeights ? weightFor(existingWeights, initialTreatment) : 10);
    const [comment, setComment] = useState("");
    const [preview, setPreview] = useState(null);
    const [previewing, setPreviewing] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    const variationOptions = flag.variations.map((variation) => ({ value: variation.id, label: variation.name }));
    const controlOptions = variationOptions.filter((option) => option.value !== treatmentId);
    const candidate = useMemo(
        () => buildRollout(flag.variations, treatmentId, percent, controlId),
        [flag.variations, treatmentId, percent, controlId],
    );
    const currentPercent = existingWeights ? weightFor(existingWeights, treatmentId) : null;
    const dirty = !existingWeights || currentPercent !== percent
        || weightFor(existingWeights, controlId) !== 100 - percent;

    const loadPreview = useCallback(async () => {
        setPreviewing(true);
        try {
            setPreview(await rolloutApi.preview(flag.key, environment.key, candidate));
            setError(null);
        } catch (previewError) {
            setError(previewError);
            setPreview(null);
        } finally {
            setPreviewing(false);
        }
    }, [candidate, environment.key, flag.key]);

    useEffect(() => {
        const timer = setTimeout(loadPreview, 220);
        return () => clearTimeout(timer);
    }, [loadPreview]);

    useEffect(() => {
        if (controlId === treatmentId) {
            setControlId(flag.variations.find((variation) => variation.id !== treatmentId)?.id);
        }
    }, [controlId, treatmentId, flag.variations]);

    const save = async () => {
        setSaving(true);
        setError(null);
        try {
            const result = await rolloutApi.update(flag.key, environment.key, {
                rollout: candidate,
                version: configuration.version,
                comment: comment.trim(),
            });
            setComment("");
            onSaved(result);
        } catch (saveError) {
            setError(saveError);
        } finally {
            setSaving(false);
        }
    };

    const projected = preview?.projected;
    const treatmentProjected = projected?.variations.find((entry) => entry.variationId === treatmentId);
    const treatmentCurrent = preview?.current.variations.find((entry) => entry.variationId === treatmentId);
    const movement = treatmentProjected && treatmentCurrent ? treatmentProjected.count - treatmentCurrent.count : null;

    return <section className="rollout-panel">
        <header className="panel-heading">
            <div>
                <h3>Progressive rollout</h3>
                <p>
                    Members are bucketed by their user key, so raising the percentage only ever adds members.
                    Nobody who already sees the new variation loses it.
                </p>
            </div>
        </header>

        {!configuration.enabled && <p className="panel-notice">
            <MaterialIcon size={15}>warning_amber</MaterialIcon>
            This flag is off in {environment.name}, so everybody receives the off variation until you turn it on.
        </p>}

        {formMessage(error) && <p className="form-error" role="alert">{formMessage(error)}</p>}
        {fieldMessage(error, "rollout.weights") && <p className="form-error" role="alert">{fieldMessage(error, "rollout.weights")}</p>}

        <div className="rollout-current">
            <span className="distribution-label">Currently serving everyone else</span>
            {existingWeights
                ? <RolloutSplitBar flag={flag} weights={existingWeights} />
                : <p className="panel-empty">A single variation. Choose a split below to start a rollout.</p>}
        </div>

        <div className="field-row">
            <div className="field">
                <span>Rolling out</span>
                <SelectMenu ariaLabel="Variation being rolled out" disabled={saving} options={variationOptions} value={treatmentId} onChange={setTreatmentId} />
            </div>
            <div className="field">
                <span>Remainder receives</span>
                <SelectMenu ariaLabel="Variation the remainder receives" disabled={saving} options={controlOptions} value={controlId} onChange={setControlId} />
            </div>
        </div>

        <RolloutStepper disabled={saving} value={percent} onChange={setPercent} />

        <div className="rollout-proposed">
            <span className="distribution-label">Proposed split</span>
            <RolloutSplitBar flag={flag} weights={candidate.weights} />
        </div>

        <div className="rollout-preview" aria-busy={previewing || undefined}>
            <DistributionList distribution={preview?.current} label={`Today across ${pluralize(preview?.current?.audienceSize || 0, "member")}`} />
            <DistributionList distribution={projected} label="After this change" />
            {movement !== null && <p className="rollout-movement">
                <MaterialIcon size={16}>{movement > 0 ? "arrow_drop_up" : movement < 0 ? "arrow_drop_down" : "check"}</MaterialIcon>
                {movement === 0
                    ? "No members change variation."
                    : `${Math.abs(movement)} ${Math.abs(movement) === 1 ? "member" : "members"} ${movement > 0 ? "gain" : "lose"} ${flag.variations.find((variation) => variation.id === treatmentId)?.name}.`}
            </p>}
        </div>

        <footer className="rollout-actions">
            <label className="field">
                <span>Comment for the audit trail</span>
                <input
                    disabled={saving}
                    maxLength={280}
                    placeholder="Why are you changing the rollout?"
                    value={comment}
                    onChange={(event) => setComment(event.target.value)}
                />
            </label>
            <button className="primary-button" disabled={saving || !dirty || previewing} type="button" onClick={save}>
                {saving ? "Saving…" : dirty ? `Set rollout to ${percent}%` : "Rollout already at this split"}
            </button>
        </footer>

        {distribution && <div className="rollout-observed">
            <span className="distribution-label">Reasons members receive their variation</span>
            <ul className="reason-counts">
                {Object.entries(distribution.reasons).map(([kind, count]) => <li key={kind}>
                    <span>{kind}</span><strong>{count}</strong>
                </li>)}
            </ul>
        </div>}
    </section>;
}
