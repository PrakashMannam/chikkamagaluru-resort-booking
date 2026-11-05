import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useStaffAuth } from '../context/StaffAuthContext';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { ErrorBanner } from '../components/ui/ErrorBanner';
import './StaffLoginPage.css';

const DEMO_ACCOUNTS = [
  {
    role: 'General Manager',
    name: 'Vikram Rao',
    lane: 'All Enquiries & Escalations',
    email: 'gm.demo@companya-estate.test',
    password: 'DEMO-ONLY-NOT-REAL-PASS-2026!',
    badge: '👑 GM Access',
  },
  {
    role: 'Weddings Specialist',
    name: 'Karthik Urs',
    lane: 'Weddings Lane Only',
    email: 'weddings.demo@companya-estate.test',
    password: 'DEMO-ONLY-NOT-REAL-PASS-2026!',
    badge: '💍 Weddings',
  },
  {
    role: 'Corporate Offsites',
    name: 'Meera Nair',
    lane: 'Offsites Lane Only',
    email: 'offsites.demo@companya-estate.test',
    password: 'DEMO-ONLY-NOT-REAL-PASS-2026!',
    badge: '💼 Offsites',
  },
  {
    role: 'Room Reservations',
    name: 'Ananya Hegde',
    lane: 'Room Booking Lane Only',
    email: 'rooms.demo@companya-estate.test',
    password: 'DEMO-ONLY-NOT-REAL-PASS-2026!',
    badge: '🛏️ Rooms',
  },
];

export function StaffLoginPage() {
  const { login, isAuthenticated } = useStaffAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loginError, setLoginError] = useState(null);

  const destination = location.state?.from?.pathname || '/staff/enquiries';

  useEffect(() => {
    if (isAuthenticated) {
      navigate(destination, { replace: true });
    }
  }, [isAuthenticated, navigate, destination]);

  const handleQuickFill = (acc) => {
    setEmail(acc.email);
    setPassword(acc.password);
    setErrors({});
    setLoginError(null);
  };

  const validate = () => {
    const newErrors = {};
    if (!email.trim()) {
      newErrors.email = 'Staff email is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      newErrors.email = 'Please enter a valid email address.';
    }
    if (!password) {
      newErrors.password = 'Password is required.';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoginError(null);

    if (!validate()) return;

    setIsSubmitting(true);
    try {
      await login(email.trim(), password);
      navigate(destination, { replace: true });
    } catch (err) {
      setLoginError(err.message || 'Invalid email or password. Please verify your credentials.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="staff-login-page">
      <div className="staff-login-container">
        {/* Estate Header */}
        <div className="staff-login-header">
          <span className="staff-login-header__icon" aria-hidden="true">☕</span>
          <h1 className="staff-login-header__title">Chikkamagaluru Estate</h1>
          <p className="staff-login-header__subtitle">Internal Staff Portal &amp; AI Concierge</p>
        </div>

        {/* Login Card */}
        <Card className="staff-login-card" padding="large">
          <div className="staff-login-card__intro">
            <h2 className="staff-login-card__title">Staff Sign In</h2>
            <p className="staff-login-card__desc">
              Sign in with your estate credentials to review guest enquiries, manage specialty lanes, and approve AI reply drafts.
            </p>
          </div>

          {loginError && (
            <div className="staff-login-card__error">
              <ErrorBanner
                title="Authentication Failed"
                message={loginError}
              />
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate className="staff-login-form">
            <div className="staff-login-form__group">
              <Input
                label="Staff Email Address *"
                type="email"
                placeholder="e.g. gm.demo@companya-estate.test"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (errors.email) setErrors({ ...errors, email: null });
                }}
                error={errors.email}
                disabled={isSubmitting}
                autoComplete="email"
                required
              />
            </div>

            <div className="staff-login-form__group">
              <Input
                label="Password *"
                type="password"
                placeholder="••••••••••••••••"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (errors.password) setErrors({ ...errors, password: null });
                }}
                error={errors.password}
                disabled={isSubmitting}
                autoComplete="current-password"
                required
              />
            </div>

            <div className="staff-login-form__actions">
              <Button
                type="submit"
                variant="primary"
                size="large"
                disabled={isSubmitting}
                className="staff-login-form__submit-btn"
              >
                {isSubmitting ? (
                  <span className="staff-login-spinner">
                    <span className="staff-login-spinner__circle"></span>
                    Authenticating...
                  </span>
                ) : (
                  'Sign In to Dashboard'
                )}
              </Button>
            </div>
          </form>

          {/* Evaluator Quick Demo Accounts */}
          <div className="staff-demo-box">
            <div className="staff-demo-box__header">
              <span className="staff-demo-box__title">Demo Quick-Select Accounts</span>
              <span className="staff-demo-box__subtitle">Click an account to autofill credentials</span>
            </div>
            <div className="staff-demo-grid">
              {DEMO_ACCOUNTS.map((acc) => (
                <button
                  key={acc.email}
                  type="button"
                  className="staff-demo-btn"
                  onClick={() => handleQuickFill(acc)}
                  disabled={isSubmitting}
                >
                  <div className="staff-demo-btn__top">
                    <span className="staff-demo-btn__badge">{acc.badge}</span>
                    <strong className="staff-demo-btn__name">{acc.name}</strong>
                  </div>
                  <span className="staff-demo-btn__lane">{acc.lane}</span>
                </button>
              ))}
            </div>
          </div>
        </Card>

        {/* Footer Link */}
        <div className="staff-login-footer">
          <Link to="/" className="staff-login-footer__link">
            ← Return to Guest Website
          </Link>
        </div>
      </div>
    </div>
  );
}
