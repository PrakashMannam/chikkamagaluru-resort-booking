import React from 'react';
import './EmptyState.css';

export function EmptyState({
  title,
  description,
  action,
  icon,
  className = '',
  ...props
}) {
  return (
    <div className={`empty-state ${className}`.trim()} {...props}>
      {icon && <div className="empty-state__icon" aria-hidden="true">{icon}</div>}
      {title && <h3 className="empty-state__title">{title}</h3>}
      {description && <p className="empty-state__description">{description}</p>}
      {action && <div className="empty-state__action">{action}</div>}
    </div>
  );
}

export default EmptyState;
