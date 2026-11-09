import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { fetchAvailability } from '../api/roomsApi';
import {
  BOOKING_WINDOW,
  calculateNights,
  formatCompactDate,
  formatLocalDate,
  parseLocalDate,
  getNextDay,
  isValidStayDateRange,
} from '../utils/dateUtils';
import { formatINR } from '../utils/formatUtils';
import { Skeleton, ErrorBanner } from '../components/ui';
import {
  BedIcon,
  CoffeeLeafIcon,
  MountainIcon,
  SparklesIcon,
  KeyIcon,
  UsersIcon,
  CalendarIcon,
} from '../components/common/Icons';
import './GuestAvailabilityPage.css';

function UserIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  );
}

function EditIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <line x1="18" y1="6" x2="6" y2="18" />
      <line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}

export function GuestAvailabilityPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  // Only use a query date pair after validating it; malformed or stale URLs return to the date-first state.
  const queryCheckIn = searchParams.get('checkIn') || '';
  const queryCheckOut = searchParams.get('checkOut') || '';
  const datesSelected = isValidStayDateRange(queryCheckIn, queryCheckOut);
  const checkIn = datesSelected ? queryCheckIn : '';
  const checkOut = datesSelected ? queryCheckOut : '';
  const rooms = Number(searchParams.get('rooms')) || 1;
  const adults = Number(searchParams.get('adults')) || 2;
  const children = Number(searchParams.get('children')) || 0;

  // Search modifier state
  const [isModifying, setIsModifying] = useState(false);
  const [formCheckIn, setFormCheckIn] = useState(checkIn);
  const [formCheckOut, setFormCheckOut] = useState(checkOut);
  const [formRooms, setFormRooms] = useState(rooms);
  const [formAdults, setFormAdults] = useState(adults);
  const [formChildren, setFormChildren] = useState(children);
  const [formError, setFormError] = useState('');

  // Keep form inputs synced when query params change externally
  useEffect(() => {
    setFormCheckIn(checkIn);
    setFormCheckOut(checkOut);
    setFormRooms(rooms);
    setFormAdults(adults);
    setFormChildren(children);
  }, [checkIn, checkOut, rooms, adults, children]);

  // API State
  const [data, setData] = useState(null);
  // Start in non-loading state when no dates are present; will update on first fetch
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const todayStr = useMemo(() => formatLocalDate(new Date()), []);
  const minCheckIn = todayStr > BOOKING_WINDOW.MIN_DATE ? todayStr : BOOKING_WINDOW.MIN_DATE;

  const loadAvailability = useCallback(async () => {
    // Do not silently fetch with absent dates — show the date-first prompt instead.
    if (!checkIn || !checkOut) {
      return;
    }
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
      setError(err.message || 'Unable to retrieve live estate availability. Please check your connection and tap Retry.');
    } finally {
      setIsLoading(false);
    }
  }, [checkIn, checkOut, rooms, adults, children]);

  useEffect(() => {
    loadAvailability();
  }, [loadAvailability]);

  function handleCheckInChange(val) {
    setFormCheckIn(val);
    setFormError('');
    if (formCheckOut && val && formCheckOut <= val) {
      setFormCheckOut(getNextDay(val));
    }
  }

  function handleCheckOutChange(val) {
    setFormCheckOut(val);
    setFormError('');
  }

  function handleSearchSubmit(e) {
    e.preventDefault();
    if (!formCheckIn) {
      setFormError('Please select an arrival date.');
      return;
    }
    if (!formCheckOut) {
      setFormError('Please select a departure date.');
      return;
    }
    if (formCheckIn < minCheckIn) {
      setFormError('Arrival date cannot be in the past.');
      return;
    }
    if (formCheckOut <= formCheckIn) {
      setFormError('Departure date must follow arrival date.');
      return;
    }
    if (formCheckOut > BOOKING_WINDOW.MAX_DATE) {
      setFormError(`Bookings are accepted up to ${BOOKING_WINDOW.MAX_DATE}.`);
      return;
    }

    setFormError('');
    setIsModifying(false);

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
  const availableCategories = useMemo(() => {
    return categories.filter((c) => c.hasAllotment && !c.isBlackedOut);
  }, [categories]);
  const hasAvailableRooms = availableCategories.length > 0;

  function getRoomImage(category) {
    if (category.code === 'VILLA') {
      return {
        src: '/images/villa-pool.webp',
        srcSet: '/images/villa-pool.webp 800w, /images/villa-pool-1600w.webp 1600w',
        sizes: '(max-width: 768px) 100vw, 420px',
        alt: `${category.name} private plunge pool overlooking coffee estate`,
      };
    }
    return {
      src: '/images/room-canopy.webp',
      srcSet: '/images/room-canopy.webp 800w, /images/room-canopy-1600w.webp 1600w',
      sizes: '(max-width: 768px) 100vw, 420px',
      alt: `${category.name} estate canopy room with private balcony`,
    };
  }

  function getRoomSpecs(category) {
    if (category.code === 'VILLA') {
      return {
        capacity: `Up to ${category.maxAdults} Adults`,
        view: 'Private Plunge Pool & Panorama',
        area: '650 sq ft',
      };
    }
    return {
      capacity: `Up to ${category.maxAdults} Adults`,
      view: 'Estate Canopy & Garden View',
      area: '330 sq ft',
    };
  }

  const focusSearchInput = () => {
    const el = document.getElementById('stay-edit-checkin') || document.querySelector('.stay-search-strip');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.focus?.();
      try { el.showPicker?.(); } catch (err) {}
    }
  };

  return (
    <div className="stay-page">
      {/* 1. Stay Hero Banner matching Panel 2 of Reference Mockup */}
      <section className="stay-hero">
        <div className="stay-hero__media">
          <img
            src="/images/coffee-villa.jpg"
            alt="Coffee-estate villa and lush veranda in Chikkamagaluru"
            className="stay-hero__img"
            loading="eager"
            fetchPriority="high"
            width="1440"
            height="420"
          />
          <div className="stay-hero__overlay" />
        </div>
        <div className="stay-hero__content">
          <span className="stay-hero__eyebrow">STAY</span>
          <h1 className="stay-hero__title">Rooms &amp; Villas at the Estate</h1>
          <p className="stay-hero__sub">
            Restful stays surrounded by coffee gardens, forest views and quiet luxury in Chikkamagaluru.
          </p>
        </div>
      </section>

      {/* 2. Overlapping Booking Search Card (Matches Panel 2) */}
      <section className="stay-search-strip" aria-label="Reservation Parameters">
        <div className="stay-search-strip__inner">
          {(datesSelected && !isModifying) ? (
            <div className="stay-search-summary">
              <div className="stay-search-summary__items">
                <div className="stay-search-summary__cell">
                  <span className="stay-search-summary__label">Check-in</span>
                  <span className="stay-search-summary__val">{formatCompactDate(checkIn)}</span>
                </div>
                <div className="stay-search-summary__divider" aria-hidden="true" />
                <div className="stay-search-summary__cell">
                  <span className="stay-search-summary__label">Check-out</span>
                  <span className="stay-search-summary__val">{formatCompactDate(checkOut)}</span>
                </div>
                <div className="stay-search-summary__divider" aria-hidden="true" />
                <div className="stay-search-summary__cell">
                  <span className="stay-search-summary__label">Guests</span>
                  <span className="stay-search-summary__val">
                    {adults} {adults === 1 ? 'Adult' : 'Adults'}
                    {children > 0 ? `, ${children} ${children === 1 ? 'Child' : 'Children'}` : ''}
                    {rooms > 1 ? ` • ${rooms} Rooms` : ''}
                  </span>
                </div>
              </div>
              <button
                type="button"
                className="stay-search-summary__modify-btn"
                onClick={() => setIsModifying(true)}
                aria-expanded={isModifying}
              >
                <EditIcon />
                <span>Modify Search</span>
              </button>
            </div>
          ) : (
            <form className="stay-search-edit" onSubmit={handleSearchSubmit} noValidate>
              <div className="stay-search-edit__header">
                <span className="stay-search-edit__title">
                  {!datesSelected ? 'Select Your Stay Dates & Guests' : 'Modify Your Stay Dates & Guests'}
                </span>
                {datesSelected && (
                  <button
                    type="button"
                    className="stay-search-edit__close-btn"
                    onClick={() => {
                      setFormCheckIn(checkIn);
                      setFormCheckOut(checkOut);
                      setFormRooms(rooms);
                      setFormAdults(adults);
                      setFormChildren(children);
                      setFormError('');
                      setIsModifying(false);
                    }}
                    aria-label="Close modification form"
                  >
                    <CloseIcon />
                  </button>
                )}
              </div>

              <div className="stay-search-edit__grid">
                <div className="stay-search-edit__field">
                  <label htmlFor="stay-edit-checkin" className="stay-search-edit__label">Check-in</label>
                  <input
                    id="stay-edit-checkin"
                    type="date"
                    min={minCheckIn}
                    max={BOOKING_WINDOW.MAX_DATE}
                    value={formCheckIn}
                    onChange={(e) => handleCheckInChange(e.target.value)}
                    required
                    className="stay-search-edit__input"
                  />
                </div>

                <div className="stay-search-edit__field">
                  <label htmlFor="stay-edit-checkout" className="stay-search-edit__label">Check-out</label>
                  <input
                    id="stay-edit-checkout"
                    type="date"
                    min={formCheckIn ? getNextDay(formCheckIn) : minCheckIn}
                    max={BOOKING_WINDOW.MAX_DATE}
                    value={formCheckOut}
                    onChange={(e) => handleCheckOutChange(e.target.value)}
                    required
                    className="stay-search-edit__input"
                  />
                </div>

                <div className="stay-search-edit__field">
                  <label htmlFor="stay-edit-rooms" className="stay-search-edit__label">Rooms</label>
                  <select
                    id="stay-edit-rooms"
                    value={formRooms}
                    onChange={(e) => setFormRooms(Number(e.target.value))}
                    className="stay-search-edit__select"
                  >
                    <option value={1}>1 Room</option>
                    <option value={2}>2 Rooms</option>
                    <option value={3}>3 Rooms</option>
                  </select>
                </div>

                <div className="stay-search-edit__field">
                  <label htmlFor="stay-edit-adults" className="stay-search-edit__label">Adults</label>
                  <select
                    id="stay-edit-adults"
                    value={formAdults}
                    onChange={(e) => setFormAdults(Number(e.target.value))}
                    className="stay-search-edit__select"
                  >
                    <option value={1}>1 Adult</option>
                    <option value={2}>2 Adults</option>
                    <option value={3}>3 Adults</option>
                    <option value={4}>4 Adults</option>
                  </select>
                </div>

                <div className="stay-search-edit__field">
                  <label htmlFor="stay-edit-children" className="stay-search-edit__label">Children</label>
                  <select
                    id="stay-edit-children"
                    value={formChildren}
                    onChange={(e) => setFormChildren(Number(e.target.value))}
                    className="stay-search-edit__select"
                  >
                    <option value={0}>0 Children</option>
                    <option value={1}>1 Child</option>
                    <option value={2}>2 Children</option>
                  </select>
                </div>

                <div className="stay-search-edit__actions">
                  <button type="submit" className="stay-search-edit__submit-btn">
                    {datesSelected ? 'Modify Search' : 'Check Availability'}
                  </button>
                  {datesSelected && (
                    <button
                      type="button"
                      className="stay-search-edit__cancel-btn"
                      onClick={() => {
                        setFormCheckIn(checkIn);
                        setFormCheckOut(checkOut);
                        setFormRooms(rooms);
                        setFormAdults(adults);
                        setFormChildren(children);
                        setFormError('');
                        setIsModifying(false);
                      }}
                    >
                      Cancel
                    </button>
                  )}
                </div>
              </div>

              {formError && (
                <div className="stay-search-edit__error" role="alert">
                  {formError}
                </div>
              )}
            </form>
          )}
        </div>
      </section>

      {/* 3. Explore Our Accommodation Discovery Section (Always visible, matches Panel 2) */}
      <section className="stay-discovery-section" id="explore-accommodation" aria-labelledby="stay-explore-title">
        <div className="stay-discovery-header">
          <h2 id="stay-explore-title" className="stay-discovery-title">Explore Our Accommodation</h2>
          <p className="stay-discovery-sub">
            Choose from thoughtfully designed rooms and private villas, perfect for a peaceful estate escape.
          </p>
        </div>

        <div className="stay-discovery-grid">
          {/* Card 1: Estate Rooms */}
          <article className="stay-discovery-card">
            <div className="stay-discovery-card__media">
              <img
                src="/images/estate-room.jpg"
                alt="Estate Room with teakwood canopy bed and garden views"
                className="stay-discovery-card__img"
                loading="lazy"
                decoding="async"
                width="600"
                height="375"
              />
            </div>
            <div className="stay-discovery-card__content">
              <h3 className="stay-discovery-card__title">Estate Rooms</h3>
              <p className="stay-discovery-card__desc">
                Comfortable, earthy rooms nestled within the coffee plantation canopy with serene garden views.
              </p>
              <div className="stay-discovery-card__pills">
                <span className="stay-meta-pill">
                  <KeyIcon size={16} color="#1E3A2F" />
                  64 Keys at the Estate
                </span>
                <span className="stay-meta-pill">
                  <UsersIcon size={16} color="#1E3A2F" />
                  Up to 2 Adults &amp; 2 Children
                </span>
              </div>
              <button
                type="button"
                className="stay-discovery-card__cta"
                onClick={focusSearchInput}
              >
                Explore Rooms &rarr;
              </button>
            </div>
          </article>

          {/* Card 2: Private Villas */}
          <article className="stay-discovery-card">
            <div className="stay-discovery-card__media">
              <img
                src="/images/coffee-villa.jpg"
                alt="Private Villa with plunge pool and panoramic hill views"
                className="stay-discovery-card__img"
                loading="lazy"
                decoding="async"
                width="600"
                height="375"
              />
            </div>
            <div className="stay-discovery-card__content">
              <h3 className="stay-discovery-card__title">Private Villas</h3>
              <p className="stay-discovery-card__desc">
                Spacious private villas with secluded sit-outs and plunge pools, ideal for families and small groups.
              </p>
              <div className="stay-discovery-card__pills">
                <span className="stay-meta-pill">
                  <KeyIcon size={16} color="#1E3A2F" />
                  12 Private Villas
                </span>
                <span className="stay-meta-pill">
                  <UsersIcon size={16} color="#1E3A2F" />
                  Up to 3 Adults &amp; 2 Children
                </span>
              </div>
              <button
                type="button"
                className="stay-discovery-card__cta"
                onClick={focusSearchInput}
              >
                Explore Villas &rarr;
              </button>
            </div>
          </article>
        </div>
      </section>

      {/* 4. Live Availability Results (Rendered When Dates Are Searched) */}
      {datesSelected && (
        <section className="stay-content" aria-labelledby="stay-results-heading">
          <div className="stay-content__inner">
            <div className="stay-heading-row">
              <h2 id="stay-results-heading" className="stay-heading-row__title">
                {isLoading
                  ? 'Checking live availability...'
                  : hasAvailableRooms
                  ? `${availableCategories.length} ${availableCategories.length === 1 ? 'accommodation type' : 'accommodation types'} available`
                  : 'No accommodation types available'}
              </h2>
              <span className="stay-heading-row__meta">
                {formatCompactDate(checkIn)} &ndash; {formatCompactDate(checkOut)} ({nights} {nights === 1 ? 'night' : 'nights'})
              </span>
            </div>

            {/* Loading Skeletons */}
            {isLoading && (
              <div className="stay-skeleton-list" aria-label="Loading available accommodations" aria-busy="true">
                {[1, 2].map((idx) => (
                  <div key={idx} className="stay-skeleton-card">
                    <Skeleton variant="rectangular" className="stay-skeleton-card__img" />
                    <div className="stay-skeleton-card__body">
                      <Skeleton variant="text" width="60%" height="2rem" />
                      <Skeleton variant="text" width="40%" height="1.2rem" style={{ marginTop: '0.5rem' }} />
                      <Skeleton variant="text" width="90%" height="1rem" style={{ marginTop: '1rem' }} />
                      <Skeleton variant="text" width="75%" height="1rem" style={{ marginTop: '0.4rem' }} />
                    </div>
                    <div className="stay-skeleton-card__pricing">
                      <Skeleton variant="text" width="80px" height="1.8rem" />
                      <Skeleton variant="text" width="100px" height="0.9rem" style={{ marginTop: '0.5rem' }} />
                      <Skeleton variant="rectangular" width="100%" height="44px" style={{ marginTop: '1.5rem' }} />
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Error Banner */}
            {!isLoading && error && (
              <div className="stay-error-wrapper">
                <ErrorBanner
                  variant="error"
                  title="Unable to refresh live availability"
                  message={error}
                  actionLabel="Retry Live Rates"
                  onAction={loadAvailability}
                />
              </div>
            )}

            {/* Empty State when Sold Out or Blacked Out */}
            {!isLoading && !error && data && !hasAvailableRooms && (
              <div className="stay-empty-state">
                <div className="stay-empty-state__card">
                  <div className="stay-empty-state__icon-wrap">
                    <CalendarIcon />
                  </div>
                  <h3 className="stay-empty-state__title">
                    All accommodations are reserved for these dates
                  </h3>
                  <p className="stay-empty-state__desc">
                    Our online allotment for {formatCompactDate(checkIn)} to {formatCompactDate(checkOut)} is fully reserved or held for a private event. Try adjusting your dates or speak directly with our estate concierge.
                  </p>
                  <div className="stay-empty-state__actions">
                    <button
                      type="button"
                      className="stay-empty-state__modify-btn"
                      onClick={() => setIsModifying(true)}
                    >
                      Modify Travel Dates
                    </button>
                    <button
                      type="button"
                      className="stay-empty-state__enquire-btn"
                      onClick={() => navigate('/enquire?type=room')}
                    >
                      Send Direct Enquiry
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Real Accommodation Results */}
            {!isLoading && !error && hasAvailableRooms && (
              <div className="stay-card-list">
                {categories.map((cat) => {
                  const img = getRoomImage(cat);
                  const specs = getRoomSpecs(cat);
                  const detailUrl = `/rooms/${cat.code}?checkIn=${encodeURIComponent(checkIn)}&checkOut=${encodeURIComponent(checkOut)}&rooms=${rooms}&adults=${adults}&children=${children}`;

                  const firstPlan = cat.ratePlans?.[0];
                  const gstPercent = firstPlan?.gstPercentageApplied ?? 18;
                  const totalWithGst = firstPlan?.totalPayableInr;

                  return (
                    <article key={cat.id || cat.code} className="stay-room-card">
                      {/* Left: Resort Photo */}
                      <div className="stay-room-card__media">
                        <img
                          src={img.src}
                          srcSet={img.srcSet}
                          sizes={img.sizes}
                          alt={img.alt}
                          width="800"
                          height="500"
                          loading="lazy"
                          className="stay-room-card__img"
                        />
                        <div className="stay-room-card__badge-wrap">
                          {cat.isBlackedOut ? (
                            <span className="stay-badge stay-badge--danger">
                              Unavailable
                            </span>
                          ) : cat.availableRooms <= 3 ? (
                            <span className="stay-badge stay-badge--warning">
                              Only {cat.availableRooms} {cat.availableRooms === 1 ? 'room' : 'rooms'} left
                            </span>
                          ) : (
                            <span className="stay-badge stay-badge--success">
                              Available
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Middle: Details */}
                      <div className="stay-room-card__body">
                        <div className="stay-room-card__header">
                          <h3 className="stay-room-card__title">{cat.name}</h3>
                          <p className="stay-room-card__subtitle">{cat.shortDescription}</p>
                        </div>

                        <div className="stay-room-card__meta">
                          <span className="stay-meta-item">
                            <UserIcon />
                            <span>{specs.capacity}</span>
                          </span>
                          <span className="stay-meta-item">
                            <span>{specs.area}</span>
                          </span>
                          <span className="stay-meta-item">
                            <span>{specs.view}</span>
                          </span>
                        </div>

                        <div className="stay-room-card__amenities">
                          {(cat.amenities || ['Forest View', 'Sit-out Veranda', 'Plantation Walk Included', 'Artisanal Coffee Bar']).map((amenity, idx) => (
                            <span key={idx} className="stay-amenity-tag">
                              {amenity}
                            </span>
                          ))}
                        </div>
                      </div>

                      {/* Right: Pricing Box & CTA */}
                      <div className="stay-room-card__pricing">
                        <div className="stay-room-card__price-box">
                          <span className="stay-price-kicker">Starting from</span>
                          <div className="stay-price-amount">
                            <span className="stay-price-amount__value">
                              {formatINR(cat.startingRatePerNight)}
                            </span>
                            <span className="stay-price-amount__unit">/ night</span>
                          </div>
                          <div className="stay-price-taxes">
                            + {gstPercent}% GST as applicable
                          </div>
                          {totalWithGst && (
                            <div className="stay-price-total">
                              {formatINR(totalWithGst)} total for {nights} {nights === 1 ? 'night' : 'nights'} (incl. GST)
                            </div>
                          )}
                        </div>

                        <button
                          type="button"
                          className="stay-room-card__cta"
                          onClick={() => navigate(detailUrl)}
                        >
                          View Details
                        </button>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </div>
        </section>
      )}

      {/* 5. Bottom "Why Stay With Us?" Banner matching Panel 2 of Reference Mockup */}
      <section className="stay-why-section">
        <div className="stay-why-banner">
          <h2 className="stay-why-banner__title">Why Stay With Us?</h2>
          <div className="stay-why-banner__grid">
            <div className="stay-why-item">
              <div className="stay-why-item__icon">
                <BedIcon size={26} color="#FFFFFF" />
              </div>
              <span className="stay-why-item__label">Thoughtful Stays</span>
            </div>
            <div className="stay-why-item">
              <div className="stay-why-item__icon">
                <CoffeeLeafIcon size={26} />
              </div>
              <span className="stay-why-item__label">Coffee Estate Setting</span>
            </div>
            <div className="stay-why-item">
              <div className="stay-why-item__icon">
                <MountainIcon size={26} color="#FFFFFF" />
              </div>
              <span className="stay-why-item__label">Scenic Western Ghats</span>
            </div>
            <div className="stay-why-item">
              <div className="stay-why-item__icon">
                <SparklesIcon size={26} color="#FFFFFF" />
              </div>
              <span className="stay-why-item__label">Direct Booking Value</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

export default GuestAvailabilityPage;
