import React, { useEffect, useCallback, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { shouldShowHeaderCta } from '../../utils/navigationUtils.js';
import { CoffeeLeafIcon } from '../common/Icons.jsx';
import { Modal } from '../ui';
import './GuestLayout.css';

export function GuestLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const showHeaderCta = shouldShowHeaderCta(location.pathname);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [dpdpModalOpen, setDpdpModalOpen] = useState(false);

  const scrollToAnchor = useCallback((targetId) => {
    const elem = document.getElementById(targetId);
    if (elem) {
      elem.scrollIntoView({ behavior: 'smooth' });
      return true;
    }
    return false;
  }, []);

  useEffect(() => {
    if ((location.pathname === '/' || location.pathname === '') && (location.hash === '#about' || location.hash === '#estate')) {
      const targetId = location.hash.replace('#', '');
      const timer = setTimeout(() => {
        const elem = document.getElementById(targetId);
        if (elem) {
          elem.scrollIntoView({ behavior: 'smooth' });
        }
      }, 100);
      return () => clearTimeout(timer);
    }
  }, [location.pathname, location.hash]);

  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname, location.search]);

  const handleAboutClick = (e) => {
    if (location.pathname === '/' && (location.hash === '#about' || location.hash === '#estate')) {
      e.preventDefault();
      scrollToAnchor('about');
    }
  };

  const handleCtaClick = () => {
    if (location.pathname === '/') {
      const el = document.getElementById('lp-checkin') || document.querySelector('.lp-search-container');
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        el.focus?.();
        try { el.showPicker?.(); } catch (err) {}
      }
    } else {
      navigate('/stay');
    }
  };

  return (
    <div className="guest-shell">
      {/* 1. Top Navbar (Matches reference design) */}
      <header className="guest-header">
        <div className="guest-header__inner">
          {/* Left Brand: Leaf SVG + Stacked Serif Text */}
          <Link to="/" className="guest-header__brand" aria-label="Chikkamagaluru Estate Home">
            <CoffeeLeafIcon size={26} />
            <div className="guest-header__brand-text">
              <span className="guest-header__brand-title">Chikkamagaluru</span>
              <span className="guest-header__brand-sub">Estate</span>
            </div>
          </Link>

          <button
            type="button"
            className="guest-header__menu-toggle"
            aria-label={mobileMenuOpen ? 'Close main navigation' : 'Open main navigation'}
            aria-expanded={mobileMenuOpen}
            aria-controls="guest-main-navigation"
            onClick={() => setMobileMenuOpen((open) => !open)}
          >
            {mobileMenuOpen ? 'Close' : 'Menu'}
          </button>

          {/* Center Links with Active indicator */}
          <nav
            id="guest-main-navigation"
            className={`guest-header__nav${mobileMenuOpen ? ' is-open' : ''}`}
            aria-label="Main navigation"
          >
            <NavLink to="/stay" className="guest-header__link">Stay</NavLink>
            <NavLink to="/weddings" className="guest-header__link">Weddings</NavLink>
            <NavLink to="/offsites" className="guest-header__link">Offsites</NavLink>
            <Link to="/#estate" className="guest-header__link" onClick={(e) => { setMobileMenuOpen(false); handleAboutClick(e); }}>About</Link>
          </nav>

          {/* Right CTA: contextually omitted on /stay, /rooms/:code, and booking funnel */}
          {showHeaderCta ? (
            <button
              type="button"
              className="guest-header__cta"
              onClick={handleCtaClick}
            >
              Check Availability
            </button>
          ) : (
            <div className="guest-header__utility-group">
              <Link to="/" className="guest-header__utility-link">
                &larr; Back to Estate
              </Link>
            </div>
          )}
        </div>
      </header>

      {/* Main Content */}
      <main className="guest-main">
        <Outlet />
      </main>

      {/* 2. Sleek Compact Boutique Footer */}
      <footer className="guest-footer" aria-label="Resort Information & Directory">
        <div className="guest-footer__top">
          {/* Left: Brand */}
          <div className="guest-footer__brand-wrap">
            <Link to="/" className="guest-footer__brand-link" aria-label="Chikkamagaluru Estate Home">
              <CoffeeLeafIcon size={22} />
              <span className="guest-footer__brand-title">Chikkamagaluru Estate</span>
            </Link>
            <p className="guest-footer__tagline">
              A 76-key coffee-estate sanctuary in the Western Ghats, Karnataka.
            </p>
          </div>

          {/* Right: Horizontal Nav Links */}
          <nav className="guest-footer__nav-row" aria-label="Footer Navigation">
            <Link to="/stay" className="guest-footer__link">Stay</Link>
            <Link to="/weddings" className="guest-footer__link">Weddings</Link>
            <Link to="/offsites" className="guest-footer__link">Corporate Offsites</Link>
            <Link to="/#estate" className="guest-footer__link" onClick={handleAboutClick}>The Estate</Link>
            <Link to="/staff/login" className="guest-footer__link">Staff Portal</Link>
          </nav>
        </div>

        {/* Bottom Legal Bar */}
        <div className="guest-footer__bottom">
          <p className="guest-footer__compliance">
            &copy; 2026 Chikkamagaluru Estate. DPDP Act 2023 Compliant &bull; Foreign Guest Form C at Check-in.
          </p>
          <div className="guest-footer__bottom-links">
            <Link to="/privacy" className="guest-footer__bottom-link">Privacy</Link>
            <span>&bull;</span>
            <Link to="/terms" className="guest-footer__bottom-link">Terms</Link>
            <span>&bull;</span>
            <button
              type="button"
              className="guest-footer__dpdp-btn"
              onClick={(e) => { e.preventDefault(); setDpdpModalOpen(true); }}
              aria-label="View DPDP Act 2023 & Check-in compliance details"
            >
              DPDP 2023 &amp; Form C
            </button>
          </div>
        </div>
      </footer>

      {/* Compliance Information Modal */}
      {dpdpModalOpen && (
        <Modal
          isOpen={dpdpModalOpen}
          onClose={() => setDpdpModalOpen(false)}
          title="Guest Privacy & Check-in Compliance"
        >
          <div style={{ padding: '0.5rem 0', color: '#1C1917', lineHeight: '1.6' }}>
            <h4 style={{ fontFamily: 'var(--font-heading)', color: '#1E3A2F', marginBottom: '0.5rem' }}>
              Digital Personal Data Protection Act, 2023 (DPDP Act)
            </h4>
            <p style={{ fontSize: '0.875rem', marginBottom: '1rem', color: '#555' }}>
              Guest contact details collected online (name, phone, email) are processed strictly for reservation confirmation, stay coordination, and booking receipts. We do not store sensitive payment card details or unnecessary personal identifiers.
            </p>

            <h4 style={{ fontFamily: 'var(--font-heading)', color: '#1E3A2F', marginBottom: '0.5rem' }}>
              Foreign Guest Registration (Bureau of Immigration Form C)
            </h4>
            <p style={{ fontSize: '0.875rem', marginBottom: '1.5rem', color: '#555' }}>
              In accordance with Ministry of Home Affairs regulations, passport verification and Form C compliance for international guests are completed in person upon physical check-in at the resort reception desk, never over unencrypted web forms.
            </p>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <Link
                to="/privacy"
                onClick={() => setDpdpModalOpen(false)}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  padding: '0.5rem 1rem',
                  fontSize: '0.875rem',
                  color: '#1E3A2F',
                  textDecoration: 'underline'
                }}
              >
                Read Full Privacy Policy &rarr;
              </Link>
              <button
                type="button"
                onClick={() => setDpdpModalOpen(false)}
                style={{
                  padding: '0.5rem 1.25rem',
                  backgroundColor: '#1E3A2F',
                  color: '#FFFFFF',
                  borderRadius: '0.375rem',
                  fontSize: '0.875rem',
                  fontWeight: 500,
                  border: 'none',
                  cursor: 'pointer'
                }}
              >
                Close
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

export default GuestLayout;
