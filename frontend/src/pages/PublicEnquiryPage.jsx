import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { submitPublicEnquiry } from '../api/enquiryApi';
import { Button, Input, Card, Badge, ErrorBanner } from '../components/ui';
import { buildRawEnquiryMessage, DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION } from '../utils/enquiryMessageBuilder';
import './PublicEnquiryPage.css';

const ENQUIRY_TYPES = [
  {
    id: 'WEDDING',
    label: 'Weddings & Celebrations',
    description: 'Destination celebrations, ceremonies, and private gatherings across scenic estate lawns.',
    icon: '💍',
  },
  {
    id: 'CORPORATE_OFFSITE',
    label: 'Corporate Offsites',
    description: 'Executive retreats, team gatherings, and offsite programs on the estate grounds.',
    icon: '💼',
  },
  {
    id: 'ROOM_BOOKING',
    label: 'Room Booking',
    description: 'Individual stays, family getaways, and private cottage or pool villa reservations.',
    icon: '☕',
  },
];

function mapQueryType(param) {
  if (!param) return 'WEDDING';
  const val = param.toLowerCase().trim();
  if (val === 'wedding' || val === 'weddings') return 'WEDDING';
  if (val === 'offsite' || val === 'offsites' || val === 'corporate' || val === 'corporate_offsite') return 'CORPORATE_OFFSITE';
  if (val === 'room' || val === 'rooms' || val === 'room_booking' || val === 'stay' || val === 'general') return 'ROOM_BOOKING';
  return 'WEDDING';
}

