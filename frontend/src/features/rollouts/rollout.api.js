import { request } from "../../shared/api/client.js";

const rolloutPath = (flagKey, environmentKey) =>
    `/flags/${encodeURIComponent(flagKey)}/environments/${encodeURIComponent(environmentKey)}/rollout`;

export const rolloutApi = {
    update: (flagKey, environmentKey, payload) =>
        request(rolloutPath(flagKey, environmentKey), { method: "PATCH", body: JSON.stringify(payload) }),
    preview: (flagKey, environmentKey, rollout) =>
        request(`${rolloutPath(flagKey, environmentKey)}/preview`,
            { method: "POST", body: JSON.stringify({ rollout }) }),
};
