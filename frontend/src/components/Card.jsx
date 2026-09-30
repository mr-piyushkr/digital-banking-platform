export default function Card({ title, subtitle, actions, flush = false, children }) {
  return (
    <section className="card">
      {(title || actions) && (
        <header className="card__header">
          <div>
            {title && <h2 className="card__title">{title}</h2>}
            {subtitle && <p className="card__subtitle">{subtitle}</p>}
          </div>
          {actions && <div className="row">{actions}</div>}
        </header>
      )}
      <div className={`card__body${flush ? " card__body--flush" : ""}`}>{children}</div>
    </section>
  );
}
