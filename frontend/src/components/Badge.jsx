export default function Badge({ tone = 'gray', children, plain = false }) {
  return <span className={`badge badge-${tone} ${plain ? 'badge-plain' : ''}`}>{children}</span>
}
