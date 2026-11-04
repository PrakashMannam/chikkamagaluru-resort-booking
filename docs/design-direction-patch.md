# Design direction: PATCH (overrides docs/design-direction.md wherever they conflict)

## 1. Contrast (computed, not claimed)

The ratios printed in the original table were wrong, but the colours themselves pass. Use these numbers in the TDD.

| Pair | Ratio | Use |
|---|---|---|
| Text #292524 on bg #F7F5F2 | 13.9:1 | Body text |
| Muted #57534E on bg | 7.0:1 | Secondary text |
| White on accent #3E2723 | 13.8:1 | Primary button label |
| Success #2E5C31 on bg | 7.2:1 | Success text |
| Danger #991B1B on bg | 7.6:1 | Error text |
| Warning #A15C0B on bg | 4.8:1 | Warning icon or large text only |
| Warning text #8A4B08 on warning tint #F5EBDD | 5.8:1 | Warning banners (use this, not #A15C0B, on a tint) |
| Danger on danger tint #F3E4E1 | 6.7:1 | Error banners |
| Success on success tint #E4ECE3 | 6.5:1 | Success badges |
| Input border #78716C on white | 4.8:1 | Form field borders (non-text contrast needs 3:1) |

Decorative dividers may use #E7E5E4. Form field borders must NOT.

## 2. Complete tokens (replaces section 4; this is src/styles/tokens.css)

```css
:root {
  /* Colour */
  --color-bg: #F7F5F2;
  --color-surface: #FFFFFF;
  --color-text: #292524;
  --color-text-muted: #57534E;
  --color-accent: #3E2723;
  --color-accent-soft: #EFE7E1;
  --color-border: #E7E5E4;
  --color-border-input: #78716C;
  --color-success: #2E5C31;
  --color-success-tint: #E4ECE3;
  --color-warning: #A15C0B;
  --color-warning-text: #8A4B08;
  --color-warning-tint: #F5EBDD;
  --color-danger: #991B1B;
  --color-danger-tint: #F3E4E1;
  --color-overlay: rgba(41, 37, 36, 0.3);
  --color-skeleton: #E7E5E4;

  /* Type */
  --font-heading: "Lora", Georgia, "Times New Roman", serif;
  --font-body: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  --text-h1: clamp(1.75rem, 4vw, 2.25rem);
  --text-h2: clamp(1.375rem, 3vw, 1.75rem);
  --text-h3: 1.25rem;
  --text-body: 1rem;
  --text-small: 0.875rem;
  --weight-regular: 400;
  --weight-medium: 500;
  --weight-semibold: 600;
  --leading-body: 1.6;
  --leading-heading: 1.25;

  /* Spacing (8px base) */
  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 1rem;
  --space-4: 1.5rem;
  --space-5: 2rem;
  --space-6: 3rem;
  --space-7: 4rem;

  /* Shape and depth */
  --radius-sm: 4px;
  --radius-md: 8px;
  --shadow-subtle: 0 4px 12px rgba(41, 37, 36, 0.06);
  --shadow-hover: 0 8px 24px rgba(41, 37, 36, 0.10);

  /* Interaction */
  --tap-min: 44px;
  --focus-ring: 0 0 0 2px var(--color-bg), 0 0 0 4px var(--color-accent);
  --motion-fast: 150ms;
}
@media (prefers-reduced-motion: reduce) {
  :root { --motion-fast: 0ms; }
  * { animation: none !important; transition: none !important; }
}
```

## 3. Rules the original left out

1. CSS variables DO NOT work inside @media queries. Use literal values: 768px (tablet) and 1024px (desktop). Remove the --bp-* variables.
2. Every interactive element is at least 44 px tall and wide, and shows --focus-ring on :focus-visible.
3. Input border is --color-border-input (not a light grey). Error state: danger border plus an error message below the field, never colour alone.
4. Banners use tints: error = danger text on danger tint, warning = warning-text on warning tint. Never use the raw warning colour for text on a tint.
5. Heading font: load only Lora 400 and 600, with font-display: swap and a Georgia fallback. Body uses the system font stack (no download).
6. Skeletons use --color-skeleton and keep the same dimensions as the content they replace.

## 4. Screen changes

1. Room detail is a real page, route /rooms/:code (photo gallery, rate plans, availability, "Select"). Not a modal. The brief says "room pages".
2. App shells:
   - Guest: slim header (resort name, "Enquire" link) and a quiet footer. No navigation clutter.
   - Staff: top bar with the user name and role, and Sign out. Desktop adds a left rail (Inbox, Rates and allotment). Mobile uses the top bar plus two tabs.
3. Step 3 shows a small "Test mode" label, a hold countdown (10 minutes), and a "Cancel hold" ghost button.
4. While the GST slab is UNVERIFIED: guests see a small neutral note under the price ("Taxes shown are indicative in this test build"). Staff admin shows the loud warning banner.
5. Staff inbox: a danger-tint banner at the top when a COMPLAINT enquiry is waiting for the GM.
6. Admin rates on mobile: one card per date with rate and allotment inputs, grouped under Rooms and Villas tabs. Desktop: the 14-day matrix. Save button is sticky.
7. The enquiry form has no photo behind it. On desktop, show one image beside the form. On mobile, show no image.

## 5. Photo plan changes

- Produce each image at two widths (800 and 1600), as WebP under 150 KB each, and use srcset with explicit width and height attributes.
- Source: Unsplash or Pexels only (free licence). Record photographer and link in docs/photo-credits.md for the TDD.
- Every image has meaningful alt text.
- Only the first image on the page loads eagerly. Everything else is loading="lazy".

## 6. Open taste decision (yours, not the AI's)

The accent #3E2723 is almost as dark as the text, so there is little visual difference between "accent" and "text".
Look at /styleguide after task F1. If the page looks flat, change only --color-accent (for example to a warmer coffee-brown) and re-check the button contrast, keeping white-on-accent above 4.5:1.
