import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

test('Dedicated Experiences: App.jsx registers /stay, /weddings, and /offsites routes', () => {
  const appSrc = fs.readFileSync('frontend/src/App.jsx', 'utf8');

  assert.ok(appSrc.includes('path="/stay"'), 'App.jsx must declare /stay route');
  assert.ok(appSrc.includes('path="/weddings"'), 'App.jsx must declare /weddings route');
  assert.ok(appSrc.includes('path="/offsites"'), 'App.jsx must declare /offsites route');
  assert.ok(appSrc.includes('path="/enquire"'), 'App.jsx must maintain /enquire route');
  assert.ok(appSrc.includes('WeddingsPage'), 'App.jsx must import WeddingsPage');
  assert.ok(appSrc.includes('OffsitesPage'), 'App.jsx must import OffsitesPage');
});

test('Dedicated Experiences: GuestLayout header provides dedicated experience navigation', () => {
  const layoutSrc = fs.readFileSync('frontend/src/components/layout/GuestLayout.jsx', 'utf8');

  assert.ok(layoutSrc.includes('to="/stay"'), 'Header must contain link to /stay');
  assert.ok(layoutSrc.includes('to="/weddings"'), 'Header must contain link to /weddings');
  assert.ok(layoutSrc.includes('to="/offsites"'), 'Header must contain link to /offsites');
  assert.ok(layoutSrc.includes('to="/#estate"') || layoutSrc.includes('to="/#about"'), 'Header must contain link to about/estate');
});

test('Dedicated Experiences: GuestLayout footer links directly to /weddings and /offsites and provides DPDP & Form C info', () => {
  const layoutSrc = fs.readFileSync('frontend/src/components/layout/GuestLayout.jsx', 'utf8');

  assert.ok(layoutSrc.includes('to="/weddings"'), 'Footer must link to /weddings');
  assert.ok(layoutSrc.includes('to="/offsites"'), 'Footer must link to /offsites');
  assert.ok(layoutSrc.includes('to="/privacy"'), 'Footer must link to /privacy');
  assert.ok(layoutSrc.includes('to="/terms"'), 'Footer must link to /terms');
  assert.ok(layoutSrc.includes('DPDP 2023 &amp; Form C') || layoutSrc.includes('DPDP 2023'), 'Footer must reference DPDP and Form C');
});

test('Dedicated Experiences: Homepage upgrades to "Choose Your Experience" 3-card section and pillars', () => {
  const landingSrc = fs.readFileSync('frontend/src/pages/LandingPage.jsx', 'utf8');

  assert.ok(landingSrc.includes('Choose Your Experience'), 'Homepage must render "Choose Your Experience" title');
  assert.ok(landingSrc.includes('Explore Stays &rarr;') || landingSrc.includes('Explore Stays →'), 'Card 1 must offer "Explore Stays →"');
  assert.ok(landingSrc.includes('Plan a Wedding &rarr;') || landingSrc.includes('Plan a Wedding →'), 'Card 2 must offer "Plan a Wedding →"');
  assert.ok(landingSrc.includes('Plan an Offsite &rarr;') || landingSrc.includes('Plan an Offsite →'), 'Card 3 must offer "Plan an Offsite →"');
  assert.ok(landingSrc.includes('to="/stay"'), 'Card 1 must navigate to /stay');
  assert.ok(landingSrc.includes('to="/weddings"'), 'Card 2 must navigate to /weddings');
  assert.ok(landingSrc.includes('to="/offsites"'), 'Card 3 must navigate to /offsites');

  // Brief numbers: 64 rooms, 12 villas, 76 keys
  assert.ok(landingSrc.includes('64 estate rooms and 12 private villas'), 'Must mention 64 rooms and 12 private villas');
  assert.ok(landingSrc.includes('76 keys for guests'), 'Must mention 76 keys for guests');

  // 3 Pillars
  assert.ok(landingSrc.includes('Coffee Estate') && landingSrc.includes('Surrounded by plantations'), 'Must render Coffee Estate pillar');
  assert.ok(landingSrc.includes('Scenic Location') && landingSrc.includes('In the Western Ghats'), 'Must render Scenic Location pillar');
  assert.ok(landingSrc.includes('Curated Hospitality') && landingSrc.includes('Thoughtful, warm service'), 'Must render Curated Hospitality pillar');
});

