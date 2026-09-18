import { request } from "../../shared/api/client.js";

export const memberApi = {
    list: () => request("/members"),
};
