import axiosClient from "./axiosClient.js";

/**
 * A fresh idempotency key per user-initiated submit. Retrying the same submit
 * reuses the key, so a timeout followed by a retry cannot charge twice.
 */
function idempotencyHeaders(key) {
  return key ? { headers: { "X-Idempotency-Key": key } } : {};
}

export const txnApi = {
  deposit: (payload, key) =>
    axiosClient.post("/transactions/deposit", payload, idempotencyHeaders(key)).then((r) => r.data),
  withdraw: (payload, key) =>
    axiosClient.post("/transactions/withdraw", payload, idempotencyHeaders(key)).then((r) => r.data),
  transfer: (payload, key) =>
    axiosClient.post("/transactions/transfer", payload, idempotencyHeaders(key)).then((r) => r.data),
  history: (accountId, page = 0, size = 20) =>
    axiosClient
      .get(`/transactions/account/${accountId}`, { params: { page, size } })
      .then((r) => r.data),
  byReference: (reference) => axiosClient.get(`/transactions/${reference}`).then((r) => r.data),
};

export function newIdempotencyKey() {
  return crypto.randomUUID();
}
