import React from 'react';
import { Link, Outlet } from 'react-router-dom';
import './GuestLayout.css';

export function GuestLayout() {
  return (
    <div className="guest-shell">
      <header className="guest-header">
        <div className="guest-header__inner">
          <Link to="/" className="guest-header__brand">
            Chikkamagaluru Estate
          </Link>
          <nav className="guest-header__nav" aria-label="Main Navigation">
            <Link to="/enquire" className="guest-header__link">
              Enquire
            </Link>
          </nav>
        </div>
      </header>

      <main className="guest-main">
        <Outlet />
      </main>

      <footer className="guest-footer">
        <div className="guest-footer__inner">
          <div>
            <p><strong>Chikkamagaluru Estate &amp; Sanctuary</strong></p>
            <p style={{ marginTop: 'var(--space-1)' }}>
              Mullayanagiri Foothills, Western Ghats, Karnataka.
            </p>
            <p style={{ fontSize: 'var(--text-small)', marginTop: 'var(--space-1)' }}>
              Taxes shown are indicative in this test build.
            </p>
          </div>
          <div className="guest-footer__links">
            <Link to="/styleguide" className="guest-footer__link">
              Design System Styleguide
            </Link>
            <Link to="/enquire" className="guest-footer__link">
              Contact Concierge
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default GuestLayout;
