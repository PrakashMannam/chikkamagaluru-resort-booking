import React from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import './GuestLayout.css';

function CoffeeLeafIcon() {
  return (
    <svg
      width="26"
      height="26"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#1E3A2F"
      strokeWidth="1.9"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="shrink-0"
      aria-hidden="true"
    >
      <path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z" />
      <path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12" />
    </svg>
  );
}

export function GuestLayout() {
  return (
    <div className="guest-shell">
      {/* 1. Top Navbar (Matches reference design) */}
      <header className="guest-header">
        <div className="guest-header__inner">
          {/* Left Brand: Leaf SVG + Stacked Serif Text */}
          <Link to="/" className="guest-header__brand" aria-label="Chikkamagaluru Estate Home">
            <CoffeeLeafIcon />
            <div className="guest-header__brand-text">
              <span className="guest-header__brand-title">Chikkamagaluru</span>
              <span className="guest-header__brand-sub">Estate</span>
            </div>
          </Link>

          {/* Center Links */}
          <nav className="guest-header__nav" aria-label="Main navigation">
            <NavLink to="/stay" className="guest-header__link">Stay</NavLink>
            <NavLink to="/enquire?type=wedding" className="guest-header__link">Weddings</NavLink>
            <NavLink to="/enquire?type=offsite" className="guest-header__link">Offsites</NavLink>
            <a href="#about" className="guest-header__link">About</a>
          </nav>

          {/* Right CTA */}
          <Link to="/stay" className="guest-header__cta">
            Check Availability
          </Link>
        </div>
      </header>

      {/* Main Content */}
      <main className="guest-main">
        <Outlet />
      </main>

      {/* Footer (Matches reference design light warm cream) */}
      <footer className="guest-footer" aria-label="Resort Information & Directory">
        <div className="guest-footer__inner">
          {/* Col 1: Brand Info */}
          <div className="guest-footer__brand-col">
            <div className="guest-footer__brand-header">
              <CoffeeLeafIcon />
              <div>
                <span className="guest-footer__brand-title">Chikkamagaluru</span>
                <span className="guest-footer__brand-sub">Estate</span>
              </div>
            </div>
            <p className="guest-footer__tagline">
              A coffee-estate resort in the Western Ghats, Karnataka.
            </p>
          </div>

          {/* Col 2: Stay */}
          <div className="guest-footer__col">
            <p className="guest-footer__heading">Stay</p>
            <Link to="/stay" className="guest-footer__link">Estate Rooms</Link>
            <Link to="/stay" className="guest-footer__link">Villas</Link>
            <Link to="/stay" className="guest-footer__link">Check Availability</Link>
          </div>

          {/* Col 3: Events & Enquiries */}
          <div className="guest-footer__col">
            <p className="guest-footer__heading">Events</p>
            <Link to="/enquire?type=wedding" className="guest-footer__link">Weddings</Link>
            <Link to="/enquire?type=offsite" className="guest-footer__link">Corporate Offsites</Link>
          </div>

          {/* Col 4: About */}
          <div className="guest-footer__col">
            <p className="guest-footer__heading">About</p>
            <a href="#about" className="guest-footer__link">The Estate</a>
            <span className="guest-footer__location">Western Ghats, Karnataka</span>
          </div>
        </div>

        {/* Bottom Sub-bar */}
        <div className="guest-footer__bottom">
          <p>© 2026 Chikkamagaluru Estate. All rights reserved.</p>
          <div className="guest-footer__bottom-links">
            <Link to="/privacy" className="guest-footer__bottom-link">Privacy</Link>
            <span>•</span>
            <Link to="/terms" className="guest-footer__bottom-link">Terms</Link>
            <span>•</span>
            <span className="guest-footer__dpdp">DPDP 2023</span>
            <span>•</span>
            <Link to="/staff/login" className="guest-footer__staff-link">Staff Login</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default GuestLayout;
