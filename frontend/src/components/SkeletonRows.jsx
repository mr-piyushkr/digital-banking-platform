/** Column widths mirror the real table so nothing shifts when data lands. */
export default function SkeletonRows({ columns = 4, rows = 5 }) {
  return (
    <tbody aria-busy="true">
      {Array.from({ length: rows }, (_, r) => (
        <tr key={r}>
          {Array.from({ length: columns }, (_, c) => (
            <td key={c}>
              <span className="skeleton-bar" style={{ width: c === 0 ? "70%" : "45%" }} />
            </td>
          ))}
        </tr>
      ))}
    </tbody>
  );
}
