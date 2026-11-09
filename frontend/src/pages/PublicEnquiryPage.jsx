import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { submitPublicEnquiry } from '../api/enquiryApi';
import { Button, Input, Card, Badge, ErrorBanner } from '../components/ui';
import {
  buildRawEnquiryMessage,
  DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION,
  DEFAULT_ACCOMMODATION_CHOICE,
  TOTAL_ESTATE_INVENTORY_KEYS,
  TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS,
} from '../utils/enquiryMessageBuilder';
import './PublicEnquiryPage.css';

const ENQUIRY_TYPES = [
  {
    id: 'WEDDING',
    label: 'Weddings & Celebrations',
    description: 'Sacred ceremonies, receptions, and gatherings across scenic estate lawns.',
    image: '/images/wedding-lawn.jpg',
  },
  {
    id: 'CORPORATE_OFFSITE',
    label: 'Corporate Offsites',
    description: 'Executive retreats, strategy sessions, and team offsites amidst coffee gardens.',
    image: '/images/corporate-dining.jpg',
  },
  {
    id: 'ROOM_BOOKING',
    label: 'Estate Stays',
    description: 'Private pool villas and heritage canopy rooms for restful coffee estate stays.',
    image: '/images/coffee-villa.jpg',
  },
];

function mapQueryType(param) {
  if (!param) return null;
  const val = param.toLowerCase().trim();
  if (val === 'wedding' || val === 'weddings') return 'WEDDING';
  if (val === 'offsite' || val === 'offsites' || val === 'corporate' || val === 'corporate_offsite') return 'CORPORATE_OFFSITE';
  if (val === 'room' || val === 'rooms' || val === 'stay' || val === 'room_booking' || val === 'general') return 'ROOM_BOOKING';
  return null;
}

function getPageContent(enquiryType, rawTypeParam) {
  const normParam = rawTypeParam ? rawTypeParam.toLowerCase().trim() : '';

  // 1. Wedding route: /enquire?type=wedding
  if (normParam === 'wedding' || normParam === 'weddings') {
    return {
      isRouteSpecific: true,
      kicker: 'Mullayanagiri Foothills, Chikkamagaluru',
      title: 'Weddings & Celebrations Enquiry',
      subtitle: 'Plan your sacred ceremonies and destination celebrations amidst coffee gardens and Western Ghats panoramas. Our wedding specialists will craft a tailored proposal.',
      backLink: '/weddings',
      backLabel: '← Back to Weddings',
      step3Title: 'Wedding & Celebration Details',
      step3Desc: 'Sharing your estimated date and guest count helps our wedding specialists check venue availability and prepare a tailored proposal.',
    };
  }

  // 2. Offsite route: /enquire?type=offsite
  if (normParam === 'offsite' || normParam === 'offsites' || normParam === 'corporate' || normParam === 'corporate_offsite') {
    return {
      isRouteSpecific: true,
      kicker: 'Mullayanagiri Foothills, Chikkamagaluru',
      title: 'Corporate Offsites & Leadership Retreats',
      subtitle: 'Design focused executive programs, team offsites, and strategy retreats on our coffee estate. Our corporate coordinator will tailor dates and venue spaces to your schedule.',
      backLink: '/offsites',
      backLabel: '← Back to Offsites',
      step3Title: 'Retreat & Offsite Specifications',
      step3Desc: 'Sharing your estimated date and attendee count helps our coordinator check conference facilities and prepare a tailored proposal.',
    };
  }

  // 3. Room booking route: /enquire?type=room
  if (normParam === 'room' || normParam === 'rooms' || normParam === 'stay') {
    return {
      isRouteSpecific: true,
      kicker: 'Mullayanagiri Foothills, Chikkamagaluru',
      title: 'Estate Stays & Room Enquiries',
      subtitle: 'For custom stay requests, family getaways, and extended plantation retreats, our reservations team will assist with your reservation.',
      backLink: '/stay',
      backLabel: '← Back to Stay',
      step3Title: 'Room Booking & Stay Preferences',
      step3Desc: 'Sharing your stay dates and guest count helps our reservations team check cottage and villa availability.',
    };
  }

  // 4. General / Umbrella enquiry: /enquire (no route parameter)
  return {
    isRouteSpecific: false,
    kicker: 'Mullayanagiri Foothills, Chikkamagaluru',
    title: 'Estate Concierge & Event Enquiries',
    subtitle: 'Whether planning an intimate destination wedding, leadership offsite, or bespoke plantation retreat, our dedicated reservations concierge will craft a tailored proposal.',
    backLink: '/',
    backLabel: '← Back to Estate',
    step3Title: enquiryType === 'WEDDING'
      ? 'Wedding & Celebration Details'
      : enquiryType === 'CORPORATE_OFFSITE'
      ? 'Retreat & Offsite Specifications'
      : 'Room Booking & Stay Preferences',
    step3Desc: enquiryType === 'WEDDING'
      ? 'Sharing your estimated date and guest count helps our wedding specialists check venue availability and prepare a tailored proposal.'
      : enquiryType === 'CORPORATE_OFFSITE'
      ? 'Sharing your estimated date and attendee count helps our coordinator check conference facilities and prepare a tailored proposal.'
      : 'Sharing your stay dates and guest count helps our reservations team check cottage and villa availability.',
  };
}

