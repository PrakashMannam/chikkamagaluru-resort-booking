import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {
  buildRawEnquiryMessage,
  DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION,
  DEFAULT_ACCOMMODATION_CHOICE,
  TOTAL_ESTATE_INVENTORY_KEYS,
  TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS,
} from '../src/utils/enquiryMessageBuilder.js';

test('Event Enquiry: DEFAULT_ACCOMMODATION_CHOICE is empty string initially', () => {
  assert.equal(
    DEFAULT_ACCOMMODATION_CHOICE,
    '',
    'Initial accommodation choice must be empty so user explicitly chooses without prefilled defaults'
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
  assert.ok(
    pageSrc.includes('Estate Concierge & Event Enquiries'),
    'Must define general concierge heading for generic /enquire route'
  );
  assert.ok(
    pageSrc.includes("backLink: '/'") && pageSrc.includes("backLabel: '← Back to Estate'"),
    'Must link back to / on generic /enquire route'
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

test('Source Contract: Form fields start empty without prefilled default numbers', () => {
  const pageSrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // Verify state initialization is completely empty
  assert.ok(
    pageSrc.includes("const [nights, setNights] = useState('');"),
    'nights state must initialize empty'
  );
  assert.ok(
    pageSrc.includes("const [guestsCount, setGuestsCount] = useState('');"),
    'guestsCount state must initialize empty'
  );
  assert.ok(
    pageSrc.includes("const [roomsCount, setRoomsCount] = useState('');"),
    'roomsCount state must initialize empty'
  );

  // Verify handleTypeChange resets counts to empty
  assert.ok(
    pageSrc.includes("setGuestsCount('');") && pageSrc.includes("setNights('');") && pageSrc.includes("setRoomsCount('');"),
    'handleTypeChange must reset counts to empty strings'
  );

  // Verify handleAccommodationChoiceChange does not inject prefilled numbers on "yes"
  assert.ok(
    !pageSrc.includes("setNights('2')"),
    'handleAccommodationChoiceChange must not prefill nights with 2'
  );
  assert.ok(
    !pageSrc.includes("setRoomsCount('30')") && !pageSrc.includes("setRoomsCount('20')"),
    'handleAccommodationChoiceChange must not prefill rooms count'
  );

  // Verify accommodation selection validation
  assert.ok(
    pageSrc.includes("newErrors.accommodationChoice = 'Please select whether your guests will require overnight accommodation"),
    'validateForm must require explicit accommodation choice on events'
  );
});


test('Event Enquiry: Constants define inventory keys (76) and technical guest ceiling (1000)', () => {
  assert.equal(TOTAL_ESTATE_INVENTORY_KEYS, 76, 'Total estate inventory must be 76 keys (64 rooms + 12 pool villas)');
  assert.equal(TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS, 1000, 'Temporary technical validation ceiling for guests must be 1000');
});

test('Source Contract: Bounded guest count and room count inputs with inventory wording', () => {
  const pageSrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // Verify guest count validation logic
  assert.ok(
    pageSrc.includes("trimmedGuests.includes('.')") && pageSrc.includes("'Guest count must be a positive whole number.'"),
    'Guests count validation must reject decimals and non-numeric characters'
  );
  assert.ok(
    pageSrc.includes('parsedGuests < 1'),
    'Guests count validation must require at least 1'
  );
  assert.ok(
    pageSrc.includes('parsedGuests > TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS'),
    'Guests count validation must enforce TEMPORARY_TECHNICAL_MAX_EVENT_GUESTS ceiling'
  );
  assert.ok(
    pageSrc.includes('technical limit of'),
    'Guest count error must refer to technical limit, NOT venue capacity'
  );

  // Verify rooms count validation logic
  assert.ok(
    pageSrc.includes('parsedRooms > TOTAL_ESTATE_INVENTORY_KEYS'),
    'Rooms count validation must enforce TOTAL_ESTATE_INVENTORY_KEYS cap'
  );
  assert.ok(
    pageSrc.includes('total estate inventory of ${TOTAL_ESTATE_INVENTORY_KEYS} keys'),
    'Rooms count error must refer to total estate inventory, not availability or venue capacity'
  );

  // Verify helper text treats 76 as estate inventory across 64 rooms and 12 villas
  assert.ok(
    pageSrc.includes('Total estate inventory is 76 keys across 64 rooms and 12 villas.'),
    'Must display accurate inventory helper text'
  );
  assert.ok(
    pageSrc.includes('Allocation and availability for your dates will be confirmed by your coordinator'),
    'Helper text must clarify allocation/availability is confirmed by coordinator'
  );

  // Verify no publication of unsubstantiated "maximum venue capacity"
  assert.ok(
    !pageSrc.toLowerCase().includes('maximum venue capacity'),
    'Must NOT publish unverified maximum venue capacity claim'
  );
  assert.ok(
    !pageSrc.toLowerCase().includes('maximum event capacity'),
    'Must NOT publish unverified maximum event capacity claim'
  );

  // Verify keypress blocking of invalid characters
  assert.ok(
    pageSrc.includes("['e', 'E', '+', '-', '.'].includes(e.key)"),
    'Numeric inputs must block exponential, sign, and decimal characters on keydown'
  );
});

test('Audit Refinements: CTA dimensions, 44px tap targets, and overflow guards', () => {
  const landingJsx = fs.readFileSync('frontend/src/pages/LandingPage.jsx', 'utf8');
  const landingCss = fs.readFileSync('frontend/src/pages/LandingPage.css', 'utf8');
  const enquiryCss = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.css', 'utf8');
  const guestLayoutCss = fs.readFileSync('frontend/src/components/layout/GuestLayout.css', 'utf8');

  // 1. Bottom CTA Banner image has explicit dimensions (prevents CLS)
  assert.ok(
    landingJsx.includes('width="800"') && landingJsx.includes('height="400"'),
    'Bottom CTA banner image must have explicit width="800" and height="400"'
  );

  // 2. Landing search inputs & selects have 44px (2.75rem) minimum touch height
  assert.ok(
    landingCss.includes('min-height: 2.75rem;'),
    'Landing search inputs must enforce at least 2.75rem (44px) touch target'
  );

  // 3. Card circle buttons are 44px (2.75rem)
  assert.ok(
    landingCss.includes('width: 2.75rem;') && landingCss.includes('height: 2.75rem;'),
    'Stay and event card action buttons must be 44px (2.75rem)'
  );

  // 4. Footer link tap target is 44px
  assert.ok(
    guestLayoutCss.includes('min-height: 2.75rem;'),
    'Footer links must enforce at least 2.75rem (44px) tap target'
  );

  // 5. Enquiry success copy button is 44px
  assert.ok(
    enquiryCss.includes('.enquiry-success__copy-btn') && enquiryCss.includes('min-height: 44px;'),
    'Enquiry success copy button must have min-height: 44px'
  );

  // 6. Enquiry page has overflow-x: hidden guard
  assert.ok(
    enquiryCss.includes('overflow-x: hidden;'),
    'Enquiry page must have overflow-x: hidden guard to prevent sub-pixel mobile overflow'
  );
});
