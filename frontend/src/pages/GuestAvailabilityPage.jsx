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
  const rawRooms = searchParams.get('rooms');
  const isGroupQuery = rawRooms === 'group' || Number(rawRooms) >= 6;
  const rooms = isGroupQuery ? 6 : (Number(rawRooms) || 1);
  const adults = Number(searchParams.get('adults')) || (isGroupQuery ? 12 : 2);
  const children = Number(searchParams.get('children')) || 0;

  // Search modifier state
  const [isModifying, setIsModifying] = useState(false);
  const [formCheckIn, setFormCheckIn] = useState(checkIn);
  const [formCheckOut, setFormCheckOut] = useState(checkOut);
  const [formRooms, setFormRooms] = useState(isGroupQuery ? 'group' : rooms);
  const [formAdults, setFormAdults] = useState(adults);
  const [formChildren, setFormChildren] = useState(children);
  const [formError, setFormError] = useState('');
  const [partyAdjustmentNotice, setPartyAdjustmentNotice] = useState('');

  // Dynamic capacity rules:
  // 1–5 Rooms online: min adults = rooms, max adults = rooms * 3, max children = rooms * 2
  // 6+ Rooms (Group Stay): triggers concierge handoff
  const isGroupSelected = formRooms === 'group' || Number(formRooms) >= 6;
  const numericFormRooms = isGroupSelected ? 6 : Math.max(1, Number(formRooms) || 1);
  const minAdults = isGroupSelected ? 6 : numericFormRooms;
  const maxAdults = isGroupSelected ? 30 : numericFormRooms * 3;
  const maxChildren = isGroupSelected ? 12 : numericFormRooms * 2;

  const handleRoomsChange = (val) => {
    if (val === 'group') {
      setFormRooms('group');
      setPartyAdjustmentNotice('');
      if (formAdults < 6) {
        setFormAdults(12);
      }
      return;
    }
    const newRooms = Number(val);
    setFormRooms(newRooms);
    const minA = newRooms;
    const maxA = newRooms * 3;
    const maxC = newRooms * 2;

    let notice = '';
    setFormAdults((prev) => {
      if (prev < minA) {
        notice = `Adult count adjusted to ${minA} to meet the minimum for ${newRooms} room${newRooms > 1 ? 's' : ''}.`;
        return minA;
      }
      if (prev > maxA) {
        notice = `Adult count adjusted to ${maxA} to fit capacity for ${newRooms} room${newRooms > 1 ? 's' : ''}.`;
        return maxA;
      }
      return prev;
    });
    setPartyAdjustmentNotice(notice);

    setFormChildren((prev) => {
      if (prev > maxC) return maxC;
      return prev;
    });
  };

  // Keep form inputs synced when query params change externally
  useEffect(() => {
    setFormCheckIn(checkIn);
    setFormCheckOut(checkOut);
    setFormRooms(isGroupQuery ? 'group' : rooms);
    setFormAdults(adults);
    setFormChildren(children);
  }, [checkIn, checkOut, rooms, adults, children, isGroupQuery]);

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
    if (formRooms === 'group' || Number(formRooms) >= 6) {
      const qCheckIn = formCheckIn || '';
      const qCheckOut = formCheckOut || '';
      const totalGuests = formAdults + formChildren;
      navigate(`/enquire?type=room&group=1${qCheckIn ? `&checkIn=${encodeURIComponent(qCheckIn)}` : ''}${qCheckOut ? `&checkOut=${encodeURIComponent(qCheckOut)}` : ''}&guests=${totalGuests}`);
      return;
    }

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

  const estateRoomCat = useMemo(() => {
    return categories.find((c) => c.code === 'ROOM');
  }, [categories]);

  const villaCat = useMemo(() => {
    return categories.find((c) => c.code === 'VILLA');
  }, [categories]);

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
    setIsModifying(true);
    setTimeout(() => {
      const el = document.getElementById('stay-edit-checkin') || document.querySelector('.stay-search-strip');
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        el.focus?.();
        try { el.showPicker?.(); } catch (err) {}
      }
    }, 60);
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
                    onChange={(e) => handleRoomsChange(e.target.value === 'group' ? 'group' : Number(e.target.value))}
                    className="stay-search-edit__select"
                  >
                    <option value={1}>1 Room</option>
                    <option value={2}>2 Rooms</option>
                    <option value={3}>3 Rooms</option>
                    <option value={4}>4 Rooms</option>
                    <option value={5}>5 Rooms</option>
                    <option value="group">6+ Rooms (Group Stay)</option>
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
                    {Array.from({ length: maxAdults - minAdults + 1 }, (_, i) => minAdults + i).map((count) => (
                      <option key={count} value={count}>
                        {count} {count === 1 ? 'Adult' : 'Adults'}
                      </option>
                    ))}
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
                    {Array.from({ length: maxChildren + 1 }, (_, i) => i).map((count) => (
                      <option key={count} value={count}>
                        {count} {count === 1 ? 'Child' : 'Children'}
                      </option>
                    ))}
                  </select>
                </div>

              {partyAdjustmentNotice && (
                <div className="stay-party-adjustment-notice" role="status" aria-live="polite">
                  <span>ℹ️</span> {partyAdjustmentNotice}
                </div>
              )}

              {isGroupSelected && (
                <div className="stay-group-banner" role="region" aria-label="Group Stay Concierge">
                  <div className="stay-group-banner__content">
                    <div className="stay-group-banner__icon" aria-hidden="true">
                      <UsersIcon size={24} />
                    </div>
                    <div className="stay-group-banner__text">
                      <h4 className="stay-group-banner__title">
                        Planning a Multi-Room Group Stay?
                      </h4>
                      <p className="stay-group-banner__desc">
                        Our reservations team will review your dates, group size, and room preferences. Sending this request does not reserve or hold rooms.
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    className="stay-group-banner__cta"
                    onClick={() => {
                      const qCheckIn = formCheckIn || checkIn || '';
                      const qCheckOut = formCheckOut || checkOut || '';
                      const totalGuests = (formAdults || 12) + (formChildren || 0);
                      navigate(`/enquire?type=room&group=1${qCheckIn ? `&checkIn=${encodeURIComponent(qCheckIn)}` : ''}${qCheckOut ? `&checkOut=${encodeURIComponent(qCheckOut)}` : ''}&guests=${totalGuests}`);
                    }}
                  >
                    Request a group-stay proposal &rarr;
                  </button>
                </div>
              )}

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

      {/* 3. Live Availability Results (Rendered When Dates Are Searched) */}
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

            {(isGroupQuery || rooms >= 6 || (adults + children) >= 12) && (
              <div className="stay-group-banner stay-group-banner--standalone" role="region" aria-label="Group Stay Concierge">
                <div className="stay-group-banner__content">
                  <div className="stay-group-banner__icon" aria-hidden="true">
                    <UsersIcon size={24} />
                  </div>
                  <div className="stay-group-banner__text">
                    <h3 className="stay-group-banner__title">
                      Planning a Multi-Room Group Stay?
                    </h3>
                    <p className="stay-group-banner__desc">
                      Our reservations team will review your dates, group size, and room preferences. Sending this request does not reserve or hold rooms.
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  className="stay-group-banner__cta"
                  onClick={() => {
                    navigate(`/enquire?type=room&group=1${checkIn ? `&checkIn=${encodeURIComponent(checkIn)}` : ''}${checkOut ? `&checkOut=${encodeURIComponent(checkOut)}` : ''}&guests=${adults + children}`);
                  }}
                >
                  Request a group-stay proposal &rarr;
                </button>
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
                  const availableCount = cat.remainingAllotment ?? cat.availableRooms ?? 0;
                  const maxAllowedAdults = cat.code === 'ROOM' ? rooms * 2 : rooms * 3;
                  const maxAllowedChildren = rooms * 2;
                  const exceedsCategoryCapacity = adults > maxAllowedAdults || children > maxAllowedChildren;
                  const insufficientAllotment = availableCount < rooms;
                  const isLargeGroupStay = rooms >= 6 || (adults + children) >= 12;

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
                              No availability
                            </span>
                          ) : !cat.hasAllotment || availableCount <= 0 ? (
                            <span className="stay-badge stay-badge--danger">
                              Sold out
                            </span>
                          ) : availableCount <= 3 ? (
                            <span className="stay-badge stay-badge--warning">
                              Only {availableCount} {availableCount === 1 ? 'room' : 'rooms'} left
                            </span>
                          ) : (
                            <span className="stay-badge stay-badge--success">
                              {availableCount} available
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Middle: Details */}
                      <div className="stay-room-card__body">
                        <div className="stay-room-card__header">
                          <div className="stay-room-card__title-row">
                            <h3 className="stay-room-card__title">{cat.name}</h3>
                            <span
                              className={`stay-avail-pill stay-avail-pill--${
                                cat.isBlackedOut || !cat.hasAllotment || availableCount <= 0
                                  ? 'soldout'
                                  : availableCount <= 3
                                  ? 'low'
                                  : 'available'
                              }`}
                            >
                              {cat.isBlackedOut
                                ? 'No availability'
                                : !cat.hasAllotment || availableCount <= 0
                                ? 'Sold out'
                                : `${availableCount} available`}
                            </span>
                          </div>
                          <p className="stay-room-card__subtitle">{cat.shortDescription || cat.baseDescription}</p>
                          {exceedsCategoryCapacity && (
                            <div className="stay-capacity-warning" role="note">
                              Selected party ({adults} Adults{children > 0 ? `, ${children} Children` : ''}) exceeds total capacity for {rooms} {cat.name} (max {maxAllowedAdults} Adults &amp; {maxAllowedChildren} Children). Consider {cat.code === 'ROOM' ? 'Private Villas or ' : ''}additional rooms.
                            </div>
                          )}
                        </div>

                        <div className="stay-room-card__meta">
                          <span className="stay-meta-item">
                            <UserIcon />
                            <span>{specs.capacity}</span>
                          </span>
                          <span className="stay-meta-divider" aria-hidden="true">•</span>
                          <span className="stay-meta-item">
                            <span>{specs.area}</span>
                          </span>
                          <span className="stay-meta-divider" aria-hidden="true">•</span>
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
                              {formatINR(totalWithGst)} total for {rooms > 1 ? `${rooms} Rooms × ${nights} ${nights === 1 ? 'Night' : 'Nights'}` : `${nights} ${nights === 1 ? 'night' : 'nights'}`} (incl. GST)
                            </div>
                          )}
                        </div>

                        {isLargeGroupStay ? (
                          <button
                            type="button"
                            className="stay-room-card__cta stay-room-card__cta--group"
                            onClick={() => navigate(`/enquire?type=room&group=1${checkIn ? `&checkIn=${encodeURIComponent(checkIn)}` : ''}${checkOut ? `&checkOut=${encodeURIComponent(checkOut)}` : ''}&guests=${adults + children}`)}
                          >
                            Request a group-stay proposal &rarr;
                          </button>
                        ) : (
                          <button
                            type="button"
                            className="stay-room-card__cta"
                            onClick={() => navigate(detailUrl)}
                          >
                            View Details &rarr;
                          </button>
                        )}
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </div>
        </section>
      )}



      {/* 3-Column Highlights Strip (Matching Weddings & Offsites) */}
      {!datesSelected && (
        <section className="stay-highlights" aria-label="Estate Accommodation Highlights">
          <div className="stay-highlights__inner">
            <div className="stay-highlight-card">
              <div className="stay-highlight-card__icon">
                <BedIcon size={26} color="#1E3A2F" />
              </div>
              <h3 className="stay-highlight-card__title">64 Estate Rooms</h3>
              <p className="stay-highlight-card__desc">
                Canopy-level rooms with private verandas, teakwood furnishings, and tranquil forest views.
              </p>
            </div>

            <div className="stay-highlight-card">
              <div className="stay-highlight-card__icon">
                <MountainIcon size={26} color="#1E3A2F" />
              </div>
              <h3 className="stay-highlight-card__title">12 Private Villas</h3>
              <p className="stay-highlight-card__desc">
                Hillside estate villas featuring private plunge pools, sun decks, and Western Ghats vistas.
              </p>
            </div>

            <div className="stay-highlight-card">
              <div className="stay-highlight-card__icon">
                <CoffeeLeafIcon size={26} color="#1E3A2F" />
              </div>
              <h3 className="stay-highlight-card__title">Transparent Direct Booking</h3>
              <p className="stay-highlight-card__desc">
                Live website allotment across EP, CP, and MAP rate plans with automatic GST slab calculation.
              </p>
            </div>
          </div>
        </section>
      )}

{/* 4. Explore Our Accommodation Discovery Section (Matches Panel 2) */}
      <section className="stay-discovery-section" id="explore-accommodation" aria-labelledby="stay-explore-title">
        <div className="stay-discovery-header">
          <span className="stay-discovery-eyebrow">OUR KEYS</span>
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
                  {datesSelected && estateRoomCat != null
                    ? estateRoomCat.isBlackedOut
                      ? 'No availability for dates (64 total keys)'
                      : (estateRoomCat.remainingAllotment ?? 0) <= 0
                      ? 'Sold out for dates (64 total keys)'
                      : `${estateRoomCat.remainingAllotment} available for dates (64 total keys)`
                    : '64 Keys at the Estate'}
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
                  {datesSelected && villaCat != null
                    ? villaCat.isBlackedOut
                      ? 'No availability for dates (12 total keys)'
                      : (villaCat.remainingAllotment ?? 0) <= 0
                      ? 'Sold out for dates (12 total keys)'
                      : `${villaCat.remainingAllotment} available for dates (12 total keys)`
                    : '12 Private Villas'}
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
