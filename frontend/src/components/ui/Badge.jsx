import React from 'react';
import './Badge.css';

export function Badge({
  children,
  variant = 'neutral',
  className = '',
  ...props
}) {
  const classes = [
    'badge',
    `badge--${variant}`,
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <span className={classes} {...props}>
      {children}
    </span>
  );
}

export default Badge;
