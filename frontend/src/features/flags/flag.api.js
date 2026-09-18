import { request } from "../../shared/api/client.js";

const withEnvironment = (environmentKey, extra = {}) => {
    const parameters = new URLSearchParams({ environment: environmentKey });
    Object.entries(extra).forEach(([key, value]) => { if (value) parameters.set(key, value); });
    return parameters.toString();
};

export const flagApi = {
    list: (environmentKey, filters = {}) => request(`/flags?${withEnvironment(environmentKey, filters)}`),
    get: (flagKey, environmentKey) =>
        request(`/flags/${encodeURIComponent(flagKey)}?${withEnvironment(environmentKey)}`),
    create: (payload) => request("/flags", { method: "POST", body: JSON.stringify(payload) }),
    update: (flagKey, payload) =>
        request(`/flags/${encodeURIComponent(flagKey)}`, { method: "PATCH", body: JSON.stringify(payload) }),
    retire: (flagKey) => request(`/flags/${encodeURIComponent(flagKey)}/retire`, { method: "POST" }),
    restore: (flagKey) => request(`/flags/${encodeURIComponent(flagKey)}/restore`, { method: "POST" }),
    remove: (flagKey) => request(`/flags/${encodeURIComponent(flagKey)}`, { method: "DELETE" }),
    toggle: (flagKey, environmentKey, payload) =>
        request(`/flags/${encodeURIComponent(flagKey)}/environments/${encodeURIComponent(environmentKey)}/toggle`,
            { method: "PATCH", body: JSON.stringify(payload) }),
};
