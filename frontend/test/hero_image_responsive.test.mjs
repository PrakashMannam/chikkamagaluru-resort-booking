import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

test('LandingPage Hero Image: Markup, Art Direction & Fallback Contract', () => {
  const landingSrc = fs.readFileSync('frontend/src/pages/LandingPage.jsx', 'utf8');

  // Must use <picture> for responsive art direction and format negotiation
  assert.ok(
    landingSrc.includes('<picture className="lp-hero-picture">'),
    'Hero markup must use responsive <picture> element'
  );

  // Must have mobile-optimized source with media query
  assert.ok(
    landingSrc.includes('media="(max-width: 640px)"'),
    'Hero picture must include mobile media query source'
  );
  assert.ok(
    landingSrc.includes('/images/hero-estate-800w.webp'),
    'Hero picture must include 800w webp source for mobile'
  );

  // Must have desktop source
  assert.ok(
    landingSrc.includes('/images/hero-estate.webp'),
    'Hero picture must include full webp source for desktop'
  );

  // Must have accessible alt text
  const expectedAlt = 'Chikkamagaluru coffee estate cottage veranda overlooking misty mountains';
  assert.ok(
    landingSrc.includes(`alt="${expectedAlt}"`),
    'Hero image must retain accessible descriptive alt text'
  );

  // Must have dimensions for layout stability (prevent CLS)
  assert.ok(
    landingSrc.includes('width="1600"') && landingSrc.includes('height="900"'),
    'Hero image must specify intrinsic dimensions to prevent CLS'
  );

  // Must have onError fallback handler
  assert.ok(
    landingSrc.includes('onError='),
    'Hero image must have an onError fallback handler'
  );
  assert.ok(
    landingSrc.includes('dataset.fallbackTried'),
    'Hero image onError must guard against infinite fallback loops'
  );
  assert.ok(
    landingSrc.includes('/images/hero-estate.png'),
    'Hero image must fallback to PNG if WebP fails'
  );
});

test('LandingPage Hero CSS: Mobile Visibility, Crop & Scrim Contrast', () => {
  const cssSrc = fs.readFileSync('frontend/src/pages/LandingPage.css', 'utf8');

  // .lp-hero-bg must have earthy background fallback
  assert.ok(
    cssSrc.includes('background-color: #2D4A3E'),
    '.lp-hero-bg must have a forest tone background fallback'
  );

  // Picture element must fill container
  assert.ok(
    cssSrc.includes('.lp-hero-picture'),
    'CSS must define .lp-hero-picture dimensions'
  );

  // Desktop object-position must be preserved
  assert.ok(
    cssSrc.includes('object-position: right bottom;'),
    'Desktop hero image must preserve right bottom alignment'
  );

  // Mobile media query must provide intentional crop
  assert.ok(
    cssSrc.includes('object-position: 68% center;'),
    'Mobile hero image must have an intentional crop centering veranda and path'
  );

  // Mobile gradient must NOT be double-stacked opaque wash
  const mobileQueryMatch = cssSrc.match(/@media\s*\(max-width:\s*640px\)[\s\S]*?\.lp-hero-gradient\s*\{([\s\S]*?)\}/);
  assert.ok(mobileQueryMatch, 'Must define mobile media query for .lp-hero-gradient');
  const gradientRules = mobileQueryMatch[1];
  
  // Must NOT contain double linear-gradient
  const countLinearGradients = (gradientRules.match(/linear-gradient/g) || []).length;
  assert.equal(
    countLinearGradients,
    1,
    'Mobile gradient must be a single scrim, not two stacked opaque washes'
  );

  // Bottom of mobile gradient must be transparent or near-transparent (<= 0.05) so image shines through
  assert.ok(
    gradientRules.includes('0.02') || gradientRules.includes('transparent') || gradientRules.includes('0.05'),
    'Bottom of mobile gradient must be transparent or translucent to show estate image'
  );
});
