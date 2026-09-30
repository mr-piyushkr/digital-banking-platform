export default function PageLoader({ label = "Loading" }) {
  return (
    <div className="page-loader" aria-busy="true">
      <span className="skeleton-bar" style={{ width: "160px" }} />
      <span className="skeleton-bar" style={{ width: "260px" }} />
      <span className="skeleton-bar" style={{ width: "200px" }} />
      <span className="visually-hidden">{label}</span>
    </div>
  );
}
