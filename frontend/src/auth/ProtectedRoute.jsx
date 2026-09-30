import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "./useAuth.js";
import PageLoader from "../components/PageLoader.jsx";

/**
 * Blocks anonymous access. While the session is being restored it renders a
 * loader rather than redirecting — redirecting first would bounce a
 * legitimately signed-in user to the login page on every refresh.
 */
export default function ProtectedRoute() {
  const { status } = useAuth();
  const location = useLocation();

  if (status === "restoring") {
    return <PageLoader label="Restoring your session" />;
  }

  if (status !== "authenticated") {
    // `from` lets the login page send the user back where they were going.
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}
