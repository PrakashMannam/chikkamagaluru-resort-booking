import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { submitPublicEnquiry } from '../api/enquiryApi';
import {
  buildRawEnquiryMessage,
  TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS,
  TOTAL_ESTATE_INVENTORY_KEYS,
  DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION,
  DEFAULT_ACCOMMODATION_CHOICE,
} from '../utils/enquiryMessageBuilder';
import {
  BriefcaseIcon,
  UsersIcon,
  SparklesIcon,
  CheckCircleIcon,
  MountainIcon,
} from '../components/common/Icons';
import './OffsitesPage.css';

export function OffsitesPage() {
  const [fullName, setFullName] = useState('');
  const [contact, setContact] = useState('');
  const [organization, setOrganization] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [accommodationChoice, setAccommodationChoice] = useState(DEFAULT_ACCOMMODATION_CHOICE);
  const [nights, setNights] = useState('');
  const [guestsCount, setGuestsCount] = useState('');
  const [roomsCount, setRoomsCount] = useState('');
  const [needsConferenceHall, setNeedsConferenceHall] = useState(DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION);
  const [budget, setBudget] = useState('');
  const [additionalMessage, setAdditionalMessage] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [successData, setSuccessData] = useState(null);
  const [copiedCode, setCopiedCode] = useState(false);

  const scrollToForm = () => {
    const el = document.getElementById('offsite-enquiry-form');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!fullName.trim()) {
      newErrors.fullName = 'Please enter contact person name.';
    } else if (fullName.trim().length < 2) {
      newErrors.fullName = 'Name must be at least 2 characters.';
    }

    if (!contact.trim()) {
      newErrors.contact = 'Please provide a work email address or mobile phone number.';
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      const phoneRegex = /^[+]?[\d\s-]{10,15}$/;
      const isEmail = emailRegex.test(contact.trim());
      const isPhone = phoneRegex.test(contact.trim().replace(/[\s-]/g, ''));
      if (!isEmail && !isPhone) {
        newErrors.contact = 'Please enter a valid work email or 10-digit mobile number.';
      }
    }

    if (!eventDate) {
      newErrors.eventDate = 'Please specify your target retreat date or month.';
    }

    const trimmedGuests = String(guestsCount).trim();
    if (!trimmedGuests) {
      newErrors.guestsCount = 'Please specify expected team size / attendee count.';
    } else if (trimmedGuests.includes('.') || !/^\d+$/.test(trimmedGuests)) {
      newErrors.guestsCount = 'Attendee count must be a positive whole number.';
    } else {
      const parsed = parseInt(trimmedGuests, 10);
      if (parsed < 1) {
        newErrors.guestsCount = 'Attendee count must be at least 1.';
      } else if (parsed > TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS) {
        newErrors.guestsCount = `Attendee count cannot exceed technical limit of ${TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS}.`;
      }
    }

    if (!accommodationChoice) {
      newErrors.accommodationChoice = 'Please select whether your team will require overnight accommodation.';
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
      enquiryType: 'CORPORATE_OFFSITE',
      eventDate,
      nights,
      guestsCount,
      roomsCount,
      organization: organization.trim(),
      needsConferenceHall,
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
      setSubmitError(err.message || 'Unable to submit your offsite enquiry. Please check your network and retry.');
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
    setOrganization('');
    setEventDate('');
    setAccommodationChoice(DEFAULT_ACCOMMODATION_CHOICE);
    setNights('');
    setGuestsCount('');
    setRoomsCount('');
    setNeedsConferenceHall(DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION);
    setBudget('');
    setAdditionalMessage('');
  };

  return (
    <div className="offsites-page">
      {/* 1. Corporate Offsites Hero Section matching Panel 4 */}
      <section className="offsites-hero">
        <div className="offsites-hero__media">
          <img
            src="/images/corporate-offsite-estate.jpg"
            alt="Open-air timber pavilion with boardroom table overlooking lush coffee hills"
            className="offsites-hero__img"
            loading="eager"
            fetchPriority="high"
            width="1440"
            height="500"
          />
          <div className="offsites-hero__overlay" />
        </div>

        <div className="offsites-hero__content">
          <span className="offsites-hero__eyebrow">CORPORATE OFFSITES</span>
          <h1 className="offsites-hero__title">
            Retreats. Meetings.<br />Team Experiences.
          </h1>
          <p className="offsites-hero__sub">
            A focused yet refreshing setting for team offsites, leadership meets, and strategy sessions amidst the coffee-estate landscape.
          </p>
          <button
            type="button"
            className="offsites-hero__cta"
            onClick={scrollToForm}
          >
            Enquire for Offsite &rarr;
          </button>
        </div>
      </section>

      {/* 2. 3-Column Corporate Pillars Row matching Panel 4 */}
      <section className="offsites-pillars">
        <div className="offsites-pillars__inner">
          <div className="offsites-pillar-card">
            <div className="offsites-pillar-card__icon">
              <UsersIcon size={26} color="#1E3A2F" />
            </div>
            <h3 className="offsites-pillar-card__title">Team Offsites</h3>
            <p className="offsites-pillar-card__desc">
              Step away from the city to focus, align, and build stronger teams together.
            </p>
          </div>

          <div className="offsites-pillar-card">
            <div className="offsites-pillar-card__icon">
              <BriefcaseIcon size={26} color="#1E3A2F" />
            </div>
            <h3 className="offsites-pillar-card__title">Leadership Meets</h3>
            <p className="offsites-pillar-card__desc">
              Quiet estate surroundings for board retreats, annual planning, and strategy sessions.
            </p>
          </div>

          <div className="offsites-pillar-card">
            <div className="offsites-pillar-card__icon">
              <SparklesIcon size={26} color="#1E3A2F" />
            </div>
            <h3 className="offsites-pillar-card__title">Refresh &amp; Reconnect</h3>
            <p className="offsites-pillar-card__desc">
              64 estate rooms, 12 private villas, and open lawns set amidst misty hills.
            </p>
          </div>
        </div>
      </section>

      {/* 3. Scenic Estate Terrace Photo */}
      <section className="offsites-feature-media">
        <div className="offsites-feature-media__container">
          <img
            src="/images/hills-panoramic.jpg"
            alt="Scenic terrace overlooking Western Ghats misty mountain peaks"
            className="offsites-feature-media__img"
            loading="lazy"
            width="1200"
            height="480"
          />
        </div>
      </section>

      {/* 4. Embedded Corporate Offsite Enquiry Form */}
      <section id="offsite-enquiry-form" className="offsites-form-section">
        <div className="offsites-form-section__header">
          <h2 className="offsites-form-section__title">Plan Your Team Offsite</h2>
          <p className="offsites-form-section__subtitle">
            Tell us about your team size, preferred dates, and meeting setup requirements, and our sales team will respond promptly.
          </p>
        </div>

        <div className="offsites-form-card">
          {successData ? (
            <div className="offsites-success" role="status" aria-live="polite">
              <div className="offsites-success__icon-wrap">
                <CheckCircleIcon size={48} color="#1E3A2F" />
              </div>
              <h3 className="offsites-success__title">Offsite Enquiry Dispatched</h3>
              <p className="offsites-success__desc">
                Thank you, <strong>{fullName}</strong>. Your offsite enquiry for {organization ? <strong>{organization}</strong> : 'your team'} has been received by our corporate coordinator. We will prepare an itinerary and facility proposal.
              </p>

              <div className="offsites-success__code-box">
                <span className="offsites-success__code-label">Tracking Reference:</span>
                <span className="offsites-success__code-val">{successData.enquiry_code}</span>
                <button
                  type="button"
                  className="offsites-success__copy-btn"
                  onClick={handleCopyCode}
                >
                  {copiedCode ? 'Copied!' : 'Copy Code'}
                </button>
              </div>

              <div className="offsites-success__actions">
                <button
                  type="button"
                  className="offsites-success__reset-btn"
                  onClick={handleReset}
                >
                  Submit Another Offsite Enquiry
                </button>
                <Link to="/" className="offsites-success__home-link">
                  Return to Estate Home
                </Link>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} noValidate className="offsites-form">
              {submitError && (
                <div className="offsites-form__alert" role="alert">
                  {submitError}
                </div>
              )}

              <div className="offsites-form__grid">
                {/* Contact Name */}
                <div className="offsites-form__field">
                  <label htmlFor="o-name" className="offsites-form__label">
                    Contact Name <span className="offsites-form__req">*</span>
                  </label>
                  <input
                    id="o-name"
                    type="text"
                    value={fullName}
                    onChange={(e) => { setFullName(e.target.value); setErrors(p => ({ ...p, fullName: '' })); }}
                    placeholder="e.g. Vikram Verma"
                    className={`offsites-form__input ${errors.fullName ? 'is-invalid' : ''}`}
                    required
                  />
                  {errors.fullName && <span className="offsites-form__err">{errors.fullName}</span>}
                </div>

                {/* Work Email / Phone */}
                <div className="offsites-form__field">
                  <label htmlFor="o-contact" className="offsites-form__label">
                    Work Email or Mobile <span className="offsites-form__req">*</span>
                  </label>
                  <input
                    id="o-contact"
                    type="text"
                    value={contact}
                    onChange={(e) => { setContact(e.target.value); setErrors(p => ({ ...p, contact: '' })); }}
                    placeholder="name@company.com or 9876543210"
                    className={`offsites-form__input ${errors.contact ? 'is-invalid' : ''}`}
                    required
                  />
                  {errors.contact && <span className="offsites-form__err">{errors.contact}</span>}
                </div>

                {/* Company / Organization Name */}
                <div className="offsites-form__field">
                  <label htmlFor="o-org" className="offsites-form__label">
                    Company / Organization Name
                  </label>
                  <input
                    id="o-org"
                    type="text"
                    value={organization}
                    onChange={(e) => setOrganization(e.target.value)}
                    placeholder="e.g. Acme Technologies Ltd."
                    className="offsites-form__input"
                  />
                </div>

                {/* Target Dates / Month */}
                <div className="offsites-form__field">
                  <label htmlFor="o-date" className="offsites-form__label">
                    Target Dates / Month <span className="offsites-form__req">*</span>
                  </label>
                  <input
                    id="o-date"
                    type="text"
                    value={eventDate}
                    onChange={(e) => { setEventDate(e.target.value); setErrors(p => ({ ...p, eventDate: '' })); }}
                    placeholder="e.g. 18-20 March 2027 or Q1 2027"
                    className={`offsites-form__input ${errors.eventDate ? 'is-invalid' : ''}`}
                    required
                  />
                  {errors.eventDate && <span className="offsites-form__err">{errors.eventDate}</span>}
                </div>

                {/* Expected Attendees */}
                <div className="offsites-form__field">
                  <label htmlFor="o-attendees" className="offsites-form__label">
                    Team Size / Attendees <span className="offsites-form__req">*</span>
                  </label>
                  <input
                    id="o-attendees"
                    type="number"
                    min="1"
                    max={TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS}
                    value={guestsCount}
                    onChange={(e) => { setGuestsCount(e.target.value); setErrors(p => ({ ...p, guestsCount: '' })); }}
                    placeholder="e.g. 35"
                    className={`offsites-form__input ${errors.guestsCount ? 'is-invalid' : ''}`}
                    required
                  />
                  {errors.guestsCount && <span className="offsites-form__err">{errors.guestsCount}</span>}
                </div>

                {/* Setup / Facility Requirements */}
                <div className="offsites-form__field">
                  <label htmlFor="o-hall" className="offsites-form__label">
                    Meeting Setup Requirements
                  </label>
                  <select
                    id="o-hall"
                    value={needsConferenceHall}
                    onChange={(e) => setNeedsConferenceHall(e.target.value)}
                    className="offsites-form__select"
                  >
                    <option value="unspecified">Select requirement...</option>
                    <option value="yes">Conference Hall &amp; AV Presentation Setup</option>
                    <option value="no">Open-Air / Casual Pavilion Discussions</option>
                  </select>
                </div>
              </div>

              {/* Accommodation Choice */}
              <div className="offsites-form__field offsites-form__field--full">
                <label className="offsites-form__label">
                  Will attendees require overnight accommodation? <span className="offsites-form__req">*</span>
                </label>
                <div className="offsites-radio-group">
                  <label className={`offsites-radio-card ${accommodationChoice === 'yes' ? 'is-selected' : ''}`}>
                    <input
                      type="radio"
                      name="o-accommodation"
                      value="yes"
                      checked={accommodationChoice === 'yes'}
                      onChange={() => { setAccommodationChoice('yes'); setErrors(p => ({ ...p, accommodationChoice: '' })); }}
                    />
                    <span>Yes, overnight rooms needed</span>
                  </label>
                  <label className={`offsites-radio-card ${accommodationChoice === 'no' ? 'is-selected' : ''}`}>
                    <input
                      type="radio"
                      name="o-accommodation"
                      value="no"
                      checked={accommodationChoice === 'no'}
                      onChange={() => {
                        setAccommodationChoice('no');
                        setRoomsCount('');
                        setNights('');
                        setErrors(p => ({ ...p, accommodationChoice: '', roomsCount: '', nights: '' }));
                      }}
                    />
                    <span>No (Day offsite only)</span>
                  </label>
                  <label className={`offsites-radio-card ${accommodationChoice === 'not_sure' ? 'is-selected' : ''}`}>
                    <input
                      type="radio"
                      name="o-accommodation"
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
                {errors.accommodationChoice && <span className="offsites-form__err">{errors.accommodationChoice}</span>}
              </div>

              {/* Conditional Stay Inputs */}
              {accommodationChoice === 'yes' && (
                <div className="offsites-form__grid offsites-form__grid--stay">
                  <div className="offsites-form__field">
                    <label htmlFor="o-nights" className="offsites-form__label">
                      Number of Nights <span className="offsites-form__req">*</span>
                    </label>
                    <input
                      id="o-nights"
                      type="number"
                      min="1"
                      value={nights}
                      onChange={(e) => { setNights(e.target.value); setErrors(p => ({ ...p, nights: '' })); }}
                      placeholder="e.g. 2"
                      className={`offsites-form__input ${errors.nights ? 'is-invalid' : ''}`}
                      required
                    />
                    {errors.nights && <span className="offsites-form__err">{errors.nights}</span>}
                  </div>

                  <div className="offsites-form__field">
                    <label htmlFor="o-rooms" className="offsites-form__label">
                      Rooms / Keys Needed (Max 76 keys total inventory) <span className="offsites-form__req">*</span>
                    </label>
                    <input
                      id="o-rooms"
                      type="number"
                      min="1"
                      max={TOTAL_ESTATE_INVENTORY_KEYS}
                      value={roomsCount}
                      onChange={(e) => { setRoomsCount(e.target.value); setErrors(p => ({ ...p, roomsCount: '' })); }}
                      placeholder="e.g. 25"
                      className={`offsites-form__input ${errors.roomsCount ? 'is-invalid' : ''}`}
                      required
                    />
                    {errors.roomsCount && <span className="offsites-form__err">{errors.roomsCount}</span>}
                  </div>
                </div>
              )}

              {/* Optional Budget & Notes */}
              <div className="offsites-form__grid">
                <div className="offsites-form__field">
                  <label htmlFor="o-budget" className="offsites-form__label">
                    Estimated Budget (Optional)
                  </label>
                  <input
                    id="o-budget"
                    type="text"
                    value={budget}
                    onChange={(e) => setBudget(e.target.value)}
                    placeholder="e.g. ₹5,00,000"
                    className="offsites-form__input"
                  />
                </div>
              </div>

              <div className="offsites-form__field offsites-form__field--full">
                <label htmlFor="o-message" className="offsites-form__label">
                  Specific Requirements &amp; Agenda Details
                </label>
                <textarea
                  id="o-message"
                  rows={4}
                  value={additionalMessage}
                  onChange={(e) => setAdditionalMessage(e.target.value)}
                  placeholder="Share details on daily agenda, breakout discussions, dining preferences, or plantation walk activities."
                  className="offsites-form__textarea"
                />
              </div>

              <div className="offsites-form__actions">
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="offsites-form__submit-btn"
                >
                  {isSubmitting ? 'Submitting Offsite Enquiry...' : 'Submit Offsite Enquiry'}
                </button>
              </div>
            </form>
          )}
        </div>
      </section>
    </div>
  );
}

export default OffsitesPage;
