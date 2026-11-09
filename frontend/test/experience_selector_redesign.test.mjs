import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

test('Experience Selector Redesign: Photographic assets and category card data', () => {
  const pageSrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // Must use photographic assets
  assert.ok(pageSrc.includes("image: '/images/wedding-lawn.jpg'"), 'Weddings must use wedding-lawn.jpg');
  assert.ok(pageSrc.includes("image: '/images/corporate-dining.jpg'"), 'Offsites must use corporate-dining.jpg');
  assert.ok(pageSrc.includes("image: '/images/coffee-villa.jpg'"), 'Stay must use coffee-villa.jpg');

  // Verify asset files exist on disk
  assert.ok(fs.existsSync('frontend/public/images/wedding-lawn.jpg'), 'wedding-lawn.jpg must exist in public/images');
  assert.ok(fs.existsSync('frontend/public/images/corporate-dining.jpg'), 'corporate-dining.jpg must exist in public/images');
  assert.ok(fs.existsSync('frontend/public/images/coffee-villa.jpg'), 'coffee-villa.jpg must exist in public/images');
});

test('Experience Selector Redesign: URL query mapping and null default for unspecified route', () => {
  const pageSrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // Must start with null when no category is in query params
  assert.ok(
    pageSrc.includes('const mappedType = mapQueryType(rawTypeParam);') &&
    pageSrc.includes('const [enquiryType, setEnquiryType] = useState(mappedType);'),
    'enquiryType state must initialize with mappedType directly (null when ?type= is omitted)'
  );

  // Must not force 'WEDDING' or 'ROOM_BOOKING' when type param is missing
  assert.ok(
    !pageSrc.includes('const initialType = mappedType || \'WEDDING\';'),
    'Must NOT default initialType to WEDDING'
  );
  assert.ok(
    !pageSrc.includes('const initialType = mappedType || \'ROOM_BOOKING\';'),
    'Must NOT default initialType to ROOM_BOOKING'
  );

  // Must support room, stay, wedding, offsite in mapQueryType
  assert.ok(pageSrc.includes("val === 'wedding' || val === 'weddings'"), 'Must map wedding/weddings');
  assert.ok(pageSrc.includes("val === 'offsite' || val === 'offsites'"), 'Must map offsite/offsites');
  assert.ok(pageSrc.includes("val === 'room' || val === 'rooms' || val === 'stay'"), 'Must map room/rooms/stay');
});

test('Experience Selector Redesign: URL synchronization on selection for persistence', () => {
  const pageSrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // handleTypeChange must update searchParams
  assert.ok(
    pageSrc.includes("const newParam = paramMap[typeId];") &&
    pageSrc.includes("setSearchParams({ type: newParam });"),
    'handleTypeChange must synchronize new selection to URL searchParams'
  );

  // useEffect must sync state when searchParams changes (Back/Forward or direct navigation)
  assert.ok(
    pageSrc.includes("const typeParam = searchParams.get('type');") &&
    pageSrc.includes("const mapped = mapQueryType(typeParam);") &&
    pageSrc.includes("setEnquiryType(mapped);"),
    'useEffect must synchronize enquiryType with searchParams on Back/Forward navigation'
  );
});

test('Experience Selector Redesign: Separation of journeys and Stay portal link', () => {
  const pageSrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // When Stay is selected, must provide direct link into the existing availability and room-booking flow (/stay)
  assert.ok(
    pageSrc.includes('to="/stay"') && pageSrc.includes('Explore Rooms &amp; Rates &rarr;'),
    'Stay journey must lead into the existing availability and room-booking flow (/stay)'
  );

  // Must ask guest to choose when no category is selected
  assert.ok(
    pageSrc.includes('Please select an occasion above to proceed with your enquiry.'),
    'Must prompt guest to select an occasion when no category is specified'
  );

  // Step 2 & Step 3 must be conditionally rendered when an occasion is chosen
  assert.ok(
    pageSrc.includes('{enquiryType && ('),
    'Form sections must only display once an experience is chosen'
  );
});

test('Experience Selector Redesign: Truthful section labels (removal of deceptive Step 1 of 3 wizard)', () => {
  const pageSrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // Must NOT claim to be a sequential 3-step wizard while displaying multiple steps simultaneously
  assert.ok(
    !pageSrc.includes('Step 1 of 3'),
    'Must remove deceptive "Step 1 of 3" badge'
  );
  assert.ok(
    !pageSrc.includes('Step 2 of 3'),
    'Must remove deceptive "Step 2 of 3" badge'
  );
  assert.ok(
    !pageSrc.includes('Step 3 of 3'),
    'Must remove deceptive "Step 3 of 3" badge'
  );
});

test('Experience Selector Redesign: Responsive photographic CSS styling', () => {
  const css = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.css', 'utf8');

  // Desktop 3 columns, mobile 1 column
  assert.ok(
    css.includes('.enquiry-type-grid {\n  display: grid;\n  grid-template-columns: 1fr;') ||
    css.includes('.enquiry-type-grid {\r\n  display: grid;\r\n  grid-template-columns: 1fr;'),
    'Mobile enquiry-type-grid must be 1fr'
  );
  assert.ok(
    css.includes('grid-template-columns: repeat(3, 1fr);'),
    'Desktop enquiry-type-grid must be repeat(3, 1fr)'
  );

  // Distinct dark green brand selected state
  assert.ok(
    css.includes('.enquiry-type-card--active {\n  border: 2px solid var(--color-accent);') ||
    css.includes('.enquiry-type-card--active {\r\n  border: 2px solid var(--color-accent);'),
    'Active card must have 2px brand accent border'
  );
  assert.ok(
    css.includes('.enquiry-type-card__indicator--selected'),
    'Must style selection indicator when active'
  );
  assert.ok(
    css.includes('.enquiry-type-card__media') && css.includes('.enquiry-type-card__img'),
    'Must style photographic card media and image'
  );
});
