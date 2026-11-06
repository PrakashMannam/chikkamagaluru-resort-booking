import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useStaffAuth } from '../context/StaffAuthContext';
import { fetchAdminEnquiries, simulateAdminEnquiry } from '../api/staffApi';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Skeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorBanner } from '../components/ui/ErrorBanner';
import { Modal } from '../components/ui/Modal';
import './StaffEnquiriesPage.css';

const CATEGORY_META = {
  WEDDING: { label: 'Wedding', badgeVariant: 'warning', icon: '💍', color: '#8C6D3F' },
  CORPORATE_OFFSITE: { label: 'Corporate Offsite', badgeVariant: 'success', icon: '💼', color: '#2E5A44' },
  ROOM_BOOKING: { label: 'Room Booking', badgeVariant: 'neutral', icon: '🛏️', color: '#5D4037' },
  COMPLAINT: { label: 'Complaint (GM)', badgeVariant: 'danger', icon: '🚨', color: '#C62828' },
  UNSORTED: { label: 'Unsorted Queue', badgeVariant: 'warning', icon: '📋', color: '#D97706' },
};

const SIM_PRESETS = [
  {
    label: '🛏️ Room Stay',
    name: 'Rohan Sharma',
    channel: 'WHATSAPP_SIM',
    contact: '+919876543210',
    message: 'Hello, looking for a luxury estate villa for 2 nights next weekend for 2 adults. Do you have plantation walk and private dining available?',
  },
  {
    label: '💍 Wedding',
    name: 'Priyadarshini Rao',
    channel: 'EMAIL_SIM',
    contact: 'priya.rao@example.com',
    message: 'Greetings! We are planning an intimate destination wedding at your coffee estate in December for 80 guests. Could you please share lawn availability, catering packages, and accommodation for our families?',
  },
  {
    label: '💼 Offsite',
    name: 'Kunal Kapoor',
    channel: 'EMAIL_SIM',
    contact: 'kunal.kapoor@techcorp.in',
    message: 'Hi team, our company is planning an annual leadership retreat for 35 delegates in January. We require conference facilities, projector setup, and 20 rooms for 2 nights.',
  },
  {
    label: '🚨 Complaint',
    name: 'Sunita Reddy',
    channel: 'WHATSAPP_SIM',
    contact: '+919123456789',
    message: 'Extremely disappointed with our booking experience. We reached out twice regarding our anniversary reservation and received no response. Please escalate this to the General Manager immediately.',
  },
];

