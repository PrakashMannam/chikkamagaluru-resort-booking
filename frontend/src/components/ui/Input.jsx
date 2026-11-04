import React, { useId } from 'react';
import './Input.css';

export function Input({
  id: customId,
  label,
  helperText,
  error,
  type = 'text',
  disabled = false,
  className = '',
  ...props
}) {
  const generatedId = useId();
  const id = customId || generatedId;
  const helperId = `${id}-helper`;
  const errorId = `${id}-error`;

  const describedBy = [
    error ? errorId : null,
    helperText ? helperId : null,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={`input-group ${className}`.trim()}>
      {label && (
        <label htmlFor={id} className="input__label">
          {label}
        </label>
      )}
      <input
        id={id}
        type={type}
        disabled={disabled}
        aria-invalid={Boolean(error)}
        aria-describedby={describedBy || undefined}
        className={`input__field ${error ? 'input__field--error' : ''}`.trim()}
        {...props}
      />
      {error && (
        <p id={errorId} className="input__error" role="alert">
          {error}
        </p>
      )}
      {!error && helperText && (
        <p id={helperId} className="input__helper">
          {helperText}
        </p>
      )}
    </div>
  );
}

export default Input;
