import React from 'react';
import { Link } from 'react-router-dom';
import './InfoPage.css';

export function PrivacyPage() {
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
        <span className="info-page__kicker">Demonstration Information</span>
        <h1 className="info-page__title">Privacy Information</h1>
        <p className="info-page__subtitle">
          An overview of data collection, processing, and simulation boundaries in this demonstration resort booking system.
        </p>
      </header>

      {/* Demonstration Notice Box */}
      <div className="info-notice" role="region" aria-label="Demonstration Notice">
        <h2 className="info-notice__title">Demonstration Application Notice</h2>
        <p className="info-notice__text">
          This system is an engineering demonstration and technical evaluation prototype built for IsoCode Labs. This document describes the actual data-handling implementation of this prototype and is not a legally reviewed commercial privacy policy.
        </p>
      </div>

      <div className="info-sections">
        {/* Section 1: Information Collected */}
        <section className="info-section">
          <h2 className="info-section__title">1. Information Provided by Guests</h2>
          <div className="info-section__content">
            <p>
              When interacting with the resort web application, guests submit information through two public workflows:
            </p>
            <ul>
              <li>
                <strong>Room Reservation Flow:</strong> Full guest name, email address, contact phone number, stay dates (check-in and check-out), selected room category, rate plan, guest counts (adults and children), optional promo code, and special requests.
              </li>
              <li>
                <strong>Public Enquiry Flow:</strong> Sender name, contact phone number or email address, occasion category (weddings, corporate offsites, or general stays), target dates, estimated guest and room counts, budget indications, and message requirements.
              </li>
            </ul>
            <p>
              <strong>Important:</strong> Do not submit sensitive personal identifiers such as passport numbers, Aadhaar numbers, PAN cards, or payment card details. The application does not require or request government-issued identity documents.
            </p>
          </div>
        </section>

        {/* Section 2: How Data is Used */}
        <section className="info-section">
          <h2 className="info-section__title">2. How Data is Used in the Application</h2>
          <div className="info-section__content">
            <p>
              Information collected through the interface is processed exclusively to support the application's core resort operations:
            </p>
            <ul>
              <li>
                <strong>Reservation Management:</strong> Calculating nightly room tariffs, applicable GST slabs, and locking physical room inventory allotments during checkout.
              </li>
              <li>
                <strong>Staff Coordination:</strong> Routing incoming inquiries to specialized staff queues (Rooms, Weddings, Corporate Offsites) and generating internal response drafts for staff review.
              </li>
              <li>
                <strong>Confirmation &amp; Receipt Access:</strong> Allowing guests to access their reservation confirmation and receipt directly via the booking's confirmation link. No unauthenticated public lookup directory is exposed.
              </li>
            </ul>
          </div>
        </section>

        {/* Section 3: Staff Access and Security */}
        <section className="info-section">
          <h2 className="info-section__title">3. Access Controls and Security</h2>
          <div className="info-section__content">
            <p>
              Access to guest records is restricted to authenticated resort staff:
            </p>
            <ul>
              <li>
                Staff must authenticate using secure credentials. User passwords are stored using BCrypt cryptographic hashing.
              </li>
              <li>
                Inquiries and draft responses are partitioned into specialty lanes (Room Bookings, Weddings, Corporate Offsites) with role-based access control (Staff vs. General Manager).
              </li>
              <li>
                Internal application logs omit raw guest email addresses and phone numbers during simulated message dispatch to prevent accidental data leakage into log aggregation systems.
              </li>
            </ul>
          </div>
        </section>

        {/* Section 4: Simulated Services & External Boundaries */}
        <section className="info-section">
          <h2 className="info-section__title">4. Simulated Services &amp; No Real Processors</h2>
          <div className="info-section__content">
            <p>
              To maintain safe isolation in this demonstration environment:
            </p>
            <ul>
              <li>
                <strong>Payment Processing is Simulated:</strong> The checkout payment step utilizes a mock webhook simulator. No credit card information, bank credentials, or actual financial transactions are processed.
              </li>
              <li>
                <strong>Messaging Delivery is Simulated:</strong> Approved replies to enquiries trigger a simulated dispatch adapter. No live SMS gateways, WhatsApp Cloud APIs, or SMTP mail servers are connected.
              </li>
              <li>
                <strong>No Third-Party Trackers:</strong> The application does not integrate external advertising trackers, data brokers, or commercial behavioral analytics.
              </li>
            </ul>
          </div>
        </section>

        {/* Section 5: Data Retention */}
        <section className="info-section">
          <h2 className="info-section__title">5. Data Retention &amp; Limitations</h2>
          <div className="info-section__content">
            <p>
              All submitted data resides in an isolated demonstration database instance. Because this is an evaluation prototype:
            </p>
            <ul>
              <li>
                The application does not maintain a formal legal data-retention schedule or automated record deletion lifecycle.
              </li>
              <li>
                Database contents may be reset, re-seeded, or cleared during development and deployment testing cycles.
              </li>
              <li>
                This demonstration makes no representations of formal statutory data protection certification or regulatory audits.
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
        <Link to="/terms" className="btn btn--secondary">
          View Demonstration Terms
        </Link>
      </div>
    </div>
  );
}

export default PrivacyPage;
