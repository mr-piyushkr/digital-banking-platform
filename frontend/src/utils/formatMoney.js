/**
 * Indian digit grouping: 1,50,000.00 — not 150,000.00. Every monetary figure in
 * the app goes through here so grouping and decimal places can never drift
 * between screens.
 */
const inr = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const plain = new Intl.NumberFormat("en-IN", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatMoney(amount) {
  const value = Number(amount);
  return Number.isFinite(value) ? inr.format(value) : "—";
}

/** Same grouping without the symbol — for table columns with a ₹ header. */
export function formatAmount(amount) {
  const value = Number(amount);
  return Number.isFinite(value) ? plain.format(value) : "—";
}

/**
 * Signed figure for ledger views. Uses U+2212 minus, not a hyphen, so the glyph
 * matches the digit width in tabular-nums columns.
 */
export function formatSigned(amount, direction) {
  const value = Math.abs(Number(amount));
  if (!Number.isFinite(value)) return "—";
  const sign = direction === "CREDIT" ? "+" : "−";
  return `${sign}${inr.format(value)}`;
}

/** ACC0000000001 -> ACC0 0000 0001, so long identifiers stay readable. */
export function formatAccountNumber(accountNumber) {
  if (!accountNumber) return "—";
  return String(accountNumber).replace(/(.{4})(?=.)/g, "$1 ");
}
