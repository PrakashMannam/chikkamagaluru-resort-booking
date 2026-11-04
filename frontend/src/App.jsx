import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

const Styleguide = lazy(() => import('./styleguide/Styleguide'));

export function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<div style={{ padding: '2rem' }}>Loading styleguide...</div>}>
        <Routes>
          <Route path="/styleguide" element={<Styleguide />} />
          <Route path="*" element={<Navigate to="/styleguide" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}

export default App;
