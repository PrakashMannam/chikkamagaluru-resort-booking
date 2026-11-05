import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useStaffAuth } from '../../context/StaffAuthContext';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import './StaffLayout.css';

export function StaffLayout({ children }) {
  const { user, logout } = useStaffAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/staff/login');
  };

  const getLaneDisplay = (lane) => {
    switch (lane) {
      case 'WEDDING': return 'Weddings Specialist';
      case 'OFFSITE': return 'Corporate Offsites Specialist';
      case 'ROOM': return 'Room Reservations Specialist';
      case 'GM_ESCALATION': return 'General Manager';
      default: return lane || 'Staff Member';
    }
  };

  const isEnquiriesActive = location.pathname.startsWith('/staff/enquiries');
  const isInventoryActive = location.pathname.startsWith('/staff/inventory');

  return (
    <div className="staff-layout">
      {/* Top Operational Bar */}
      <header className="staff-topbar">
        <div className="staff-topbar__inner">
          <div className="staff-topbar__brand">
            <Link to="/staff/enquiries" className="staff-topbar__logo">
              <span className="staff-topbar__logo-icon">☕</span>
              <div className="staff-topbar__logo-text">
                <span className="staff-topbar__logo-title">Chikkamagaluru Estate</span>
                <span className="staff-topbar__logo-sub">Operations &amp; AI Concierge</span>
              </div>
            </Link>
          </div>

          <div className="staff-topbar__user-section">
            {user && (
              <div className="staff-user-pill">
                <div className="staff-user-pill__avatar" aria-hidden="true">
                  {user.fullName ? user.fullName.charAt(0).toUpperCase() : 'S'}
                </div>
                <div className="staff-user-pill__info">
                  <span className="staff-user-pill__name">{user.fullName || user.email}</span>
                  <span className="staff-user-pill__lane">
                    {getLaneDisplay(user.specialtyLane || user.specialty_lane)}
                    {(user.isGm || user.is_gm) && <span className="staff-user-pill__gm-tag">GM</span>}
                  </span>
                </div>
              </div>
            )}

            <div className="staff-topbar__actions">
              <Link to="/" className="staff-topbar__public-link" target="_blank" rel="noreferrer">
                Guest Site ↗
              </Link>
              <Button
                variant="ghost"
                size="small"
                onClick={handleLogout}
                className="staff-topbar__logout-btn"
              >
                Sign Out
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Staff Secondary Nav / Breadcrumb Bar */}
      <nav className="staff-subnav" aria-label="Staff Navigation">
        <div className="staff-subnav__inner">
          <div className="staff-subnav__links">
            <Link
              to="/staff/enquiries"
              className={`staff-subnav__link ${isEnquiriesActive ? 'staff-subnav__link--active' : ''}`}
            >
              📥 Enquiries Inbox
            </Link>
            <Link
              to="/staff/inventory"
              className={`staff-subnav__link ${isInventoryActive ? 'staff-subnav__link--active' : ''}`}
            >
              Rates &amp; allotment
            </Link>
          </div>
          <div className="staff-subnav__meta">
            {(user?.isGm || user?.is_gm) ? (
              <Badge variant="warning">👑 All-Enquiry &amp; GM Escalation Access</Badge>
            ) : (
              <Badge variant="neutral">
                🔒 Filtered: {getLaneDisplay(user?.specialtyLane || user?.specialty_lane)}
              </Badge>
            )}
          </div>
        </div>
      </nav>

      {/* Main Content Workspace */}
      <main className="staff-main-content">
        {children}
      </main>
    </div>
  );
}
