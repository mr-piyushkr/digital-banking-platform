/**
 * Tokens live in sessionStorage, not localStorage: closing the tab ends the
 * session, and a shared machine does not keep a usable token on disk.
 *
 * The genuinely safer option is an httpOnly cookie, which JavaScript cannot
 * read at all — that needs CSRF protection and a same-site deployment, so it
 * is a deliberate trade-off rather than an oversight.
 */
const ACCESS_KEY = "bankflow.accessToken";
const REFRESH_KEY = "bankflow.refreshToken";

export function getTokens() {
  return {
    accessToken: sessionStorage.getItem(ACCESS_KEY),
    refreshToken: sessionStorage.getItem(REFRESH_KEY),
  };
}

export function setTokens(accessToken, refreshToken) {
  if (accessToken) sessionStorage.setItem(ACCESS_KEY, accessToken);
  if (refreshToken) sessionStorage.setItem(REFRESH_KEY, refreshToken);
}

export function clearTokens() {
  sessionStorage.removeItem(ACCESS_KEY);
  sessionStorage.removeItem(REFRESH_KEY);
}
