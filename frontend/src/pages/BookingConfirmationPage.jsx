import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { fetchBooking } from '../api/roomsApi';
import { formatDisplayDate } from '../utils/dateUtils';
import { formatINR } from '../utils/formatUtils';
import {
  Button,
  Badge,
  Skeleton,
  EmptyState,
  ErrorBanner,
  StepIndicator,
} from '../components/ui';
import './BookingConfirmationPage.css';

export function BookingConfirmationPage() {
  const { ref } = useParams();
  const navigate = useNavigate();

  const [booking, setBooking] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadBooking = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await fetchBooking(ref);
      setBooking(data);
      // Ensure one-time payment token is eliminated from session storage upon reaching confirmation
      sessionStorage.removeItem(`booking_${ref}`);
    } catch (err) {
      setError(err.message || 'Unable to load booking confirmation.');
    } finally {
      setIsLoading(false);
    }
  }, [ref]);

  useEffect(() => {
    loadBooking();
  }, [loadBooking]);

  const isConfirmed = booking?.status === 'CONFIRMED';
  const roomImageSrc = booking?.roomCategoryName?.includes('Villa') ? '/images/villa-pool.webp' : '/images/room-canopy.webp';

  return (
    <div className="confirmation-page">
      {/* Step Indicator */}
      <StepIndicator currentStep={4} />

      {/* Loading Skeletons */}
      {isLoading && (
        <div className="confirmation-card" aria-label="Loading confirmed reservation" aria-busy="true">
          <Skeleton variant="text" width="40%" height="2.25rem" />
          <Skeleton variant="text" width="60%" height="1.25rem" style={{ marginTop: 'var(--space-2)' }} />
          <Skeleton variant="rectangular" width="100%" height="140px" style={{ marginTop: 'var(--space-4)' }} />
          <Skeleton variant="rectangular" width="100%" height="200px" style={{ marginTop: 'var(--space-4)' }} />
        </div>
      )}

      {/* Error Banner */}
      {!isLoading && error && (
        <ErrorBanner
          variant="error"
          title="Reservation Lookup Issue"
          message={error}
          actionLabel="Return to discovery"
          onAction={() => navigate('/')}
        />
      )}

      {/* Main Confirmation Presentation */}
      {!isLoading && booking && (
        <main className="confirmation-card">
          <header className="confirmation-header">
            <div className="confirmation-header__status-badge">
              {isConfirmed ? (
                <Badge variant="success">Reservation Confirmed</Badge>
              ) : booking.status === 'CANCELLED' ? (
                <Badge variant="neutral">Reservation Cancelled</Badge>
              ) : booking.status === 'EXPIRED' ? (
                <Badge variant="danger">Hold Expired</Badge>
              ) : (
                <Badge variant="warning">{booking.status}</Badge>
              )}
            </div>

            <h1 className="confirmation-header__title">
              {isConfirmed ? 'Your Estate Sanctuary is Reserved' : 'Reservation Summary'}
            </h1>
            <p className="confirmation-header__subtitle">
              {isConfirmed
                ? 'We look forward to welcoming you into the misty coffee hills of Mullayanagiri.'
                : `Booking reference status is currently ${booking.status}.`}
            </p>
          </header>

          {/* Reference Highlight Box */}
          <section className="confirmation-ref-box" aria-labelledby="booking-ref-label">
            <div className="confirmation-ref-box__item">
              <span id="booking-ref-label" className="confirmation-ref-box__label">
                Official Booking Reference
              </span>
              <span className="confirmation-ref-box__value">{booking.bookingRef}</span>
            </div>
            <div className="confirmation-ref-box__item">
              <span className="confirmation-ref-box__label">Payment Status</span>
              <span className="confirmation-ref-box__status">
                {isConfirmed ? '✓ Test Payment Confirmed' : booking.status}
              </span>
            </div>
          </section>

          {/* Accommodation & Stay Overview */}
          <section className="confirmation-details">
            <div className="confirmation-details__media">
              <img
                src={roomImageSrc}
                alt=""
                width="240"
                height="150"
                className="confirmation-details__img"
              />
            </div>

            <div className="confirmation-details__content">
              <h2 className="confirmation-details__category-title">{booking.roomCategoryName}</h2>
              <span className="confirmation-details__plan-badge">
                <Badge variant="neutral">{booking.ratePlanName}</Badge>
              </span>

              <div className="confirmation-grid">
                <div className="confirmation-grid__cell">
                  <span className="confirmation-grid__label">Check-in</span>
                  <span className="confirmation-grid__val">
                    {formatDisplayDate(booking.checkInDate)} (2:00 PM)
                  </span>
                </div>

                <div className="confirmation-grid__cell">
                  <span className="confirmation-grid__label">Check-out</span>
                  <span className="confirmation-grid__val">
                    {formatDisplayDate(booking.checkOutDate)} (11:00 AM)
                  </span>
                </div>

                <div className="confirmation-grid__cell">
                  <span className="confirmation-grid__label">Stay Length</span>
                  <span className="confirmation-grid__val">
                    {booking.nights} {booking.nights === 1 ? 'Night' : 'Nights'}
                  </span>
                </div>

                <div className="confirmation-grid__cell">
                  <span className="confirmation-grid__label">Occupancy</span>
                  <span className="confirmation-grid__val">
                    {booking.adults} Adults{booking.children > 0 ? `, ${booking.children} Children` : ''} &bull; {booking.roomsCount} Room
                  </span>
                </div>
              </div>
            </div>
          </section>

          {/* Guest Identity (Strictly Masked) */}
          <section className="confirmation-guest-info">
            <h3 className="confirmation-guest-info__title">Primary Guest Information</h3>
            <div className="confirmation-guest-info__grid">
              <div className="confirmation-guest-info__cell">
                <span className="confirmation-guest-info__label">Guest Name</span>
                <span className="confirmation-guest-info__val">{booking.guestName}</span>
              </div>
              <div className="confirmation-guest-info__cell">
                <span className="confirmation-guest-info__label">Email (Masked)</span>
                <span className="confirmation-guest-info__val">{booking.guestEmail}</span>
              </div>
              <div className="confirmation-guest-info__cell">
                <span className="confirmation-guest-info__label">Mobile Phone (Masked)</span>
                <span className="confirmation-guest-info__val">{booking.guestPhone}</span>
              </div>
            </div>
          </section>

          {/* Tariff Breakdown from Server */}
          <section className="confirmation-financials">
            <h3 className="confirmation-financials__title">Tariff &amp; Tax Receipt</h3>
            <div className="confirmation-financials__lines">
              <div className="confirmation-financials__row">
                <span>Accommodations Subtotal</span>
                <span>{formatINR(booking.subtotalBeforeDiscountInr)}</span>
              </div>

              {booking.discountAmountInr > 0 && (
                <div className="confirmation-financials__row confirmation-financials__row--discount">
                  <span>Promotional Discount Applied</span>
                  <span>&minus;{formatINR(booking.discountAmountInr)}</span>
                </div>
              )}

              <div className="confirmation-financials__row">
                <span>Taxable Amount</span>
                <span>{formatINR(booking.taxableAmountInr)}</span>
              </div>

              <div className="confirmation-financials__row">
                <span>Goods &amp; Services Tax ({booking.gstPercentageApplied}%)</span>
                <span>{formatINR(booking.gstAmountInr)}</span>
              </div>

              <div className="confirmation-financials__total">
                <span className="confirmation-financials__total-label">Total Amount Paid</span>
                <span className="confirmation-financials__total-val">
                  {formatINR(booking.totalPayableInr)}
                </span>
              </div>
            </div>
          </section>

          {/* Estate Notes */}
          <section className="confirmation-notes">
            <h3 className="confirmation-notes__heading">Estate Arrival Notes</h3>
            <ul className="confirmation-notes__list">
              <li>Check-in begins at 2:00 PM. Early arrival may enjoy our Arabica plantation lounge and estate trails.</li>
              <li>Valid government-issued photo ID required for all adult guests. International guests will be assisted with Form C registration.</li>
              <li>Estate Concierge: For dining reservations or airport transfers, contact <code>concierge@chikkamagalururesort.com</code>.</li>
            </ul>
          </section>

          {/* Clear Next Action */}
          <div className="confirmation-actions">
            <Button
              variant="primary"
              onClick={() => navigate('/')}
              className="confirmation-actions__btn"
            >
              Return to Resort Discovery
            </Button>
          </div>
        </main>
      )}
    </div>
  );
}

export default BookingConfirmationPage;
