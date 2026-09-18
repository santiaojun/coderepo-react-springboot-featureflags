import { request } from "../../shared/api/client.js";

export const environmentApi = {
    list: () => request("/environments"),
    summary: (environmentKey) => request(`/environments/${encodeURIComponent(environmentKey)}/summary`),
};
