import React, { Suspense, lazy, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { GuestLayout } from './components/layout/GuestLayout';
import { StaffLayout } from './components/staff/StaffLayout';
import { ProtectedRoute } from './components/staff/ProtectedRoute';
import { StaffAuthProvider } from './context/StaffAuthContext';
import { Skeleton, ErrorBoundary } from './components/ui';

const LandingPage = lazy(() => import('./pages/LandingPage'));
const GuestAvailabilityPage = lazy(() => import('./pages/GuestAvailabilityPage'));
const RoomDetailPage = lazy(() => import('./pages/RoomDetailPage'));
const GuestCheckoutPage = lazy(() => import('./pages/GuestCheckoutPage'));
const GuestPaymentPage = lazy(() => import('./pages/GuestPaymentPage'));
const BookingConfirmationPage = lazy(() => import('./pages/BookingConfirmationPage'));
const PublicEnquiryPage = lazy(() => import('./pages/PublicEnquiryPage'));
const WeddingsPage = lazy(() => import('./pages/WeddingsPage'));
const OffsitesPage = lazy(() => import('./pages/OffsitesPage'));
const PrivacyPage = lazy(() => import('./pages/PrivacyPage'));
const TermsPage = lazy(() => import('./pages/TermsPage'));
const StaffLoginPage = lazy(() => import('./pages/StaffLoginPage').then(m => ({ default: m.StaffLoginPage })));
const StaffEnquiriesPage = lazy(() => import('./pages/StaffEnquiriesPage').then(m => ({ default: m.StaffEnquiriesPage })));
const StaffEnquiryDetailPage = lazy(() => import('./pages/StaffEnquiryDetailPage').then(m => ({ default: m.StaffEnquiryDetailPage })));
const StaffInventoryPage = lazy(() => import('./pages/staff/StaffInventoryPage').then(m => ({ default: m.StaffInventoryPage })));
const Styleguide = lazy(() => import('./styleguide/Styleguide'));
const NotFoundPage = lazy(() => import('./pages/NotFoundPage'));


function ScrollToTop() {
  const { pathname, hash } = useLocation();

  useEffect(() => {
    document.body.style.overflow = '';
    if (!hash) {
      window.scrollTo(0, 0);
    }
  }, [pathname, hash]);

  return null;
}

function PageFallback() {
  return (
    <div style={{ maxWidth: '72rem', margin: '0 auto', padding: 'var(--space-6) var(--space-4)' }}>
      <Skeleton variant="text" width="40%" height="2rem" />
      <Skeleton variant="text" width="60%" height="1.25rem" style={{ marginTop: 'var(--space-2)' }} />
      <Skeleton variant="rectangular" width="100%" height="20rem" style={{ marginTop: 'var(--space-4)' }} />
    </div>
  );
}

export function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <StaffAuthProvider>
        <ErrorBoundary>
        <Suspense fallback={<PageFallback />}>
          <Routes>
            {/* Guest Public Flow */}
            <Route element={<GuestLayout />}>
              <Route path="/" element={<LandingPage />} />
              <Route path="/stay" element={<GuestAvailabilityPage />} />
              <Route path="/rooms" element={<Navigate to="/stay" replace />} />
              <Route path="/rooms/:code" element={<RoomDetailPage />} />
              <Route path="/checkout" element={<GuestCheckoutPage />} />
              <Route path="/stay/checkout" element={<GuestCheckoutPage />} />
              <Route path="/payment/:ref" element={<GuestPaymentPage />} />
              <Route path="/stay/payment/:ref" element={<GuestPaymentPage />} />
              <Route path="/bookings/:ref" element={<BookingConfirmationPage />} />
              <Route path="/stay/bookings/:ref" element={<BookingConfirmationPage />} />
              <Route path="/weddings" element={<WeddingsPage />} />
              <Route path="/offsites" element={<OffsitesPage />} />
              <Route path="/enquire" element={<PublicEnquiryPage />} />
              <Route path="/privacy" element={<PrivacyPage />} />
              <Route path="/terms" element={<TermsPage />} />
            </Route>

            {/* Staff Authentication & Portal */}
            <Route path="/staff/login" element={<StaffLoginPage />} />
            <Route path="/staff" element={<Navigate to="/staff/enquiries" replace />} />
            <Route
              path="/staff/enquiries"
              element={
                <ProtectedRoute>
                  <StaffLayout>
                    <StaffEnquiriesPage />
                  </StaffLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/staff/enquiries/:id"
              element={
                <ProtectedRoute>
                  <StaffLayout>
                    <StaffEnquiryDetailPage />
                  </StaffLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/staff/inventory"
              element={
                <ProtectedRoute>
                  <StaffLayout>
                    <StaffInventoryPage />
                  </StaffLayout>
                </ProtectedRoute>
              }
            />

            {/* Design System Styleguide */}
            <Route path="/styleguide" element={<Styleguide />} />

            {/* Fallback */}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Suspense>
      </ErrorBoundary>
      </StaffAuthProvider>
    </BrowserRouter>
  );
}

export default App;
