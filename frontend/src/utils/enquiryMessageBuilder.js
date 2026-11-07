/**
 * enquiryMessageBuilder.js
 * Pure utility functions for building guest enquiry messages and managing default form options.
 */

export const DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION = 'unspecified';

/**
 * Builds the composite raw message string submitted to the concierge/enquiry API.
 *
 * @param {Object} params
 * @param {string} params.enquiryType - 'WEDDING' | 'CORPORATE_OFFSITE' | 'ROOM_BOOKING'
 * @param {string} [params.eventDate] - ISO date string or formatted date
 * @param {string|number} [params.nights] - Number of nights
 * @param {string|number} [params.guestsCount] - Number of guests / attendees
 * @param {string|number} [params.roomsCount] - Number of rooms requested
 * @param {string} [params.budget] - Guest budget note
 * @param {string} [params.additionalMessage] - Specific requirements
 * @param {string} [params.organization] - Company or organizing body name
 * @param {string} [params.needsConferenceHall] - 'unspecified' | 'no' | 'yes'
 * @param {string} [params.stayPreference] - 'VILLA' | 'ROOM'
 * @returns {string} Sanitized formatted message
 */
export function buildRawEnquiryMessage({
  enquiryType,
  eventDate,
  nights,
  guestsCount,
  roomsCount,
  budget,
  additionalMessage,
  organization,
  needsConferenceHall = DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION,
  stayPreference = 'VILLA',
} = {}) {
  if (enquiryType === 'WEDDING') {
    const parts = [
      `Venue for my wedding on ${eventDate}`,
      nights ? `${nights} nights` : null,
      guestsCount ? `about ${guestsCount} guests` : null,
      roomsCount ? `${roomsCount} rooms` : null,
      budget ? `Approximate budget: ${budget}` : null,
      additionalMessage ? `Event details: ${additionalMessage}` : null,
    ].filter(Boolean);
    return parts.join(', ');
  }

  if (enquiryType === 'CORPORATE_OFFSITE') {
    const parts = [
      organization ? `Corporate offsite for ${organization}` : `Corporate offsite inquiry`,
      guestsCount ? `Team of ${guestsCount}` : null,
      `wants an offsite on ${eventDate}`,
      nights ? `for ${nights} nights` : null,
      roomsCount ? `with ${roomsCount} rooms` : null,
      needsConferenceHall === 'yes' ? 'with a conference hall' : null,
      budget ? `Approximate budget: ${budget}` : null,
      additionalMessage ? `Requirements: ${additionalMessage}` : null,
    ].filter(Boolean);
    return parts.join(', ');
  }

  // Room Booking / Stay
  const stayLabel = stayPreference === 'VILLA' ? 'Private Pool Villa' : stayPreference === 'ROOM' ? 'Estate Room' : 'Estate Stay';
  const parts = [
    `Enquiry for ${stayLabel} from ${eventDate}`,
    nights ? `for ${nights} nights` : null,
    guestsCount ? `for ${guestsCount} guests` : null,
    roomsCount ? `${roomsCount} rooms` : null,
    budget ? `Approximate budget: ${budget}` : null,
    additionalMessage ? `Details: ${additionalMessage}` : null,
  ].filter(Boolean);
  return parts.join(', ');
}
