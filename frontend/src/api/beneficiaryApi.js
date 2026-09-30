import axiosClient from "./axiosClient.js";

export const beneficiaryApi = {
  list: () => axiosClient.get("/beneficiaries").then((r) => r.data),
  add: (payload) => axiosClient.post("/beneficiaries", payload).then((r) => r.data),
  verify: (id) => axiosClient.post(`/beneficiaries/${id}/verify`).then((r) => r.data),
  remove: (id) => axiosClient.delete(`/beneficiaries/${id}`),
};
