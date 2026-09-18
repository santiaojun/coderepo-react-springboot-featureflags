import { request } from "../../shared/api/client.js";

const configurationPath = (flagKey, environmentKey) =>
    `/flags/${encodeURIComponent(flagKey)}/environments/${encodeURIComponent(environmentKey)}`;

export const targetingApi = {
    update: (flagKey, environmentKey, payload) =>
        request(`${configurationPath(flagKey, environmentKey)}/targeting`,
            { method: "PUT", body: JSON.stringify(payload) }),
    evaluate: (flagKey, environmentKey, userKey) =>
        request(`${configurationPath(flagKey, environmentKey)}/evaluate`,
            { method: "POST", body: JSON.stringify({ userKey }) }),
    distribution: (flagKey, environmentKey) =>
        request(`${configurationPath(flagKey, environmentKey)}/distribution`),
    restore: (flagKey, environmentKey, version, comment) =>
        request(`${configurationPath(flagKey, environmentKey)}/restore`,
            { method: "POST", body: JSON.stringify({ version, comment }) }),
};