test('Dedicated Experiences: Dedicated /stay page includes Hero, Explore Accommodation, and Why Stay Banner', () => {
  const staySrc = fs.readFileSync('frontend/src/pages/GuestAvailabilityPage.jsx', 'utf8');

  // Stay Hero
  assert.ok(staySrc.includes('Rooms &amp; Villas at the Estate') || staySrc.includes('Rooms & Villas at the Estate'), 'Stay hero must display "Rooms & Villas at the Estate"');

  // Room options strictly labeled 1 Room, 2 Rooms, 3 Rooms (NEVER 1 Suite / Villa)
  assert.ok(staySrc.includes('<option value={1}>1 Room</option>'), 'Rooms dropdown must use "1 Room"');
  assert.ok(staySrc.includes('<option value={2}>2 Rooms</option>'), 'Rooms dropdown must use "2 Rooms"');
  assert.ok(staySrc.includes('<option value={3}>3 Rooms</option>'), 'Rooms dropdown must use "3 Rooms"');
  assert.ok(!staySrc.includes('1 Suite / Villa'), 'Rooms dropdown must NEVER use "1 Suite / Villa"');

  // Discovery section
  assert.ok(staySrc.includes('Explore Our Accommodation'), 'Must render "Explore Our Accommodation" section');
  assert.ok(staySrc.includes('Estate Rooms'), 'Must feature Estate Rooms card');
  assert.ok(staySrc.includes('Private Villas'), 'Must feature Private Villas card');
  assert.ok(staySrc.includes('64 Keys at the Estate'), 'Must mention 64 keys for estate rooms');
  assert.ok(staySrc.includes('12 Private Villas'), 'Must mention 12 private villas');

  // Why Stay banner
  assert.ok(staySrc.includes('Why Stay With Us?'), 'Must render "Why Stay With Us?" banner');
  assert.ok(staySrc.includes('Thoughtful Stays'), 'Must include Thoughtful Stays');
  assert.ok(staySrc.includes('Coffee Estate Setting'), 'Must include Coffee Estate Setting');
  assert.ok(staySrc.includes('Scenic Western Ghats'), 'Must include Scenic Western Ghats');
  assert.ok(staySrc.includes('Direct Booking Value'), 'Must include Direct Booking Value');
});

test('Dedicated Experiences: Dedicated /weddings page features hero, editorial pillars, and embedded form', () => {
  const weddingSrc = fs.readFileSync('frontend/src/pages/WeddingsPage.jsx', 'utf8');

  // Hero
  assert.ok(weddingSrc.includes('Weddings &amp; Celebrations') || weddingSrc.includes('Weddings & Celebrations'), 'Wedding hero must display "Weddings & Celebrations"');
  assert.ok(weddingSrc.includes('Enquire Now &rarr;') || weddingSrc.includes('Enquire Now →'), 'Wedding hero must offer "Enquire Now →"');

  // Editorial Highlights
  assert.ok(weddingSrc.includes('Scenic Estate Lawns'), 'Must include Scenic Estate Lawns highlight');
  assert.ok(weddingSrc.includes('76 Keys for Guests'), 'Must include 76 Keys for Guests highlight');
  assert.ok(weddingSrc.includes('Tailored Celebrations'), 'Must include Tailored Celebrations highlight');

  // Form
  assert.ok(weddingSrc.includes('Plan Your Celebration'), 'Must include "Plan Your Celebration" form heading');
  assert.ok(weddingSrc.includes('TOTAL_ESTATE_INVENTORY_KEYS'), 'Must enforce TOTAL_ESTATE_INVENTORY_KEYS');
  assert.ok(weddingSrc.includes('enquiryType: \'WEDDING\''), 'Must lock enquiry type to WEDDING');
  assert.ok(weddingSrc.includes('submitPublicEnquiry'), 'Must dispatch via submitPublicEnquiry');
});

test('Dedicated Experiences: Dedicated /offsites page features hero, corporate pillars, and embedded form', () => {
  const offsiteSrc = fs.readFileSync('frontend/src/pages/OffsitesPage.jsx', 'utf8');

  // Hero
  assert.ok(offsiteSrc.includes('Retreats. Meetings.'), 'Offsite hero must display "Retreats. Meetings. Team Experiences."');
  assert.ok(offsiteSrc.includes('Enquire for Offsite &rarr;') || offsiteSrc.includes('Enquire for Offsite →'), 'Offsite hero must offer "Enquire for Offsite →"');

  // Corporate Pillars
  assert.ok(offsiteSrc.includes('Team Offsites'), 'Must include Team Offsites pillar');
  assert.ok(offsiteSrc.includes('Leadership Meets'), 'Must include Leadership Meets pillar');
  assert.ok(offsiteSrc.includes('Refresh &amp; Reconnect') || offsiteSrc.includes('Refresh & Reconnect'), 'Must include Refresh & Reconnect pillar');

  // Form
  assert.ok(offsiteSrc.includes('Plan Your Team Offsite'), 'Must include "Plan Your Team Offsite" form heading');
  assert.ok(offsiteSrc.includes('Select requirement...'), 'Conference setup dropdown must start with neutral "Select requirement..."');
  assert.ok(offsiteSrc.includes('TOTAL_ESTATE_INVENTORY_KEYS'), 'Must enforce TOTAL_ESTATE_INVENTORY_KEYS');
  assert.ok(offsiteSrc.includes('enquiryType: \'CORPORATE_OFFSITE\''), 'Must lock enquiry type to CORPORATE_OFFSITE');
  assert.ok(offsiteSrc.includes('submitPublicEnquiry'), 'Must dispatch via submitPublicEnquiry');
});
