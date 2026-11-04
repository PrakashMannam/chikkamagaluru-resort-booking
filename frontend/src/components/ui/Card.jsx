import React from 'react';
import './Card.css';

export function Card({
  children,
  interactive = false,
  className = '',
  as: Component = 'div',
  ...props
}) {
  const classes = [
    'card',
    interactive ? 'card--interactive' : '',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <Component
      className={classes}
      tabIndex={interactive ? 0 : undefined}
      {...props}
    >
      {children}
    </Component>
  );
}

export default Card;
