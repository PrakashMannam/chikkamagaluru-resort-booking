import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useStaffAuth } from '../../context/StaffAuthContext';
import { Skeleton } from '../ui/Skeleton';

export function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useStaffAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div style={{ maxWidth: '800px', margin: '4rem auto', padding: '1rem' }}>
        <Skeleton variant="text" width="60%" height="2rem" />
        <Skeleton variant="rectangular" width="100%" height="300px" style={{ marginTop: '1.5rem' }} />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/staff/login" state={{ from: location }} replace />;
  }

  return children;
}
