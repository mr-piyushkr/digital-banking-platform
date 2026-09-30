import axios from "axios";
import { getTokens, setTokens, clearTokens } from "../auth/tokenStore.js";

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
  timeout: 15000,
  headers: { "Content-Type": "application/json" },
});

/** Endpoints that must never carry a token or trigger a refresh. */
const PUBLIC_PATHS = ["/auth/login", "/auth/register", "/auth/refresh", "/health"];

const isPublic = (url = "") => PUBLIC_PATHS.some((path) => url.startsWith(path));

axiosClient.interceptors.request.use((config) => {
  const { accessToken } = getTokens();
  if (accessToken && !isPublic(config.url)) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

/**
 * A single in-flight refresh, with everything else queued behind it.
 *
 * Without this, a page that fires four requests at once would send four
 * refresh calls on expiry. Since refresh rotates the token server-side, the
 * first would succeed and the rest would present an already-revoked jti and
 * log the user out.
 */
let refreshInFlight = null;
const queue = [];

function resolveQueue(error, accessToken) {
  queue.splice(0).forEach(({ resolve, reject }) =>
    error ? reject(error) : resolve(accessToken),
  );
}

async function refreshAccessToken() {
  const { refreshToken } = getTokens();
  if (!refreshToken) {
    throw new Error("No refresh token");
  }

  // A bare axios call, so this request cannot recurse through this interceptor.
  const { data } = await axios.post(
    `${axiosClient.defaults.baseURL}/auth/refresh`,
    { refreshToken },
    { headers: { "Content-Type": "application/json" } },
  );

  setTokens(data.accessToken, data.refreshToken);
  return data.accessToken;
}

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error;

    if (response?.status === 401 && config && !config._retried && !isPublic(config.url)) {
      config._retried = true;

      if (!refreshInFlight) {
        refreshInFlight = refreshAccessToken()
          .then((token) => {
            resolveQueue(null, token);
            return token;
          })
          .catch((err) => {
            resolveQueue(err, null);
            clearTokens();
            // Let AuthContext react rather than hard-navigating from here.
            window.dispatchEvent(new Event("bankflow:session-expired"));
            throw err;
          })
          .finally(() => {
            refreshInFlight = null;
          });
      }

      try {
        const token = await new Promise((resolve, reject) => {
          queue.push({ resolve, reject });
          refreshInFlight.catch(() => {});
        });
        config.headers.Authorization = `Bearer ${token}`;
        return axiosClient(config);
      } catch {
        return Promise.reject(normalise(error));
      }
    }

    return Promise.reject(normalise(error));
  },
);

/** One error shape for the whole app, so screens never inspect an Axios error. */
function normalise(error) {
  if (error.response) {
    const { status, data, headers } = error.response;
    return {
      status,
      code: data?.code ?? "UNEXPECTED_ERROR",
      message: data?.message ?? "Something went wrong.",
      fieldErrors: data?.fieldErrors ?? null,
      retryAfter: headers?.["retry-after"] ?? null,
    };
  }

  if (error.code === "ECONNABORTED") {
    return { status: 0, code: "TIMEOUT", message: "The server took too long to respond." };
  }

  return {
    status: 0,
    code: "NETWORK_ERROR",
    message: "Cannot reach the server. Check that the backend is running.",
  };
}

export default axiosClient;
