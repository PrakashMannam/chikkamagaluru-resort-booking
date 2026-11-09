import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {
  buildRawEnquiryMessage,
  DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION,
  DEFAULT_ACCOMMODATION_CHOICE,
} from '../src/utils/enquiryMessageBuilder.js';

test('Event Enquiry: DEFAULT_ACCOMMODATION_CHOICE is "not_sure"', () => {
  assert.equal(
    DEFAULT_ACCOMMODATION_CHOICE,
    'not_sure',
    'Default accommodation choice for events must be "not_sure"'
  );
});

test('Event Enquiry Message: Wedding without accommodation omits nights/rooms', () => {
  // 1. Explicit 'no' accommodation
  const noStayMsg = buildRawEnquiryMessage({
    enquiryType: 'WEDDING',
    eventDate: '2026-12-15',
    guestsCount: '200',
    accommodationChoice: 'no',
    additionalMessage: 'Mandap on lawn',
  });
  assert.ok(
    noStayMsg.includes('Day event only (no overnight accommodation required)'),
    'Wedding message with accommodation="no" must state day event only'
  );
  assert.ok(
    !noStayMsg.includes('nights'),
    'Wedding message with accommodation="no" must not mention nights'
  );
  assert.ok(
    !noStayMsg.includes('rooms'),
    'Wedding message with accommodation="no" must not mention rooms'
  );
  assert.ok(
    noStayMsg.includes('about 200 guests'),
    'Must include guest count'
  );

  // 2. 'not_sure' accommodation
  const notSureMsg = buildRawEnquiryMessage({
    enquiryType: 'WEDDING',
    eventDate: '2026-12-15',
    guestsCount: '180',
    accommodationChoice: 'not_sure',
  });
  assert.ok(
    notSureMsg.includes('Overnight accommodation: to be decided'),
    'Wedding message with accommodation="not_sure" must indicate accommodation is to be decided'
  );
  assert.ok(
    !notSureMsg.includes('nights') && !notSureMsg.includes('rooms'),
    'Undecided accommodation must omit night and room numbers'
  );

  // 3. Explicit 'yes' accommodation
  const yesStayMsg = buildRawEnquiryMessage({
    enquiryType: 'WEDDING',
    eventDate: '2026-12-15',
    guestsCount: '200',
    nights: '2',
    roomsCount: '30',
    accommodationChoice: 'yes',
  });
  assert.ok(
    yesStayMsg.includes('2 nights') && yesStayMsg.includes('30 rooms'),
    'Wedding with accommodation="yes" must include nights and rooms count'
  );
});

test('Event Enquiry Message: Corporate offsite without accommodation omits nights/rooms', () => {
  // 1. Day offsite (accommodation="no")
  const dayOffsiteMsg = buildRawEnquiryMessage({
    enquiryType: 'CORPORATE_OFFSITE',
    organization: 'ThoughtWorks',
    eventDate: '2026-12-10',
    guestsCount: '50',
    accommodationChoice: 'no',
    needsConferenceHall: 'yes',
  });
  assert.ok(
    dayOffsiteMsg.includes('Day event only (no overnight accommodation required)'),
    'Corporate offsite with accommodation="no" must state day event only'
  );
  assert.ok(
    !dayOffsiteMsg.includes('nights') && !dayOffsiteMsg.includes('rooms'),
    'Day offsite must not include night or room numbers'
  );
  assert.ok(
    dayOffsiteMsg.includes('with a conference hall'),
    'Must preserve conference hall requirement'
  );

  // 2. Offsite with accommodation ("yes")
  const residentialOffsiteMsg = buildRawEnquiryMessage({
    enquiryType: 'CORPORATE_OFFSITE',
    organization: 'Infosys',
    eventDate: '2026-12-10',
    guestsCount: '40',
    nights: '3',
    roomsCount: '25',
    accommodationChoice: 'yes',
  });
  assert.ok(
    residentialOffsiteMsg.includes('for 3 nights') && residentialOffsiteMsg.includes('with 25 rooms'),
    'Residential offsite must include nights and rooms'
  );
});