export function PublicEnquiryPage() {
  const [searchParams] = useSearchParams();
  const initialType = mapQueryType(searchParams.get('type'));
  const [enquiryType, setEnquiryType] = useState(initialType);

  // Contact fields
  const [fullName, setFullName] = useState('');
  const [contact, setContact] = useState('');

  // Event / Stay details
  const [eventDate, setEventDate] = useState('');
  const [nights, setNights] = useState('2');
  const [guestsCount, setGuestsCount] = useState(
    initialType === 'WEDDING' ? '200' : initialType === 'CORPORATE_OFFSITE' ? '45' : '4'
  );
  const [roomsCount, setRoomsCount] = useState(
    initialType === 'WEDDING' ? '30' : initialType === 'CORPORATE_OFFSITE' ? '20' : '2'
  );
  const [budget, setBudget] = useState('');

  // Type specific details
  const [organization, setOrganization] = useState('');
  const [needsConferenceHall, setNeedsConferenceHall] = useState(DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION);
  const [stayPreference, setStayPreference] = useState('VILLA');
  const [additionalMessage, setAdditionalMessage] = useState('');

  // UI state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [successData, setSuccessData] = useState(null);
  const [copiedCode, setCopiedCode] = useState(false);

  // Switch type and set sensible default counts
  const handleTypeChange = (typeId) => {
    setEnquiryType(typeId);
    setErrors({});
    setSubmitError(null);

    if (typeId === 'WEDDING') {
      setGuestsCount('200');
      setNights('2');
      setRoomsCount('30');
    } else if (typeId === 'CORPORATE_OFFSITE') {
      setGuestsCount('45');
      setNights('2');
      setRoomsCount('20');
      setNeedsConferenceHall(DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION);
    } else {
      setGuestsCount('4');
      setNights('2');
      setRoomsCount('2');
    }
  };

  // Sync state if URL query params change dynamically
  useEffect(() => {
    const typeParam = searchParams.get('type');
    if (typeParam) {
      const mapped = mapQueryType(typeParam);
      handleTypeChange(mapped);
    }
  }, [searchParams]);

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
        newErrors.contact = 'Please enter a valid email address (e.g. name@example.com) or 10-digit phone number.';
      }
    }

    if (!eventDate) {
      newErrors.eventDate = 'Please select your preferred date.';
    }

    const parsedGuests = parseInt(guestsCount, 10);
    if (!guestsCount || isNaN(parsedGuests) || parsedGuests < 1) {
      newErrors.guestsCount = 'Please specify expected number of guests.';
    }

    const parsedNights = parseInt(nights, 10);
    if (!nights || isNaN(parsedNights) || parsedNights < 1) {
      newErrors.nights = 'Number of nights must be at least 1.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const buildRawMessage = () => buildRawEnquiryMessage({
    enquiryType,
    eventDate,
    nights,
    guestsCount,
    roomsCount,
    budget,
    additionalMessage,
    organization,
    needsConferenceHall,
    stayPreference,
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitError(null);

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    const payload = {
      sender_name: fullName.trim(),
      sender_contact: contact.trim(),
      raw_message: buildRawMessage(),
    };

    try {
      const response = await submitPublicEnquiry(payload);
      setSuccessData(response);
    } catch (err) {
      setSubmitError(err.message || 'We could not dispatch your enquiry. Please check your network and retry.');
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

  const handleResetForm = () => {
    setSuccessData(null);
    setSubmitError(null);
    setErrors({});
    setFullName('');
    setContact('');
    setEventDate('');
    setBudget('');
    setAdditionalMessage('');
  };

  return (
    <div className="enquiry-page">
      {/* Hero Header */}
      <section className="enquiry-hero">
        <div className="enquiry-hero__content">
          <span className="enquiry-hero__kicker">Mullayanagiri Foothills, Chikkamagaluru</span>
          <h1 className="enquiry-hero__title">Estate Concierge &amp; Event Enquiries</h1>
          <p className="enquiry-hero__subtitle">
            Whether planning an intimate destination wedding, leadership offsite, or bespoke plantation retreat, our dedicated reservations concierge will craft a tailored proposal.
          </p>
        </div>
      </section>

      <div className="enquiry-container">
        {/* Success View */}
        {successData ? (
          <Card className="enquiry-success-card" padding="large">
            <div className="enquiry-success__header">
              <span className="enquiry-success__badge-icon" aria-hidden="true">✓</span>
              <Badge variant="success">Enquiry Received</Badge>
              <h2 className="enquiry-success__title">Thank You, {fullName || 'Guest'}</h2>
              <p className="enquiry-success__subtitle">
                Your enquiry has been submitted successfully. Our team will review your request and contact you using the details provided. Please note that this enquiry does not reserve rooms or event space. Your reservation is confirmed only after completing the booking process.
              </p>
            </div>

            <div className="enquiry-success__reference-box">
              <span className="enquiry-success__reference-label">Your Unique Reference</span>
              <div className="enquiry-success__reference-row">
                <span className="enquiry-success__reference-code">{successData.enquiry_code}</span>
                <button
                  type="button"
                  className="enquiry-success__copy-btn"
                  onClick={handleCopyCode}
                  title="Copy reference code"
                >
                  {copiedCode ? '✓ Copied' : 'Copy'}
                </button>
              </div>
              <p className="enquiry-success__reference-note">
                Please quote this reference in any follow-up emails or calls with our front desk.
              </p>
            </div>

            <div className="enquiry-success__summary-grid">
              <div className="enquiry-success__summary-item">
                <span className="enquiry-success__meta-label">Enquiry Type</span>
                <span className="enquiry-success__meta-value">
                  {ENQUIRY_TYPES.find((t) => t.id === enquiryType)?.label}
                </span>
              </div>
              <div className="enquiry-success__summary-item">
                <span className="enquiry-success__meta-label">Contact</span>
                <span className="enquiry-success__meta-value">{contact}</span>
              </div>
              <div className="enquiry-success__summary-item">
                <span className="enquiry-success__meta-label">Preferred Date</span>
                <span className="enquiry-success__meta-value">{eventDate}</span>
              </div>
              <div className="enquiry-success__summary-item">
                <span className="enquiry-success__meta-label">Estimated Guests</span>
                <span className="enquiry-success__meta-value">{guestsCount} guests</span>
              </div>
            </div>

            <div className="enquiry-success__timeline">
              <h3 className="enquiry-success__timeline-title">What Happens Next</h3>
              <ol className="enquiry-success__timeline-list">
                <li>
                  <strong>Specialist Review:</strong> Our team will review your request against current estate availability.
                </li>
                <li>
                  <strong>Follow-up &amp; Consultation:</strong> Our team will review your enquiry and may contact you using the details provided to discuss your requirements. Any quotation and availability will be confirmed separately.
                </li>
                <li>
                  <strong>Booking &amp; Confirmation:</strong> Submitting an enquiry does not hold dates or reserve inventory. Rooms and event spaces are confirmed only through the separate booking process.
                </li>
              </ol>
            </div>

            <div className="enquiry-success__actions">
              <Button variant="primary" onClick={handleResetForm}>
                Submit Another Enquiry
              </Button>
              <Link to="/stay" className="btn btn--secondary">
                Explore Rooms &amp; Book
              </Link>
            </div>
          </Card>
        ) : (
          /* Submission Form */
          <form className="enquiry-form" onSubmit={handleSubmit} noValidate>
            {submitError && (
              <ErrorBanner
                title="Submission Interrupted"
                message={submitError}
                onRetry={handleSubmit}
              />
            )}

            {/* Step 1: Enquiry Type Selector */}
            <Card className="enquiry-card" padding="large">
              <div className="enquiry-card__header">
                <span className="enquiry-card__step">Step 1 of 3</span>
                <h2 className="enquiry-card__title">Select Your Occasion</h2>
                <p className="enquiry-card__description">
                  Choose the category that best matches your planned visit so we route your request to the right estate specialist.
                </p>
              </div>

              <div className="enquiry-type-grid" role="radiogroup" aria-label="Occasion Category">
                {ENQUIRY_TYPES.map((type) => {
                  const isSelected = enquiryType === type.id;
                  return (
                    <button
                      key={type.id}
                      type="button"
                      role="radio"
                      aria-checked={isSelected}
                      disabled={isSubmitting}
                      className={`enquiry-type-card ${isSelected ? 'enquiry-type-card--active' : ''}`}
                      onClick={() => handleTypeChange(type.id)}
                    >
                      <span className="enquiry-type-card__icon" aria-hidden="true">{type.icon}</span>
                      <div className="enquiry-type-card__content">
                        <strong className="enquiry-type-card__label">{type.label}</strong>
                        <p className="enquiry-type-card__desc">{type.description}</p>
                      </div>
                      <span className="enquiry-type-card__radio" aria-hidden="true">
                        {isSelected ? '●' : '○'}
                      </span>
                    </button>
                  );
                })}
              </div>
            </Card>

            {/* Step 2: Guest Contact Details */}
            <Card className="enquiry-card" padding="large">
              <div className="enquiry-card__header">
                <span className="enquiry-card__step">Step 2 of 3</span>
                <h2 className="enquiry-card__title">Your Contact Information</h2>
                <p className="enquiry-card__description">
                  Our reservations manager will reach out directly with rate plans and estate availability.
                </p>
              </div>

              <div className="enquiry-form__grid-2">
                <Input
                  label="Full Name *"
                  placeholder="e.g. Ananya Hegde"
                  value={fullName}
                  disabled={isSubmitting}
                  error={errors.fullName}
                  onChange={(e) => {
                    setFullName(e.target.value);
                    if (errors.fullName) setErrors({ ...errors, fullName: null });
                  }}
                  required
                />

                <Input
                  label="Email or Mobile Phone *"
                  placeholder="e.g. ananya@company.com or +91 98765 43210"
                  value={contact}
                  disabled={isSubmitting}
                  error={errors.contact}
                  helperText="We never share your contact details."
                  onChange={(e) => {
                    setContact(e.target.value);
                    if (errors.contact) setErrors({ ...errors, contact: null });
                  }}
                  required
                />
              </div>

              {enquiryType === 'CORPORATE_OFFSITE' && (
                <div style={{ marginTop: 'var(--space-3)' }}>
                  <Input
                    label="Company or Organization Name (Optional)"
                    placeholder="e.g. ThoughtWorks / Infosys Engineering"
                    value={organization}
                    disabled={isSubmitting}
                    onChange={(e) => setOrganization(e.target.value)}
                  />
                </div>
              )}
            </Card>

            {/* Step 3: Event & Stay Specifications */}
            <Card className="enquiry-card" padding="large">
              <div className="enquiry-card__header">
                <span className="enquiry-card__step">Step 3 of 3</span>
                <h2 className="enquiry-card__title">
                  {enquiryType === 'WEDDING' && 'Wedding & Celebration Details'}
                  {enquiryType === 'CORPORATE_OFFSITE' && 'Retreat & Offsite Specifications'}
                  {enquiryType === 'ROOM_BOOKING' && 'Room Booking & Stay Preferences'}
                </h2>
                <p className="enquiry-card__description">
                  Approximate figures help us calculate inventory allotment and quote authoritative rates.
                </p>
              </div>

              {/* Conditional Field: Stay Preference for Room Booking */}
              {enquiryType === 'ROOM_BOOKING' && (
                <div className="enquiry-field-group">
                  <label className="input__label" htmlFor="stay-pref">Accommodation Preference</label>
                  <select
                    id="stay-pref"
                    className="input__field enquiry-select"
                    value={stayPreference}
                    disabled={isSubmitting}
                    onChange={(e) => setStayPreference(e.target.value)}
                  >
                    <option value="VILLA">Private Pool Villa</option>
                    <option value="ROOM">Estate Room</option>
                    <option value="BUYOUT">Full Estate Buyout (64 Rooms + 12 Villas)</option>
                  </select>
                </div>
              )}

              <div className="enquiry-form__grid-2">
                <Input
                  label={enquiryType === 'ROOM_BOOKING' ? 'Preferred Check-in Date *' : 'Target Event Date *'}
                  type="date"
                  value={eventDate}
                  disabled={isSubmitting}
                  error={errors.eventDate}
                  onChange={(e) => {
                    setEventDate(e.target.value);
                    if (errors.eventDate) setErrors({ ...errors, eventDate: null });
                  }}
                  required
                />

                <Input
                  label="Number of Nights *"
                  type="number"
                  min="1"
                  max="30"
                  value={nights}
                  disabled={isSubmitting}
                  error={errors.nights}
                  onChange={(e) => {
                    setNights(e.target.value);
                    if (errors.nights) setErrors({ ...errors, nights: null });
                  }}
                  required
                />
              </div>

              <div className="enquiry-form__grid-2" style={{ marginTop: 'var(--space-3)' }}>
                <Input
                  label="Expected Guests Count *"
                  type="number"
                  min="1"
                  max="1000"
                  placeholder={enquiryType === 'WEDDING' ? '200' : enquiryType === 'CORPORATE_OFFSITE' ? '45' : '4'}
                  value={guestsCount}
                  disabled={isSubmitting}
                  error={errors.guestsCount}
                  onChange={(e) => {
                    setGuestsCount(e.target.value);
                    if (errors.guestsCount) setErrors({ ...errors, guestsCount: null });
                  }}
                  required
                />

                <Input
                  label="Estimated Rooms / Keys Needed"
                  type="number"
                  min="1"
                  max="76"
                  placeholder={enquiryType === 'WEDDING' ? '30' : enquiryType === 'CORPORATE_OFFSITE' ? '20' : '2'}
                  value={roomsCount}
                  disabled={isSubmitting}
                  helperText="Total estate capacity: 76 keys."
                  onChange={(e) => setRoomsCount(e.target.value)}
                />
              </div>

              {/* Conditional: Meeting setup for offsite */}
              {enquiryType === 'CORPORATE_OFFSITE' && (
                <div className="enquiry-field-group" style={{ marginTop: 'var(--space-3)' }}>
                  <label className="input__label" htmlFor="conf-hall">Meeting Space &amp; AV Setup</label>
                  <select
                    id="conf-hall"
                    className="input__field enquiry-select"
                    value={needsConferenceHall}
                    disabled={isSubmitting}
                    onChange={(e) => setNeedsConferenceHall(e.target.value)}
                  >
                    <option value="unspecified">Not specified / please advise</option>
                    <option value="yes">Yes — Dedicated meeting space with AV setup</option>
                    <option value="no">No — Outdoor gathering and retreat stay</option>
                  </select>
                </div>
              )}

              {/* Budget */}
              <div style={{ marginTop: 'var(--space-3)' }}>
                <Input
                  label="Approximate Budget in INR (Optional)"
                  placeholder="e.g. ₹5,00,000 or 5 Lakhs"
                  value={budget}
                  disabled={isSubmitting}
                  helperText="Helps us suggest suitable catering and rate packages."
                  onChange={(e) => setBudget(e.target.value)}
                />
              </div>

              {/* Textarea for additional requirements */}
              <div className="enquiry-field-group" style={{ marginTop: 'var(--space-3)' }}>
                <label className="input__label" htmlFor="additional-msg">
                  {enquiryType === 'WEDDING' && 'Ceremonies, Catering & Event Details (Optional)'}
                  {enquiryType === 'CORPORATE_OFFSITE' && 'Workshops, Dining & Team Activities (Optional)'}
                  {enquiryType === 'ROOM_BOOKING' && 'Special Requests or Inquiries (Optional)'}
                </label>
                <textarea
                  id="additional-msg"
                  className="input__field enquiry-textarea"
                  rows="4"
                  disabled={isSubmitting}
                  placeholder={
                    enquiryType === 'WEDDING'
                      ? 'e.g. Looking for outdoor reception on estate lawns on evening 1, followed by morning traditional ceremony.'
                      : enquiryType === 'CORPORATE_OFFSITE'
                      ? 'e.g. Need high-speed Wi-Fi, workshop layout for 40 participants, and estate dining.'
                      : 'e.g. Celebrating a milestone anniversary; please arrange airport transit from Mangalore if possible.'
                  }
                  value={additionalMessage}
                  onChange={(e) => setAdditionalMessage(e.target.value)}
                />
              </div>
            </Card>

            {/* Submission Actions */}
            <div className="enquiry-submit-bar">
              <div className="enquiry-submit-bar__info">
                <span className="enquiry-submit-bar__lock" aria-hidden="true">🔒</span>
                <span>Direct submission to Chikkamagaluru Concierge. No booking fee or payment required.</span>
              </div>
              <Button
                type="submit"
                variant="primary"
                className="enquiry-submit-btn"
                isLoading={isSubmitting}
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Submitting to Concierge...' : 'Submit Estate Enquiry'}
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

export default PublicEnquiryPage;
