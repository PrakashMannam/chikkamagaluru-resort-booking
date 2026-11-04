import React from 'react';
import './Button.css';

export function Button({
  children,
  variant = 'primary',
  isLoading = false,
  disabled = false,
  type = 'button',
  className = '',
  ...props
}) {
  const classes = [
    'btn',
    `btn--${variant}`,
    isLoading ? 'btn--loading' : '',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <button
      type={type}
      disabled={disabled || isLoading}
      aria-busy={isLoading}
      className={classes}
      {...props}
    >
      {isLoading && (
        <span className="btn__spinner" aria-hidden="true" />
      )}
      <span>{children}</span>
    </button>
  );
}

export default Button;
