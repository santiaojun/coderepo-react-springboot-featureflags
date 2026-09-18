const MINUTE = 60000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

export function relativeTime(value) {
    if (!value) return "";
    const elapsed = Date.now() - new Date(value).getTime();
    if (elapsed < MINUTE) return "just now";
    if (elapsed < HOUR) return `${Math.floor(elapsed / MINUTE)}m ago`;
    if (elapsed < DAY) return `${Math.floor(elapsed / HOUR)}h ago`;
    if (elapsed < 30 * DAY) return `${Math.floor(elapsed / DAY)}d ago`;
    return new Date(value).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

export function fullTime(value) {
    if (!value) return "";
    return new Date(value).toLocaleString("en-US", {
        month: "short", day: "numeric", year: "numeric", hour: "numeric", minute: "2-digit",
    });
}

export function variationValueLabel(value) {
    if (typeof value === "boolean") return value ? "true" : "false";
    if (value === null || value === undefined) return "—";
    return String(value);
}

export function titleCase(value) {
    return String(value || "").replace(/[-_]/g, " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function pluralize(count, singular, plural = `${singular}s`) {
    return `${count} ${count === 1 ? singular : plural}`;
}

export function attributeLabel(attribute) {
    const labels = { key: "User key", name: "Name", email: "Email", plan: "Plan", country: "Country", signupYear: "Signup year", betaOptIn: "Beta opt-in", deviceType: "Device" };
    return labels[attribute] || titleCase(attribute);
}
