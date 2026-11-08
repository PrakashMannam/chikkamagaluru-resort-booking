import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { fetchBooking, simulatePayment, cancelHold } from '../api/roomsApi';
import { formatDisplayDate, parseServerExpiry } from '../utils/dateUtils';
import { formatINR } from '../utils/formatUtils';
import {
  Button,
  Badge,
  Skeleton,
  EmptyState,
  ErrorBanner,
  StepIndicator,
} from '../components/ui';
import './GuestPaymentPage.css';

export function GuestPaymentPage() {
  const { ref } = useParams();
  const navigate = useNavigate();

  // Booking and hold state
  const [bookingData, setBookingData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Payment interaction state
  const [isProcessing, setIsProcessing] = useState(false);
  const [processingAction, setProcessingAction] = useState(null); // 'success' | 'fail' | 'cancel'
  const [paymentResult, setPaymentResult] = useState(null); // 'FAILED' | 'CANCELLED'

  // Retrieve cached token and expiry from sessionStorage
  const sessionItem = useMemo(() => {
    try {
      const raw = sessionStorage.getItem(`booking_${ref}`);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }, [ref]);

  const token = sessionItem?.token;

  // Countdown calculations
  const [timeLeft, setTimeLeft] = useState(600); // in seconds
  const [isExpired, setIsExpired] = useState(false);

  const loadBooking = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await fetchBooking(ref);
      setBookingData(data);

      if (data.status === 'CONFIRMED') {
        sessionStorage.removeItem(`booking_${ref}`);
        navigate(`/bookings/${ref}`, { replace: true });
        return;
      }
      if (data.status === 'EXPIRED') {
        setIsExpired(true);
        sessionStorage.removeItem(`booking_${ref}`);
      } else if (data.status === 'CANCELLED') {
        setPaymentResult('CANCELLED');
        sessionStorage.removeItem(`booking_${ref}`);
      } else if (data.status === 'FAILED') {
        setPaymentResult('FAILED');
        sessionStorage.removeItem(`booking_${ref}`);
      }
    } catch (err) {
      setError(err.message || 'Unable to load reservation details.');
    } finally {
      setIsLoading(false);
    }
  }, [ref, navigate]);

  useEffect(() => {
    loadBooking();
  }, [loadBooking]);

  // Expiration countdown based on server expiresAt or session creation time
  useEffect(() => {
    let targetTime;
    const serverParsed = parseServerExpiry(bookingData?.expiresAt);
    const sessionParsed = parseServerExpiry(sessionItem?.expiresAt);

    if (serverParsed) {
      targetTime = serverParsed;
    } else if (sessionParsed) {
      targetTime = sessionParsed;
    } else if (sessionItem?.createdAt) {
      targetTime = sessionItem.createdAt + 10 * 60 * 1000;
    } else {
      targetTime = Date.now() + 10 * 60 * 1000;
    }

    function tick() {
      const diff = Math.max(0, Math.floor((targetTime - Date.now()) / 1000));
      setTimeLeft(diff);
      // NOTE: The client timer must NOT independently invalidate a booking or delete
      // the payment token merely on client-side arithmetic. Server-side validation
      // remains authoritative. Token is preserved and payment is permitted.
    }

    tick();
    const interval = setInterval(tick, 1000);
    return () => clearInterval(interval);
  }, [bookingData, sessionItem, ref]);

  function formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }

  // Simulate payment (SUCCESS or FAILURE)
  async function handleSimulate(status) {
    if (!token) {
      setError('Payment token session expired or not found. Please create a new reservation hold.');
      return;
    }

    setIsProcessing(true);
    setProcessingAction(status === 'SUCCESS' ? 'success' : 'fail');
    setError(null);

    try {
      const result = await simulatePayment(ref, token, status);
      if (status === 'SUCCESS' && result.status === 'CONFIRMED') {
        // Clear session token on confirmed success
        sessionStorage.removeItem(`booking_${ref}`);
        navigate(`/bookings/${encodeURIComponent(ref)}`);
      } else if (status === 'FAILURE' || result.status === 'FAILED') {
        setPaymentResult('FAILED');
        sessionStorage.removeItem(`booking_${ref}`);
      }
    } catch (err) {
      const isExp = (err.status === 409) ||
                    (err.message && err.message.toLowerCase().includes('expired')) ||
                    (err.serverBody?.error && typeof err.serverBody.error === 'string' && err.serverBody.error.toLowerCase().includes('expired'));
      if (isExp) {
        setIsExpired(true);
        sessionStorage.removeItem(`booking_${ref}`);
      } else {
        setError(err.message || 'Payment simulation failed.');
      }
    } finally {
      setIsProcessing(false);
      setProcessingAction(null);
    }
  }

  // Cancel hold
  async function handleCancelHold() {
    if (!token) {
      setError('Payment token session not found.');
      return;
    }

    setIsProcessing(true);
    setProcessingAction('cancel');
    setError(null);

    try {
      await cancelHold(ref, token);
      setPaymentResult('CANCELLED');
      sessionStorage.removeItem(`booking_${ref}`);
    } catch (err) {
      setError(err.message || 'Unable to cancel booking hold.');
    } finally {
      setIsProcessing(false);
      setProcessingAction(null);
    }
  }

  const roomImageSrc = bookingData?.roomCategoryName?.includes('Villa') ? '/images/villa-pool.webp' : '/images/room-canopy.webp';

  return (
    <div className="payment-page">
      {/* 1. Progress Step Indicator */}
      <StepIndicator currentStep={2} />

      {/* Loading Skeletons */}
      {isLoading && (
        <div className="payment-card" aria-label="Loading reservation hold" aria-busy="true">
          <Skeleton variant="text" width="40%" height="2rem" />
          <Skeleton variant="text" width="60%" height="1.25rem" style={{ marginTop: 'var(--space-2)' }} />
          <Skeleton variant="rectangular" width="100%" height="180px" style={{ marginTop: 'var(--space-4)' }} />
          <Skeleton variant="rectangular" width="100%" height="60px" style={{ marginTop: 'var(--space-3)' }} />
        </div>
      )}

      {/* Initial Load Error */}
      {!isLoading && error && !bookingData && (
        <ErrorBanner
          variant="error"
          title="Could not find reservation"
          message={error}
          actionLabel="Return to discovery"
          onAction={() => navigate('/stay')}
        />
      )}

      {/* Main Payment Experience */}
      {!isLoading && bookingData && (
        <main className="payment-container">
          {/* A. Hold Expired State */}
          {isExpired && (
            <div className="payment-card payment-card--status">
              <Badge variant="danger">Hold Expired</Badge>
              <h1 className="payment-status__title">Reservation Hold Expired</h1>
              <p className="payment-status__desc">
                Your 10-minute hold window for reservation <strong>{ref}</strong> has expired. To ensure fairness across all guests, any temporarily held room allotment has been released back to our live booking calendar.
              </p>
              <div className="payment-status__actions">
                <Button variant="primary" onClick={() => navigate('/stay')}>
                  Start new reservation
                </Button>
              </div>
            </div>
          )}

          {/* B. Hold Cancelled State */}
          {!isExpired && paymentResult === 'CANCELLED' && (
            <div className="payment-card payment-card--status">
              <Badge variant="neutral">Hold Cancelled</Badge>
              <h1 className="payment-status__title">Hold Successfully Cancelled</h1>
              <p className="payment-status__desc">
                Your reservation hold <strong>{ref}</strong> has been cancelled and the held estate keys have been released immediately back into online allotment.
              </p>
              <div className="payment-status__actions">
                <Button variant="primary" onClick={() => navigate('/stay')}>
                  Browse estate accommodations
                </Button>
              </div>
            </div>
          )}

          {/* C. Payment Failed State */}
          {!isExpired && paymentResult === 'FAILED' && (
            <div className="payment-card payment-card--status">
              <Badge variant="danger">Payment Failed</Badge>
              <h1 className="payment-status__title">Simulated Transaction Declined</h1>
              <p className="payment-status__desc">
                The simulated transaction for reservation <strong>{ref}</strong> failed. As per resort policy, the allotment hold has been released. You may start a new booking whenever you wish.
              </p>
              <div className="payment-status__actions">
                <Button variant="primary" onClick={() => navigate('/stay')}>
                  Return to available rooms
                </Button>
              </div>
            </div>
          )}

          {/* D. Active Payment Hold Simulator */}
          {!isExpired && !paymentResult && (
            <div className="payment-card">
              {/* Test Mode Badge Header */}
              <div className="payment-test-badge">
                <span className="payment-test-badge__dot" aria-hidden="true" />
                <span className="payment-test-badge__text">
                  Test Mode &bull; Simulated Payment Gateway
                </span>
              </div>

              <header className="payment-card__header">
                <div className="payment-card__meta-top">
                  <span className="payment-card__kicker">Step 2 of 3 &bull; Payment</span>
                  <div className="payment-countdown" aria-live="polite">
                    <span className="payment-countdown__label">Hold expires in:</span>
                    <span className="payment-countdown__timer">{formatTime(timeLeft)}</span>
                  </div>
                </div>

                <h1 className="payment-card__title">Complete Your Estate Reservation</h1>
                <p className="payment-card__subtitle">
                  Review your stay details and authorize the mock reservation payment.
                </p>
              </header>

              {error && (
                <div style={{ marginBottom: 'var(--space-3)' }}>
                  <ErrorBanner
                    variant="error"
                    title="Payment Notice"
                    message={error}
                  />
                </div>
              )}

              {/* Reservation Reference Banner */}
              <div className="payment-ref-strip">
                <div className="payment-ref-item">
                  <span className="payment-ref-label">Booking Reference</span>
                  <span className="payment-ref-val">{ref}</span>
                </div>
                <div className="payment-ref-item">
                  <span className="payment-ref-label">Hold Status</span>
                  <Badge variant="warning">Pending Payment</Badge>
                </div>
              </div>

              {/* Booking Summary Box */}
              <div className="payment-summary">
                <div className="payment-summary__left">
                  <img
                    src={roomImageSrc}
                    alt=""
                    width="120"
                    height="80"
                    className="payment-summary__img"
                  />
                  <div className="payment-summary__details">
                    <h2 className="payment-summary__room-title">{bookingData.roomCategoryName}</h2>
                    <span className="payment-summary__plan">{bookingData.ratePlanName}</span>
                    <span className="payment-summary__dates">
                      {formatDisplayDate(bookingData.checkInDate)} &ndash; {formatDisplayDate(bookingData.checkOutDate)} ({bookingData.nights} {bookingData.nights === 1 ? 'Night' : 'Nights'})
                    </span>
                    <span className="payment-summary__guests">
                      {bookingData.adults} Adults{bookingData.children > 0 ? `, ${bookingData.children} Children` : ''} &bull; {bookingData.roomsCount} Room
                    </span>
                  </div>
                </div>

                <div className="payment-summary__pricing">
                  <div className="payment-summary__price-line">
                    <span>Pre-tax Subtotal</span>
                    <span>{formatINR(bookingData.subtotalBeforeDiscountInr)}</span>
                  </div>
                  {bookingData.discountAmountInr > 0 && (
                    <div className="payment-summary__price-line payment-summary__price-line--discount">
                      <span>Promotional Discount</span>
                      <span>&minus;{formatINR(bookingData.discountAmountInr)}</span>
                    </div>
                  )}
                  <div className="payment-summary__price-line">
                    <span>GST ({bookingData.gstPercentageApplied}%)</span>
                    <span>{formatINR(bookingData.gstAmountInr)}</span>
                  </div>
                  <div className="payment-summary__tax-note">
                    GST rates are provisional and pending verification. Final tax treatment must be confirmed by the property.
                  </div>
                  <div className="payment-summary__total-line">
                    <span className="payment-summary__total-label">Total Payable</span>
                    <span className="payment-summary__total-val">
                      {formatINR(bookingData.totalPayableInr)}
                    </span>
                  </div>
                </div>
              </div>

              {/* Mock Payment Authorization Controls */}
              <div className="payment-controls">
                <h3 className="payment-controls__heading">
                  Authorize Test Transaction
                </h3>
                <p className="payment-controls__desc">
                  Simulate your preferred payment outcome to verify end-to-end booking confirmation and safe inventory release.
                </p>

                <div className="payment-controls__button-group">
                  <Button
                    variant="primary"
                    loading={isProcessing && processingAction === 'success'}
                    disabled={isProcessing}
                    onClick={() => handleSimulate('SUCCESS')}
                    className="payment-btn payment-btn--success"
                  >
                    Simulate Successful Payment ({formatINR(bookingData.totalPayableInr)})
                  </Button>

                  <Button
                    variant="secondary"
                    loading={isProcessing && processingAction === 'fail'}
                    disabled={isProcessing}
                    onClick={() => handleSimulate('FAILURE')}
                    className="payment-btn"
                  >
                    Simulate Payment Failure
                  </Button>

                  <Button
                    variant="ghost"
                    loading={isProcessing && processingAction === 'cancel'}
                    disabled={isProcessing}
                    onClick={handleCancelHold}
                    className="payment-btn payment-btn--cancel"
                  >
                    Cancel Hold &amp; Release Keys
                  </Button>
                </div>
              </div>
            </div>
          )}
        </main>
      )}
    </div>
  );
}

export default GuestPaymentPage;
