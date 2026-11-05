import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { staffLogin, fetchStaffProfile } from '../api/staffApi';

const TOKEN_STORAGE_KEY = 'chikkamagaluru_staff_jwt';

const StaffAuthContext = createContext(null);

export function StaffAuthProvider({ children }) {
  const [token, setToken] = useState(() => {
    try {
      return sessionStorage.getItem(TOKEN_STORAGE_KEY) || null;
    } catch (e) {
      return null;
    }
  });
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    try {
      sessionStorage.removeItem(TOKEN_STORAGE_KEY);
    } catch (e) {}
  }, []);

  const refreshProfile = useCallback(async (activeToken) => {
    if (!activeToken) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const profile = await fetchStaffProfile(activeToken);
      setUser(profile);
    } catch (err) {
      logout();
    } finally {
      setLoading(false);
    }
  }, [logout]);

  useEffect(() => {
    if (token) {
      refreshProfile(token);
    } else {
      setLoading(false);
    }
  }, [token, refreshProfile]);

  const login = async (email, password) => {
    const data = await staffLogin(email, password);
    const jwtToken = data.token;
    try {
      sessionStorage.setItem(TOKEN_STORAGE_KEY, jwtToken);
    } catch (e) {}
    setToken(jwtToken);
    const profile = await fetchStaffProfile(jwtToken);
    setUser(profile);
    return profile;
  };

  const value = {
    token,
    user,
    isAuthenticated: Boolean(token && user),
    loading,
    login,
    logout,
    refreshProfile: () => refreshProfile(token),
  };

  return (
    <StaffAuthContext.Provider value={value}>
      {children}
    </StaffAuthContext.Provider>
  );
}

export function useStaffAuth() {
  const context = useContext(StaffAuthContext);
  if (!context) {
    throw new Error('useStaffAuth must be used within a StaffAuthProvider');
  }
  return context;
}
