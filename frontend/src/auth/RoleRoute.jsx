import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./useAuth.js";

/**
 * Hides staff routes from customers. This is a usability layer only — the
 * server enforces the same rules, so removing it would change what the UI
 * shows, not what the API allows.
 */
export default function RoleRoute({ allow = [] }) {
  const { roles } = useAuth();
  const permitted = roles.some((role) => allow.includes(role));

  return permitted ? <Outlet /> : <Navigate to="/403" replace />;
}
