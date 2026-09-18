import { pluralize } from "../../shared/utils/format.js";

export function variationById(flag, variationId) {
    return flag?.variations?.find((variation) => variation.id === variationId) || null;
}

export function variationName(flag, variationId) {
    return variationById(flag, variationId)?.name || "Unknown variation";
}

export function rolloutWeights(configuration) {
    const rollout = configuration?.fallthrough?.rollout;
    if (configuration?.fallthrough?.kind !== "rollout" || !rollout?.weights?.length) return null;
    return rollout.weights;
}

export function describeServe(flag, serve) {
    if (!serve) return "Unknown variation";
    if (serve.kind !== "rollout" || !serve.rollout?.weights?.length) return variationName(flag, serve.variationId);
    return serve.rollout.weights
        .filter((weight) => weight.weight > 0)
        .map((weight) => `${weight.weight}% ${variationName(flag, weight.variationId)}`)
        .join(", ");
}

export function servingLabel(flag, configuration) {
    if (!configuration) return "Not configured";
    if (!configuration.enabled) return variationName(flag, configuration.offVariationId);
    return describeServe(flag, configuration.fallthrough);
}

export function targetedUserCount(configuration) {
    return (configuration?.targets || []).reduce((total, target) => total + target.userKeys.length, 0);
}

export function stateBadges(configuration) {
    if (!configuration) return [];
    const badges = [];
    if (rolloutWeights(configuration) && configuration.enabled) badges.push({ tone: "rollout", label: "Rolling out" });
    if (configuration.rules?.length) {
        badges.push({ tone: "targeted", label: pluralize(configuration.rules.length, "rule") });
    }
    const targeted = targetedUserCount(configuration);
    if (targeted) badges.push({ tone: "targeted", label: `${pluralize(targeted, "user")} targeted` });
    return badges;
}

export const FLAG_STATE_FILTERS = [
    { value: "all", label: "Any state" },
    { value: "on", label: "On" },
    { value: "off", label: "Off" },
    { value: "rollout", label: "Rolling out" },
    { value: "targeted", label: "Has rules" },
];

export const FLAG_STATUS_FILTERS = [
    { value: "active", label: "Active flags" },
    { value: "retired", label: "Retired flags" },
    { value: "all", label: "All flags" },
];
