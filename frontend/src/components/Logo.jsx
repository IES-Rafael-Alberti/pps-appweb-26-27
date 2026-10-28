/** Marca: huella canina en verde corporativo + wordmark. */
export default function Logo({ size = 52, className = 'brand-logo' }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 64 64" role="img" aria-label="Galgo Español">
      <circle cx="32" cy="32" r="32" fill="var(--green, #009a3e)" />
      <g fill="#ffffff">
        <ellipse cx="32" cy="42" rx="12.5" ry="10.5" />
        <circle cx="16.5" cy="28" r="5.6" />
        <circle cx="26" cy="21" r="5.6" />
        <circle cx="38" cy="21" r="5.6" />
        <circle cx="47.5" cy="28" r="5.6" />
      </g>
    </svg>
  )
}
