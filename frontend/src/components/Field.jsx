import { useId } from "react";

/**
 * Renders a real <label for> bound to the control, so clicking the label
 * focuses the input and screen readers announce the two together.
 */
export default function Field({ label, error, hint, numeric = false, children, ...rest }) {
  const id = useId();
  const errorId = `${id}-error`;
  const hintId = `${id}-hint`;

  const describedBy = [error ? errorId : null, hint ? hintId : null]
    .filter(Boolean)
    .join(" ");

  return (
    <div className="field">
      <label className="field__label" htmlFor={id}>
        {label}
      </label>
      {children ?? (
        <input
          id={id}
          className={`input${numeric ? " input--numeric" : ""}${error ? " input--error" : ""}`}
          aria-invalid={Boolean(error)}
          aria-describedby={describedBy || undefined}
          {...rest}
        />
      )}
      {hint && !error && (
        <span id={hintId} className="field__hint">
          {hint}
        </span>
      )}
      {error && (
        <span id={errorId} className="field__error">
          {error}
        </span>
      )}
    </div>
  );
}
