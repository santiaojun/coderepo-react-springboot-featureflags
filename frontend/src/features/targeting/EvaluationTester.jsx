import { useState } from "react";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { attributeLabel, variationValueLabel } from "../../shared/utils/format.js";
import { UserPicker } from "../audience/UserPicker.jsx";
import { reasonLabel } from "./clause-operators.js";
import { targetingApi } from "./targeting.api.js";

export function EvaluationTester({ dirty = false, environmentKey, flagKey }) {
    const [result, setResult] = useState(null);
    const [status, setStatus] = useState({ loading: false, error: "" });

    const evaluate = async (user) => {
        setStatus({ loading: true, error: "" });
        setResult(null);
        try {
            setResult(await targetingApi.evaluate(flagKey, environmentKey, user.key));
            setStatus({ loading: false, error: "" });
        } catch (error) {
            setStatus({ loading: false, error: error.message });
        }
    };

    return <section className="evaluation-tester">
        <header className="panel-heading">
            <div>
                <h3>Test a member</h3>
                <p>Pick somebody from the audience to see the variation this environment serves them, and why.</p>
            </div>
        </header>
        {dirty && <p className="panel-notice">
            <MaterialIcon size={15}>warning_amber</MaterialIcon>
            This tests the saved configuration. Save your changes to test them.
        </p>}
        <UserPicker label="Test a member" placeholder="Search the audience" onSelect={evaluate} />
        {status.loading && <p className="panel-empty" role="status">Evaluating…</p>}
        {status.error && <p className="form-error" role="alert">{status.error}</p>}
        {result && <div className="evaluation-result">
            <div className="evaluation-user">
                <strong>{result.user.name}</strong>
                <code>{result.user.key}</code>
                <ul className="evaluation-attributes">
                    {Object.entries(result.user.attributes).map(([attribute, value]) => <li key={attribute}>
                        <span>{attributeLabel(attribute)}</span>
                        <strong>{variationValueLabel(value)}</strong>
                    </li>)}
                </ul>
            </div>
            <div className="evaluation-outcome">
                <span className="evaluation-label">Receives</span>
                <strong className="evaluation-variation">{result.evaluation.variationName}</strong>
                <code className="evaluation-value">{variationValueLabel(result.evaluation.value)}</code>
                <span className={`evaluation-reason tone-${result.evaluation.reason.kind}`}>{reasonLabel(result.evaluation.reason)}</span>
                {result.evaluation.reason.ruleDescription && <small>Rule: {result.evaluation.reason.ruleDescription}</small>}
            </div>
        </div>}
    </section>;
}