export function StaffEnquiriesPage() {
  const { token, user } = useStaffAuth();

  const [enquiries, setEnquiries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [selectedChannel, setSelectedChannel] = useState('');
  const [selectedConfidence, setSelectedConfidence] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Simulator State
  const [isSimulatorOpen, setIsSimulatorOpen] = useState(false);
  const [simChannel, setSimChannel] = useState('EMAIL_SIM');
  const [simSenderName, setSimSenderName] = useState('');
  const [simSenderContact, setSimSenderContact] = useState('');
  const [simRawMessage, setSimRawMessage] = useState('');
  const [simErrors, setSimErrors] = useState({});
  const [isSimulating, setIsSimulating] = useState(false);
  const [simApiError, setSimApiError] = useState(null);
  const [simulationSuccess, setSimulationSuccess] = useState(null);

  const isGm = Boolean(user?.isGm || user?.is_gm);
  const userLane = (user?.specialtyLane || user?.specialty_lane || '').toUpperCase();

  const loadEnquiries = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const filters = {};
      if (selectedCategory) filters.category = selectedCategory;
      if (selectedStatus) filters.status = selectedStatus;
      if (selectedChannel) filters.channel = selectedChannel;
      if (selectedConfidence === 'HIGH') filters.min_confidence = '0.80';

      const data = await fetchAdminEnquiries(token, filters);
      setEnquiries(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to retrieve enquiries.');
    } finally {
      setLoading(false);
    }
  }, [token, selectedCategory, selectedStatus, selectedChannel, selectedConfidence]);

  useEffect(() => {
    loadEnquiries();
  }, [loadEnquiries]);

  // Client-side search and low-confidence filtering
  const filteredEnquiries = useMemo(() => {
    return enquiries.filter((item) => {
      if (selectedConfidence === 'LOW') {
        const conf = item.ai_confidence != null ? Number(item.ai_confidence) : 0;
        if (conf >= 0.80) return false;
      }

      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const codeMatch = item.enquiry_code && item.enquiry_code.toLowerCase().includes(q);
        const nameMatch = item.sender_name && item.sender_name.toLowerCase().includes(q);
        const contactMatch = item.sender_contact && item.sender_contact.toLowerCase().includes(q);
        if (!codeMatch && !nameMatch && !contactMatch) return false;
      }

      return true;
    });
  }, [enquiries, selectedConfidence, searchQuery]);

  // Executive KPI metrics computed from loaded dataset
  const metrics = useMemo(() => {
    const totalOpen = enquiries.filter((e) => e.status === 'OPEN').length;
    const pendingDrafts = enquiries.filter((e) => e.has_active_draft).length;
    const complaints = enquiries.filter((e) => e.category === 'COMPLAINT' || e.gm_alerted).length;
    const unsorted = enquiries.filter((e) => e.category === 'UNSORTED' || (e.ai_confidence != null && Number(e.ai_confidence) < 0.80 && e.status === 'OPEN')).length;

    return { totalOpen, pendingDrafts, complaints, unsorted };
  }, [enquiries]);

  const formatDateTime = (isoString) => {
    if (!isoString) return '—';
    try {
      const date = new Date(isoString);
      return date.toLocaleString('en-IN', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch (e) {
      return isoString;
    }
  };

  const applyPreset = (preset) => {
    setSimChannel(preset.channel);
    setSimSenderName(preset.name);
    setSimSenderContact(preset.contact);
    setSimRawMessage(preset.message);
    setSimErrors({});
    setSimApiError(null);
  };

  const validateSimulatorForm = () => {
    const errors = {};
    if (!simSenderName.trim()) {
      errors.senderName = 'Sender name is required';
    }
    if (!simSenderContact.trim()) {
      errors.senderContact = 'Sender contact is required';
    } else if (simChannel === 'EMAIL_SIM') {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(simSenderContact.trim())) {
        errors.senderContact = 'Please enter a valid email address';
      }
    } else if (simChannel === 'WHATSAPP_SIM') {
      const digitsOnly = simSenderContact.replace(/\D/g, '');
      if (digitsOnly.length < 7) {
        errors.senderContact = 'Please enter a valid phone number (at least 7 digits)';
      }
    }
    if (!simRawMessage.trim()) {
      errors.rawMessage = 'Message text is required';
    }
    return errors;
  };

  const handleSimulateSubmit = async (e) => {
    if (e) e.preventDefault();
    const validationErrors = validateSimulatorForm();
    if (Object.keys(validationErrors).length > 0) {
      setSimErrors(validationErrors);
      return;
    }

    setSimErrors({});
    setSimApiError(null);
    setIsSimulating(true);

    try {
      const payload = {
        sourceChannel: simChannel,
        senderName: simSenderName.trim(),
        senderContact: simSenderContact.trim(),
        rawMessage: simRawMessage.trim(),
      };

      const result = await simulateAdminEnquiry(token, payload);

      // Successfully simulated
      setIsSimulatorOpen(false);
      setSimulationSuccess(result);

      // Reset form
      setSimSenderName('');
      setSimSenderContact('');
      setSimRawMessage('');

      // Refresh inbox list so the newly created enquiry is visible
      await loadEnquiries();
    } catch (err) {
      setSimApiError(err.message || 'Failed to simulate enquiry. Please try again.');
    } finally {
      setIsSimulating(false);
    }
  };

  return (
    <div className="staff-enquiries-page">
      {/* Page Header */}
      <div className="staff-page-header">
        <div>
          <h1 className="staff-page-title">
            {isGm ? 'Estate Concierge & Enquiry Management' : `${user?.fullName || 'Staff'}’s Specialty Lane`}
          </h1>
          <p className="staff-page-subtitle">
            {isGm
              ? 'Complete overview across weddings, corporate retreats, room reservations, and GM escalations.'
              : `Restricted lane view: Showing enquiries assigned or routed to ${userLane}.`}
          </p>
        </div>
        <div className="staff-page-actions">
          <Button
            variant="primary"
            size="small"
            onClick={() => {
              setSimApiError(null);
              setIsSimulatorOpen(true);
            }}
          >
            ⚡ Simulate Incoming Enquiry
          </Button>
          <Button variant="secondary" size="small" onClick={loadEnquiries} disabled={loading}>
            🔄 Refresh Inbox
          </Button>
        </div>
      </div>

      {/* Simulation Success Banner */}
      {simulationSuccess && (
        <div className="staff-sim-success-banner" role="status">
          <span className="staff-sim-success-banner__icon" aria-hidden="true">✨</span>
          <div className="staff-sim-success-banner__content">
            <strong className="staff-sim-success-banner__title">
              Inbound Enquiry Simulated &amp; Routed Successfully!
            </strong>
            <p className="staff-sim-success-banner__text">
              Reference: <strong className="staff-code-pill">{simulationSuccess.enquiry_code}</strong> |
              Channel: <strong>{simulationSuccess.source_channel === 'WHATSAPP_SIM' ? 'WhatsApp (Sim)' : 'Email (Sim)'}</strong> |
              Category: <strong>{simulationSuccess.category}</strong> |
              Assigned to: <strong>{simulationSuccess.assigned_to_user_name || 'Unassigned'}</strong>
              {simulationSuccess.has_active_draft ? ' • Draft ready for review' : ''}
            </p>
          </div>
          <div className="staff-sim-success-banner__actions">
            <Link
              to={`/staff/enquiries/${simulationSuccess.id}`}
              className="btn btn--small btn--primary staff-sim-success-banner__link"
            >
              Review &amp; Reply →
            </Link>
            <button
              type="button"
              className="staff-sim-success-banner__close"
              onClick={() => setSimulationSuccess(null)}
              aria-label="Dismiss banner"
            >
              ✕
            </button>
          </div>
        </div>
      )}

      {/* Prominent GM Alert Banner for Complaints */}
      {isGm && metrics.complaints > 0 && (
        <div className="gm-alert-banner" role="alert">
          <span className="gm-alert-banner__icon" aria-hidden="true">🚨</span>
          <div className="gm-alert-banner__content">
            <strong className="gm-alert-banner__title">Priority General Manager Attention Required</strong>
            <p className="gm-alert-banner__text">
              {metrics.complaints} guest {metrics.complaints === 1 ? 'complaint requires' : 'complaints require'} immediate executive review.
            </p>
          </div>
          <Button
            variant="danger"
            size="small"
            onClick={() => setSelectedCategory('COMPLAINT')}
          >
            View Complaints
          </Button>
        </div>
      )}

      {/* Metrics Row */}
      <div className="staff-metrics-grid">
        <Card className="staff-metric-card" padding="medium">
          <span className="staff-metric-card__label">Total Open</span>
          <div className="staff-metric-card__value-row">
            <span className="staff-metric-card__number">{metrics.totalOpen}</span>
            <span className="staff-metric-card__tag">Active</span>
          </div>
        </Card>

        <Card className="staff-metric-card" padding="medium">
          <span className="staff-metric-card__label">Pending AI Drafts</span>
          <div className="staff-metric-card__value-row">
            <span className="staff-metric-card__number">{metrics.pendingDrafts}</span>
            <span className="staff-metric-card__tag staff-metric-card__tag--pending">Awaiting Review</span>
          </div>
        </Card>

        {isGm && (
          <Card className={`staff-metric-card ${metrics.complaints > 0 ? 'staff-metric-card--alert' : ''}`} padding="medium">
            <span className="staff-metric-card__label">GM Complaints</span>
            <div className="staff-metric-card__value-row">
              <span className="staff-metric-card__number">{metrics.complaints}</span>
              <span className="staff-metric-card__tag staff-metric-card__tag--danger">Escalated</span>
            </div>
          </Card>
        )}

        {isGm && (
          <Card className="staff-metric-card" padding="medium">
            <span className="staff-metric-card__label">Unsorted / Low Conf.</span>
            <div className="staff-metric-card__value-row">
              <span className="staff-metric-card__number">{metrics.unsorted}</span>
              <span className="staff-metric-card__tag staff-metric-card__tag--amber">Review Queue</span>
            </div>
          </Card>
        )}
      </div>

      {/* Filter Toolbar */}
      <Card className="staff-filter-card" padding="medium">
        <div className="staff-filter-grid">
          {/* Search */}
          <div className="staff-filter-item">
            <label className="staff-filter-label" htmlFor="search-input">Search Ref / Guest</label>
            <input
              id="search-input"
              type="text"
              placeholder="e.g. ENQ-D2LV or Pooja"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="staff-filter-input"
            />
          </div>

          {/* Category Filter */}
          <div className="staff-filter-item">
            <label className="staff-filter-label" htmlFor="category-select">Occasion Category</label>
            <select
              id="category-select"
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="staff-filter-select"
            >
              <option value="">All Categories</option>
              {isGm ? (
                <>
                  <option value="WEDDING">💍 Wedding</option>
                  <option value="CORPORATE_OFFSITE">💼 Corporate Offsite</option>
                  <option value="ROOM_BOOKING">🛏️ Room Booking</option>
                  <option value="COMPLAINT">🚨 Complaint</option>
                  <option value="UNSORTED">📋 Unsorted</option>
                </>
              ) : (
                <>
                  {userLane === 'WEDDING' && <option value="WEDDING">💍 Wedding</option>}
                  {userLane === 'OFFSITE' && <option value="CORPORATE_OFFSITE">💼 Corporate Offsite</option>}
                  {userLane === 'ROOM' && <option value="ROOM_BOOKING">🛏️ Room Booking</option>}
                </>
              )}
            </select>
          </div>

          {/* Status Filter */}
          <div className="staff-filter-item">
            <label className="staff-filter-label" htmlFor="status-select">Status</label>
            <select
              id="status-select"
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="staff-filter-select"
            >
              <option value="">All Statuses</option>
              <option value="OPEN">Open (Awaiting Staff Action)</option>
              <option value="REPLIED">Replied</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>

          {/* Channel Filter */}
          <div className="staff-filter-item">
            <label className="staff-filter-label" htmlFor="channel-select">Channel</label>
            <select
              id="channel-select"
              value={selectedChannel}
              onChange={(e) => setSelectedChannel(e.target.value)}
              className="staff-filter-select"
            >
              <option value="">All Channels</option>
              <option value="WEB_FORM">Web Form</option>
              <option value="WHATSAPP_SIM">WhatsApp (Sim)</option>
              <option value="EMAIL_SIM">Email (Sim)</option>
            </select>
          </div>

          {/* Confidence Filter */}
          <div className="staff-filter-item">
            <label className="staff-filter-label" htmlFor="confidence-select">AI Confidence</label>
            <select
              id="confidence-select"
              value={selectedConfidence}
              onChange={(e) => setSelectedConfidence(e.target.value)}
              className="staff-filter-select"
            >
              <option value="">All Confidence</option>
              <option value="HIGH">High (≥ 80%)</option>
              <option value="LOW">Low (&lt; 80% / Fallback)</option>
            </select>
          </div>
        </div>

        {(selectedCategory || selectedStatus || selectedChannel || selectedConfidence || searchQuery) && (
          <div className="staff-filter-reset-row">
            <button
              type="button"
              className="staff-filter-reset-btn"
              onClick={() => {
                setSelectedCategory('');
                setSelectedStatus('');
                setSelectedChannel('');
                setSelectedConfidence('');
                setSearchQuery('');
              }}
            >
              Clear All Filters
            </button>
            <span className="staff-filter-count">
              Showing {filteredEnquiries.length} of {enquiries.length} enquiries
            </span>
          </div>
        )}
      </Card>

      {/* Error Banner */}
      {error && (
        <ErrorBanner
          title="Could Not Load Enquiries"
          message={error}
          onRetry={loadEnquiries}
        />
      )}

      {/* Loading Skeletons */}
      {loading && (
        <Card padding="large" className="staff-table-card">
          <Skeleton variant="text" width="40%" height="1.5rem" />
          <Skeleton variant="rectangular" width="100%" height="220px" style={{ marginTop: '1rem' }} />
        </Card>
      )}

      {/* Enquiries Table and Cards */}
      {!loading && !error && (
        <>
          {filteredEnquiries.length === 0 ? (
            <Card padding="large" className="staff-table-card">
              <EmptyState
                icon="📭"
                title="No Enquiries Found"
                description={
                  enquiries.length === 0
                    ? 'No enquiries are currently registered in this lane.'
                    : 'No enquiries match the selected filters or search query.'
                }
                action={
                  (selectedCategory || selectedStatus || selectedChannel || selectedConfidence || searchQuery) ? (
                    <Button
                      variant="secondary"
                      size="small"
                      onClick={() => {
                        setSelectedCategory('');
                        setSelectedStatus('');
                        setSelectedChannel('');
                        setSelectedConfidence('');
                        setSearchQuery('');
                      }}
                    >
                      Clear Filters
                    </Button>
                  ) : null
                }
              />
            </Card>
          ) : (
            <Card padding="none" className="staff-table-card">
              {/* Desktop Table */}
              <div className="staff-table-wrapper">
                <table className="staff-table">
                  <thead>
                    <tr>
                      <th scope="col">Reference</th>
                      <th scope="col">Guest</th>
                      <th scope="col">Occasion</th>
                      <th scope="col">Received</th>
                      <th scope="col">Assigned To</th>
                      <th scope="col">AI Confidence</th>
                      <th scope="col">Draft Status</th>
                      <th scope="col" className="staff-table__th-action">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredEnquiries.map((item) => {
                      const meta = CATEGORY_META[item.category] || CATEGORY_META.UNSORTED;
                      const conf = item.ai_confidence != null ? Math.round(Number(item.ai_confidence) * 100) : null;
                      const isComplaint = item.category === 'COMPLAINT' || item.gm_alerted;

                      return (
                        <tr key={item.id} className={isComplaint ? 'staff-tr--complaint' : ''}>
                          {/* Reference */}
                          <td className="staff-td__code">
                            <span className="staff-code-pill">{item.enquiry_code}</span>
                            {isComplaint && <span className="staff-badge-alert" title="GM Escalation Alert">🚨 GM</span>}
                          </td>

                          {/* Guest */}
                          <td>
                            <strong className="staff-guest-name">{item.sender_name || 'Guest'}</strong>
                            <div className="staff-guest-contact">{item.sender_contact || '—'}</div>
                          </td>

                          {/* Occasion / Category */}
                          <td>
                            <Badge variant={meta.badgeVariant}>
                              {meta.icon} {meta.label}
                            </Badge>
                          </td>

                          {/* Received */}
                          <td className="staff-td__time">
                            {formatDateTime(item.received_at)}
                          </td>

                          {/* Assigned To */}
                          <td>
                            <div className="staff-assignee">
                              <span className="staff-assignee__name">{item.assigned_to_user_name || 'Unassigned'}</span>
                              {item.assigned_to_user_lane && (
                                <span className="staff-assignee__lane">({item.assigned_to_user_lane})</span>
                              )}
                            </div>
                          </td>

                          {/* AI Confidence */}
                          <td>
                            {item.routing_method === 'RULE_FALLBACK' ? (
                              <span className="staff-conf-tag staff-conf-tag--fallback" title={item.ai_error_status || 'Deterministic Rule Fallback'}>
                                Fallback ({item.ai_error_status || 'Rules'})
                              </span>
                            ) : conf != null ? (
                              <span className={`staff-conf-tag ${conf >= 80 ? 'staff-conf-tag--high' : 'staff-conf-tag--low'}`}>
                                {conf}% AI Conf.
                              </span>
                            ) : (
                              <span className="staff-conf-tag staff-conf-tag--fallback">N/A</span>
                            )}
                          </td>

                          {/* Draft Status */}
                          <td>
                            {item.status === 'REPLIED' ? (
                              <Badge variant="success">Replied</Badge>
                            ) : item.has_active_draft ? (
                              <Badge variant="warning">Draft Ready</Badge>
                            ) : (
                              <Badge variant="neutral">Open</Badge>
                            )}
                          </td>

                          {/* Action */}
                          <td className="staff-table__td-action">
                            <Link
                              to={`/staff/enquiries/${item.id}`}
                              className="btn btn--small btn--primary staff-review-btn"
                            >
                              Review &amp; Reply →
                            </Link>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {/* Mobile Card List (375px responsive view) */}
              <div className="staff-mobile-cards">
                {filteredEnquiries.map((item) => {
                  const meta = CATEGORY_META[item.category] || CATEGORY_META.UNSORTED;
                  const conf = item.ai_confidence != null ? Math.round(Number(item.ai_confidence) * 100) : null;
                  const isComplaint = item.category === 'COMPLAINT' || item.gm_alerted;

                  return (
                    <div
                      key={item.id}
                      className={`staff-mobile-card ${isComplaint ? 'staff-mobile-card--complaint' : ''}`}
                    >
                      <div className="staff-mobile-card__header">
                        <span className="staff-code-pill">{item.enquiry_code}</span>
                        <Badge variant={meta.badgeVariant}>
                          {meta.icon} {meta.label}
                        </Badge>
                      </div>

                      <div className="staff-mobile-card__body">
                        <strong className="staff-guest-name">{item.sender_name || 'Guest'}</strong>
                        <div className="staff-guest-contact">{item.sender_contact || '—'}</div>
                        <div className="staff-mobile-card__meta">
                          <span>Received: {formatDateTime(item.received_at)}</span>
                          <span>Owner: {item.assigned_to_user_name || 'Unassigned'}</span>
                        </div>
                      </div>

                      <div className="staff-mobile-card__footer">
                        <div className="staff-mobile-card__badges">
                          {item.routing_method === 'RULE_FALLBACK' ? (
                            <span className="staff-conf-tag staff-conf-tag--fallback">
                              Fallback
                            </span>
                          ) : conf != null ? (
                            <span className={`staff-conf-tag ${conf >= 80 ? 'staff-conf-tag--high' : 'staff-conf-tag--low'}`}>
                              {conf}% AI
                            </span>
                          ) : null}

                          {item.status === 'REPLIED' ? (
                            <Badge variant="success">Replied</Badge>
                          ) : item.has_active_draft ? (
                            <Badge variant="warning">Draft Ready</Badge>
                          ) : (
                            <Badge variant="neutral">Open</Badge>
                          )}
                        </div>

                        <Link
                          to={`/staff/enquiries/${item.id}`}
                          className="btn btn--small btn--primary staff-mobile-card__btn"
                        >
                          Review &amp; Reply →
                        </Link>
                      </div>
                    </div>
                  );
                })}
              </div>
            </Card>
          )}
        </>
      )}

      {/* Simulator Modal */}
      <Modal
        isOpen={isSimulatorOpen}
        onClose={() => {
          if (!isSimulating) {
            setIsSimulatorOpen(false);
            setSimApiError(null);
          }
        }}
        title="Simulate Inbound Staff Enquiry"
        className="staff-simulator-modal"
        footer={
          <div className="staff-sim-footer">
            <Button
              variant="secondary"
              size="medium"
              onClick={() => setIsSimulatorOpen(false)}
              disabled={isSimulating}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="medium"
              onClick={handleSimulateSubmit}
              disabled={isSimulating}
            >
              {isSimulating ? 'Processing Pipeline…' : 'Simulate & Route Enquiry'}
            </Button>
          </div>
        }
      >
        <form onSubmit={handleSimulateSubmit} className="staff-sim-form">
          <div className="staff-sim-intro">
            Simulate an inbound guest enquiry via <strong>Email</strong> or <strong>WhatsApp</strong>. The enquiry will execute the backend AI extraction, classification, and routing pipeline, and generate a draft in <em>Draft Pending Review</em>.
          </div>

          {/* Quick Presets */}
          <div className="staff-sim-preset-section">
            <span className="staff-sim-preset-title">Quick Test Presets:</span>
            <div className="staff-sim-preset-buttons">
              {SIM_PRESETS.map((p, i) => (
                <button
                  key={i}
                  type="button"
                  className="staff-sim-preset-btn"
                  onClick={() => applyPreset(p)}
                >
                  {p.label}
                </button>
              ))}
            </div>
          </div>

          {/* Channel Selector */}
          <div className="staff-sim-field">
            <label className="staff-sim-label" id="sim-channel-label">Channel *</label>
            <div className="staff-sim-channels" role="radiogroup" aria-labelledby="sim-channel-label">
              <button
                type="button"
                role="radio"
                aria-checked={simChannel === 'EMAIL_SIM'}
                className={`staff-sim-channel-pill ${simChannel === 'EMAIL_SIM' ? 'staff-sim-channel-pill--active' : ''}`}
                onClick={() => {
                  setSimChannel('EMAIL_SIM');
                  setSimErrors((prev) => ({ ...prev, senderContact: undefined }));
                }}
              >
                📧 Email (EMAIL_SIM)
              </button>
              <button
                type="button"
                role="radio"
                aria-checked={simChannel === 'WHATSAPP_SIM'}
                className={`staff-sim-channel-pill ${simChannel === 'WHATSAPP_SIM' ? 'staff-sim-channel-pill--active' : ''}`}
                onClick={() => {
                  setSimChannel('WHATSAPP_SIM');
                  setSimErrors((prev) => ({ ...prev, senderContact: undefined }));
                }}
              >
                💬 WhatsApp (WHATSAPP_SIM)
              </button>
            </div>
          </div>

          {/* Sender Name */}
          <div className="staff-sim-field">
            <label className="staff-sim-label" htmlFor="sim-sender-name">
              Sender Name *
            </label>
            <input
              id="sim-sender-name"
              type="text"
              placeholder="e.g. Priyadarshini Rao"
              value={simSenderName}
              onChange={(e) => {
                setSimSenderName(e.target.value);
                if (simErrors.senderName) setSimErrors((prev) => ({ ...prev, senderName: undefined }));
              }}
              className={`staff-sim-input ${simErrors.senderName ? 'staff-sim-input--error' : ''}`}
            />
            {simErrors.senderName && <span className="staff-sim-error-text">{simErrors.senderName}</span>}
          </div>

          {/* Sender Contact */}
          <div className="staff-sim-field">
            <label className="staff-sim-label" htmlFor="sim-sender-contact">
              {simChannel === 'EMAIL_SIM' ? 'Sender Email Address *' : 'Sender WhatsApp Number *'}
            </label>
            <input
              id="sim-sender-contact"
              type={simChannel === 'EMAIL_SIM' ? 'email' : 'text'}
              placeholder={simChannel === 'EMAIL_SIM' ? 'e.g. priya.rao@example.com' : 'e.g. +91 98765 43210'}
              value={simSenderContact}
              onChange={(e) => {
                setSimSenderContact(e.target.value);
                if (simErrors.senderContact) setSimErrors((prev) => ({ ...prev, senderContact: undefined }));
              }}
              className={`staff-sim-input ${simErrors.senderContact ? 'staff-sim-input--error' : ''}`}
            />
            {simErrors.senderContact && <span className="staff-sim-error-text">{simErrors.senderContact}</span>}
          </div>

          {/* Raw Message */}
          <div className="staff-sim-field">
            <label className="staff-sim-label" htmlFor="sim-raw-message">
              Guest Message Content *
            </label>
            <textarea
              id="sim-raw-message"
              rows={4}
              placeholder="e.g. We are planning a 3-day family holiday in December for 4 guests. Do you have 2 villas available?"
              value={simRawMessage}
              onChange={(e) => {
                setSimRawMessage(e.target.value);
                if (simErrors.rawMessage) setSimErrors((prev) => ({ ...prev, rawMessage: undefined }));
              }}
              className={`staff-sim-textarea ${simErrors.rawMessage ? 'staff-sim-input--error' : ''}`}
            />
            {simErrors.rawMessage && <span className="staff-sim-error-text">{simErrors.rawMessage}</span>}
          </div>

          {/* API Error Banner if any */}
          {simApiError && (
            <div className="staff-sim-api-error" role="alert">
              ⚠️ {simApiError}
            </div>
          )}
        </form>
      </Modal>
    </div>
  );
}
