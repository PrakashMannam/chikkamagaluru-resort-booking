import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useStaffAuth } from '../context/StaffAuthContext';
import {
  fetchAdminEnquiryDetail,
  fetchStaffMembers,
  routeAdminEnquiry,
  approveAdminDraft,
  discardAdminDraft,
} from '../api/staffApi';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Skeleton } from '../components/ui/Skeleton';
import { ErrorBanner } from '../components/ui/ErrorBanner';
import './StaffEnquiryDetailPage.css';

const CATEGORIES = [
  { id: 'WEDDING', label: '💍 Wedding' },
  { id: 'CORPORATE_OFFSITE', label: '💼 Corporate Offsite' },
  { id: 'ROOM_BOOKING', label: '🛏️ Room Booking' },
  { id: 'COMPLAINT', label: '🚨 Complaint (GM Escalation)' },
  { id: 'UNSORTED', label: '📋 Unsorted Queue' },
];

export function StaffEnquiryDetailPage() {
  const { id } = useParams();
  const { token, user } = useStaffAuth();

  const [enquiry, setEnquiry] = useState(null);
  const [staffList, setStaffList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Draft review state
  const [draftBody, setDraftBody] = useState('');
  const [isApproving, setIsApproving] = useState(false);
  const [approvalResult, setApprovalResult] = useState(null);
  const [draftActionError, setDraftActionError] = useState(null);

  // Rerouting state
  const [editCategory, setEditCategory] = useState('');
  const [editAssigneeId, setEditAssigneeId] = useState('');
  const [isRerouting, setIsRerouting] = useState(false);
  const [routeSuccessMsg, setRouteSuccessMsg] = useState(null);
  const [routeError, setRouteError] = useState(null);

  const isGm = Boolean(user?.isGm || user?.is_gm);

  const loadData = useCallback(async () => {
    if (!token || !id) return;
    setLoading(true);
    setError(null);
    try {
      const [detail, staff] = await Promise.all([
        fetchAdminEnquiryDetail(token, id),
        fetchStaffMembers(token).catch(() => []),
      ]);
      setEnquiry(detail);
      setStaffList(staff);
      setEditCategory(detail.category || 'UNSORTED');
      setEditAssigneeId(detail.assigned_to_user_id ? String(detail.assigned_to_user_id) : '');
      if (detail.active_draft) {
        setDraftBody(detail.active_draft.draft_body || '');
      }
    } catch (err) {
      setError(err.message || 'Unable to load enquiry details');
    } finally {
      setLoading(false);
    }
  }, [token, id]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Handle Reroute
  const handleReroute = async (e) => {
    e.preventDefault();
    setRouteError(null);
    setRouteSuccessMsg(null);
    setIsRerouting(true);
    try {
      const payload = {
        category: editCategory,
        assigned_to_user_id: editAssigneeId ? Number(editAssigneeId) : null,
      };
      const updated = await routeAdminEnquiry(token, id, payload);
      setEnquiry(updated);
      setRouteSuccessMsg('Enquiry successfully rerouted and reassigned.');
      setTimeout(() => setRouteSuccessMsg(null), 5000);
    } catch (err) {
      setRouteError(err.message || 'Failed to reroute enquiry');
    } finally {
      setIsRerouting(false);
    }
  };

  // Handle Approve Draft
  const handleApproveDraft = async () => {
    if (!draftBody.trim()) {
      setDraftActionError('Suggested response text cannot be empty.');
      return;
    }
    setDraftActionError(null);
    setIsApproving(true);
    try {
      const payload = { draft_body: draftBody };
      const res = await approveAdminDraft(token, id, payload);
      setApprovalResult(res);
      // Refresh local enquiry state
      const refreshed = await fetchAdminEnquiryDetail(token, id);
      setEnquiry(refreshed);
    } catch (err) {
      setDraftActionError(err.message || 'Approval failed');
    } finally {
      setIsApproving(false);
    }
  };

  // Handle Discard Draft
  const handleDiscardDraft = async () => {
    if (!window.confirm('Are you sure you want to discard this reply draft?')) {
      return;
    }
    setDraftActionError(null);
    try {
      const updated = await discardAdminDraft(token, id);
      setEnquiry(updated);
      setDraftBody('');
    } catch (err) {
      setDraftActionError(err.message || 'Failed to discard draft');
    }
  };

  const formatCurrency = (val) => {
    if (val == null) return null;
    const num = Number(val);
    if (isNaN(num)) return null;
    return `₹${num.toLocaleString('en-IN')}`;
  };

  if (loading) {
    return (
      <div className="staff-detail-loading">
        <Skeleton variant="text" width="30%" height="2rem" />
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginTop: '1.5rem' }}>
          <Skeleton variant="rectangular" width="100%" height="400px" />
          <Skeleton variant="rectangular" width="100%" height="400px" />
        </div>
      </div>
    );
  }

  if (error || !enquiry) {
    return (
      <div className="staff-detail-error">
        <ErrorBanner
          title="Access Interrupted"
          message={error || 'Enquiry record not found or inaccessible.'}
          onRetry={loadData}
        />
        <div style={{ marginTop: '1rem' }}>
          <Link to="/staff/enquiries" className="btn btn--secondary">
            ← Back to Enquiries Inbox
          </Link>
        </div>
      </div>
    );
  }

  const isComplaint = enquiry.category === 'COMPLAINT' || enquiry.gm_alerted;
  const activeDraft = enquiry.active_draft;
  const isDraftPending = activeDraft && activeDraft.status === 'DRAFT_PENDING_REVIEW';
  const isReplied = enquiry.status === 'REPLIED';

  return (
    <div className="staff-enquiry-detail-page">
      {/* Top Breadcrumb & Status */}
      <div className="staff-detail-nav">
        <Link to="/staff/enquiries" className="staff-detail-back-link">
          ← Back to Enquiries Inbox
        </Link>
        <div className="staff-detail-nav__badges">
          <Badge variant={enquiry.status === 'REPLIED' ? 'success' : 'neutral'}>
            Status: {enquiry.status}
          </Badge>
          <Badge variant="warning">
            Ref: {enquiry.enquiry_code}
          </Badge>
        </div>
      </div>

      {/* Prominent GM Alert for Complaints */}
      {isComplaint && (
        <div className="gm-complaint-banner" role="alert">
          <span className="gm-complaint-banner__icon" aria-hidden="true">🚨</span>
          <div className="gm-complaint-banner__text">
            <strong>Escalated to General Manager: Priority Guest Attention</strong>
            <p>
              This enquiry is flagged as a guest complaint. General Manager review is mandatory before any reply is sent.
            </p>
          </div>
        </div>
      )}

      {/* Two Column Operational Layout */}
      <div className="staff-detail-grid">
        {/* LEFT COLUMN: Guest & Extraction Context */}
        <div className="staff-detail-left">
          {/* 1. Guest Information Card */}
          <Card className="staff-detail-card" padding="medium">
            <h2 className="staff-detail-card__title">Guest Contact Information</h2>
            <div className="staff-meta-grid">
              <div className="staff-meta-item">
                <span className="staff-meta-label">Guest Name</span>
                <span className="staff-meta-value staff-meta-value--bold">{enquiry.sender_name || 'Guest'}</span>
              </div>
              <div className="staff-meta-item">
                <span className="staff-meta-label">Contact</span>
                <span className="staff-meta-value">{enquiry.sender_contact || '—'}</span>
              </div>
              <div className="staff-meta-item">
                <span className="staff-meta-label">Source Channel</span>
                <span className="staff-meta-value">
                  <Badge variant="neutral">{enquiry.source_channel || 'WEB_FORM'}</Badge>
                </span>
              </div>
              <div className="staff-meta-item">
                <span className="staff-meta-label">Received At</span>
                <span className="staff-meta-value">{new Date(enquiry.received_at).toLocaleString('en-IN')}</span>
              </div>
            </div>
          </Card>

          {/* 2. Extracted Requirements Card */}
          <Card className="staff-detail-card" padding="medium">
            <h2 className="staff-detail-card__title">Extracted Stay &amp; Event Parameters</h2>
            <div className="staff-meta-grid">
              <div className="staff-meta-item">
                <span className="staff-meta-label">Target / Event Date</span>
                <span className="staff-meta-value staff-meta-value--bold">
                  {enquiry.extracted_check_in || 'Flexible / In Message'}
                </span>
              </div>
              <div className="staff-meta-item">
                <span className="staff-meta-label">Guests Count</span>
                <span className="staff-meta-value">
                  {enquiry.extracted_guests != null ? `${enquiry.extracted_guests} guests` : 'Not specified'}
                </span>
              </div>
              <div className="staff-meta-item">
                <span className="staff-meta-label">Required Rooms</span>
                <span className="staff-meta-value">
                  {enquiry.extracted_rooms != null ? `${enquiry.extracted_rooms} keys` : 'Not specified'}
                </span>
              </div>
              <div className="staff-meta-item">
                <span className="staff-meta-label">Duration</span>
                <span className="staff-meta-value">
                  {enquiry.extracted_nights != null ? `${enquiry.extracted_nights} nights` : 'Not specified'}
                </span>
              </div>
              <div className="staff-meta-item staff-meta-item--span2">
                <span className="staff-meta-label">Approximate Budget</span>
                <span className="staff-meta-value staff-meta-value--bold">
                  {formatCurrency(enquiry.extracted_budget_inr) || 'Not explicitly stated'}
                </span>
              </div>
            </div>
          </Card>

          {/* 3. Verbatim Guest Message */}
          <Card className="staff-detail-card" padding="medium">
            <h2 className="staff-detail-card__title">Original Message from Guest</h2>
            <blockquote className="staff-raw-message">
              {enquiry.raw_message || 'No text provided.'}
            </blockquote>
          </Card>

          {/* 4. AI Diagnostics & Routing Audit */}
          <Card className="staff-detail-card" padding="medium">
            <h2 className="staff-detail-card__title">AI Extraction &amp; Routing Audit</h2>
            <div className="staff-meta-grid">
              <div className="staff-meta-item">
                <span className="staff-meta-label">Classification</span>
                <span className="staff-meta-value">
                  <Badge variant="warning">{enquiry.category}</Badge>
                </span>
              </div>
              <div className="staff-meta-item">
                <span className="staff-meta-label">AI Confidence</span>
                <span className="staff-meta-value">
                  {enquiry.ai_confidence != null ? `${Math.round(Number(enquiry.ai_confidence) * 100)}%` : '0%'}
                </span>
              </div>
              <div className="staff-meta-item">
                <span className="staff-meta-label">Routing Method</span>
                <span className="staff-meta-value">{enquiry.routing_method || 'RULE_FALLBACK'}</span>
              </div>
              <div className="staff-meta-item">
                <span className="staff-meta-label">AI Error Status</span>
                <span className="staff-meta-value">
                  {enquiry.ai_error_status ? (
                    <span className="staff-error-tag">{enquiry.ai_error_status}</span>
                  ) : (
                    'None (Clean Execution)'
                  )}
                </span>
              </div>
              <div className="staff-meta-item staff-meta-item--span2">
                <span className="staff-meta-label">AI Summary</span>
                <p className="staff-ai-summary-text">{enquiry.ai_summary || 'No summary generated.'}</p>
              </div>
            </div>
          </Card>

          {/* 5. Rerouting & Assignment (Staff or GM) */}
          <Card className="staff-detail-card" padding="medium">
            <h2 className="staff-detail-card__title">Reroute &amp; Specialist Assignment</h2>
            <p className="staff-card-desc">
              Correct inaccurate classifications or reassign this enquiry to another specialty lane coordinator.
            </p>

            {routeSuccessMsg && (
              <div className="staff-success-alert" role="status">
                ✓ {routeSuccessMsg}
              </div>
            )}

            {routeError && (
              <div className="staff-error-alert" role="alert">
                ⚠️ {routeError}
              </div>
            )}

            <form onSubmit={handleReroute} className="staff-reroute-form">
              <div className="staff-form-group">
                <label className="staff-form-label" htmlFor="reroute-category">Occasion Category</label>
                <select
                  id="reroute-category"
                  value={editCategory}
                  onChange={(e) => setEditCategory(e.target.value)}
                  className="staff-form-select"
                  disabled={isRerouting}
                >
                  {CATEGORIES.map((c) => (
                    <option key={c.id} value={c.id}>{c.label}</option>
                  ))}
                </select>
              </div>

              <div className="staff-form-group">
                <label className="staff-form-label" htmlFor="reroute-assignee">Assigned Staff Coordinator</label>
                <select
                  id="reroute-assignee"
                  value={editAssigneeId}
                  onChange={(e) => setEditAssigneeId(e.target.value)}
                  className="staff-form-select"
                  disabled={isRerouting}
                >
                  <option value="">Auto-Assign by Category</option>
                  {staffList.map((s) => (
                    <option key={s.id} value={String(s.id)}>
                      {s.fullName || s.full_name} ({s.specialtyLane || s.specialty_lane})
                      {(s.isGm || s.is_gm) ? ' [GM]' : ''}
                    </option>
                  ))}
                </select>
              </div>

              <Button
                type="submit"
                variant="secondary"
                size="small"
                disabled={isRerouting}
              >
                {isRerouting ? 'Updating Route...' : 'Save Category & Assignment'}
              </Button>
            </form>
          </Card>
        </div>

        {/* RIGHT COLUMN: AI Reply Review & Approval */}
        <div className="staff-detail-right">
          <Card className="staff-detail-card staff-draft-card" padding="large">
            <div className="staff-draft-card__header">
              <div>
                <span className="staff-draft-badge">
                  {isReplied ? '✓ Reply Dispatched' : 'AI Draft Response (Awaiting Review)'}
                </span>
                <h2 className="staff-draft-title">Review &amp; Approve Response</h2>
              </div>
              <Badge variant={isReplied ? 'success' : isDraftPending ? 'warning' : 'neutral'}>
                {activeDraft ? activeDraft.status : 'No Active Draft'}
              </Badge>
            </div>

            <p className="staff-draft-explainer">
              Draft synthesized by estate AI and verified against current rate plans. Staff review and manual approval is mandatory before any message is dispatched.
            </p>

            {/* Quoted Rate Breakdown Box */}
            {activeDraft && activeDraft.quoted_rate_breakdown_json && (
              <div className="staff-quote-breakdown-box">
                <h3 className="staff-quote-breakdown-title">
                  📊 Authoritative Database Rate Plan Attached
                </h3>
                <pre className="staff-quote-code">
                  {JSON.stringify(JSON.parse(activeDraft.quoted_rate_breakdown_json), null, 2)}
                </pre>
              </div>
            )}

            {/* Simulated Delivery Confirmation Banner */}
            {approvalResult && (
              <div className="staff-delivery-success-card" role="status">
                <span className="staff-delivery-success__icon" aria-hidden="true">✓</span>
                <div className="staff-delivery-success__content">
                  <strong>Simulated Dispatch Successful</strong>
                  <p>
                    Delivered to <strong>{approvalResult.recipient}</strong> via <strong>{approvalResult.delivery_channel}</strong> simulation log at {new Date(approvalResult.dispatched_at).toLocaleTimeString('en-IN')}.
                  </p>
                  <span className="staff-delivery-note">
                    (Simulated delivery — no real external email/WhatsApp sent)
                  </span>
                </div>
              </div>
            )}

            {draftActionError && (
              <div className="staff-error-alert" role="alert">
                ⚠️ {draftActionError}
              </div>
            )}

            {/* Reply Textarea */}
            <div className="staff-draft-editor">
              <label className="staff-form-label" htmlFor="reply-textarea">
                Response Message (Editable by Staff Coordinator) *
              </label>
              <textarea
                id="reply-textarea"
                rows={12}
                value={draftBody}
                onChange={(e) => setDraftBody(e.target.value)}
                disabled={isApproving || isReplied}
                className="staff-draft-textarea"
                placeholder="Compose or refine the estate's response to the guest..."
              />
            </div>

            {/* Draft Metadata */}
            {activeDraft && (
              <div className="staff-draft-meta-row">
                <span>Generated via: <strong>{activeDraft.generated_by || 'AI_ROUTER'}</strong></span>
                {activeDraft.sent_by_user_name && (
                  <span>Approved by: <strong>{activeDraft.sent_by_user_name}</strong></span>
                )}
                {activeDraft.sent_at && (
                  <span>Dispatched: <strong>{new Date(activeDraft.sent_at).toLocaleString('en-IN')}</strong></span>
                )}
              </div>
            )}

            {/* Action Bar */}
            <div className="staff-draft-actions">
              <div className="staff-simulation-notice">
                <span className="staff-simulation-notice__icon">ℹ️</span>
                <span>
                  Approval executes <strong>simulated message delivery</strong>. No actual WhatsApp or email will be transmitted to external servers.
                </span>
              </div>

              <div className="staff-draft-btn-group">
                {isDraftPending && (
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={handleDiscardDraft}
                    disabled={isApproving}
                    className="staff-btn--discard"
                  >
                    Discard Draft
                  </Button>
                )}

                <Button
                  type="button"
                  variant="primary"
                  size="large"
                  onClick={handleApproveDraft}
                  disabled={isApproving || isReplied || !draftBody.trim()}
                  className="staff-btn--approve"
                >
                  {isApproving
                    ? 'Processing Simulated Delivery...'
                    : isReplied
                    ? '✓ Response Dispatched'
                    : 'Approve & Dispatch Simulated Reply →'}
                </Button>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
