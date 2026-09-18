import { ROLLOUT_STEPS } from "./rollout-math.js";

export function RolloutStepper({ disabled = false, onChange, value }) {
    return <div className="rollout-stepper">
        <div className="rollout-steps" role="group" aria-label="Rollout percentage steps">
            {ROLLOUT_STEPS.map((step) => <button
                aria-pressed={value === step}
                className="rollout-step"
                disabled={disabled}
                key={step}
                type="button"
                onClick={() => onChange(step)}
            >{step}%</button>)}
        </div>
        <label className="field rollout-custom">
            <span>Custom</span>
            <input
                disabled={disabled}
                max={100}
                min={0}
                step={1}
                type="number"
                value={value}
                onChange={(event) => onChange(Math.max(0, Math.min(100, Number(event.target.value))))}
            />
        </label>
        <label className="field rollout-slider-field">
            <span className="sr-only">Rollout percentage</span>
            <input
                aria-label="Rollout percentage"
                className="rollout-slider"
                disabled={disabled}
                max={100}
                min={0}
                step={1}
                type="range"
                value={value}
                onChange={(event) => onChange(Number(event.target.value))}
            />
        </label>
    </div>;
}
