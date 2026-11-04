import React, { useState } from 'react';
import './Styleguide.css';
import {
  Button,
  Input,
  Card,
  Badge,
  Skeleton,
  EmptyState,
  ErrorBanner,
  Modal,
  Tabs,
  Toast,
} from '../components/ui';

export function Styleguide() {
  const [modalOpen, setModalOpen] = useState(false);
  const [inputValue, setInputValue] = useState('Standard Room with Balcony');
  const [activeToast, setActiveToast] = useState(null);
  const [bannerVisible, setBannerVisible] = useState(true);
  const [warningBannerVisible, setWarningBannerVisible] = useState(true);

  const tabItems = [
    {
      id: 'details',
      label: 'Room Details',
      content: (
        <div>
          <p>
            Spacious plantation-view room featuring teakwood furnishings,
            private verandah overlooking the Western Ghats, and artisan coffee bar.
          </p>
        </div>
      ),
    },
    {
      id: 'amenities',
      label: 'Amenities',
      content: (
        <div>
          <p>King-sized bed, high-speed Wi-Fi, rain shower, and local organic bath products.</p>
        </div>
      ),
    },
    {
      id: 'policies',
      label: 'Policies',
      content: (
        <div>
          <p>Check-in: 14:00 | Check-out: 11:00. 100% smoke-free property.</p>
        </div>
      ),
    },
  ];

  return (
    <div className="styleguide">
      <header className="styleguide__header">
        <h1>Design System Styleguide</h1>
        <p className="styleguide__subtitle">
          Task F1: Component catalogue and design token verification for Chikkamagaluru Resort.
        </p>
      </header>

      {/* 1. Design Tokens & Typography */}
      <section className="styleguide__section" aria-labelledby="sec-tokens">
        <h2 id="sec-tokens" className="styleguide__section-title">
          1. Typography & Colors
        </h2>
        <div className="styleguide__grid">
          <div>
            <h1>Heading 1 (Lora 600) — Estate Suites</h1>
            <h2>Heading 2 (Lora 600) — Cottage & Villa Allotment</h2>
            <h3>Heading 3 (Lora 600) — Hillside Sanctuary</h3>
            <p style={{ marginTop: 'var(--space-2)' }}>
              Body text: Set in the clean system font stack with 1.6 line height.
              Chikkamagaluru is renowned for its mist-covered hills, tranquil coffee plantations,
              and rich heritage in the Western Ghats.
            </p>
            <p style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-muted)', marginTop: 'var(--space-1)' }}>
              Small text: Taxes shown are indicative in this test build.
            </p>
            <p style={{ marginTop: 'var(--space-2)' }}>
              <a href="#sec-tokens" className="styleguide__link">
                Accessible link with visible focus and fast transition
              </a>
            </p>
          </div>

          <div className="styleguide__color-grid">
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-bg)' }} />
              <span className="styleguide__token-name">--color-bg</span>
              <span className="styleguide__token-val">#F7F5F2</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-surface)' }} />
              <span className="styleguide__token-name">--color-surface</span>
              <span className="styleguide__token-val">#FFFFFF</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-text)' }} />
              <span className="styleguide__token-name">--color-text</span>
              <span className="styleguide__token-val">#292524</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-text-muted)' }} />
              <span className="styleguide__token-name">--color-text-muted</span>
              <span className="styleguide__token-val">#57534E</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-accent)' }} />
              <span className="styleguide__token-name">--color-accent</span>
              <span className="styleguide__token-val">#3E2723</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-accent-soft)' }} />
              <span className="styleguide__token-name">--color-accent-soft</span>
              <span className="styleguide__token-val">#EFE7E1</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-border)' }} />
              <span className="styleguide__token-name">--color-border</span>
              <span className="styleguide__token-val">#E7E5E4</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-border-input)' }} />
              <span className="styleguide__token-name">--color-border-input</span>
              <span className="styleguide__token-val">#78716C</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-success)' }} />
              <span className="styleguide__token-name">--color-success</span>
              <span className="styleguide__token-val">#2E5C31</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-success-tint)' }} />
              <span className="styleguide__token-name">--color-success-tint</span>
              <span className="styleguide__token-val">#E4ECE3</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-warning-text)' }} />
              <span className="styleguide__token-name">--color-warning-text</span>
              <span className="styleguide__token-val">#8A4B08</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-warning-tint)' }} />
              <span className="styleguide__token-name">--color-warning-tint</span>
              <span className="styleguide__token-val">#F5EBDD</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-danger)' }} />
              <span className="styleguide__token-name">--color-danger</span>
              <span className="styleguide__token-val">#991B1B</span>
            </div>
            <div className="styleguide__color-swatch">
              <div className="styleguide__color-box" style={{ backgroundColor: 'var(--color-danger-tint)' }} />
              <span className="styleguide__token-name">--color-danger-tint</span>
              <span className="styleguide__token-val">#F3E4E1</span>
            </div>
          </div>
        </div>
      </section>

      {/* 2. Button */}
      <section className="styleguide__section" aria-labelledby="sec-buttons">
        <h2 id="sec-buttons" className="styleguide__section-title">
          2. Button (primary, secondary, ghost, disabled, loading)
        </h2>
        <div className="styleguide__grid">
          <div>
            <h3 style={{ marginBottom: 'var(--space-2)' }}>Primary</h3>
            <div className="styleguide__row">
              <Button variant="primary">Confirm booking</Button>
              <Button variant="primary" isLoading>
                Holding room...
              </Button>
              <Button variant="primary" disabled>
                Unavailable
              </Button>
            </div>
          </div>

          <div>
            <h3 style={{ marginBottom: 'var(--space-2)' }}>Secondary</h3>
            <div className="styleguide__row">
              <Button variant="secondary">Check availability</Button>
              <Button variant="secondary" isLoading>
                Checking...
              </Button>
              <Button variant="secondary" disabled>
                Sold out
              </Button>
            </div>
          </div>

          <div>
            <h3 style={{ marginBottom: 'var(--space-2)' }}>Ghost</h3>
            <div className="styleguide__row">
              <Button variant="ghost">Cancel hold</Button>
              <Button variant="ghost" isLoading>
                Cancelling...
              </Button>
              <Button variant="ghost" disabled>
                Cancelled
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* 3. Input */}
      <section className="styleguide__section" aria-labelledby="sec-inputs">
        <h2 id="sec-inputs" className="styleguide__section-title">
          3. Input (label, helper text, error state with message)
        </h2>
        <div className="styleguide__column">
          <Input
            id="input-default"
            label="Guest Full Name"
            placeholder="e.g. Priya Sharma"
            helperText="Enter your name as it appears on your government ID"
          />

          <Input
            id="input-filled"
            label="Selected Room Category"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            helperText="Tap to adjust your chosen accommodation"
          />

          <Input
            id="input-error"
            label="Email Address"
            defaultValue="priya@"
            error="Please enter a valid email address (e.g. guest@example.com)"
          />

          <Input
            id="input-disabled"
            label="Booking Reference Number"
            value="CKM-2026-9041"
            disabled
            helperText="Assigned automatically upon hold confirmation"
          />
        </div>
      </section>

      {/* 4. Card */}
      <section className="styleguide__section" aria-labelledby="sec-cards">
        <h2 id="sec-cards" className="styleguide__section-title">
          4. Card (standard, interactive)
        </h2>
        <div className="styleguide__cards">
          <Card className="styleguide__card">
            <h3>Plantation Cottage</h3>
            <p style={{ marginTop: 'var(--space-2)', color: 'var(--color-text-muted)' }}>
              Standard surface card with subtle elevation and 1px border.
            </p>
            <div style={{ marginTop: 'var(--space-3)' }}>
              <Badge variant="success">Available</Badge>
            </div>
          </Card>

          <Card
            interactive
            onClick={() => alert('Card clicked')}
            className="styleguide__card"
          >
            <h3>Estate Pool Villa (Interactive)</h3>
            <p style={{ marginTop: 'var(--space-2)', color: 'var(--color-text-muted)' }}>
              Hover or focus with keyboard to view elevation change.
            </p>
            <div style={{ marginTop: 'var(--space-3)' }}>
              <Button variant="secondary">View details</Button>
            </div>
          </Card>
        </div>
      </section>

      {/* 5. Badge */}
      <section className="styleguide__section" aria-labelledby="sec-badges">
        <h2 id="sec-badges" className="styleguide__section-title">
          5. Badge (neutral, success, warning, danger)
        </h2>
        <div className="styleguide__row">
          <Badge variant="neutral">Hold active (8 mins)</Badge>
          <Badge variant="success">Confirmed &amp; Paid</Badge>
          <Badge variant="warning">Awaiting GM Review</Badge>
          <Badge variant="danger">Expired hold</Badge>
        </div>
      </section>

      {/* 6. Skeleton */}
      <section className="styleguide__section" aria-labelledby="sec-skeletons">
        <h2 id="sec-skeletons" className="styleguide__section-title">
          6. Skeleton (text, rectangular, circular)
        </h2>
        <div className="styleguide__column">
          <div>
            <p style={{ fontSize: 'var(--text-small)', marginBottom: 'var(--space-1)' }}>Text placeholders:</p>
            <Skeleton variant="text" width="80%" />
            <Skeleton variant="text" width="60%" />
            <Skeleton variant="text" width="40%" />
          </div>

          <div>
            <p style={{ fontSize: 'var(--text-small)', marginBottom: 'var(--space-1)' }}>Circular avatar:</p>
            <Skeleton variant="circular" width="44px" height="44px" />
          </div>

          <div>
            <p style={{ fontSize: 'var(--text-small)', marginBottom: 'var(--space-1)' }}>Card block placeholder:</p>
            <Skeleton variant="rectangular" width="100%" height="120px" />
          </div>
        </div>
      </section>

      {/* 7. EmptyState */}
      <section className="styleguide__section" aria-labelledby="sec-empty">
        <h2 id="sec-empty" className="styleguide__section-title">
          7. EmptyState
        </h2>
        <EmptyState
          icon={
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
          }
          title="No accommodations available"
          description="We could not find any vacant cottages for your selected dates. Please adjust your stay window or contact our concierge."
          action={<Button variant="primary">Change dates</Button>}
        />
      </section>

      {/* 8. ErrorBanner */}
      <section className="styleguide__section" aria-labelledby="sec-banners">
        <h2 id="sec-banners" className="styleguide__section-title">
          8. ErrorBanner (error, warning)
        </h2>
        <div className="styleguide__grid">
          {bannerVisible && (
            <ErrorBanner
              variant="error"
              title="Your reservation hold has expired"
              message="The 10-minute hold window for this villa elapsed. Please re-select your preferred room to secure your booking."
              actionLabel="Restart booking"
              onAction={() => alert('Restarting booking...')}
              onDismiss={() => setBannerVisible(false)}
            />
          )}

          {warningBannerVisible && (
            <ErrorBanner
              variant="warning"
              title="GST Slab Configuration Warning"
              message="Warning text var(--color-warning-text) on var(--color-warning-tint). Verify tax slabs in administrative settings."
              actionLabel="Review settings"
              onAction={() => alert('Reviewing settings...')}
              onDismiss={() => setWarningBannerVisible(false)}
            />
          )}
        </div>
      </section>

      {/* 9. Modal */}
      <section className="styleguide__section" aria-labelledby="sec-modal">
        <h2 id="sec-modal" className="styleguide__section-title">
          9. Modal (focus trapped, Escape closes)
        </h2>
        <div>
          <Button variant="primary" onClick={() => setModalOpen(true)}>
            Open Sample Modal
          </Button>

          <Modal
            isOpen={modalOpen}
            onClose={() => setModalOpen(false)}
            title="Cancel Reservation Hold"
            footer={
              <>
                <Button variant="ghost" onClick={() => setModalOpen(false)}>
                  Keep Reservation
                </Button>
                <Button
                  variant="primary"
                  onClick={() => {
                    setModalOpen(false);
                    alert('Hold cancelled');
                  }}
                >
                  Yes, Release Room
                </Button>
              </>
            }
          >
            <p>
              Are you sure you wish to release your 10-minute hold on the Plantation Cottage?
              The room will immediately become available for other guests to book.
            </p>
            <p style={{ marginTop: 'var(--space-2)', fontSize: 'var(--text-small)', color: 'var(--color-text-muted)' }}>
              (Press Escape or Tab to test keyboard trap and escape closing.)
            </p>
          </Modal>
        </div>
      </section>

      {/* 10. Tabs */}
      <section className="styleguide__section" aria-labelledby="sec-tabs">
        <h2 id="sec-tabs" className="styleguide__section-title">
          10. Tabs (keyboard arrow navigation)
        </h2>
        <Tabs items={tabItems} defaultTab="details" />
      </section>

      {/* 11. Toast */}
      <section className="styleguide__section" aria-labelledby="sec-toast">
        <h2 id="sec-toast" className="styleguide__section-title">
          11. Toast
        </h2>
        <div className="styleguide__grid">
          <p style={{ color: 'var(--color-text-muted)' }}>Static variants:</p>
          <div className="styleguide__toasts">
            <Toast variant="neutral" message="Draft enquiry saved to session." onClose={() => {}} />
            <Toast variant="success" message="Hold confirmed for 10 minutes." onClose={() => {}} />
            <Toast variant="warning" message="High demand: only 1 villa remaining." onClose={() => {}} />
            <Toast variant="danger" message="Payment gateway connection failed." onClose={() => {}} />
          </div>

          <div style={{ marginTop: 'var(--space-3)' }}>
            <p style={{ color: 'var(--color-text-muted)', marginBottom: 'var(--space-2)' }}>
              Interactive floating toast demo:
            </p>
            <div className="styleguide__row">
              <Button
                variant="secondary"
                onClick={() =>
                  setActiveToast({
                    variant: 'success',
                    message: 'Interactive toast triggered. Auto-closing in 4 seconds.',
                  })
                }
              >
                Trigger Floating Success Toast
              </Button>
              <Button
                variant="secondary"
                onClick={() =>
                  setActiveToast({
                    variant: 'danger',
                    message: 'Interactive error toast triggered.',
                  })
                }
              >
                Trigger Floating Error Toast
              </Button>
            </div>
          </div>

          {activeToast && (
            <Toast
              isFixed
              variant={activeToast.variant}
              message={activeToast.message}
              duration={4000}
              onClose={() => setActiveToast(null)}
            />
          )}
        </div>
      </section>
    </div>
  );
}

export default Styleguide;
