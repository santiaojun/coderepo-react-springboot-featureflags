import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { relativeTime } from "../../shared/utils/format.js";
import { FlagToggle } from "./FlagToggle.jsx";
import { RolloutSplitBar } from "../rollouts/RolloutSplitBar.jsx";
import { rolloutWeights, servingLabel, stateBadges } from "./flag-format.js";

export function FlagListItem({ flag, onOpen, onToggle, pending = false }) {
    const configuration = flag.configuration;
    const weights = rolloutWeights(configuration);
    const badges = stateBadges(configuration);
    return <li className={`flag-row ${flag.status === "retired" ? "retired" : ""}`}>
        <button className="flag-row-main" type="button" onClick={() => onOpen(flag.key)}>
            <span className="flag-row-heading">
                <strong>{flag.name}</strong>
                {flag.status === "retired" && <span className="flag-tag flag-tag-retired">Retired</span>}
                {!flag.temporary && <span className="flag-tag">Permanent</span>}
                {flag.tags.map((tag) => <span className="flag-tag" key={tag}>{tag}</span>)}
            </span>
            <code className="flag-row-key">{flag.key}</code>
            {flag.description && <span className="flag-row-description">{flag.description}</span>}
            <span className="flag-row-meta">
                <span className="flag-row-serving">
                    <MaterialIcon size={14}>{configuration?.enabled ? "check_circle" : "error_outline"}</MaterialIcon>
                    Serving {servingLabel(flag, configuration)}
                </span>
                {badges.map((badge) => <span className={`flag-badge tone-${badge.tone}`} key={badge.label}>{badge.label}</span>)}
                {configuration?.lastModifiedAt && <span className="flag-row-modified">
                    v{configuration.version} · {configuration.lastModifiedByName} {relativeTime(configuration.lastModifiedAt)}
                </span>}
            </span>
            {weights && configuration.enabled && <RolloutSplitBar compact flag={flag} weights={weights} />}
        </button>
        <div className="flag-row-actions">
            <FlagToggle
                disabled={flag.status === "retired"}
                enabled={Boolean(configuration?.enabled)}
                flagName={flag.name}
                pending={pending}
                onChange={(next) => onToggle(flag, next)}
            />
        </div>
    </li>;
}
