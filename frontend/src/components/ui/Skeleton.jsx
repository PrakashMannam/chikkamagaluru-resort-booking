import React from 'react';
import './Skeleton.css';

export function Skeleton({
  width,
  height,
  variant = 'rectangular',
  borderRadius,
  className = '',
  style = {},
  ...props
}) {
  const inlineStyles = {
    ...(width ? { width } : {}),
    ...(height ? { height } : {}),
    ...(borderRadius ? { borderRadius } : {}),
    ...style,
  };

  const classes = [
    'skeleton',
    `skeleton--${variant}`,
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <span
      className={classes}
      style={inlineStyles}
      aria-hidden="true"
      {...props}
    />
  );
}

export default Skeleton;
