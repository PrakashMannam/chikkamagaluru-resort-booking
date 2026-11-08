import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { fetchAvailability, fetchPricingQuote, createBooking } from '../api/roomsApi';
import {
  DEFAULT_CHECK_IN,
  DEFAULT_CHECK_OUT,
  calculateNights,
  formatDisplayDate,
} from '../utils/dateUtils';
import { formatINR } from '../utils/formatUtils';
import {
  Button,
  Input,
  Badge,
  Skeleton,
  EmptyState,
  ErrorBanner,
  StepIndicator,
} from '../components/ui';
import './GuestCheckoutPage.css';

function generateUUID() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

export function GuestCheckoutPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // Query parameters
  const checkIn = searchParams.get('checkIn') || DEFAULT_CHECK_IN;
  const checkOut = searchParams.get('checkOut') || DEFAULT_CHECK_OUT;
  const rooms = Number(searchParams.get('rooms')) || 1;
  const adults = Number(searchParams.get('adults')) || 2;
  const children = Number(searchParams.get('children')) || 0;
  const categoryCode = searchParams.get('category') || 'ROOM';
  const planCode = searchParams.get('plan') || 'EP';

  // Availability & pricing data
  const [availabilityData, setAvailabilityData] = useState(null);
  const [pricingQuote, setPricingQuote] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // Guest details form state
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [dpdpConsent, setDpdpConsent] = useState(false);

  // Promo code state
  const [promoInput, setPromoInput] = useState('');
  const [appliedPromo, setAppliedPromo] = useState('');
  const [isApplyingPromo, setIsApplyingPromo] = useState(false);

  // Field validation errors
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Idempotency key per booking attempt
  const idempotencyKeyRef = useRef(generateUUID());

  // Refresh idempotency key when booking search parameters change
  useEffect(() => {
    idempotencyKeyRef.current = generateUUID();
  }, [categoryCode, planCode, checkIn, checkOut, rooms, adults, children]);

  // Regenerate idempotency key whenever any booking field changes
  function handleFieldChange(setter, fieldName) {
    return (e) => {
      setter(e.target.value);
      idempotencyKeyRef.current = generateUUID();
      if (fieldErrors[fieldName]) {
        setFieldErrors((prev) => ({ ...prev, [fieldName]: null }));
      }
      setSubmitError(null);
    };
  }

  useEffect(() => {
    if (!checkIn || !checkOut) {
      navigate('/stay', { replace: true });
    }
  }, [checkIn, checkOut, navigate]);

  const loadData = useCallback(async () => {
    if (!checkIn || !checkOut) {
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setLoadError(null);
    try {
      const [data, quote] = await Promise.all([
        fetchAvailability({
          checkIn,
          checkOut,
          rooms,
          adults,
          children,
        }),
        fetchPricingQuote({
          categoryCode,
          planCode,
          checkIn,
          checkOut,
          rooms,
          adults,
          children,
        }),
      ]);
      setAvailabilityData(data);
      setPricingQuote(quote);
    } catch (err) {
      setLoadError(err.message || 'Unable to retrieve reservation details.');
    } finally {
      setIsLoading(false);
    }
  }, [checkIn, checkOut, rooms, adults, children, categoryCode, planCode]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  async function handleApplyPromo(e) {
    if (e) e.preventDefault();
    const cleanCode = promoInput.trim();
    if (!cleanCode) return;

    setIsApplyingPromo(true);
    setFieldErrors((prev) => ({ ...prev, promoCode: null }));
    try {
      const quote = await fetchPricingQuote({
        categoryCode,
        planCode,
        checkIn,
        checkOut,
        rooms,
        adults,
        children,
        promoCode: cleanCode,
      });
      setPricingQuote(quote);
      setAppliedPromo(quote.promoCode || cleanCode);
      idempotencyKeyRef.current = generateUUID();
    } catch (err) {
      setFieldErrors((prev) => ({
        ...prev,
        promoCode: err.message || 'Invalid or inactive promotional code.',
      }));
    } finally {
      setIsApplyingPromo(false);
    }
  }

  async function handleRemovePromo() {
    setPromoInput('');
    setAppliedPromo('');
    setFieldErrors((prev) => ({ ...prev, promoCode: null }));
    setIsApplyingPromo(true);
    try {
      const quote = await fetchPricingQuote({
        categoryCode,
        planCode,
        checkIn,
        checkOut,
        rooms,
        adults,
        children,
      });
      setPricingQuote(quote);
      idempotencyKeyRef.current = generateUUID();
    } catch (err) {
      setLoadError(err.message || 'Unable to update pricing quote.');
    } finally {
      setIsApplyingPromo(false);
    }
  }

  const nights = calculateNights(checkIn, checkOut);

  // Match category and plan from server availability response
  const category = availabilityData?.categories?.find(
    (c) => c.code?.toLowerCase() === categoryCode.toLowerCase()
  );
  const selectedPlan = category?.ratePlans?.find(
    (p) => p.code?.toLowerCase() === planCode.toLowerCase()
  );

  const backUrl = `/rooms/${categoryCode}?checkIn=${encodeURIComponent(checkIn)}&checkOut=${encodeURIComponent(checkOut)}&rooms=${rooms}&adults=${adults}&children=${children}`;

  function validateForm() {
    const errors = {};
    if (!fullName.trim()) {
      errors.fullName = 'Please enter your full name.';
    }
    if (!email.trim()) {
      errors.email = 'Please enter your email address.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errors.email = 'Please enter a valid email address.';
    }
    const cleanedPhone = phone.trim().replace(/[\s\-()]/g, '');
    let corePhone = cleanedPhone;
    if (corePhone.startsWith('+91')) corePhone = corePhone.slice(3);
    else if (corePhone.startsWith('91') && corePhone.length === 12) corePhone = corePhone.slice(2);
    else if (corePhone.startsWith('0') && corePhone.length === 11) corePhone = corePhone.slice(1);

    const isAllSameDigits = corePhone.length > 0 && /^(\d)\1+$/.test(corePhone);

    if (!phone.trim()) {
      errors.phone = 'Please enter your contact phone number.';
    } else if (!/^[6-9]\d{9}$/.test(corePhone) || isAllSameDigits) {
      errors.phone = 'Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.';
    }
    if (!dpdpConsent) {
      errors.dpdpConsent = 'Consent is mandatory under the Digital Personal Data Protection Act 2023.';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitError(null);

    if (!validateForm()) {
      return;
    }

    if (!category || !selectedPlan || !pricingQuote) {
      setSubmitError('The selected accommodation or dining plan is unavailable. Please return to room selection.');
      return;
    }

    setIsSubmitting(true);

    const bookingPayload = {
      roomCategoryId: category.id,
      ratePlanId: selectedPlan.id,
      checkInDate: checkIn,
      checkOutDate: checkOut,
      roomsCount: rooms,
      adults,
      children,
      guestName: fullName.trim(),
      guestEmail: email.trim(),
      guestPhone: (() => {
        const cleaned = phone.trim().replace(/[\s\-()]/g, '');
        let core = cleaned;
        if (core.startsWith('+91')) core = core.slice(3);
        else if (core.startsWith('91') && core.length === 12) core = core.slice(2);
        else if (core.startsWith('0') && core.length === 11) core = core.slice(1);
        return /^[6-9]\d{9}$/.test(core) ? core : phone.trim();
      })(),
      promoCode: appliedPromo || promoInput.trim() || undefined,
      dpdpConsentGiven: true,
    };

    try {
      const response = await createBooking(bookingPayload, idempotencyKeyRef.current);

      // Save token and expiry into sessionStorage under booking reference (never in URL)
      if (response.bookingRef && response.oneTimePaymentToken) {
        sessionStorage.setItem(
          `booking_${response.bookingRef}`,
          JSON.stringify({
            token: response.oneTimePaymentToken,
            expiresAt: response.expiresAt,
            createdAt: Date.now(),
            bookingRef: response.bookingRef,
            categoryName: response.roomCategoryName,
            planName: response.ratePlanName,
            totalPayableInr: response.totalPayableInr,
          })
        );
      }

      // Generate a fresh key for any subsequent booking attempt
      idempotencyKeyRef.current = generateUUID();

      // Navigate to payment step
      navigate(`/payment/${encodeURIComponent(response.bookingRef)}`);
    } catch (err) {
      if (err.status === 422 && (err.serverBody?.error === 'Invalid Promo Code' || err.message?.toLowerCase().includes('promo'))) {
        setFieldErrors((prev) => ({
          ...prev,
          promoCode: err.message,
        }));
      } else {
        setSubmitError(err.message || 'Unable to place reservation hold. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  const roomImageSrc = categoryCode === 'VILLA' ? '/images/villa-pool.webp' : '/images/room-canopy.webp';
  const roomImageSrcSet = categoryCode === 'VILLA'
    ? '/images/villa-pool.webp 800w, /images/villa-pool-1600w.webp 1600w'
    : '/images/room-canopy.webp 800w, /images/room-canopy-1600w.webp 1600w';

  return (
    <div className="checkout-page">
      {/* 1. Progress Step Indicator */}
      <StepIndicator currentStep={1} />

      {/* Navigation Breadcrumb */}
      <nav className="checkout__nav" aria-label="Breadcrumb">
        <Link to={backUrl} className="checkout__back-link">
          &larr; Back to accommodation details
        </Link>
      </nav>

      {/* Loading Skeletons */}
      {isLoading && (
        <div className="checkout__layout" aria-label="Loading checkout details" aria-busy="true">
          <div className="checkout-card">
            <Skeleton variant="text" width="50%" height="2rem" />
            <Skeleton variant="text" width="70%" height="1.25rem" style={{ marginTop: 'var(--space-2)' }} />
            <Skeleton variant="rectangular" width="100%" height="48px" style={{ marginTop: 'var(--space-4)' }} />
            <Skeleton variant="rectangular" width="100%" height="48px" style={{ marginTop: 'var(--space-3)' }} />
            <Skeleton variant="rectangular" width="100%" height="48px" style={{ marginTop: 'var(--space-3)' }} />
          </div>
          <div className="summary-card">
            <Skeleton variant="rectangular" width="100%" height="180px" />
            <Skeleton variant="text" width="60%" height="1.5rem" style={{ marginTop: 'var(--space-3)' }} />
            <Skeleton variant="rectangular" width="100%" height="120px" style={{ marginTop: 'var(--space-3)' }} />
          </div>
        </div>
      )}

      {/* Initial Fetch Error */}
      {!isLoading && loadError && (
        <ErrorBanner
          variant="error"
          title="Could not prepare checkout"
          message={loadError}
          actionLabel="Retry"
          onAction={loadData}
        />
      )}

      {/* Missing or Sold-out Category */}
      {!isLoading && !loadError && (!category || !selectedPlan || !pricingQuote) && (
        <EmptyState
          title="Accommodation or Plan Unavailable"
          description="The selected accommodation tier or dining plan is not currently available for these dates."
          action={
            <Button variant="primary" onClick={() => navigate('/stay')}>
              Browse available dates
            </Button>
          }
        />
      )}

      {/* Main Checkout Experience */}
      {!isLoading && !loadError && category && selectedPlan && pricingQuote && (
        <div className="checkout__layout">
          {/* Left Column: Guest Information & Form */}
          <main className="checkout-main">
            <div className="checkout-card">
              <header className="checkout-card__header">
                <span className="checkout-card__kicker">Step 1 of 3</span>
                <h1 className="checkout-card__title">Guest Details &amp; Consent</h1>
                <p className="checkout-card__desc">
                  Please provide primary guest details to secure your accommodation hold.
                </p>
              </header>

              {submitError && (
                <div style={{ marginBottom: 'var(--space-4)' }}>
                  <ErrorBanner
                    variant="error"
                    title="Reservation Hold Issue"
                    message={submitError}
                  />
                </div>
              )}

              <form className="checkout-form" onSubmit={handleSubmit} noValidate>
                <div className="checkout-form__row">
                  <Input
                    id="checkout-full-name"
                    label="Full Legal Name"
                    name="fullName"
                    value={fullName}
                    onChange={handleFieldChange(setFullName, 'fullName')}
                    placeholder="e.g. Ananya Sharma"
                    required
                    error={fieldErrors.fullName}
                    helperText="As printed on government identity documentation"
                  />
                </div>

                <div className="checkout-form__grid">
                  <Input
                    id="checkout-email"
                    label="Email Address"
                    name="email"
                    type="email"
                    value={email}
                    onChange={handleFieldChange(setEmail, 'email')}
                    placeholder="name@example.com"
                    required
                    error={fieldErrors.email}
                    helperText="Booking reference and confirmation will be sent here"
                  />

                  <Input
                    id="checkout-phone"
                    label="Mobile Phone"
                    name="phone"
                    type="tel"
                    value={phone}
                    onChange={handleFieldChange(setPhone, 'phone')}
                    placeholder="e.g. 9876543210"
                    required
                    error={fieldErrors.phone}
                    helperText="Primary contact for resort arrival concierge"
                  />
                </div>

                {/* Promotional Code */}
                <div className="checkout-form__promo-section">
                  <div className="checkout-form__promo-wrap">
                    <Input
                      id="checkout-promo"
                      label="Promotional Code"
                      name="promoCode"
                      value={promoInput}
                      onChange={(e) => {
                        setPromoInput(e.target.value);
                        if (fieldErrors.promoCode) {
                          setFieldErrors((prev) => ({ ...prev, promoCode: null }));
                        }
                      }}
                      placeholder="e.g. DIRECT10"
                      error={fieldErrors.promoCode}
                      helperText={
                        appliedPromo
                          ? `Promo code "${appliedPromo}" applied successfully!`
                          : "Discounts and eligible taxes are computed deterministically by the estate reservation server"
                      }
                    />
                    <div className="checkout-form__promo-actions">
                      {appliedPromo ? (
                        <Button
                          type="button"
                          variant="ghost"
                          onClick={handleRemovePromo}
                          disabled={isApplyingPromo}
                          className="checkout-form__promo-btn checkout-form__promo-btn--remove"
                        >
                          Remove
                        </Button>
                      ) : (
                        <Button
                          type="button"
                          variant="secondary"
                          onClick={handleApplyPromo}
                          disabled={isApplyingPromo || !promoInput.trim()}
                          className="checkout-form__promo-btn"
                        >
                          {isApplyingPromo ? 'Applying…' : 'Apply'}
                        </Button>
                      )}
                    </div>
                  </div>
                </div>

                {/* International Guest Notice */}
                <div className="checkout-notice">
                  <span className="checkout-notice__icon" aria-hidden="true">ℹ</span>
                  <p className="checkout-notice__text">
                    <strong>International Guests:</strong> In accordance with Bureau of Immigration guidelines, physical passport verification and Form C compliance will be completed at estate check-in.
                  </p>
                </div>

                {/* DPDP Act 2023 Mandatory Consent */}
                <div className="dpdp-consent">
                  <label className="dpdp-consent__label" htmlFor="dpdp-consent">
                    <input
                      id="dpdp-consent"
                      type="checkbox"
                      checked={dpdpConsent}
                      onChange={(e) => {
                        setDpdpConsent(e.target.checked);
                        if (fieldErrors.dpdpConsent) {
                          setFieldErrors((prev) => ({ ...prev, dpdpConsent: null }));
                        }
                      }}
                      className="dpdp-consent__checkbox"
                      required
                    />
                    <span className="dpdp-consent__text">
                      I consent to Chikkamagaluru Estate securely processing my contact information solely for this reservation hold and guest concierge communications in compliance with the Digital Personal Data Protection Act 2023.
                    </span>
                  </label>
                  {fieldErrors.dpdpConsent && (
                    <span className="dpdp-consent__error" role="alert">
                      {fieldErrors.dpdpConsent}
                    </span>
                  )}
                </div>

                {/* Submission CTA */}
                <div className="checkout-form__actions">
                  <Button
                    type="submit"
                    variant="primary"
                    loading={isSubmitting}
                    disabled={isSubmitting}
                    className="checkout-form__submit-btn"
                  >
                    {isSubmitting ? 'Securing 10-Minute Hold…' : 'Proceed to Payment'}
                  </Button>
                  <p className="checkout-form__guarantee-note">
                    Holding allotment for 10 minutes without charge. No real payment required in test build.
                  </p>
                </div>
              </form>
            </div>
          </main>

          {/* Right Column: Reservation & Pricing Summary */}
          <aside className="checkout-sidebar">
            <div className="summary-card">
              <div className="summary-card__media">
                <img
                  src={roomImageSrc}
                  srcSet={roomImageSrcSet}
                  sizes="(max-width: 768px) 100vw, 400px"
                  alt={`${category.name}`}
                  width="800"
                  height="500"
                  loading="eager"
                  className="summary-card__img"
                />
                <div className="summary-card__badge-tag">
                  <Badge variant="neutral">{category.code}</Badge>
                </div>
              </div>

              <div className="summary-card__body">
                <h2 className="summary-card__title">{category.name}</h2>
                <div className="summary-card__plan-tag">
                  <Badge variant="neutral">{selectedPlan.code}</Badge>
                  <span className="summary-card__plan-name">{selectedPlan.name}</span>
                </div>

                <div className="summary-card__stay-info">
                  <div className="summary-card__stay-row">
                    <span className="summary-card__stay-label">Dates</span>
                    <span className="summary-card__stay-val">
                      {formatDisplayDate(checkIn)} &ndash; {formatDisplayDate(checkOut)}
                    </span>
                  </div>
                  <div className="summary-card__stay-row">
                    <span className="summary-card__stay-label">Duration</span>
                    <span className="summary-card__stay-val">
                      {nights} {nights === 1 ? 'Night' : 'Nights'}
                    </span>
                  </div>
                  <div className="summary-card__stay-row">
                    <span className="summary-card__stay-label">Guests &amp; Keys</span>
                    <span className="summary-card__stay-val">
                      {adults} {adults === 1 ? 'Adult' : 'Adults'}{children > 0 ? `, ${children} Children` : ''} &bull; {rooms} {rooms === 1 ? 'Room' : 'Rooms'}
                    </span>
                  </div>
                </div>

                {/* Itemized Pricing from Authoritative Server Quote */}
                <div className="summary-card__pricing">
                  <h3 className="summary-card__pricing-heading">Tariff Summary</h3>

                  <div className="summary-card__price-line">
                    <span>Base Accommodations ({nights} {nights === 1 ? 'Night' : 'Nights'})</span>
                    <span>{formatINR(pricingQuote.baseAccommodationTotalInr)}</span>
                  </div>

                  {pricingQuote.ratePlanAddonTotalInr > 0 && (
                    <div className="summary-card__price-line">
                      <span>Dining Plan Addon ({pricingQuote.ratePlanCode || selectedPlan.code})</span>
                      <span>{formatINR(pricingQuote.ratePlanAddonTotalInr)}</span>
                    </div>
                  )}

                  <div className="summary-card__price-line summary-card__price-line--subtotal">
                    <span>Subtotal Before Discount</span>
                    <span>{formatINR(pricingQuote.subtotalBeforeDiscountInr)}</span>
                  </div>

                  {pricingQuote.discountAmountInr > 0 && (
                    <div className="summary-card__price-line summary-card__price-line--discount">
                      <span>Promotional Discount ({pricingQuote.promoCode})</span>
                      <span>&minus;{formatINR(pricingQuote.discountAmountInr)}</span>
                    </div>
                  )}

                  {pricingQuote.discountAmountInr > 0 && (
                    <div className="summary-card__price-line">
                      <span>Taxable Amount</span>
                      <span>{formatINR(pricingQuote.taxableAmountInr)}</span>
                    </div>
                  )}

                  <div className="summary-card__price-line">
                    <span>
                      Applicable GST ({pricingQuote.gstPercentageApplied}%)
                    </span>
                    <span>{formatINR(pricingQuote.gstAmountInr)}</span>
                  </div>

                  <div className="summary-card__tax-note">
                    GST rates are provisional and pending verification. Final tax treatment must be confirmed by the property.
                  </div>

                  <div className="summary-card__total-row">
                    <span className="summary-card__total-label">Total Payable</span>
                    <span className="summary-card__total-val">
                      {formatINR(pricingQuote.totalPayableInr)}
                    </span>
                  </div>

                  {/* Nightly breakdown if more than 1 night */}
                  {pricingQuote.nightlyRates && pricingQuote.nightlyRates.length > 1 && (
                    <details className="summary-card__nightly-details">
                      <summary className="summary-card__nightly-summary">
                        Nightly rate details ({pricingQuote.nightlyRates.length} nights)
                      </summary>
                      <div className="summary-card__nightly-list">
                        {pricingQuote.nightlyRates.map((n) => (
                          <div key={n.stayDate} className="summary-card__nightly-row">
                            <span className="summary-card__nightly-date">{n.stayDate}</span>
                            <span className="summary-card__nightly-rate">{formatINR(n.nightlyTotalInr)}/night</span>
                          </div>
                        ))}
                      </div>
                    </details>
                  )}
                </div>
              </div>
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}

export default GuestCheckoutPage;
