import React, { useEffect } from 'react';
import './Toast.css';

export function Toast({
  isOpen = true,
  onClose,
  message,
  children,
  variant = 'neutral',
  isFixed = false,
  duration,
  className = '',
  ...props
}) {
  useEffect(() => {
    if (!isOpen || !duration || !onClose) return;

    const timer = setTimeout(() => {
      onClose();
    }, duration);

    return () => clearTimeout(timer);
  }, [isOpen, duration, onClose]);

  if (!isOpen) return null;

  const role = variant === 'danger' ? 'alert' : 'status';

  const classes = [
    'toast',
    `toast--${variant}`,
    isFixed ? 'toast--fixed' : '',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div
      role={role}
      aria-live={variant === 'danger' ? 'assertive' : 'polite'}
      className={classes}
      {...props}
    >
      <div className="toast__content">
        {message || children}
      </div>
      {onClose && (
        <button
          type="button"
          className="toast__close-btn"
          onClick={onClose}
          aria-label="Close notification"
        >
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      )}
    </div>
  );
}

export default Toast;
