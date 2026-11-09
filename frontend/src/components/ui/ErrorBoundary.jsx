import React from 'react';

export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ maxWidth: '40rem', margin: '4rem auto', padding: '2rem', textAlign: 'center' }}>
          <h2 style={{ fontFamily: 'var(--font-heading, serif)', color: 'var(--color-green-deep, #1b382b)' }}>
            Something went wrong
          </h2>
          <p style={{ color: 'var(--color-charcoal-light, #555)', margin: '1rem 0 2rem' }}>
            We encountered an unexpected issue while loading this page.
          </p>
          <a
            href="/"
            style={{
              display: 'inline-block',
              padding: '0.75rem 1.5rem',
              backgroundColor: 'var(--color-green-deep, #1b382b)',
              color: '#ffffff',
              borderRadius: '4px',
              textDecoration: 'none',
              fontWeight: 500,
              minHeight: '44px',
              lineHeight: '28px'
            }}
          >
            Return to Estate
          </a>
        </div>
      );
    }
    return this.props.children;
  }
}
