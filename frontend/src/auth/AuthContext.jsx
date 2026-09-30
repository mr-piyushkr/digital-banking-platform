import { createContext, useCallback, useEffect, useMemo, useState } from "react";
import { authApi } from "../api/authApi.js";
import { clearTokens, getTokens, setTokens } from "./tokenStore.js";

export const AuthContext = createContext(null);

const ROLE_ADMIN = "ROLE_ADMIN";
const ROLE_AUDITOR = "ROLE_AUDITOR";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState("restoring");

  // On reload the tokens are still in sessionStorage but the user object is
  // gone, so the profile is fetched once to rehydrate it. Treating a stored
  // token as proof of identity without asking the server would let a stale or
  // revoked token render a signed-in shell.
  useEffect(() => {
    const { accessToken } = getTokens();
    if (!accessToken) {
      setStatus("anonymous");
      return;
    }

    let cancelled = false;
    authApi
      .profile()
      .then((profile) => {
        if (cancelled) return;
        setUser(profile);
        setStatus("authenticated");
      })
      .catch(() => {
        if (cancelled) return;
        clearTokens();
        setStatus("anonymous");
      });

    return () => {
      cancelled = true;
    };
  }, []);

  // The Axios interceptor fires this when a refresh fails, so a dead session
  // is cleared even if the user never clicks anything.
  useEffect(() => {
    const onExpired = () => {
      setUser(null);
      setStatus("anonymous");
    };
    window.addEventListener("bankflow:session-expired", onExpired);
    return () => window.removeEventListener("bankflow:session-expired", onExpired);
  }, []);

  const login = useCallback(async (credentials) => {
    const data = await authApi.login(credentials);
    setTokens(data.accessToken, data.refreshToken);
    setUser(data.user);
    setStatus("authenticated");
    return data.user;
  }, []);

  const register = useCallback(async (payload) => {
    const data = await authApi.register(payload);
    setTokens(data.accessToken, data.refreshToken);
    setUser(data.user);
    setStatus("authenticated");
    return data.user;
  }, []);

  const logout = useCallback(async () => {
    const { refreshToken } = getTokens();
    try {
      if (refreshToken) await authApi.logout(refreshToken);
    } catch {
      // The local session is cleared regardless — a failed server-side
      // revocation must not leave the user stuck signed in.
    } finally {
      clearTokens();
      setUser(null);
      setStatus("anonymous");
    }
  }, []);

  const value = useMemo(() => {
    const roles = user?.roles ?? [];
    return {
      user,
      status,
      isAuthenticated: status === "authenticated",
      isAdmin: roles.includes(ROLE_ADMIN),
      isAuditor: roles.includes(ROLE_AUDITOR),
      isStaff: roles.includes(ROLE_ADMIN) || roles.includes(ROLE_AUDITOR),
      roles,
      login,
      register,
      logout,
      setUser,
    };
  }, [user, status, login, register, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
