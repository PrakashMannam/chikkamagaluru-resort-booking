import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useStaffAuth } from '../context/StaffAuthContext';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { ErrorBanner } from '../components/ui/ErrorBanner';
import './StaffLoginPage.css';

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
                placeholder="name@resort.internal"
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
