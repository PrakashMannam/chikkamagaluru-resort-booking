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
} from '../utils/dateUtils';
import { formatINR } from '../utils/formatUtils';
import { Skeleton, ErrorBanner } from '../components/ui';
import './GuestAvailabilityPage.css';

function CalendarIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
  );
}

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

function SparklesIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3Z" />
    </svg>
  );
}

export function GuestAvailabilityPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  // Search parameters from URL — no silent defaults; null means guest has not selected dates yet
  const checkIn = searchParams.get('checkIn') || null;
  const checkOut = searchParams.get('checkOut') || null;
  const datesSelected = Boolean(checkIn && checkOut);
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
      setIsLoading(false);
      setData(null);
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

  return (
    <div className="stay-page">
      {/* 1. Compact Top Search Strip (Storyboard Screen 2) */}
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
                    <option value={1}>1 Suite / Villa</option>
                    <option value={2}>2 Suites / Villas</option>
                    <option value={3}>3 Suites / Villas</option>
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
                    Search availability
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

      {/* 2. Main Accommodations List Section */}
      <section className="stay-content" aria-labelledby="stay-results-heading">
        <div className="stay-content__inner">
          {/* Section Heading matching Screen 2 */}
          <div className="stay-heading-row">
            <h1 id="stay-results-heading" className="stay-heading-row__title">
              {!datesSelected
                ? 'Select your stay dates to view availability'
                : isLoading
                ? 'Checking live availability...'
                : hasAvailableRooms
                ? `${availableCategories.length} ${availableCategories.length === 1 ? 'accommodation type' : 'accommodation types'} available`
                : 'No accommodation types available'}
            </h1>
            {datesSelected && (
            <span className="stay-heading-row__meta">
              {formatCompactDate(checkIn)} &ndash; {formatCompactDate(checkOut)} ({nights} {nights === 1 ? 'night' : 'nights'})
            </span>
            )}
          </div>

          {/* Date Selection Prompt: shown when guest arrives with no query parameters */}
          {!datesSelected && !isLoading && (
            <div className="stay-date-prompt" role="status" aria-live="polite">
              <div className="stay-date-prompt__icon" aria-hidden="true">📅</div>
              <h2 className="stay-date-prompt__heading">Select your stay dates</h2>
              <p className="stay-date-prompt__body">
                Choose your check-in and check-out dates above and tap{' '}
                <strong>Search Availability</strong> to see live room availability and pricing.
              </p>
            </div>
          )}

          {/* Loading Skeletons */}
          {datesSelected && isLoading && (
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
          {datesSelected && !isLoading && error && (
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

          {/* Empty State */}
          {!isLoading && !error && (!hasAvailableRooms || categories.length === 0) && (
            <div className="stay-empty-state">
              <div className="stay-empty-state__card">
                <div className="stay-empty-state__icon-wrap">
                  <CalendarIcon />
                </div>
                <h2 className="stay-empty-state__title">
                  All accommodations are reserved for these dates
                </h2>
                <p className="stay-empty-state__desc">
                  Our online allotment for {formatCompactDate(checkIn)} to {formatCompactDate(checkOut)} is fully booked or held for a private event. Try adjusting your dates or speak directly with our estate concierge.
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
                    onClick={() => navigate('/enquire')}
                  >
                    Send Direct Enquiry
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Real Accommodation Results (Screen 2 Layout) */}
          {!isLoading && !error && hasAvailableRooms && (
            <div className="stay-card-list">
              {categories.map((cat) => {
                const img = getRoomImage(cat);
                const specs = getRoomSpecs(cat);
                const detailUrl = `/rooms/${cat.code}?checkIn=${encodeURIComponent(checkIn)}&checkOut=${encodeURIComponent(checkOut)}&rooms=${rooms}&adults=${adults}&children=${children}`;

                // Backend dynamic pricing & GST data
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
                      {/* Availability status badge */}
                      <div className="stay-room-card__badge-wrap">
                        {cat.isBlackedOut ? (
                          <span className="stay-badge stay-badge--danger">
                            Blackout: {cat.blackoutReason || 'Reserved'}
                          </span>
                        ) : cat.remainingAllotment <= 3 && cat.remainingAllotment > 0 ? (
                          <span className="stay-badge stay-badge--warning">
                            Only {cat.remainingAllotment} left online
                          </span>
                        ) : (
                          <span className="stay-badge stay-badge--success">
                            {cat.remainingAllotment} available
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Middle: Content & Specs */}
                    <div className="stay-room-card__content">
                      <h2 className="stay-room-card__title">
                        {cat.name}
                      </h2>

                      {/* Specs Row */}
                      <div className="stay-room-card__specs">
                        <span className="stay-spec-item">
                          <UserIcon />
                          <span>{specs.capacity}</span>
                        </span>
                        <span className="stay-spec-divider">&bull;</span>
                        <span className="stay-spec-item">
                          <span>{specs.view}</span>
                        </span>
                        <span className="stay-spec-divider">&bull;</span>
                        <span className="stay-spec-item">
                          <span>{specs.area}</span>
                        </span>
                      </div>

                      {/* Description */}
                      <p className="stay-room-card__desc">
                        {cat.baseDescription}
                      </p>

                      {/* Rate Plans List */}
                      {cat.ratePlans && cat.ratePlans.length > 0 && (
                        <div className="stay-room-card__plans">
                          <span className="stay-plans-title">Available Meal Plans:</span>
                          <div className="stay-plans-tags">
                            {cat.ratePlans.map((plan) => (
                              <span key={plan.code} className="stay-plan-tag" title={plan.name}>
                                <SparklesIcon />
                                <span>{plan.code}: {plan.name.replace(/\(.*?\)/g, '').trim()}</span>
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Right: Pricing & CTA Column */}
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
    </div>
  );
}

export default GuestAvailabilityPage;
