import axios from "axios";

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
  timeout: 15000,
  headers: { "Content-Type": "application/json" },
});

/**
 * Normalises every failure into the same shape so screens never have to inspect
 * an Axios error. Token attachment and 401 refresh are added in Phase 3, once
 * AuthContext exists to own the tokens.
 */
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      return Promise.reject({
        status,
        code: data?.code ?? "UNEXPECTED_ERROR",
        message: data?.message ?? data?.detail ?? "Something went wrong.",
        fieldErrors: data?.fieldErrors ?? null,
        retryAfter: error.response.headers?.["retry-after"] ?? null,
      });
    }

    if (error.code === "ECONNABORTED") {
      return Promise.reject({
        status: 0,
        code: "TIMEOUT",
        message: "The server took too long to respond.",
      });
    }

    return Promise.reject({
      status: 0,
      code: "NETWORK_ERROR",
      message: "Cannot reach the server. Check that the backend is running.",
    });
  },
);

export default axiosClient;
