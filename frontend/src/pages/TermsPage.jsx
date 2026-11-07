import React from 'react';
import { Link } from 'react-router-dom';
import './InfoPage.css';

export function TermsPage() {
  return (
    <div className="info-page">
      {/* Breadcrumb Navigation */}
      <nav className="info-page__nav" aria-label="Breadcrumb">
        <Link to="/" className="info-page__back-link">
          &larr; Return to Estate Home
        </Link>
      </nav>

      {/* Header */}
      <header className="info-page__header">
        <span className="info-page__kicker">Demonstration Terms</span>
        <h1 className="info-page__title">Terms &amp; Information</h1>
        <p className="info-page__subtitle">
          Operational terms, simulation parameters, and functional boundaries governing the demonstration resort booking system.
        </p>
      </header>

      {/* Demonstration Notice Box */}
      <div className="info-notice" role="region" aria-label="Demonstration Notice">
        <h2 className="info-notice__title">Demonstration Application Notice</h2>
        <p className="info-notice__text">
          This system is an engineering assessment and technical evaluation prototype built for IsoCode Labs. These terms describe the functional rules and constraints implemented in the prototype and do not constitute a commercial hospitality contract.
        </p>
      </div>

      <div className="info-sections">
        {/* Section 1: Availability and Nightly Inventory */}
        <section className="info-section">
          <h2 className="info-section__title">1. Availability and Nightly Inventory</h2>
          <div className="info-section__content">
            <p>
              Room inventory and nightly tariff rates are retrieved dynamically from the reservation engine:
            </p>
            <ul>
              <li>
                <strong>Physical Key Limits:</strong> Estate inventory is capped by total physical keys per room category (64 Coffee Estate Canopy Rooms and 12 Private Plantation Pool Villas).
              </li>
              <li>
                <strong>Nightly Inventory Locking:</strong> The booking engine manages inventory at the category and stay-date level through nightly inventory records. Creating a booking hold locks the requested room count across the selected stay dates under database-level concurrency controls; individual numbered room suites are not pre-assigned.
              </li>
              <li>
                <strong>Rate Plans:</strong> Available rate plans (European Plan / Room Only, Continental Plan / Breakfast Included, and Modified American Plan / Breakfast &amp; Dinner) apply fixed add-ons per room per night to base accommodation rates.
              </li>
            </ul>
          </div>
        </section>

        {/* Section 2: Enquiries vs. Confirmed Bookings */}
        <section className="info-section">
          <h2 className="info-section__title">2. Enquiries vs. Confirmed Bookings</h2>
          <div className="info-section__content">
            <p>
              The platform maintains a strict operational distinction between public enquiries and room reservations:
            </p>
            <ul>
              <li>
                <strong>Submitting an Enquiry:</strong> Transmits an inquiry record to the staff coordination inbox for event planning (weddings, corporate offsites, or retreat consultations). <em>Submitting an enquiry does not lock inventory rows, hold dates, or reserve event lawns.</em>
              </li>
              <li>
                <strong>Creating a Reservation:</strong> A room reservation is initiated only through the formal booking checkout flow, which locks nightly inventory records for the requested dates under a temporary 10-minute countdown hold.
              </li>
              <li>
                <strong>Confirmation Requirement:</strong> A booking is confirmed only after simulated payment authorization completes successfully before the 10-minute hold timer expires.
              </li>
            </ul>
          </div>
        </section>

        {/* Section 3: Simulated Payment Experience */}
        <section className="info-section">
          <h2 className="info-section__title">3. Simulated Payment Experience</h2>
          <div className="info-section__content">
            <p>
              To ensure safety during technical evaluation:
            </p>
            <ul>
              <li>
                The payment screen presents an integrated test webhook simulator designed to test mock authorization, failed webhooks, late payments, and user cancellations.
              </li>
              <li>
                No real credit cards, bank accounts, UPI IDs, or financial transactions are accepted, debited, or transferred.
              </li>
              <li>
                No commercial refund, chargeback, or monetary claims can arise from test bookings.
              </li>
            </ul>
          </div>
        </section>

        {/* Section 4: Promotional Codes and Lifecycle Rules */}
        <section className="info-section">
          <h2 className="info-section__title">4. Promotional Codes &amp; Usage Rules</h2>
          <div className="info-section__content">
            <p>
              Promotional discounts (e.g. direct booking discount codes) are enforced by server-side business logic:
            </p>
            <ul>
              <li>
                <strong>Validation:</strong> Promo codes are validated against configured active date ranges and room category restrictions.
              </li>
              <li>
                <strong>Hold Reservation:</strong> When a booking hold is created, promo usage is atomically reserved and its usage counter is incremented.
              </li>
              <li>
                <strong>Confirmed Bookings:</strong> Successfully confirmed bookings retain the reserved promo usage.
              </li>
              <li>
                <strong>Release on Cancellation or Expiry:</strong> If a pending hold is cancelled by the guest, payment fails, or the 10-minute countdown timer expires, the reserved usage is restored and decremented exactly once.
              </li>
              <li>
                <strong>Read-Only Quotes:</strong> Generating pricing quotes via the quote endpoint is strictly read-only and never mutates or reserves promo usage counters.
              </li>
            </ul>
          </div>
        </section>

        {/* Section 5: Provisional Taxes and GST Slabs */}
        <section className="info-section">
          <h2 className="info-section__title">5. Provisional Taxes &amp; GST Slabs</h2>
          <div className="info-section__content">
            <p>
              The application calculates estimated Goods and Services Tax (GST) using database-configured slab values:
            </p>
            <ul>
              <li>
                <strong>Configured Slabs:</strong> The prototype database is currently seeded with demo slabs applying 5.00% GST for nightly room tariffs below ₹7,500 and 18.00% GST for nightly room tariffs at or above ₹7,500.
              </li>
              <li>
                <strong>Provisional &amp; Unverified:</strong> All calculated tax amounts are explicitly flagged by the pricing engine with an <em>UNVERIFIED</em> status.
              </li>
              <li>
                <strong>No Tax Advice:</strong> Displayed tax values are provisional estimations for prototype demonstration only. They do not constitute legal or statutory tax advice, and the property accountant must verify actual tax treatment and statutory rates before any commercial deployment.
              </li>
            </ul>
          </div>
        </section>

        {/* Section 6: Prototype Limitations */}
        <section className="info-section">
          <h2 className="info-section__title">6. Demonstration Evaluation Scope</h2>
          <div className="info-section__content">
            <p>
              This application is delivered as an evaluation prototype:
            </p>
            <ul>
              <li>
                Demo user accounts, simulated notifications, and test bookings exist solely to demonstrate full-stack software architecture and responsive user experience.
              </li>
              <li>
                This software must not be used or represented as an active commercial resort operation.
              </li>
            </ul>
          </div>
        </section>
      </div>

      {/* Action Navigation */}
      <div className="info-page__actions">
        <Link to="/stay" className="btn btn--primary">
          Explore Rooms &amp; Book
        </Link>
        <Link to="/" className="btn btn--secondary">
          Return to Estate Homepage
        </Link>
        <Link to="/privacy" className="btn btn--secondary">
          View Privacy Information
        </Link>
      </div>
    </div>
  );
}

export default TermsPage;
