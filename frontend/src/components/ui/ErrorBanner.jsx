import React from 'react';
import './ErrorBanner.css';

export function ErrorBanner({
  title,
  message,
  children,
  variant = 'error',
  actionLabel,
  onAction,
  onDismiss,
  className = '',
  ...props
}) {
  const classes = [
    'error-banner',
    `error-banner--${variant}`,
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={classes} role="alert" {...props}>
      <div className="error-banner__content">
        <span className="error-banner__icon" aria-hidden="true">
          {variant === 'error' ? (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          ) : (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
          )}
        </span>
        <div className="error-banner__body">
          {title && <h4 className="error-banner__title">{title}</h4>}
          {(message || children) && (
            <div className="error-banner__message">{message || children}</div>
          )}
        </div>
      </div>
      {(actionLabel || onDismiss) && (
        <div className="error-banner__actions">
          {actionLabel && onAction && (
            <button
              type="button"
              className="error-banner__btn"
              onClick={onAction}
            >
              {actionLabel}
            </button>
          )}
          {onDismiss && (
            <button
              type="button"
              className="error-banner__btn"
              onClick={onDismiss}
              aria-label="Dismiss banner"
            >
              Dismiss
            </button>
          )}
        </div>
      )}
    </div>
  );
}

export default ErrorBanner;
