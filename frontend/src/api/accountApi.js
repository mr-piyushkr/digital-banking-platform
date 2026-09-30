import axiosClient from "./axiosClient.js";

export const accountApi = {
  list: () => axiosClient.get("/accounts").then((r) => r.data),
  byId: (accountId) => axiosClient.get(`/accounts/${accountId}`).then((r) => r.data),
  open: (payload) => axiosClient.post("/accounts", payload).then((r) => r.data),
  statement: (accountId, from, to) =>
    axiosClient
      .get(`/statements/account/${accountId}`, { params: { from, to } })
      .then((r) => r.data),
};
