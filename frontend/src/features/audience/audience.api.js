import { request } from "../../shared/api/client.js";

export const audienceApi = {
    list: (filters = {}) => {
        const parameters = new URLSearchParams();
        Object.entries(filters).forEach(([key, value]) => { if (value) parameters.set(key, String(value)); });
        return request(`/audience/users?${parameters}`);
    },
    flags: (userKey, environmentKey) =>
        request(`/audience/users/${encodeURIComponent(userKey)}/flags?${new URLSearchParams({ environment: environmentKey })}`),
};
