export function Field({ label, error, required, hint, children, className = '' }) {
  return (
    <div className={`field ${className}`}>
      {label && (
        <label>
          {label}
          {required && <span className="req"> *</span>}
        </label>
      )}
      {children}
      {hint && !error && <span className="hint">{hint}</span>}
      {error && <span className="error-text">{error}</span>}
    </div>
  )
}

export function TextInput({ invalid, className = '', ...props }) {
  return <input className={`input ${invalid ? 'invalid' : ''} ${className}`} {...props} />
}

export function TextArea({ invalid, className = '', ...props }) {
  return <textarea className={`textarea ${invalid ? 'invalid' : ''} ${className}`} {...props} />
}

export function SelectInput({ invalid, options = [], placeholder, className = '', ...props }) {
  return (
    <select className={`select ${invalid ? 'invalid' : ''} ${className}`} {...props}>
      {placeholder !== undefined && <option value="">{placeholder}</option>}
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  )
}
