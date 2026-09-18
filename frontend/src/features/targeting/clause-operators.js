import { attributeLabel } from "../../shared/utils/format.js";

export const OPERATORS = [
    { value: "in", label: "is one of" },
    { value: "contains", label: "contains" },
    { value: "startsWith", label: "starts with" },
    { value: "endsWith", label: "ends with" },
    { value: "greaterThan", label: "is greater than" },
    { value: "lessThan", label: "is less than" },
];

export const ATTRIBUTES = ["key", "name", "email", "plan", "country", "signupYear", "betaOptIn", "deviceType"];

export const ATTRIBUTE_OPTIONS = ATTRIBUTES.map((attribute) => ({ value: attribute, label: attributeLabel(attribute) }));

export function operatorLabel(op) {
    return OPERATORS.find((operator) => operator.value === op)?.label || op;
}

/** Splits the comma separated field back into the typed values the API expects. */
export function parseClauseValues(text) {
    return String(text)
        .split(",")
        .map((part) => part.trim())
        .filter(Boolean)
        .map((part) => {
            if (part === "true") return true;
            if (part === "false") return false;
            if (part !== "" && !Number.isNaN(Number(part))) return Number(part);
            return part;
        });
}

export function formatClauseValues(values = []) {
    return values.map((value) => String(value)).join(", ");
}

export function clauseSummary(clause) {
    const negation = clause.negate ? "is not" : null;
    const operator = clause.negate && clause.op === "in" ? negation : `${clause.negate ? "not " : ""}${operatorLabel(clause.op)}`;
    return `${attributeLabel(clause.attribute)} ${operator} ${formatClauseValues(clause.values)}`.replace(/\s+/g, " ");
}

export const REASON_LABELS = {
    off: "Flag is off",
    targetMatch: "Individually targeted",
    ruleMatch: "Matched a rule",
    segmentMatch: "Matched a segment",
    rollout: "Percentage rollout",
    fallthrough: "Default for everyone else",
};

export function reasonLabel(reason) {
    const base = REASON_LABELS[reason?.kind] || reason?.kind || "Unknown";
    if (reason?.segmentKey) return `${base}: ${reason.segmentKey}`;
    if (reason?.kind === "rollout" && reason.percentage !== null && reason.percentage !== undefined) {
        return `${base} (bucket ${reason.percentage.toFixed(1)}%)`;
    }
    return base;
}