test('Event Enquiry Message: Room booking always includes stay details', () => {
  const roomMsg = buildRawEnquiryMessage({
    enquiryType: 'ROOM_BOOKING',
    eventDate: '2026-11-20',
    nights: '3',
    guestsCount: '2',
    roomsCount: '1',
    stayPreference: 'VILLA',
  });
  assert.ok(
    roomMsg.includes('for 3 nights'),
    'Room booking must always include nights'
  );
  assert.ok(
    roomMsg.includes('1 rooms'),
    'Room booking must always include rooms count'
  );
  assert.ok(
    roomMsg.includes('Private Pool Villa'),
    'Room booking must include stay preference label'
  );
});

test('Source Contract: PublicEnquiryPage route-specific content & umbrella heading removal', () => {
  const pageSrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // Must have getPageContent function handling wedding, offsite, room, and generic routes
  assert.ok(
    pageSrc.includes('function getPageContent('),
    'PublicEnquiryPage must implement getPageContent helper'
  );
  assert.ok(
    pageSrc.includes('Weddings & Celebrations Enquiry'),
    'Must define wedding-specific heading'
  );
  assert.ok(
    pageSrc.includes('Corporate Offsites & Leadership Retreats'),
    'Must define corporate offsite-specific heading'
  );
  assert.ok(
    pageSrc.includes('Estate Stays & Room Enquiries'),
    'Must define room booking-specific heading'
  );

  // Must have back links to parent event pages
  assert.ok(
    pageSrc.includes("backLink: '/weddings'") && pageSrc.includes("backLabel: '← Back to Weddings'"),
    'Must link back to /weddings on wedding route'
  );
  assert.ok(
    pageSrc.includes("backLink: '/offsites'") && pageSrc.includes("backLabel: '← Back to Offsites'"),
    'Must link back to /offsites on offsite route'
  );

  // Must not have misleading "Total estate capacity: 76 keys." helper on event enquiry
  assert.ok(
    !pageSrc.includes('Total estate capacity: 76 keys.'),
    'Must remove misleading 76-key capacity helper from event enquiry'
  );

  // Must not promise authoritative rates or allotment calculation
  assert.ok(
    !pageSrc.includes('calculate inventory allotment and quote authoritative rates'),
    'Must remove misleading authoritative rate and allotment promise'
  );

  // Must have explicit 3-option accommodation choice: Yes / No / Not sure yet
  assert.ok(
    pageSrc.includes("id: 'yes', label: 'Yes'"),
    'Accommodation choice must include Yes'
  );
  assert.ok(
    pageSrc.includes("id: 'no', label: 'No'"),
    'Accommodation choice must include No'
  );
  assert.ok(
    pageSrc.includes("id: 'not_sure', label: 'Not sure yet'"),
    'Accommodation choice must include Not sure yet'
  );

  // Must conditionally require nights only when accommodation is requested or for room booking
  assert.ok(
    pageSrc.includes('requiresAccommodation = enquiryType === \'ROOM_BOOKING\' || accommodationChoice === \'yes\''),
    'Validation must only require nights when accommodation is yes or room booking'
  );
});

test('Source Contract: PublicEnquiryPage cleans stale values on accommodation change', () => {
  const pageSrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // Check handleAccommodationChoiceChange clears nights and roomsCount on no or not_sure
  assert.ok(
    pageSrc.includes("setNights('')"),
    'handleAccommodationChoiceChange must clear nights when accommodation not requested'
  );
  assert.ok(
    pageSrc.includes("setRoomsCount('')"),
    'handleAccommodationChoiceChange must clear roomsCount when accommodation not requested'
  );
  assert.ok(
    pageSrc.includes("delete next.nights") && pageSrc.includes("delete next.roomsCount"),
    'handleAccommodationChoiceChange must clear stale validation errors'
  );
});
