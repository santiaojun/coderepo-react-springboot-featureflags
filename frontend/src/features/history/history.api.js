import { request } from "../../shared/api/client.js";

export const historyApi = {
    list: (filters = {}) => {
        const parameters = new URLSearchParams();
        Object.entries(filters).forEach(([key, value]) => { if (value) parameters.set(key, String(value)); });
        return request(`/change-history?${parameters}`);
    },
};
