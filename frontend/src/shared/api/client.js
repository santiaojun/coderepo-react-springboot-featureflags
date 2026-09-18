const baseUrl = import.meta.env.VITE_API_URL || "/api/v1";
let sessionToken = localStorage.getItem("flagpost-session-token") || "";
let profileToken = localStorage.getItem("flagpost-profile-token") || "";

export function setSessionToken(token) {
    sessionToken = token || "";
    if (sessionToken) localStorage.setItem("flagpost-session-token", sessionToken);
    else localStorage.removeItem("flagpost-session-token");
}

export function setProfileToken(token) {
    profileToken = token || "";
    if (profileToken) localStorage.setItem("flagpost-profile-token", profileToken);
    else localStorage.removeItem("flagpost-profile-token");
}

export const hasSessionToken = () => Boolean(sessionToken || profileToken);
export const hasProfileToken = () => Boolean(profileToken);

export async function request(path, options = {}) {
    const response = await fetch(`${baseUrl}${path}`, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(profileToken || sessionToken ? { Authorization: `Bearer ${profileToken || sessionToken}` } : {}),
            ...options.headers,
        },
    });
    if (response.status === 204) return null;
    const contentType = response.headers?.get?.("content-type") || "";
    const payload = contentType.includes("application/json") || !response.headers
        ? await response.json()
        : null;
    if (!response.ok) {
        const code = payload?.error?.code;
        const message = payload?.error?.message || `Flagpost service returned ${response.status}.`;
        if (response.status === 401 && ["AUTH_REQUIRED", "INVALID_TOKEN", "ACCOUNT_UNAVAILABLE"].includes(code)) {
            setProfileToken(""); setSessionToken(""); localStorage.removeItem("flagpost-profile-id");
            window.dispatchEvent(new CustomEvent("flagpost-session-expired", { detail: message }));
        }
        const error = new Error(message);
        error.code = code;
        error.details = payload?.error?.details || null;
        error.fieldErrors = payload?.error?.details?.fieldErrors || null;
        error.formErrors = payload?.error?.details?.formErrors || null;
        throw error;
    }
    if (!payload || !("data" in payload)) throw new Error("Flagpost service returned an invalid response.");
    return payload.data;
}
