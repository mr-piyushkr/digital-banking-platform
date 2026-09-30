/**
 * Status always renders as text, never as colour alone — a colour-blind user
 * or a greyscale printout must still read the state.
 */
const TONE = {
  ACTIVE: "success",
  SUCCESS: "success",
  VERIFIED: "success",
  PENDING: "warning",
  FLAGGED: "warning",
  FROZEN: "warning",
  BLOCKED: "danger",
  FAILED: "danger",
  REJECTED: "danger",
  CLOSED: "neutral",
  REVERSED: "neutral",
};

const LABEL = {
  IN_APP: "In-app",
};

export default function StatusBadge({ status }) {
  if (!status) return <span className="badge badge--neutral">Unknown</span>;

  const tone = TONE[status] ?? "info";
  const label =
    LABEL[status] ??
    status.charAt(0) + status.slice(1).toLowerCase().replace(/_/g, " ");

  return <span className={`badge badge--${tone}`}>{label}</span>;
}
