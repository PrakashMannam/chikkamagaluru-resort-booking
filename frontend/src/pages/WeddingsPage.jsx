import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { submitPublicEnquiry } from '../api/enquiryApi';
import {
  buildRawEnquiryMessage,
  TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS,
  TOTAL_ESTATE_INVENTORY_KEYS,
  DEFAULT_ACCOMMODATION_CHOICE,
} from '../utils/enquiryMessageBuilder';
import {
  UsersIcon,
  KeyIcon,
  SparklesIcon,
  CheckCircleIcon,
  CalendarIcon,
  CoffeeLeafIcon,
} from '../components/common/Icons';
import './WeddingsPage.css';

export function WeddingsPage() {
  const [fullName, setFullName] = useState('');
  const [contact, setContact] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [accommodationChoice, setAccommodationChoice] = useState(DEFAULT_ACCOMMODATION_CHOICE);
  const [nights, setNights] = useState('');
  const [guestsCount, setGuestsCount] = useState('');
  const [roomsCount, setRoomsCount] = useState('');
  const [budget, setBudget] = useState('');
  const [additionalMessage, setAdditionalMessage] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [successData, setSuccessData] = useState(null);
  const [copiedCode, setCopiedCode] = useState(false);

  const scrollToForm = () => {
    const el = document.getElementById('wedding-enquiry-form');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!fullName.trim()) {
      newErrors.fullName = 'Please enter your full name.';
    } else if (fullName.trim().length < 2) {
      newErrors.fullName = 'Name must be at least 2 characters.';
    }

    if (!contact.trim()) {
      newErrors.contact = 'Please provide an email address or mobile phone number.';
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      const phoneRegex = /^[+]?[\d\s-]{10,15}$/;
      const isEmail = emailRegex.test(contact.trim());
      const isPhone = phoneRegex.test(contact.trim().replace(/[\s-]/g, ''));
      if (!isEmail && !isPhone) {
        newErrors.contact = 'Please enter a valid email address or 10-digit phone number.';
      }
    }

    if (!eventDate) {
      newErrors.eventDate = 'Please specify your preferred celebration date or month.';
    }

    const trimmedGuests = String(guestsCount).trim();
    if (!trimmedGuests) {
      newErrors.guestsCount = 'Please specify expected number of wedding guests.';
    } else if (trimmedGuests.includes('.') || !/^\d+$/.test(trimmedGuests)) {
      newErrors.guestsCount = 'Guest count must be a positive whole number.';
    } else {
      const parsed = parseInt(trimmedGuests, 10);
      if (parsed < 1) {
        newErrors.guestsCount = 'Guest count must be at least 1.';
      } else if (parsed > TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS) {
        newErrors.guestsCount = `Guest count cannot exceed technical limit of ${TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS}.`;
      }
    }

    if (!accommodationChoice) {
      newErrors.accommodationChoice = 'Please select whether your guests will require overnight accommodation.';
    }

    if (accommodationChoice === 'yes') {
      const parsedNights = parseInt(nights, 10);
      if (!nights || isNaN(parsedNights) || parsedNights < 1 || String(nights).includes('.')) {
        newErrors.nights = 'Number of nights must be at least 1.';
      }

      const trimmedRooms = String(roomsCount).trim();
      if (!trimmedRooms) {
        newErrors.roomsCount = 'Please specify the number of rooms needed (up to 76 keys).';
      } else if (trimmedRooms.includes('.') || !/^\d+$/.test(trimmedRooms)) {
        newErrors.roomsCount = 'Number of rooms must be a positive whole number.';
      } else {
        const parsedRooms = parseInt(trimmedRooms, 10);
        if (parsedRooms < 1) {
          newErrors.roomsCount = 'Number of rooms must be at least 1.';
        } else if (parsedRooms > TOTAL_ESTATE_INVENTORY_KEYS) {
          newErrors.roomsCount = `Requested rooms cannot exceed total estate inventory of ${TOTAL_ESTATE_INVENTORY_KEYS} keys.`;
        }
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitError(null);

    if (!validateForm()) return;

    setIsSubmitting(true);

    const parsedGuests = parseInt(guestsCount, 10);
    const parsedRooms = accommodationChoice === 'yes' && roomsCount ? parseInt(roomsCount, 10) : null;

    const rawMessage = buildRawEnquiryMessage({
      enquiryType: 'WEDDING',
      eventDate,
      nights,
      guestsCount,
      roomsCount,
      budget,
      additionalMessage,
      accommodationChoice,
    });

    const payload = {
      sender_name: fullName.trim(),
      sender_contact: contact.trim(),
      raw_message: rawMessage,
      expected_guests: Number.isFinite(parsedGuests) ? parsedGuests : null,
      rooms_count: Number.isFinite(parsedRooms) ? parsedRooms : null,
      accommodation_choice: accommodationChoice,
    };

    try {
      const response = await submitPublicEnquiry(payload);
      setSuccessData(response);
    } catch (err) {
      setSubmitError(err.message || 'Unable to submit your enquiry. Please check your network and retry.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCopyCode = () => {
    if (successData?.enquiry_code) {
      navigator.clipboard.writeText(successData.enquiry_code);
      setCopiedCode(true);
      setTimeout(() => setCopiedCode(false), 2500);
    }
  };

  const handleReset = () => {
    setSuccessData(null);
    setSubmitError(null);
    setErrors({});
    setFullName('');
    setContact('');
    setEventDate('');
    setAccommodationChoice(DEFAULT_ACCOMMODATION_CHOICE);
    setNights('');
    setGuestsCount('');
    setRoomsCount('');
    setBudget('');
    setAdditionalMessage('');
  };

  return (
    <div className="weddings-page">
      {/* 1. Hero Section matching Panel 3 */}
      <section className="weddings-hero">
        <div className="weddings-hero__media">
          <img
            src="/images/wedding-lawn.jpg"
            alt="Outdoor floral mandap and estate lawn in Chikkamagaluru"
            className="weddings-hero__img"
            loading="eager"
            fetchPriority="high"
            decoding="async"
            width="1200"
            height="750"
          />
          <div className="weddings-hero__overlay" />
        </div>

        <div className="weddings-hero__content">
          <span className="weddings-hero__eyebrow">WEDDINGS</span>
          <h1 className="weddings-hero__title">Weddings &amp; Celebrations</h1>
          <p className="weddings-hero__sub">
            A picturesque coffee-estate setting for intimate gatherings and grand celebrations in Chikkamagaluru.
          </p>
          <button
            type="button"
            className="weddings-hero__cta"
            onClick={scrollToForm}
          >
            Enquire Now &rarr;
          </button>
        </div>
      </section>

      {/* 2. 3-Column Highlights Row matching Panel 3 */}
      <section className="weddings-highlights">
        <div className="weddings-highlights__inner">
          <div className="weddings-highlight-card">
            <div className="weddings-highlight-card__icon">
              <CoffeeLeafIcon size={26} />
            </div>
            <h3 className="weddings-highlight-card__title">Scenic Estate Lawns</h3>
            <p className="weddings-highlight-card__desc">
              Expansive outdoor lawns surrounded by coffee plantations for ceremonies and receptions.
            </p>
          </div>

          <div className="weddings-highlight-card">
            <div className="weddings-highlight-card__icon">
              <KeyIcon size={26} color="#1E3A2F" />
            </div>
            <h3 className="weddings-highlight-card__title">76 Keys for Guests</h3>
            <p className="weddings-highlight-card__desc">
              64 estate rooms and 12 private villas to host your family and wedding party on-site.
            </p>
          </div>

          <div className="weddings-highlight-card">
            <div className="weddings-highlight-card__icon">
              <SparklesIcon size={26} color="#1E3A2F" />
            </div>
            <h3 className="weddings-highlight-card__title">Tailored Celebrations</h3>
            <p className="weddings-highlight-card__desc">
              Dedicated wedding specialists to coordinate your dates, accommodation buyouts, and requirements.
            </p>
          </div>
        </div>
      </section>

      {/* 3. Wide Lawn Feature Image */}
      <section className="weddings-feature-media">
        <div className="weddings-feature-media__container">
          <img
            src="/images/wedding-lawn-estate.jpg"
            alt="Scenic view of outdoor wedding reception lawns at Chikkamagaluru Estate"
            className="weddings-feature-media__img"
            loading="lazy"
            width="1200"
            height="480"
          />
        </div>
      </section>

      {/* 4. Embedded Wedding Enquiry Form */}
      <section id="wedding-enquiry-form" className="weddings-form-section">
        <div className="weddings-form-section__header">
          <h2 className="weddings-form-section__title">Plan Your Celebration</h2>
          <p className="weddings-form-section__subtitle">
            Share your preferred dates, estimated guest count, and celebration details. Our wedding team will prepare a tailored proposal.
          </p>
        </div>

        <div className="weddings-form-card">
          {successData ? (
            <div className="weddings-success" role="status" aria-live="polite">
              <div className="weddings-success__icon-wrap">
                <CheckCircleIcon size={48} color="#1E3A2F" />
              </div>
              <h3 className="weddings-success__title">Enquiry Dispatched to Wedding Concierge</h3>
              <p className="weddings-success__desc">
                Thank you, <strong>{fullName}</strong>. Your wedding enquiry has been assigned to our celebrations desk. Our team will review your estimated dates and guest arrangements and reach out promptly.
              </p>

              <div className="weddings-success__code-box">
                <span className="weddings-success__code-label">Tracking Reference:</span>
                <span className="weddings-success__code-val">{successData.enquiry_code}</span>
                <button
                  type="button"
                  className="weddings-success__copy-btn"
                  onClick={handleCopyCode}
                >
                  {copiedCode ? 'Copied!' : 'Copy Code'}
                </button>
              </div>

              <div className="weddings-success__actions">
                <button
                  type="button"
                  className="weddings-success__reset-btn"
                  onClick={handleReset}
                >
                  Submit Another Wedding Enquiry
                </button>
                <Link to="/" className="weddings-success__home-link">
                  Return to Estate Home
                </Link>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} noValidate className="weddings-form">
              {submitError && (
                <div className="weddings-form__alert" role="alert">
                  {submitError}
                </div>
              )}

              <div className="weddings-form__grid">
                {/* Full Name */}
                <div className="weddings-form__field">
                  <label htmlFor="w-name" className="weddings-form__label">
                    Full Name <span className="weddings-form__req">*</span>
                  </label>
                  <input
                    id="w-name"
                    type="text"
                    value={fullName}
                    onChange={(e) => { setFullName(e.target.value); setErrors(p => ({ ...p, fullName: '' })); }}
                    placeholder="e.g. Radhika Sharma"
                    className={`weddings-form__input ${errors.fullName ? 'is-invalid' : ''}`}
                    required
                  />
                  {errors.fullName && <span className="weddings-form__err">{errors.fullName}</span>}
                </div>

                {/* Email or Phone */}
                <div className="weddings-form__field">
                  <label htmlFor="w-contact" className="weddings-form__label">
                    Email Address or Mobile <span className="weddings-form__req">*</span>
                  </label>
                  <input
                    id="w-contact"
                    type="text"
                    value={contact}
                    onChange={(e) => { setContact(e.target.value); setErrors(p => ({ ...p, contact: '' })); }}
                    placeholder="name@example.com or 9876543210"
                    className={`weddings-form__input ${errors.contact ? 'is-invalid' : ''}`}
                    required
                  />
                  {errors.contact && <span className="weddings-form__err">{errors.contact}</span>}
                </div>

                {/* Event Date / Month */}
                <div className="weddings-form__field">
                  <label htmlFor="w-date" className="weddings-form__label">
                    Preferred Celebration Date / Month <span className="weddings-form__req">*</span>
                  </label>
                  <input
                    id="w-date"
                    type="text"
                    value={eventDate}
                    onChange={(e) => { setEventDate(e.target.value); setErrors(p => ({ ...p, eventDate: '' })); }}
                    placeholder="e.g. 14 February 2027 or Nov 2027"
                    className={`weddings-form__input ${errors.eventDate ? 'is-invalid' : ''}`}
                    required
                  />
                  {errors.eventDate && <span className="weddings-form__err">{errors.eventDate}</span>}
                </div>

                {/* Expected Guest Count */}
                <div className="weddings-form__field">
                  <label htmlFor="w-guests" className="weddings-form__label">
                    Estimated Guest Count <span className="weddings-form__req">*</span>
                  </label>
                  <input
                    id="w-guests"
                    type="number"
                    min="1"
                    max={TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS}
                    value={guestsCount}
                    onChange={(e) => { setGuestsCount(e.target.value); setErrors(p => ({ ...p, guestsCount: '' })); }}
                    placeholder="e.g. 150"
                    className={`weddings-form__input ${errors.guestsCount ? 'is-invalid' : ''}`}
                    required
                  />
                  {errors.guestsCount && <span className="weddings-form__err">{errors.guestsCount}</span>}
                </div>
              </div>

              {/* Accommodation Choice */}
              <div className="weddings-form__field weddings-form__field--full">
                <label className="weddings-form__label">
                  Will guests require overnight accommodation? <span className="weddings-form__req">*</span>
                </label>
                <div className="weddings-radio-group">
                  <label className={`weddings-radio-card ${accommodationChoice === 'yes' ? 'is-selected' : ''}`}>
                    <input
                      type="radio"
                      name="w-accommodation"
                      value="yes"
                      checked={accommodationChoice === 'yes'}
                      onChange={() => { setAccommodationChoice('yes'); setErrors(p => ({ ...p, accommodationChoice: '' })); }}
                    />
                    <span>Yes, overnight rooms needed</span>
                  </label>
                  <label className={`weddings-radio-card ${accommodationChoice === 'no' ? 'is-selected' : ''}`}>
                    <input
                      type="radio"
                      name="w-accommodation"
                      value="no"
                      checked={accommodationChoice === 'no'}
                      onChange={() => {
                        setAccommodationChoice('no');
                        setRoomsCount('');
                        setNights('');
                        setErrors(p => ({ ...p, accommodationChoice: '', roomsCount: '', nights: '' }));
                      }}
                    />
                    <span>No (Day celebration only)</span>
                  </label>
                  <label className={`weddings-radio-card ${accommodationChoice === 'not_sure' ? 'is-selected' : ''}`}>
                    <input
                      type="radio"
                      name="w-accommodation"
                      value="not_sure"
                      checked={accommodationChoice === 'not_sure'}
                      onChange={() => {
                        setAccommodationChoice('not_sure');
                        setRoomsCount('');
                        setNights('');
                        setErrors(p => ({ ...p, accommodationChoice: '', roomsCount: '', nights: '' }));
                      }}
                    />
                    <span>Not sure yet</span>
                  </label>
                </div>
                {errors.accommodationChoice && <span className="weddings-form__err">{errors.accommodationChoice}</span>}
              </div>

              {/* Conditional Stay Inputs */}
              {accommodationChoice === 'yes' && (
                <div className="weddings-form__grid weddings-form__grid--stay">
                  <div className="weddings-form__field">
                    <label htmlFor="w-nights" className="weddings-form__label">
                      Number of Nights <span className="weddings-form__req">*</span>
                    </label>
                    <input
                      id="w-nights"
                      type="number"
                      min="1"
                      value={nights}
                      onChange={(e) => { setNights(e.target.value); setErrors(p => ({ ...p, nights: '' })); }}
                      placeholder="e.g. 2"
                      className={`weddings-form__input ${errors.nights ? 'is-invalid' : ''}`}
                      required
                    />
                    {errors.nights && <span className="weddings-form__err">{errors.nights}</span>}
                  </div>

                  <div className="weddings-form__field">
                    <label htmlFor="w-rooms" className="weddings-form__label">
                      Rooms / Keys Needed (Max 76 keys total inventory) <span className="weddings-form__req">*</span>
                    </label>
                    <input
                      id="w-rooms"
                      type="number"
                      min="1"
                      max={TOTAL_ESTATE_INVENTORY_KEYS}
                      value={roomsCount}
                      onChange={(e) => { setRoomsCount(e.target.value); setErrors(p => ({ ...p, roomsCount: '' })); }}
                      placeholder="e.g. 30"
                      className={`weddings-form__input ${errors.roomsCount ? 'is-invalid' : ''}`}
                      required
                    />
                    {errors.roomsCount && <span className="weddings-form__err">{errors.roomsCount}</span>}
                  </div>
                </div>
              )}

              {/* Optional Budget & Notes */}
              <div className="weddings-form__grid">
                <div className="weddings-form__field">
                  <label htmlFor="w-budget" className="weddings-form__label">
                    Estimated Budget (Optional)
                  </label>
                  <input
                    id="w-budget"
                    type="text"
                    value={budget}
                    onChange={(e) => setBudget(e.target.value)}
                    placeholder="e.g. ₹15,00,000"
                    className="weddings-form__input"
                  />
                </div>
              </div>

              <div className="weddings-form__field weddings-form__field--full">
                <label htmlFor="w-message" className="weddings-form__label">
                  Celebration Requirements &amp; Special Notes
                </label>
                <textarea
                  id="w-message"
                  rows={4}
                  value={additionalMessage}
                  onChange={(e) => setAdditionalMessage(e.target.value)}
                  placeholder="Share details on functions (Mehendi, Sangeet, Muhurtham), catering preferences, or questions."
                  className="weddings-form__textarea"
                />
              </div>

              <div className="weddings-form__actions">
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="weddings-form__submit-btn"
                >
                  {isSubmitting ? 'Submitting Enquiry...' : 'Submit Wedding Enquiry'}
                </button>
              </div>
            </form>
          )}
        </div>
      </section>
    </div>
  );
}

export default WeddingsPage;
