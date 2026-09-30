import axiosClient from "./axiosClient.js";

export const adminApi = {
  dashboard: () => axiosClient.get("/admin/dashboard").then((r) => r.data),

  users: (params) => axiosClient.get("/admin/users", { params }).then((r) => r.data),
  user: (userId) => axiosClient.get(`/admin/users/${userId}`).then((r) => r.data),
  enableUser: (userId) => axiosClient.post(`/admin/users/${userId}/enable`).then((r) => r.data),
  disableUser: (userId) => axiosClient.post(`/admin/users/${userId}/disable`).then((r) => r.data),

  accounts: (params) => axiosClient.get("/admin/accounts", { params }).then((r) => r.data),
  changeAccountStatus: (accountId, status, reason) =>
    axiosClient.post(`/admin/accounts/${accountId}/status`, { status, reason }).then((r) => r.data),

  transactions: (params) => axiosClient.get("/admin/transactions", { params }).then((r) => r.data),
  clearFlag: (reference) =>
    axiosClient.post(`/admin/transactions/${reference}/clear-flag`).then((r) => r.data),

  auditLogs: (params) => axiosClient.get("/admin/audit-logs", { params }).then((r) => r.data),
};
