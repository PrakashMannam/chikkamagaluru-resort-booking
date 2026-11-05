import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { fetchAvailability } from '../api/roomsApi';
import {
  BOOKING_WINDOW,
  calculateNights,
  formatDisplayDate,
} from '../utils/dateUtils';
import { formatINR } from '../utils/formatUtils';
import {
  Button,
  Badge,
  Skeleton,
  EmptyState,
  ErrorBanner,
} from '../components/ui';
import './GuestAvailabilityPage.css';

export function GuestAvailabilityPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  // Search state from query parameters or defaults
  const checkIn = searchParams.get('checkIn') || BOOKING_WINDOW.DEFAULT_CHECK_IN;
  const checkOut = searchParams.get('checkOut') || BOOKING_WINDOW.DEFAULT_CHECK_OUT;
  const rooms = Number(searchParams.get('rooms')) || 1;
  const adults = Number(searchParams.get('adults')) || 2;
  const children = Number(searchParams.get('children')) || 0;

  // Form controls
  const [formCheckIn, setFormCheckIn] = useState(checkIn);
  const [formCheckOut, setFormCheckOut] = useState(checkOut);
  const [formRooms, setFormRooms] = useState(rooms);
  const [formAdults, setFormAdults] = useState(adults);
  const [formChildren, setFormChildren] = useState(children);

  // API State
  const [data, setData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadAvailability = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const result = await fetchAvailability({
        checkIn,
        checkOut,
        rooms,
        adults,
        children,
      });
      setData(result);
    } catch (err) {
      setError(err.message || 'Unable to retrieve live estate availability.');
    } finally {
      setIsLoading(false);
    }
  }, [checkIn, checkOut, rooms, adults, children]);

  useEffect(() => {
    loadAvailability();
  }, [loadAvailability]);

  function handleSearchSubmit(e) {
    e.preventDefault();
    setSearchParams({
      checkIn: formCheckIn,
      checkOut: formCheckOut,
      rooms: String(formRooms),
      adults: String(formAdults),
      children: String(formChildren),
    });
  }

  const nights = calculateNights(checkIn, checkOut);
  const categories = data?.categories || [];
  const hasAvailableRooms = categories.some((c) => c.hasAllotment && !c.isBlackedOut);

  function getRoomImageProps(category) {
    if (category.code === 'VILLA') {
      return {
        src: '/images/villa-pool.webp',
        srcSet: '/images/villa-pool.webp 800w, /images/villa-pool-1600w.webp 1600w',
        sizes: '(max-width: 768px) 100vw, 600px',
        alt: `${category.name} featuring secluded plunge pool and estate panorama`,
      };
    }
    return {
      src: '/images/room-canopy.webp',
      srcSet: '/images/room-canopy.webp 800w, /images/room-canopy-1600w.webp 1600w',
      sizes: '(max-width: 768px) 100vw, 600px',
      alt: `${category.name} nestled in coffee estate canopy with private balcony`,
    };
  }

  function getRoomHighlights(category) {
    if (category.code === 'VILLA') {
      return 'Private heated plunge pool \u2022 Estate valley panorama \u2022 Butler pantry';
    }
    return 'Private forest canopy verandah \u2022 Handcrafted teak finishes \u2022 Rainforest shower';
  }

  return (
    <div className="availability-page">
      {/* 1. Atmospheric Estate Sanctuary Hero */}
      <section className="availability-hero">
        <div className="availability-hero__image-wrap">
          <img
            src="/images/hero-estate.webp"
            srcSet="/images/hero-estate-800w.webp 800w, /images/hero-estate.webp 1600w"
            sizes="(max-width: 768px) 100vw, 1200px"
            alt="Misty Mullayanagiri mountain foothills and Arabica coffee canopy in Chikkamagaluru"
            width="1600"
            height="900"
            loading="eager"
            className="availability-hero__img"
          />
          <div className="availability-hero__overlay" aria-hidden="true" />
        </div>
        <div className="availability-hero__content">
          <span className="availability-hero__kicker">
            Mullayanagiri, Karnataka &bull; Est. 1924
          </span>
          <h1 className="availability-hero__title">
            Sanctuary in the Coffee Hills
          </h1>
          <p className="availability-hero__subtitle">
            Secluded canopy cottages and private plunge pool villas nestled within centuries-old Arabica plantations in the Western Ghats.
          </p>
        </div>
      </section>

      {/* 2. Structured Stay Selection Card */}
      <div className="availability-container">
        <div className="search-card">
          <div className="search-card__header">
            <h2 className="search-card__title">Plan Your Estate Stay</h2>
          </div>
          <form className="search-form" onSubmit={handleSearchSubmit}>
            <div className="search-form__field">
              <label htmlFor="search-check-in" className="search-form__label">
                Check-in Date
              </label>
              <input
                id="search-check-in"
                type="date"
                min={BOOKING_WINDOW.MIN_DATE}
                max={BOOKING_WINDOW.MAX_DATE}
                value={formCheckIn}
                onChange={(e) => setFormCheckIn(e.target.value)}
                required
                className="search-form__input"
              />
            </div>

            <div className="search-form__field">
              <label htmlFor="search-check-out" className="search-form__label">
                Check-out Date
              </label>
              <input
                id="search-check-out"
                type="date"
                min={formCheckIn || BOOKING_WINDOW.MIN_DATE}
                max={BOOKING_WINDOW.MAX_DATE}
                value={formCheckOut}
                onChange={(e) => setFormCheckOut(e.target.value)}
                required
                className="search-form__input"
              />
            </div>

            <div className="search-form__field">
              <label htmlFor="search-rooms" className="search-form__label">
                Rooms
              </label>
              <select
                id="search-rooms"
                value={formRooms}
                onChange={(e) => setFormRooms(Number(e.target.value))}
                className="search-form__select"
              >
                <option value={1}>1 Suite / Villa</option>
                <option value={2}>2 Suites / Villas</option>
                <option value={3}>3 Suites / Villas</option>
              </select>
            </div>

            <div className="search-form__field">
              <label htmlFor="search-adults" className="search-form__label">
                Adults
              </label>
              <select
                id="search-adults"
                value={formAdults}
                onChange={(e) => setFormAdults(Number(e.target.value))}
                className="search-form__select"
              >
                <option value={1}>1 Adult</option>
                <option value={2}>2 Adults</option>
                <option value={3}>3 Adults</option>
                <option value={4}>4 Adults</option>
              </select>
            </div>

            <div className="search-form__field">
              <label htmlFor="search-children" className="search-form__label">
                Children
              </label>
              <select
                id="search-children"
                value={formChildren}
                onChange={(e) => setFormChildren(Number(e.target.value))}
                className="search-form__select"
              >
                <option value={0}>0 Children</option>
                <option value={1}>1 Child</option>
                <option value={2}>2 Children</option>
              </select>
            </div>

            <Button type="submit" variant="primary" className="search-form__btn">
              Explore availability
            </Button>
          </form>
        </div>
      </div>

      {/* 3. Available Accommodations Section */}
      <section className="availability-container results-section" aria-labelledby="results-title">
        <div className="results-header">
          <h2 id="results-title" className="results-header__title">
            Estate Accommodations
          </h2>
          <span className="results-header__meta">
            {formatDisplayDate(checkIn)} &ndash; {formatDisplayDate(checkOut)} ({nights} {nights === 1 ? 'night' : 'nights'}) &bull; {rooms} {rooms === 1 ? 'Room' : 'Rooms'}
          </span>
        </div>

        {/* Loading Skeletons */}
        {isLoading && (
          <div className="rooms-grid" aria-label="Loading available accommodations" aria-busy="true">
            {[1, 2].map((idx) => (
              <div key={idx} className="room-skeleton-card">
                <Skeleton variant="rectangular" className="room-skeleton-card__img" />
                <div className="room-skeleton-card__body">
                  <Skeleton variant="text" width="60%" height="1.5rem" />
                  <Skeleton variant="text" width="40%" height="1rem" />
                  <Skeleton variant="text" width="90%" height="1rem" />
                  <Skeleton variant="rectangular" width="100%" height="44px" style={{ marginTop: 'var(--space-3)' }} />
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Error State */}
        {!isLoading && error && (
          <ErrorBanner
            variant="error"
            title="Unable to refresh live availability"
            message={error}
            actionLabel="Retry"
            onAction={loadAvailability}
          />
        )}

        {/* Empty State */}
        {!isLoading && !error && (!hasAvailableRooms || categories.length === 0) && (
          <EmptyState
            title="Our online allotment for these dates is fully reserved"
            description="Our online allotment for these dates is fully reserved (or blacked out for a private event). Send a direct enquiry below and our team will check house availability within 30 minutes."
            action={
              <Button
                variant="primary"
                onClick={() => navigate('/enquire')}
              >
                Send direct enquiry
              </Button>
            }
          />
        )}

        {/* Room Cards Showcase */}
        {!isLoading && !error && hasAvailableRooms && (
          <div className="rooms-grid">
            {categories.map((cat) => {
              const detailUrl = `/rooms/${cat.code}?checkIn=${encodeURIComponent(checkIn)}&checkOut=${encodeURIComponent(checkOut)}&rooms=${rooms}&adults=${adults}&children=${children}`;
              const imgMeta = getRoomImageProps(cat);

              return (
                <article key={cat.id || cat.code} className="room-card">
                  <div className="room-card__image-wrap">
                    <img
                      src={imgMeta.src}
                      srcSet={imgMeta.srcSet}
                      sizes={imgMeta.sizes}
                      alt={imgMeta.alt}
                      width="800"
                      height="500"
                      loading="lazy"
                      className="room-card__img"
                    />
                    <div className="room-card__badge-overlay">
                      {cat.isBlackedOut ? (
                        <Badge variant="danger">
                          Blackout: {cat.blackoutReason || 'Reserved'}
                        </Badge>
                      ) : cat.remainingAllotment <= 3 && cat.remainingAllotment > 0 ? (
                        <Badge variant="warning">
                          Only {cat.remainingAllotment} left on website
                        </Badge>
                      ) : (
                        <Badge variant="success">
                          {cat.remainingAllotment} units available online
                        </Badge>
                      )}
                    </div>
                  </div>

                  <div className="room-card__body">
                    <h3 className="room-card__title">{cat.name}</h3>
                    <p className="room-card__specs">
                      Up to {cat.maxAdults} adults, {cat.maxChildren} children &bull; {cat.totalPhysicalKeys} physical keys
                    </p>
                    <p className="room-card__desc">{cat.baseDescription}</p>

                    <div className="room-card__highlights">
                      {getRoomHighlights(cat)}
                    </div>

                    <div className="room-card__footer">
                      <div className="room-card__pricing">
                        <span className="room-card__price-label">Starting tariff</span>
                        <span className="room-card__price-value">
                          {formatINR(cat.startingRatePerNight)}
                          <span style={{ fontSize: 'var(--text-small)', fontWeight: 'normal', color: 'var(--color-text-muted)' }}>
                            {' '}/ night
                          </span>
                        </span>
                        <span className="room-card__tax-note">
                          Taxes shown are indicative in this test build
                        </span>
                      </div>

                      <Button
                        variant="primary"
                        onClick={() => navigate(detailUrl)}
                      >
                        View details &amp; rates
                      </Button>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}

export default GuestAvailabilityPage;
