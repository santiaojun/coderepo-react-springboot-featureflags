import { request } from "../../shared/api/client.js";

const query = (environmentKey) => new URLSearchParams({ environment: environmentKey }).toString();

export const segmentApi = {
    list: (environmentKey) => request(`/segments?${query(environmentKey)}`),
    members: (segmentKey, environmentKey) =>
        request(`/segments/${encodeURIComponent(segmentKey)}/members?${query(environmentKey)}`),
    create: (environmentKey, payload) =>
        request(`/segments?${query(environmentKey)}`, { method: "POST", body: JSON.stringify(payload) }),
    update: (segmentKey, environmentKey, payload) =>
        request(`/segments/${encodeURIComponent(segmentKey)}?${query(environmentKey)}`,
            { method: "PATCH", body: JSON.stringify(payload) }),
    remove: (segmentKey, environmentKey) =>
        request(`/segments/${encodeURIComponent(segmentKey)}?${query(environmentKey)}`, { method: "DELETE" }),
};
