import React, { useState, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { BOOKING_WINDOW, parseLocalDate, formatLocalDate } from '../utils/dateUtils';
import './LandingPage.css';

// SVG Icons for clean, pixel-perfect rendering
function CalendarIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#5C5852" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
  );
}

function UserIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#5C5852" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  );
}

function ArrowRightIcon({ width = 16, height = 16, strokeWidth = 2.2 }) {
  return (
    <svg
      width={width}
      height={height}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className="shrink-0"
    >
      <line x1="5" y1="12" x2="19" y2="12" />
      <polyline points="12 5 19 12 12 19" />
    </svg>
  );
}

function KeyIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 2l-2 2m-1.5 1.5L14 9l-1.5-1.5M3 13a7 7 0 1 0 10 5.6L21 11V7h-4l-1.5 1.5" />
    </svg>
  );
}

function RingsIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="9" cy="12" r="6" />
      <circle cx="15" cy="12" r="6" />
    </svg>
  );
}

function BriefcaseIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
      <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
    </svg>
  );
}

function LocationIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
      <circle cx="12" cy="10" r="3" />
    </svg>
  );
}

export default function LandingPage() {
  const navigate = useNavigate();
  const [checkIn, setCheckIn] = useState('');
  const [checkOut, setCheckOut] = useState('');
  const [adults, setAdults] = useState(2);
  const [checkInFocused, setCheckInFocused] = useState(false);
  const [checkOutFocused, setCheckOutFocused] = useState(false);
  const [error, setError] = useState('');

  const checkInInputRef = useRef(null);
  const checkOutInputRef = useRef(null);

  const todayStr = formatLocalDate(new Date());
  const minCheckIn = todayStr > BOOKING_WINDOW.MIN_DATE ? todayStr : BOOKING_WINDOW.MIN_DATE;

  function getNextDay(dateStr) {
    const d = parseLocalDate(dateStr);
    if (!d) return '';
    d.setDate(d.getDate() + 1);
    return formatLocalDate(d);
  }

  function handleCheckInChange(val) {
    setCheckIn(val);
    setError('');
    if (checkOut && val && checkOut <= val) {
      setCheckOut('');
    }
  }

  function handleCheckOutChange(val) {
    setCheckOut(val);
    setError('');
  }

  function handleSearch(e) {
    e.preventDefault();
    if (!checkIn) {
      setError('Please select an arrival date.');
      checkInInputRef.current?.focus();
      return;
    }
    if (!checkOut) {
      setError('Please select a departure date.');
      checkOutInputRef.current?.focus();
      return;
    }
    if (checkIn < minCheckIn) {
      setError('Arrival date cannot be in the past.');
      return;
    }
    if (checkOut <= checkIn) {
      setError('Departure date must follow arrival date.');
      return;
    }
    if (checkOut > BOOKING_WINDOW.MAX_DATE) {
      setError(`Bookings are accepted up to ${BOOKING_WINDOW.MAX_DATE}.`);
      return;
    }
    setError('');
    const params = new URLSearchParams({
      checkIn,
      checkOut,
      rooms: '1',
      adults: String(adults),
      children: '0',
    });
    navigate(`/stay?${params.toString()}`);
  }

  return (
    <div className="lp-root">
      {/* 2. Hero Section + Floating Search Bar */}
      <section className="lp-hero-section">
        <div className="lp-hero-wrapper">
          {/* Background image & gradient blend */}
          <div className="lp-hero-bg">
            <img
              src="/images/hero-estate.webp"
              srcSet="/images/hero-estate-800w.webp 800w, /images/hero-estate.webp 1600w"
              sizes="100vw"
              alt="Chikkamagaluru coffee estate cottage veranda overlooking misty mountains"
              className="lp-hero-img"
              fetchpriority="high"
            />
            <div className="lp-hero-gradient" />
          </div>

          {/* Left Content Column */}
          <div className="lp-hero-content">
            <h1 className="lp-hero-heading">
              A Coffee-Estate Retreat in Chikkamagaluru
            </h1>
            <p className="lp-hero-sub">
              Stay amidst coffee gardens, misty hills and the quiet of the Western Ghats.
            </p>
            <div className="lp-hero-buttons">
              <button
                type="button"
                className="lp-btn-book"
                onClick={() => {
                  const el = checkInInputRef.current || document.getElementById('lp-checkin');
                  if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    el.focus();
                    try { el.showPicker?.(); } catch (err) {}
                  }
                }}
              >
                Book a Stay
              </button>
              <Link to="/enquire" className="lp-btn-event">
                Plan an Event
              </Link>
            </div>
          </div>
        </div>

        {/* Floating Search Bar */}
        <div className="lp-search-container">
          <form className="lp-search-bar" onSubmit={handleSearch} noValidate>
            {/* Cell 1: Check-in */}
            <div
              className="lp-search-cell"
              onClick={() => checkInInputRef.current?.focus()}
            >
              <CalendarIcon />
              <div className="lp-search-field">
                <label htmlFor="lp-checkin" className="lp-search-label">Check-in</label>
                <input
                  id="lp-checkin"
                  ref={checkInInputRef}
                  type={checkInFocused || checkIn ? 'date' : 'text'}
                  placeholder="Add arrival date"
                  className="lp-search-input"
                  value={checkIn}
                  min={minCheckIn}
                  max={BOOKING_WINDOW.MAX_DATE}
                  onFocus={(e) => {
                    setCheckInFocused(true);
                    try { e.target.showPicker?.(); } catch (err) {}
                  }}
                  onBlur={() => setCheckInFocused(false)}
                  onChange={(e) => handleCheckInChange(e.target.value)}
                />
              </div>
            </div>

            {/* Cell 2: Check-out */}
            <div
              className="lp-search-cell"
              onClick={() => checkOutInputRef.current?.focus()}
            >
              <CalendarIcon />
              <div className="lp-search-field">
                <label htmlFor="lp-checkout" className="lp-search-label">Check-out</label>
                <input
                  id="lp-checkout"
                  ref={checkOutInputRef}
                  type={checkOutFocused || checkOut ? 'date' : 'text'}
                  placeholder="Add departure date"
                  className="lp-search-input"
                  value={checkOut}
                  min={checkIn ? getNextDay(checkIn) : minCheckIn}
                  max={BOOKING_WINDOW.MAX_DATE}
                  onFocus={(e) => {
                    setCheckOutFocused(true);
                    try { e.target.showPicker?.(); } catch (err) {}
                  }}
                  onBlur={() => setCheckOutFocused(false)}
                  onChange={(e) => handleCheckOutChange(e.target.value)}
                />
              </div>
            </div>

            {/* Cell 3: Guests */}
            <div className="lp-search-cell">
              <UserIcon />
              <div className="lp-search-field">
                <label className="lp-search-label">Guests</label>
                <select
                  className="lp-search-select"
                  value={adults}
                  onChange={(e) => setAdults(Number(e.target.value))}
                >
                  <option value={1}>1 Adult</option>
                  <option value={2}>2 Adults</option>
                  <option value={3}>3 Adults</option>
                  <option value={4}>4 Adults</option>
                  <option value={5}>5 Adults</option>
                  <option value={6}>6 Adults</option>
                </select>
              </div>
            </div>

            {/* Cell 4: Submit Button */}
            <div className="lp-search-action">
              <button type="submit" className="lp-search-submit">
                Search Availability
              </button>
            </div>

            {error && (
              <div className="lp-search-error" role="alert">
                {error}
              </div>
            )}
          </form>
        </div>
      </section>

      {/* 3. 4-Item Estate Highlights Strip */}
      <section className="lp-highlights-strip">
        <div className="lp-highlights-grid">
          {/* Item 1 */}
          <div className="lp-highlight-item">
            <div className="lp-highlight-icon">
              <KeyIcon />
            </div>
            <div className="lp-highlight-text">
              <span className="lp-highlight-title">76 Keys</span>
              <span className="lp-highlight-desc">64 Rooms • 12 Villas</span>
            </div>
          </div>

          {/* Item 2 */}
          <div className="lp-highlight-item">
            <div className="lp-highlight-icon">
              <RingsIcon />
            </div>
            <div className="lp-highlight-text">
              <span className="lp-highlight-title">Weddings</span>
              <span className="lp-highlight-desc">Scenic venues</span>
            </div>
          </div>

          {/* Item 3 */}
          <div className="lp-highlight-item">
            <div className="lp-highlight-icon">
              <BriefcaseIcon />
            </div>
            <div className="lp-highlight-text">
              <span className="lp-highlight-title">Corporate Offsites</span>
              <span className="lp-highlight-desc">For teams &amp; leaders</span>
            </div>
          </div>

          {/* Item 4 */}
          <div className="lp-highlight-item">
            <div className="lp-highlight-icon">
              <LocationIcon />
            </div>
            <div className="lp-highlight-text">
              <span className="lp-highlight-title">Western Ghats</span>
              <span className="lp-highlight-desc">Chikkamagaluru, Karnataka</span>
            </div>
          </div>
        </div>
      </section>

      {/* 4. THE ESTATE — A Stay Rooted in Nature */}
      <section id="about" className="lp-section lp-estate-section">
        <span id="estate" className="lp-anchor-target" />
        <div className="lp-estate-grid">
          <div className="lp-estate-content">
            <span className="lp-eyebrow">THE ESTATE</span>
            <h2 className="lp-section-title">A Stay Rooted in Nature</h2>
            <p className="lp-section-lead">
              A serene coffee-estate retreat where thoughtful stays, heartfelt hospitality and the beauty of Chikkamagaluru come together.
            </p>
            <Link to="/stay" className="lp-link-underline">
              Explore Stays &rarr;
            </Link>
          </div>

          <div className="lp-estate-media">
            <img
              src="/images/coffee-plantation-cherries.jpg"
              alt="Cluster of ripe red coffee cherries on estate tree in Chikkamagaluru"
              className="lp-estate-img"
              loading="lazy"
              decoding="async"
              width="600"
              height="400"
            />
          </div>
        </div>
      </section>

      {/* 5. Stay With Us (Accommodation Showcase) */}
      <section className="lp-section lp-stay-section">
        <div className="lp-stay-header">
          <div>
            <h2 className="lp-section-title">Stay With Us</h2>
            <p className="lp-section-sub">
              Thoughtfully designed rooms and villas for restful stays amidst coffee gardens.
            </p>
          </div>
        </div>

        <div className="lp-stay-grid">
          {/* Card 1: Estate Rooms */}
          <article className="lp-card-stay">
            <div className="lp-card-stay__media">
              <img
                src="/images/room-canopy.webp"
                alt="Estate Room with teakwood canopy bed and garden views"
                className="lp-card-stay__img"
                loading="lazy"
                decoding="async"
                width="600"
                height="400"
              />
            </div>
            <div className="lp-card-stay__body">
              <div className="lp-card-stay__info">
                <h3 className="lp-card-stay__title">Estate Rooms</h3>
                <p className="lp-card-stay__desc">
                  Nestled in the lush coffee plantation canopy with private balcony and serene estate views.
                </p>
              </div>

              <div className="lp-card-stay__actions">
                <Link
                  to="/rooms/ROOM"
                  className="lp-card-stay__link"
                >
                  View Details
                </Link>
                <Link
                  to="/rooms/ROOM"
                  className="lp-card-stay__btn"
                  aria-label="View Estate Rooms"
                >
                  <ArrowRightIcon />
                </Link>
              </div>
            </div>
          </article>

          {/* Card 2: Villas */}
          <article className="lp-card-stay">
            <div className="lp-card-stay__media">
              <img
                src="/images/villa-pool.webp"
                alt="Private villa terrace with plunge pool overlooking misty Western Ghats hills"
                className="lp-card-stay__img"
                loading="lazy"
                decoding="async"
                width="600"
                height="400"
              />
            </div>
            <div className="lp-card-stay__body">
              <div className="lp-card-stay__info">
                <h3 className="lp-card-stay__title">Villas</h3>
                <p className="lp-card-stay__desc">
                  Spacious private villas featuring secluded outdoor sit-outs, plunge pools, and sweeping estate panoramas.
                </p>
              </div>

              <div className="lp-card-stay__actions">
                <Link
                  to="/rooms/VILLA"
                  className="lp-card-stay__link"
                >
                  View Details
                </Link>
                <Link
                  to="/rooms/VILLA"
                  className="lp-card-stay__btn"
                  aria-label="View Villas"
                >
                  <ArrowRightIcon />
                </Link>
              </div>
            </div>
          </article>
        </div>
      </section>

      {/* 6. More Than a Stay (Weddings & Corporate Offsites) */}
      <section className="lp-section lp-more-section">
        <h2 className="lp-section-title lp-section-title--space">More Than a Stay</h2>

        {/* 2-Column Split Event Cards */}
        <div className="lp-events-grid">
          {/* Event 1: Weddings */}
          <article className="lp-event-card">
            <div className="lp-event-card__info">
              <div>
                <h3 className="lp-event-card__title">Weddings</h3>
                <p className="lp-event-card__desc">
                  Open lawns for celebrations and memorable gatherings.
                </p>
              </div>
              <div className="lp-event-card__actions">
                <Link
                  to="/enquire?type=wedding"
                  className="lp-event-card__link"
                >
                  Plan a Wedding &rarr;
                </Link>
                <Link
                  to="/enquire?type=wedding"
                  className="lp-event-card__btn"
                  aria-label="Plan a destination wedding"
                >
                  <ArrowRightIcon />
                </Link>
              </div>
            </div>
            <div className="lp-event-card__media">
              <img
                src="/images/wedding-lawn-estate.jpg"
                alt="Open lawns and wedding mandap on coffee estate grounds"
                className="lp-event-card__img"
                loading="eager"
                decoding="async"
                width="600"
                height="400"
              />
            </div>
          </article>

          {/* Event 2: Corporate Offsites */}
          <article className="lp-event-card">
            <div className="lp-event-card__info">
              <div>
                <h3 className="lp-event-card__title">Corporate Offsites</h3>
                <p className="lp-event-card__desc">
                  A setting for teams to gather, connect and work together.
                </p>
              </div>
              <div className="lp-event-card__actions">
                <Link
                  to="/enquire?type=offsite"
                  className="lp-event-card__link"
                >
                  Plan an Offsite &rarr;
                </Link>
                <Link
                  to="/enquire?type=offsite"
                  className="lp-event-card__btn"
                  aria-label="Plan a corporate offsite"
                >
                  <ArrowRightIcon />
                </Link>
              </div>
            </div>
            <div className="lp-event-card__media">
              <img
                src="/images/corporate-offsite-estate.jpg"
                alt="Open-air executive retreat pavilion overlooking misty coffee hills"
                className="lp-event-card__img"
                loading="eager"
                decoding="async"
                width="600"
                height="400"
              />
            </div>
          </article>
        </div>
      </section>

      {/* 7. Bottom CTA Banner */}
      <section className="lp-cta-banner-wrapper">
        <div className="lp-cta-banner">
          <img
            src="/images/hero-estate-800w.webp"
            alt="Estate hills background"
            className="lp-cta-banner__bg"
            loading="lazy"
            decoding="async"
          />
          <div className="lp-cta-banner__overlay" />
          <div className="lp-cta-banner__content">
            <h2 className="lp-cta-banner__title">Ready for a Coffee-Estate Escape?</h2>
            <p className="lp-cta-banner__sub">
              Check availability and plan your stay at Chikkamagaluru Estate.
            </p>
            <Link to="/stay" className="lp-cta-banner__btn">
              Book a Stay
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
