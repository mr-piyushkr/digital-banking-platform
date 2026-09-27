# BankFlow — Design System

The frontend has to read like software a bank actually runs, not like a template.
These are binding rules, not suggestions. Every new screen follows them.

---

## 1. Hard prohibitions

These are the things that make an interface look generated. None of them appear
anywhere in this project.

| Never | Instead |
|---|---|
| Emoji in UI text, buttons, headings, empty states | A 16px Lucide icon, or nothing |
| Gradient fills on buttons, cards, headers, sidebars | Flat fills, one hairline border |
| Purple / violet / indigo accents | The institutional blue in `tokens.css` |
| Glassmorphism, blur, neon glow, `box-shadow` with colour | The single neutral `--shadow-1` |
| `border-radius` above 8px on containers | 3 / 5 / 7px. Pills only on status badges |
| Centred paragraphs of body text | Left-aligned, max 68ch |
| Marketing copy ("Banking, reimagined") | Plain labels ("Accounts", "Transfer funds") |
| Three-column feature grids with big icons | Data. Tables, figures, status |
| Spinners for table loads | Skeleton rows matching the real column widths |
| `Loading...` / `No data` as bare text | Real empty states with a primary action |
| More than two font weights on one screen | 500 for labels, 600 for headings |
| Decorative illustrations, stock photos, blobs | Nothing. Whitespace is fine |

## 2. Money and numbers — the biggest tell

Getting numbers right is what separates real financial UI from a demo.

- Every monetary figure renders through `formatMoney()` — never string concatenation.
- **Indian digit grouping**: `₹1,50,000.00`, not `₹150,000.00`. `Intl.NumberFormat('en-IN')`.
- Always two decimal places. `₹500.00`, never `₹500`.
- `font-variant-numeric: tabular-nums` on every number so columns align vertically.
- Monetary table columns are **right-aligned**. Text columns are left-aligned.
- Credits are `--credit-600` with a leading `+`. Debits are `--debit-600` with `−`
  (U+2212 minus, not a hyphen).
- Account numbers and transaction references use `--font-mono`, grouped in fours:
  `ACC0 0000 0001`.
- Dates: `28 Sep 2026, 11:42 PM` for detail views, `28/09/26` in dense tables.
  Never raw ISO strings in the UI.

## 3. Colour

Defined once in `src/styles/tokens.css`. Components never hardcode a hex value.

Neutrals carry a slight cool cast so they sit correctly against the blue. The
accent is a deep institutional blue — the colour of a bank, not of a startup
landing page. Semantic colours are desaturated on purpose: a flagged transaction
should read as serious, not as a notification badge.

Backgrounds pair with a matching `-100` token, never with opacity on the `-600`.

## 4. Typography

System stack, led by Segoe UI Variable on Windows 11 — no web font download, so
first paint has no layout shift.

Scale: 11, 12, 13, 14, 16, 20, 26, 32. Nothing in between.
Weights: 400 body, 500 labels and table headers, 600 headings. No 700, no 300.
Line height 1.25 for headings, 1.5 for body.

13px is the workhorse size for dense financial UI. 14px is for forms. 16px body
text is for documentation pages only.

## 5. Spacing and layout

4px base scale: 4, 8, 12, 16, 20, 24, 32, 40, 56. No arbitrary values.

- App shell: fixed 232px sidebar, 56px top bar, content max-width 1280px.
- Cards: 20px padding, 1px `--line` border, `--r-3`, `--shadow-1`. Never a border
  *and* a heavy shadow.
- Tables: 36px row height, 12px horizontal cell padding, sticky header,
  `--surface-1` on hover. No zebra striping — the hairline row border is enough.
- Forms: label above input, 6px gap, 8px between fields, 20px between groups.
  Error text sits directly under the field in `--debit-600` at 12px.

## 6. Interaction

- Every focusable element gets a visible `:focus-visible` ring — 2px `--focus`
  with 2px offset. Never `outline: none` without a replacement.
- Buttons have four distinct states: rest, hover, active, disabled. Disabled is
  reduced opacity plus `cursor: not-allowed`, never a different hue.
- Destructive actions (block account, delete beneficiary) always route through a
  confirm dialog that names the exact target.
- Any action that moves money shows a review step with the final amount and
  destination before it submits.
- Transitions: 120ms `ease-out` on colour and background only. Never animate
  layout, never bounce, no spring easing.
- Touch targets at least 32px tall, 40px on mobile.

## 7. Accessibility

Not optional — and it is also what makes the UI feel engineered.

- Body text meets WCAG AA (4.5:1). `--ink-400` is for 13px+ secondary text only.
- Colour is never the sole signal: a flagged row carries a text label too.
- Tables use real `<th scope>`; forms use real `<label for>`.
- Modals trap focus, close on Escape, and restore focus to the trigger.
- Skeletons carry `aria-busy`; toasts use `role="status"`.

## 8. Dark mode

`tokens.css` defines a `[data-theme="dark"]` block. Because components only ever
read tokens, dark mode costs nothing per screen. Shipped in Phase 5.

## 9. Code conventions

- One component per file. Co-located CSS: `DataTable.jsx` + `DataTable.css`.
- Plain CSS with tokens. No CSS-in-JS, no utility framework.
- Class naming is flat and scoped by component: `.datatable`, `.datatable__cell`,
  `.datatable__cell--numeric`.
- Comments explain *why*, never *what*. No comment restates the line below it.
- No `console.log` in committed code.
