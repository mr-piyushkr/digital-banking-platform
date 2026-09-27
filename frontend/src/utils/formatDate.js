/** Raw ISO strings never reach the UI. Detail views and dense tables differ. */

const detail = new Intl.DateTimeFormat("en-IN", {
  day: "2-digit",
  month: "short",
  year: "numeric",
  hour: "numeric",
  minute: "2-digit",
  hour12: true,
});

const compact = new Intl.DateTimeFormat("en-IN", {
  day: "2-digit",
  month: "2-digit",
  year: "2-digit",
});

const dayOnly = new Intl.DateTimeFormat("en-IN", {
  day: "2-digit",
  month: "short",
  year: "numeric",
});

function parse(value) {
  if (!value) return null;
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** 28 Sep 2026, 11:42 PM */
export function formatDateTime(value) {
  const date = parse(value);
  return date ? detail.format(date) : "—";
}

/** 28/09/26 — for dense table columns */
export function formatDateCompact(value) {
  const date = parse(value);
  return date ? compact.format(date) : "—";
}

/** 28 Sep 2026 */
export function formatDate(value) {
  const date = parse(value);
  return date ? dayOnly.format(date) : "—";
}
