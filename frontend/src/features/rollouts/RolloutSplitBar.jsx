import { variationName } from "../flags/flag-format.js";

const TONES = ["one", "two", "three", "four", "five", "six", "seven", "eight"];

export function RolloutSplitBar({ compact = false, flag, weights = [] }) {
    const visible = weights.filter((weight) => weight.weight > 0);
    if (!visible.length) return null;
    return <div className={`rollout-split ${compact ? "compact" : ""}`}>
        <div className="rollout-split-bar" role="img" aria-label={visible.map((weight) => `${weight.weight}% ${variationName(flag, weight.variationId)}`).join(", ")}>
            {visible.map((weight) => <span
                className={`rollout-split-slice tone-${TONES[flag.variations.findIndex((variation) => variation.id === weight.variationId) % TONES.length]}`}
                key={weight.variationId}
                style={{ width: `${weight.weight}%` }}
            />)}
        </div>
        {!compact && <ul className="rollout-split-legend">
            {visible.map((weight) => <li key={weight.variationId}>
                <span className={`rollout-legend-dot tone-${TONES[flag.variations.findIndex((variation) => variation.id === weight.variationId) % TONES.length]}`} />
                <strong>{weight.weight}%</strong>
                <span>{variationName(flag, weight.variationId)}</span>
            </li>)}
        </ul>}
    </div>;
}
