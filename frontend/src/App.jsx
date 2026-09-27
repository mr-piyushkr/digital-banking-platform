import { useEffect, useState } from "react";
import { Database, RefreshCw, ServerCog } from "lucide-react";
import axiosClient from "./api/axiosClient.js";
import { formatDateTime } from "./utils/formatDate.js";
import "./App.css";

const STATE = { LOADING: "loading", READY: "ready", FAILED: "failed" };

export default function App() {
  const [state, setState] = useState(STATE.LOADING);
  const [health, setHealth] = useState(null);
  const [error, setError] = useState(null);
  const [checkedAt, setCheckedAt] = useState(null);
  const [attempt, setAttempt] = useState(0);

  // Bumping `attempt` re-runs the effect, so the retry path and the mount path
  // share one code path. The cancel flag keeps a late response from a unmounted
  // component (or StrictMode's double invoke) from writing state.
  useEffect(() => {
    let cancelled = false;

    axiosClient
      .get("/health")
      .then(({ data }) => {
        if (cancelled) return;
        setHealth(data);
        setState(STATE.READY);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err);
        setState(STATE.FAILED);
      })
      .finally(() => {
        if (!cancelled) setCheckedAt(new Date());
      });

    return () => {
      cancelled = true;
    };
  }, [attempt]);

  const recheck = () => {
    setState(STATE.LOADING);
    setError(null);
    setAttempt((n) => n + 1);
  };

  const db = health?.database;

  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar__inner">
          <span className="wordmark">BankFlow</span>
          <span className="topbar__env">
            {health?.profiles?.length ? health.profiles.join(", ") : "—"}
          </span>
        </div>
      </header>

      <main className="content">
        <div className="page-head">
          <h1 className="page-head__title">System status</h1>
          <p className="page-head__sub">
            Phase 1 verification — the React app, the Spring Boot API and the
            MySQL schema are checked end to end.
          </p>
        </div>

        <section className="card" aria-busy={state === STATE.LOADING}>
          <div className="card__head">
            <div className="card__heading">
              <ServerCog size={16} strokeWidth={1.75} aria-hidden="true" />
              <h2>Backend API</h2>
            </div>
            <button
              type="button"
              className="btn btn--ghost"
              onClick={recheck}
              disabled={state === STATE.LOADING}
            >
              <RefreshCw size={14} strokeWidth={1.75} aria-hidden="true" />
              Re-check
            </button>
          </div>

          {state === STATE.LOADING && <SkeletonRows count={4} />}

          {state === STATE.FAILED && (
            <div className="notice notice--error" role="status">
              <p className="notice__title">{error?.message}</p>
              <p className="notice__body">
                Start the API from the <code>backend</code> module, then re-check.
                If it is already running, confirm the MySQL schema and the
                credentials in <code>.env</code>.
              </p>
            </div>
          )}

          {state === STATE.READY && (
            <>
              <dl className="facts">
                <Fact label="Status">
                  <StatusPill tone="credit" label={health.status} />
                </Fact>
                <Fact label="Application">{health.application}</Fact>
                <Fact label="Version" mono>
                  {health.version}
                </Fact>
                <Fact label="Active profile">
                  {health.profiles?.join(", ") || "default"}
                </Fact>
              </dl>

              <div className="card__divider" />

              <div className="card__heading card__heading--sub">
                <Database size={16} strokeWidth={1.75} aria-hidden="true" />
                <h3>Database</h3>
              </div>

              <dl className="facts">
                <Fact label="Reachable">
                  <StatusPill
                    tone={db?.reachable ? "credit" : "debit"}
                    label={db?.reachable ? "Connected" : "Unreachable"}
                  />
                </Fact>
                {db?.reachable ? (
                  <>
                    <Fact label="Engine">{db.product}</Fact>
                    <Fact label="Version" mono>
                      {db.version}
                    </Fact>
                    <Fact label="Schema" mono>
                      {db.schema}
                    </Fact>
                  </>
                ) : (
                  <Fact label="Reason">{db?.error ?? "Unknown"}</Fact>
                )}
              </dl>
            </>
          )}
        </section>

        {checkedAt && (
          <p className="timestamp">Last checked {formatDateTime(checkedAt)}</p>
        )}
      </main>
    </div>
  );
}

function Fact({ label, children, mono = false }) {
  return (
    <div className="facts__row">
      <dt className="facts__label">{label}</dt>
      <dd className={`facts__value${mono ? " mono" : ""}`}>{children}</dd>
    </div>
  );
}

function StatusPill({ tone, label }) {
  return <span className={`pill pill--${tone}`}>{label}</span>;
}

function SkeletonRows({ count }) {
  return (
    <div className="skeleton">
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="skeleton__row">
          <span className="skeleton__bar skeleton__bar--label" />
          <span className="skeleton__bar skeleton__bar--value" />
        </div>
      ))}
      <span className="visually-hidden">Checking backend status</span>
    </div>
  );
}