export function PublicEnquiryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const rawTypeParam = searchParams.get('type');
  const mappedType = mapQueryType(rawTypeParam);

  // When no category is specified, start with no selection and ask the guest to choose
  const [enquiryType, setEnquiryType] = useState(mappedType);

  // Contact fields
  const [fullName, setFullName] = useState('');
  const [contact, setContact] = useState('');

  // Event / Stay details
  const [eventDate, setEventDate] = useState('');
  const [accommodationChoice, setAccommodationChoice] = useState(
    mappedType === 'ROOM_BOOKING' ? 'yes' : DEFAULT_ACCOMMODATION_CHOICE
  );
  const [nights, setNights] = useState('');
  const [guestsCount, setGuestsCount] = useState('');
  const [roomsCount, setRoomsCount] = useState('');
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

  const pageContent = getPageContent(enquiryType, rawTypeParam);

  // Switch type and reset form counts
  const handleTypeChange = (typeId) => {
    const paramMap = {
      'WEDDING': 'wedding',
      'CORPORATE_OFFSITE': 'offsite',
      'ROOM_BOOKING': 'room',
    };
    const newParam = paramMap[typeId];
    if (newParam) {
      setSearchParams({ type: newParam });
    }
    setEnquiryType(typeId);
    setErrors({});
    setSubmitError(null);
    setGuestsCount('');
    setNights('');
    setRoomsCount('');
    setAccommodationChoice(typeId === 'ROOM_BOOKING' ? 'yes' : DEFAULT_ACCOMMODATION_CHOICE);
    if (typeId === 'CORPORATE_OFFSITE') {
      setNeedsConferenceHall(DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION);
    }
  };

  // Switch accommodation choice for events
  const handleAccommodationChoiceChange = (choice) => {
    setAccommodationChoice(choice);
    if (choice === 'no' || choice === 'not_sure') {
      setNights('');
      setRoomsCount('');
      setErrors((prev) => {
        const next = { ...prev };
        delete next.nights;
        delete next.roomsCount;
        delete next.accommodationChoice;
        return next;
      });
    } else {
      setErrors((prev) => {
        const next = { ...prev };
        delete next.accommodationChoice;
        return next;
      });
    }
  };

  // Sync selection state if URL query params change dynamically (Back/Forward or direct navigation)
  useEffect(() => {
    const typeParam = searchParams.get('type');
    const mapped = mapQueryType(typeParam);
    setEnquiryType(mapped);
    if (mapped === 'ROOM_BOOKING') {
      setAccommodationChoice('yes');
    } else if (mapped) {
      setAccommodationChoice(DEFAULT_ACCOMMODATION_CHOICE);
    }
  }, [searchParams]);

  const validateForm = () => {
    const newErrors = {};

    if (!enquiryType) {
      newErrors.enquiryType = 'Please select your planned experience (Weddings, Corporate Offsites, or Estate Stays).';
    }

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
      newErrors.eventDate = enquiryType === 'ROOM_BOOKING' ? 'Please select your preferred check-in date.' : 'Please select your preferred event date.';
    }

    // Guest count validation (positive whole number within technical ceiling)
    const trimmedGuests = String(guestsCount).trim();
    if (!trimmedGuests) {
      newErrors.guestsCount = enquiryType === 'ROOM_BOOKING' ? 'Please specify number of guests.' : 'Please specify expected number of attendees.';
    } else if (trimmedGuests.includes('.') || !/^\d+$/.test(trimmedGuests)) {
      newErrors.guestsCount = 'Guest count must be a positive whole number.';
    } else {
      const parsedGuests = parseInt(trimmedGuests, 10);
      if (parsedGuests < 1) {
        newErrors.guestsCount = 'Expected guest count must be at least 1.';
      } else if (parsedGuests > TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS) {
        newErrors.guestsCount = `Expected guest count cannot exceed technical limit of ${TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS}.`;
      }
    }

    if (!isRoomBooking && !accommodationChoice) {
      newErrors.accommodationChoice = 'Please select whether your guests will require overnight accommodation (Yes, No, or Not sure yet).';
    }

    const requiresAccommodation = enquiryType === 'ROOM_BOOKING' || accommodationChoice === 'yes';
    if (requiresAccommodation) {
      const parsedNights = parseInt(nights, 10);
      if (!nights || isNaN(parsedNights) || parsedNights < 1 || String(nights).includes('.')) {
        newErrors.nights = 'Number of nights must be at least 1.';
      }

      // Rooms count validation: required if accommodationChoice is 'yes'
      if (accommodationChoice === 'yes') {
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
      } else if (isRoomBooking && roomsCount) {
        const trimmedRooms = String(roomsCount).trim();
        if (trimmedRooms.includes('.') || !/^\d+$/.test(trimmedRooms)) {
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
    accommodationChoice,
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitError(null);

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    const parsedGuests = parseInt(guestsCount, 10);
    const parsedRooms = showAccommodationInputs && roomsCount ? parseInt(roomsCount, 10) : null;
    const payload = {
      sender_name: fullName.trim(),
      sender_contact: contact.trim(),
      raw_message: buildRawMessage(),
      expected_guests: Number.isFinite(parsedGuests) ? parsedGuests : null,
      rooms_count: Number.isFinite(parsedRooms) ? parsedRooms : null,
      accommodation_choice: !isRoomBooking ? accommodationChoice : 'yes',
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
    setNights('');
    setGuestsCount('');
    setRoomsCount('');
    setAccommodationChoice(enquiryType === 'ROOM_BOOKING' ? 'yes' : DEFAULT_ACCOMMODATION_CHOICE);
    setBudget('');
    setAdditionalMessage('');
  };

  const isRoomBooking = enquiryType === 'ROOM_BOOKING';
  const showAccommodationInputs = isRoomBooking || accommodationChoice === 'yes';

  return (
    <div className="enquiry-page">
      {/* Route-Aware Utility Navigation */}
      <div className="enquiry-top-nav">
        <Link to={pageContent.backLink} className="enquiry-back-link">
          {pageContent.backLabel}
        </Link>
      </div>

      {/* Hero Header */}
      <section className="enquiry-hero">
        <div className="enquiry-hero__content">
          <span className="enquiry-hero__kicker">{pageContent.kicker}</span>
          <h1 className="enquiry-hero__title">{pageContent.title}</h1>
          <p className="enquiry-hero__subtitle">{pageContent.subtitle}</p>
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
              {!isRoomBooking && (
                <div className="enquiry-success__summary-item">
                  <span className="enquiry-success__meta-label">Accommodation</span>
                  <span className="enquiry-success__meta-value">
                    {accommodationChoice === 'no'
                      ? 'Day event only (no stay)'
                      : accommodationChoice === 'not_sure'
                      ? 'Undecided / to be discussed'
                      : `${nights} nights (${roomsCount ? roomsCount + ' rooms' : 'rooms TBD'})`}
                  </span>
                </div>
              )}
            </div>

            <div className="enquiry-success__timeline">
              <h3 className="enquiry-success__timeline-title">What Happens Next</h3>
              <ol className="enquiry-success__timeline-list">
                <li>
                  <strong>Specialist Review:</strong> Our team will review your request against current estate schedule and venue availability.
                </li>
                <li>
                  <strong>Follow-up &amp; Consultation:</strong> Our team will review your enquiry and contact you using the details provided to discuss your requirements. Any quotation and availability will be confirmed separately.
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

            {/* Occasion Category Selector */}
            <Card className="enquiry-card" padding="large">
              <div className="enquiry-card__header">
                <span className="enquiry-card__step">Select Experience</span>
                <h2 className="enquiry-card__title">Choose Your Occasion</h2>
                <p className="enquiry-card__description">
                  Select the experience that best matches your visit to open your tailored enquiry or booking options.
                </p>
              </div>

              {errors.enquiryType && (
                <div className="enquiry-card-error" role="alert" style={{ marginBottom: 'var(--space-3)', color: 'var(--color-error)' }}>
                  {errors.enquiryType}
                </div>
              )}

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
                      <div className="enquiry-type-card__media">
                        <img
                          src={type.image}
                          alt={type.label}
                          className="enquiry-type-card__img"
                          loading="eager"
                          decoding="async"
                          width="600"
                          height="400"
                        />
                      </div>
                      <div className="enquiry-type-card__body">
                        <div className="enquiry-type-card__title-row">
                          <h3 className="enquiry-type-card__label">{type.label}</h3>
                          <span
                            className={`enquiry-type-card__indicator ${isSelected ? 'enquiry-type-card__indicator--selected' : ''}`}
                            aria-hidden="true"
                          >
                            {isSelected ? '✓' : ''}
                          </span>
                        </div>
                        <p className="enquiry-type-card__desc">{type.description}</p>
                      </div>
                    </button>
                  );
                })}
              </div>
            </Card>

            {/* Prompt when no category is selected yet */}
            {!enquiryType && (
              <div className="enquiry-unselected-prompt">
                <p>Please select an occasion above to proceed with your enquiry.</p>
              </div>
            )}

            {/* If Stay is selected, provide direct instant reservation portal link */}
            {isRoomBooking && (
              <div className="enquiry-stay-direct-banner">
                <div>
                  <h3 className="enquiry-stay-direct-banner__title">Looking for Immediate Room Bookings?</h3>
                  <p className="enquiry-stay-direct-banner__desc">
                    Explore live daily rates, view available dates, and confirm your villa or estate room booking immediately.
                  </p>
                </div>
                <Link to="/stay" className="btn btn--secondary enquiry-stay-direct-btn">
                  Explore Rooms &amp; Rates &rarr;
                </Link>
              </div>
            )}

            {enquiryType && (
            <>
              {/* Step 2: Guest Contact Details */}
            <Card className="enquiry-card" padding="large">
              <div className="enquiry-card__header">
                <span className="enquiry-card__step">Contact Details</span>
                <h2 className="enquiry-card__title">Your Contact Information</h2>
                <p className="enquiry-card__description">
                  {isRoomBooking
                    ? 'Our reservations team will reach out directly with rate plans and estate availability.'
                    : 'Our event specialist will reach out directly to discuss dates, venues, and custom catering arrangements.'}
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
                <span className="enquiry-card__step">Specifications</span>
                <h2 className="enquiry-card__title">{pageContent.step3Title}</h2>
                <p className="enquiry-card__description">{pageContent.step3Desc}</p>
              </div>

              {/* Conditional Field: Stay Preference for Room Booking */}
              {isRoomBooking && (
                <div className="enquiry-field-group" style={{ marginBottom: 'var(--space-3)' }}>
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

              {/* Explicit Accommodation Choice for Events (Weddings & Corporate Offsites) */}
              {!isRoomBooking && (
                <div className="enquiry-field-group" style={{ marginBottom: 'var(--space-4)' }}>
                  <label className="input__label" id="acc-choice-label">
                    Overnight Guest Accommodation
                  </label>
                  <div
                    className="enquiry-accommodation-grid"
                    role="radiogroup"
                    aria-labelledby="acc-choice-label"
                  >
                    {[
                      { id: 'yes', label: 'Yes', desc: 'Rooms required for overnight stay' },
                      { id: 'no', label: 'No', desc: 'Day event only (attendees arrange own stay)' },
                      { id: 'not_sure', label: 'Not sure yet', desc: 'To be decided based on proposal' },
                    ].map((opt) => {
                      const isSelected = accommodationChoice === opt.id;
                      return (
                        <button
                          key={opt.id}
                          type="button"
                          role="radio"
                          aria-checked={isSelected}
                          disabled={isSubmitting}
                          className={`enquiry-acc-option ${isSelected ? 'enquiry-acc-option--active' : ''}`}
                          onClick={() => handleAccommodationChoiceChange(opt.id)}
                        >
                          <span className="enquiry-acc-option__header">
                            <strong className="enquiry-acc-option__title">{opt.label}</strong>
                            <span className="enquiry-acc-option__radio" aria-hidden="true">
                              {isSelected ? '●' : '○'}
                            </span>
                          </span>
                          <span className="enquiry-acc-option__desc">{opt.desc}</span>
                        </button>
                      );
                    })}
                  </div>
                  {errors.accommodationChoice && (
                    <span className="input__error" role="alert" style={{ display: 'block', marginTop: 'var(--space-2)' }}>
                      {errors.accommodationChoice}
                    </span>
                  )}
                </div>
              )}

              {/* Date & Optional Nights Grid */}
              <div className="enquiry-form__grid-2">
                <Input
                  label={isRoomBooking ? 'Preferred Check-in Date *' : 'Target Event Date *'}
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

                {showAccommodationInputs && (
                  <Input
                    label="Number of Nights *"
                    type="number"
                    min="1"
                    max="30"
                    placeholder="e.g. 2"
                    value={nights}
                    disabled={isSubmitting}
                    error={errors.nights}
                    onChange={(e) => {
                      setNights(e.target.value);
                      if (errors.nights) setErrors({ ...errors, nights: null });
                    }}
                    required
                  />
                )}
              </div>

              {/* Guest Count & Optional Rooms Grid */}
              <div className="enquiry-form__grid-2" style={{ marginTop: 'var(--space-3)' }}>
                <Input
                  label={isRoomBooking ? 'Number of Guests *' : 'Expected Guests Count *'}
                  type="number"
                  min="1"
                  max={TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS}
                  placeholder={enquiryType === 'WEDDING' ? 'e.g. 150' : enquiryType === 'CORPORATE_OFFSITE' ? 'e.g. 45' : 'e.g. 2'}
                  value={guestsCount}
                  disabled={isSubmitting}
                  error={errors.guestsCount}
                  onKeyDown={(e) => {
                    if (['e', 'E', '+', '-', '.'].includes(e.key)) e.preventDefault();
                  }}
                  onChange={(e) => {
                    setGuestsCount(e.target.value);
                    if (errors.guestsCount) setErrors({ ...errors, guestsCount: null });
                  }}
                  required
                />

                {showAccommodationInputs && (
                  <Input
                    label="Estimated Rooms / Keys Needed"
                    type="number"
                    min="1"
                    max={TOTAL_ESTATE_INVENTORY_KEYS}
                    placeholder={enquiryType === 'WEDDING' ? 'e.g. 30' : enquiryType === 'CORPORATE_OFFSITE' ? 'e.g. 20' : 'e.g. 1'}
                    value={roomsCount}
                    disabled={isSubmitting}
                    error={errors.roomsCount}
                    helperText="Total estate inventory is 76 keys across 64 rooms and 12 villas. (Allocation and availability for your dates will be confirmed by your coordinator.)"
                    onKeyDown={(e) => {
                      if (['e', 'E', '+', '-', '.'].includes(e.key)) e.preventDefault();
                    }}
                    onChange={(e) => {
                      setRoomsCount(e.target.value);
                      if (errors.roomsCount) setErrors({ ...errors, roomsCount: null });
                    }}
                  />
                )}
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
                  label="Approximate Budget / Notes (Optional)"
                  placeholder="e.g. ₹5,00,000 or flexible depending on package"
                  value={budget}
                  disabled={isSubmitting}
                  onChange={(e) => setBudget(e.target.value)}
                />
              </div>

              {/* Additional Requirements */}
              <div className="enquiry-field-group" style={{ marginTop: 'var(--space-3)' }}>
                <label className="input__label" htmlFor="enquiry-notes">
                  Specific Requirements or Message
                </label>
                <textarea
                  id="enquiry-notes"
                  className="input__field enquiry-textarea"
                  rows={4}
                  placeholder={
                    enquiryType === 'WEDDING'
                      ? 'Tell us about ceremonial preferences, catering styles, mandap setup on lawn, decor themes...'
                      : enquiryType === 'CORPORATE_OFFSITE'
                      ? 'Tell us about team size, workshop session schedule, projector/whiteboard needs, outdoor team activities...'
                      : 'Special requests, cottage location preference, anniversary notes...'
                  }
                  value={additionalMessage}
                  disabled={isSubmitting}
                  onChange={(e) => setAdditionalMessage(e.target.value)}
                />
              </div>

              {/* Submit CTA */}
              <div className="enquiry-form__footer" style={{ marginTop: 'var(--space-4)' }}>
                <Button
                  type="submit"
                  variant="primary"
                  size="large"
                  disabled={isSubmitting}
                  className="enquiry-submit-btn"
                >
                  {isSubmitting ? 'Dispatching Enquiry...' : 'Submit Enquiry'}
                </Button>
                <p className="enquiry-form__disclaimer">
                  By submitting this enquiry, you agree to allow our reservations team to contact you regarding your request. We respect your privacy and never share your details with third parties.
                </p>
              </div>
            </Card>
            </>
          )}
          </form>
        )}
      </div>
    </div>
  );
}

export default PublicEnquiryPage;
