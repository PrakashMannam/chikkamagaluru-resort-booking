import React from 'react';
import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div style={{ maxWidth: '40rem', margin: '5rem auto', padding: '2rem', textAlign: 'center', fontFamily: 'inherit' }}>
      <h1 style={{ fontFamily: 'var(--font-heading, serif)', fontSize: '2.5rem', color: '#1E3A2F', marginBottom: '1rem' }}>
        Page Not Found
      </h1>
      <p style={{ color: '#5C5852', fontSize: '1rem', lineHeight: '1.6', marginBottom: '2rem' }}>
        The page you are looking for does not exist or has been moved.
      </p>
      <Link
        to="/"
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '0.75rem 1.75rem',
          backgroundColor: '#1E3A2F',
          color: '#FFFFFF',
          borderRadius: '0.5rem',
          textDecoration: 'none',
          fontWeight: 500,
          minHeight: '44px'
        }}
      >
        &larr; Return to Estate Home
      </Link>
    </div>
  );
}

export default NotFoundPage;
