import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useStaffAuth } from '../../context/StaffAuthContext';
import {
  fetchInventoryRates,
  bulkUpdateInventoryRates,
  fetchAdminBlackouts,
  createAdminBlackout,
  deleteAdminBlackout,
  fetchAdminPromos,
  createAdminPromo,
  updateAdminPromoActive,
} from '../../api/staffApi';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Skeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorBanner } from '../../components/ui/ErrorBanner';
import { Tabs } from '../../components/ui/Tabs';
import './StaffInventoryPage.css';

const SEED_MIN_DATE = '2026-11-01';
const SEED_MAX_DATE = '2027-04-30';

// Default to 14 days starting from demo baseline Nov 5, 2026
const DEFAULT_FROM = '2026-11-05';
const DEFAULT_TO = '2026-11-18';

export function StaffInventoryPage() {
  const { token } = useStaffAuth();
  const [activeTab, setActiveTab] = useState('rates');

  // ==========================================
  // TAB 1: RATES & ALLOTMENT STATE
  // ==========================================
  const [filterFrom, setFilterFrom] = useState(DEFAULT_FROM);
  const [filterTo, setFilterTo] = useState(DEFAULT_TO);
  const [rates, setRates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Bulk editor state
  const [bulkCategory, setBulkCategory] = useState('ROOM');
  const [bulkFrom, setBulkFrom] = useState(DEFAULT_FROM);
  const [bulkTo, setBulkTo] = useState(DEFAULT_TO);
  const [bulkRate, setBulkRate] = useState('');
  const [bulkAllotment, setBulkAllotment] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [bulkError, setBulkError] = useState(null);
  const [bulkSuccess, setBulkSuccess] = useState(null);

  // ==========================================
  // TAB 2: BLACKOUT DATES STATE
  // ==========================================
  const [blackouts, setBlackouts] = useState([]);
  const [blackoutLoading, setBlackoutLoading] = useState(false);
  const [blackoutError, setBlackoutError] = useState(null);
  const [blackoutSuccess, setBlackoutSuccess] = useState(null);
  const [newBlackoutCategory, setNewBlackoutCategory] = useState('ALL');
  const [newBlackoutDate, setNewBlackoutDate] = useState('');
  const [newBlackoutReason, setNewBlackoutReason] = useState('');
  const [isCreatingBlackout, setIsCreatingBlackout] = useState(false);
  const [deletingBlackoutId, setDeletingBlackoutId] = useState(null);

  // ==========================================
  // TAB 3: PROMO CODES STATE
  // ==========================================
  const [promos, setPromos] = useState([]);
  const [promoLoading, setPromoLoading] = useState(false);
  const [promoError, setPromoError] = useState(null);
  const [promoSuccess, setPromoSuccess] = useState(null);
  const [newPromoCode, setNewPromoCode] = useState('');
  const [newPromoType, setNewPromoType] = useState('PERCENTAGE');
  const [newPromoValue, setNewPromoValue] = useState('');
  const [newPromoFrom, setNewPromoFrom] = useState(SEED_MIN_DATE);
  const [newPromoTo, setNewPromoTo] = useState(SEED_MAX_DATE);
  const [newPromoMinNights, setNewPromoMinNights] = useState('1');
  const [newPromoMaxUses, setNewPromoMaxUses] = useState('500');
  const [isCreatingPromo, setIsCreatingPromo] = useState(false);
  const [togglingPromoId, setTogglingPromoId] = useState(null);

  // ==========================================
  // LOADERS
  // ==========================================
  const loadRates = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const data = await fetchInventoryRates(token, filterFrom, filterTo);
      setRates(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to load inventory rates.');
    } finally {
      setLoading(false);
    }
  }, [token, filterFrom, filterTo]);

  const loadBlackouts = useCallback(async () => {
    if (!token) return;
    setBlackoutLoading(true);
    setBlackoutError(null);
    try {
      const data = await fetchAdminBlackouts(token);
      setBlackouts(Array.isArray(data) ? data : []);
    } catch (err) {
      setBlackoutError(err.message || 'Failed to load blackout dates.');
    } finally {
      setBlackoutLoading(false);
    }
  }, [token]);

  const loadPromos = useCallback(async () => {
    if (!token) return;
    setPromoLoading(true);
    setPromoError(null);
    try {
      const data = await fetchAdminPromos(token);
      setPromos(Array.isArray(data) ? data : []);
    } catch (err) {
      setPromoError(err.message || 'Failed to load promotional codes.');
    } finally {
      setPromoLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadRates();
  }, [loadRates]);

  useEffect(() => {
    if (activeTab === 'blackouts') {
      loadBlackouts();
    } else if (activeTab === 'promos') {
      loadPromos();
    }
  }, [activeTab, loadBlackouts, loadPromos]);

  // Helpers
  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    try {
      const [year, month, day] = dateStr.split('-');
      const d = new Date(year, month - 1, day);
      return d.toLocaleDateString('en-IN', {
        weekday: 'short',
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      });
    } catch (e) {
      return dateStr;
    }
  };

  const formatPrice = (val) => {
    if (val == null) return '—';
    return `₹${Number(val).toLocaleString('en-IN')}`;
  };

  // Group rates by category
  const roomRates = useMemo(() => {
    return rates.filter((r) => r.categoryCode === 'ROOM');
  }, [rates]);

  const villaRates = useMemo(() => {
    return rates.filter((r) => r.categoryCode === 'VILLA');
  }, [rates]);

  // Preset range
  const setRangePreset = (days) => {
    const start = new Date(DEFAULT_FROM + 'T00:00:00');
    const end = new Date(start);
    end.setDate(start.getDate() + (days - 1));

    const endStr = end.toISOString().split('T')[0];
    setFilterFrom(DEFAULT_FROM);
    setFilterTo(endStr);
  };

  // Bulk rate submit
  const handleBulkSubmit = async (e) => {
    e.preventDefault();
    if (!token) return;

    setBulkError(null);
    setBulkSuccess(null);

    const rateNum = parseFloat(bulkRate);
    if (isNaN(rateNum) || rateNum <= 0) {
      setBulkError('Please enter a valid base rate greater than 0.');
      return;
    }

    const allotmentNum = parseInt(bulkAllotment, 10);
    if (isNaN(allotmentNum) || allotmentNum < 0) {
      setBulkError('Please enter a valid website allotment (0 or greater).');
      return;
    }

    const payload = {
      roomCategoryCode: bulkCategory,
      from: bulkFrom,
      to: bulkTo,
      baseRateInr: rateNum,
      websiteAllotmentTotal: allotmentNum,
    };

    setIsSaving(true);
    try {
      const updatedRows = await bulkUpdateInventoryRates(token, payload);
      setBulkSuccess(
        `Successfully updated ${updatedRows.length} daily records for ${
          bulkCategory === 'ROOM' ? 'Estate Rooms' : 'Private Villas'
        }!`
      );
      await loadRates();
    } catch (err) {
      let msg = err.message || 'Failed to apply bulk update.';
      if (err.data && err.data.missingDates && Array.isArray(err.data.missingDates)) {
        msg = `${err.message} (Dates: ${err.data.missingDates.join(', ')})`;
      }
      setBulkError(msg);
    } finally {
      setIsSaving(false);
    }
  };

  // Blackout Handlers
  const handleCreateBlackout = async (e) => {
    e.preventDefault();
    if (!token) return;

    setBlackoutError(null);
    setBlackoutSuccess(null);

    if (!newBlackoutDate) {
      setBlackoutError('Please select a blackout date.');
      return;
    }

    const payload = {
      roomCategoryCode: newBlackoutCategory === 'ALL' ? null : newBlackoutCategory,
      blackoutDate: newBlackoutDate,
      reason: newBlackoutReason.trim() || 'Resort Blackout',
    };

    setIsCreatingBlackout(true);
    try {
      const created = await createAdminBlackout(token, payload);
      setBlackoutSuccess(
        `Created blackout for ${created.roomCategoryName || 'All Resort'} on ${formatDate(
          created.blackoutDate
        )}.`
      );
      setNewBlackoutDate('');
      setNewBlackoutReason('');
      await loadBlackouts();
      loadRates();
    } catch (err) {
      setBlackoutError(err.message || 'Failed to create blackout date.');
    } finally {
      setIsCreatingBlackout(false);
    }
  };

  const handleDeleteBlackout = async (id, blackoutDate) => {
    if (!token) return;
    setBlackoutError(null);
    setBlackoutSuccess(null);
    setDeletingBlackoutId(id);
    try {
      await deleteAdminBlackout(token, id);
      setBlackoutSuccess(`Removed blackout date for ${formatDate(blackoutDate)}.`);
      await loadBlackouts();
      loadRates();
    } catch (err) {
      setBlackoutError(err.message || 'Failed to remove blackout date.');
    } finally {
      setDeletingBlackoutId(null);
    }
  };

  // Promo Handlers
  const handleCreatePromo = async (e) => {
    e.preventDefault();
    if (!token) return;

    setPromoError(null);
    setPromoSuccess(null);

    const valNum = parseFloat(newPromoValue);
    if (isNaN(valNum) || valNum <= 0) {
      setPromoError('Please enter a valid discount value greater than 0.');
      return;
    }
    if (newPromoType === 'PERCENTAGE' && valNum > 100) {
      setPromoError('Percentage discount cannot exceed 100%.');
      return;
    }

    const minN = parseInt(newPromoMinNights, 10) || 1;
    const maxU = parseInt(newPromoMaxUses, 10);
    if (isNaN(maxU) || maxU < 1) {
      setPromoError('Max uses must be at least 1.');
      return;
    }

    const payload = {
      code: newPromoCode.trim().toUpperCase(),
      discountType: newPromoType,
      discountValue: valNum,
      validFrom: newPromoFrom,
      validTo: newPromoTo,
      minNights: minN,
      maxUses: maxU,
      isActive: true,
    };

    setIsCreatingPromo(true);
    try {
      const created = await createAdminPromo(token, payload);
      setPromoSuccess(`Created promotional code '${created.code}' successfully!`);
      setNewPromoCode('');
      setNewPromoValue('');
      await loadPromos();
    } catch (err) {
      setPromoError(err.message || 'Failed to create promo code.');
    } finally {
      setIsCreatingPromo(false);
    }
  };

  const handleTogglePromoActive = async (id, currentActive, code) => {
    if (!token) return;
    setPromoError(null);
    setPromoSuccess(null);
    setTogglingPromoId(id);
    try {
      const updated = await updateAdminPromoActive(token, id, !currentActive);
      setPromoSuccess(
        `Promotional code '${updated.code}' is now ${updated.isActive ? 'Active' : 'Inactive'}.`
      );
      await loadPromos();
    } catch (err) {
      setPromoError(err.message || 'Failed to update promo status.');
    } finally {
      setTogglingPromoId(null);
    }
  };

  // Render Rates Table
  const renderRatesTable = (rows, categoryName, defaultPhysicalKeys) => {
    if (rows.length === 0) {
      return (
        <div style={{ padding: '1rem', color: 'var(--color-text-muted)' }}>
          No records found for {categoryName} in this date range.
        </div>
      );
    }

    return (
      <>
        {/* Desktop Table View */}
        <div className="staff-inventory__table-wrapper">
          <table className="staff-inventory__table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Base Rate (INR)</th>
                <th>Website Allotment</th>
                <th>Booked Count</th>
                <th>Remaining Keys</th>
                <th>Physical Key Cap</th>
                <th>Status / Blackout</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => {
                const remaining = row.websiteAllotmentTotal - row.bookedCount;
                return (
                  <tr
                    key={`${row.categoryCode}-${row.stayDate}`}
                    className={row.blackout ? 'staff-inventory__table tr--blackout' : ''}
                  >
                    <td>
                      <strong>{formatDate(row.stayDate)}</strong>
                      <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                        {row.stayDate}
                      </div>
                    </td>
                    <td className="staff-inventory__rate-val">{formatPrice(row.baseRateInr)}</td>
                    <td className="staff-inventory__allotment-val">{row.websiteAllotmentTotal}</td>
                    <td>{row.bookedCount}</td>
                    <td>
                      <span
                        className={`staff-inventory__remaining-val ${
                          remaining > 0
                            ? 'staff-inventory__remaining-val--available'
                            : 'staff-inventory__remaining-val--zero'
                        }`}
                      >
                        {remaining} remaining
                      </span>
                    </td>
                    <td className="staff-inventory__cap-badge">
                      {row.totalPhysicalKeys || defaultPhysicalKeys} keys max
                    </td>
                    <td>
                      {row.blackout ? (
                        <div>
                          <Badge variant="danger">Blackout</Badge>
                          {row.blackoutReason && (
                            <div className="staff-inventory__blackout-reason" title={row.blackoutReason}>
                              {row.blackoutReason}
                            </div>
                          )}
                        </div>
                      ) : (
                        <span style={{ color: 'var(--color-text-muted)' }}>Available</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Mobile Cards View (<= 768px) */}
        <div className="staff-inventory__mobile-cards staff-inventory__rates-mobile-cards">
          {rows.map((row) => {
            const remaining = row.websiteAllotmentTotal - row.bookedCount;
            return (
              <div
                key={`mobile-${row.categoryCode}-${row.stayDate}`}
                className={`staff-inventory__rate-card ${row.blackout ? 'staff-inventory__rate-card--blackout' : ''}`}
              >
                <div className="staff-inventory__rate-card-header">
                  <div>
                    <strong>{formatDate(row.stayDate)}</strong>
                    <span className="staff-inventory__rate-card-subdate">{row.stayDate}</span>
                  </div>
                  <div className="staff-inventory__rate-val">{formatPrice(row.baseRateInr)}</div>
                </div>
                <div className="staff-inventory__rate-card-body">
                  <div className="staff-inventory__rate-card-metrics">
                    <div className="staff-inventory__rate-card-metric">
                      <span className="staff-inventory__rate-card-metric-label">Allotment</span>
                      <span className="staff-inventory__rate-card-metric-val">{row.websiteAllotmentTotal}</span>
                    </div>
                    <div className="staff-inventory__rate-card-metric">
                      <span className="staff-inventory__rate-card-metric-label">Booked</span>
                      <span className="staff-inventory__rate-card-metric-val">{row.bookedCount}</span>
                    </div>
                    <div className="staff-inventory__rate-card-metric">
                      <span className="staff-inventory__rate-card-metric-label">Cap</span>
                      <span className="staff-inventory__rate-card-metric-val">
                        {row.totalPhysicalKeys || defaultPhysicalKeys} max
                      </span>
                    </div>
                  </div>
                  <div className="staff-inventory__rate-card-status">
                    <span
                      className={`staff-inventory__remaining-val ${
                        remaining > 0
                          ? 'staff-inventory__remaining-val--available'
                          : 'staff-inventory__remaining-val--zero'
                      }`}
                    >
                      {remaining} remaining
                    </span>
                    {row.blackout ? (
                      <span className="staff-inventory__blackout-inline">
                        <Badge variant="danger">Blackout</Badge>
                        {row.blackoutReason && (
                          <span className="staff-inventory__blackout-reason-text" title={row.blackoutReason}>
                            {row.blackoutReason}
                          </span>
                        )}
                      </span>
                    ) : (
                      <span style={{ fontSize: '0.8125rem', color: 'var(--color-text-muted)' }}>Available</span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </>
    );
  };

  // Tab Item Definitions
  const tabItems = [
    {
      id: 'rates',
      label: '📅 Rates & Allotment',
      content: (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', marginTop: '1rem' }}>
          {/* Date Range Filter Bar */}
          <div className="staff-inventory__filter-card">
            <div className="staff-inventory__filter-row">
              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="inv-from">
                  From Date
                </label>
                <input
                  id="inv-from"
                  type="date"
                  className="staff-inventory__input"
                  value={filterFrom}
                  min={SEED_MIN_DATE}
                  max={SEED_MAX_DATE}
                  onChange={(e) => setFilterFrom(e.target.value)}
                />
              </div>

              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="inv-to">
                  To Date (Max 62 Days)
                </label>
                <input
                  id="inv-to"
                  type="date"
                  className="staff-inventory__input"
                  value={filterTo}
                  min={SEED_MIN_DATE}
                  max={SEED_MAX_DATE}
                  onChange={(e) => setFilterTo(e.target.value)}
                />
              </div>

              <div className="staff-inventory__filter-actions">
                <button
                  type="button"
                  className="staff-inventory__quick-btn"
                  onClick={() => setRangePreset(14)}
                >
                  Next 14 Days
                </button>
                <button
                  type="button"
                  className="staff-inventory__quick-btn"
                  onClick={() => setRangePreset(30)}
                >
                  Next 30 Days
                </button>
                <Button
                  variant="outline"
                  size="medium"
                  onClick={loadRates}
                  disabled={loading}
                >
                  {loading ? 'Loading...' : 'Refresh Grid'}
                </Button>
              </div>
            </div>
          </div>

          {/* Bulk Rate & Allotment Editor */}
          <div className="staff-inventory__bulk-card">
            <div className="staff-inventory__bulk-header">
              <h2 className="staff-inventory__bulk-title">Bulk Rate &amp; Allotment Editor</h2>
              <p className="staff-inventory__bulk-subtitle">
                Update base rate and allotment for a category across a consecutive date range.
                Booked count and version are preserved safely.
              </p>
            </div>

            <form onSubmit={handleBulkSubmit} className="staff-inventory__bulk-form">
              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="bulk-cat">
                  Accommodation Type
                </label>
                <select
                  id="bulk-cat"
                  className="staff-inventory__select"
                  value={bulkCategory}
                  onChange={(e) => setBulkCategory(e.target.value)}
                  disabled={isSaving}
                >
                  <option value="ROOM">Estate Rooms (Cap: 64 keys)</option>
                  <option value="VILLA">Private Villas (Cap: 12 keys)</option>
                </select>
              </div>

              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="bulk-from">
                  Start Date
                </label>
                <input
                  id="bulk-from"
                  type="date"
                  className="staff-inventory__input"
                  value={bulkFrom}
                  min={SEED_MIN_DATE}
                  max={SEED_MAX_DATE}
                  onChange={(e) => setBulkFrom(e.target.value)}
                  disabled={isSaving}
                  required
                />
              </div>

              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="bulk-to">
                  End Date
                </label>
                <input
                  id="bulk-to"
                  type="date"
                  className="staff-inventory__input"
                  value={bulkTo}
                  min={SEED_MIN_DATE}
                  max={SEED_MAX_DATE}
                  onChange={(e) => setBulkTo(e.target.value)}
                  disabled={isSaving}
                  required
                />
              </div>

              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="bulk-rate">
                  Base Rate (₹ INR)
                </label>
                <input
                  id="bulk-rate"
                  type="number"
                  step="0.01"
                  min="1"
                  placeholder="e.g. 9500"
                  className="staff-inventory__input"
                  value={bulkRate}
                  onChange={(e) => setBulkRate(e.target.value)}
                  disabled={isSaving}
                  required
                />
              </div>

              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="bulk-allotment">
                  Website Allotment
                </label>
                <input
                  id="bulk-allotment"
                  type="number"
                  step="1"
                  min="0"
                  max={bulkCategory === 'ROOM' ? 64 : 12}
                  placeholder={bulkCategory === 'ROOM' ? 'Max 64' : 'Max 12'}
                  className="staff-inventory__input"
                  value={bulkAllotment}
                  onChange={(e) => setBulkAllotment(e.target.value)}
                  disabled={isSaving}
                  required
                />
              </div>

              <div className="staff-inventory__bulk-submit">
                <Button
                  type="submit"
                  variant="primary"
                  size="medium"
                  disabled={isSaving || !bulkRate || bulkAllotment === ''}
                >
                  {isSaving ? 'Saving Changes...' : 'Save Changes'}
                </Button>
              </div>
            </form>

            {bulkError && (
              <div className="staff-inventory__feedback">
                <ErrorBanner
                  title="Failed to update inventory"
                  message={bulkError}
                  onDismiss={() => setBulkError(null)}
                />
              </div>
            )}

            {bulkSuccess && (
              <div className="staff-inventory__feedback">
                <div
                  style={{
                    backgroundColor: 'var(--color-success-tint, #E4ECE3)',
                    color: 'var(--color-success, #2E5C31)',
                    padding: '0.75rem 1rem',
                    borderRadius: 'var(--radius-sm, 2px)',
                    fontWeight: 500,
                    fontSize: '0.875rem',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                  }}
                >
                  <span>✓ {bulkSuccess}</span>
                  <button
                    type="button"
                    className="staff-inventory__dismiss-btn"
                    aria-label="Dismiss success message"
                    onClick={() => setBulkSuccess(null)}
                  >
                    ✕
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Grid Error Banner */}
          {error && (
            <ErrorBanner
              title="Could not load inventory rates"
              message={error}
              onDismiss={() => setError(null)}
            />
          )}

          {/* Rates Grid */}
          {loading ? (
            <div className="staff-inventory__groups">
              <div className="staff-inventory__group-card">
                <Skeleton variant="text" width="200px" height="1.5rem" style={{ marginBottom: '1rem' }} />
                <Skeleton variant="rectangular" width="100%" height="240px" />
              </div>
              <div className="staff-inventory__group-card">
                <Skeleton variant="text" width="200px" height="1.5rem" style={{ marginBottom: '1rem' }} />
                <Skeleton variant="rectangular" width="100%" height="240px" />
              </div>
            </div>
          ) : rates.length === 0 ? (
            <EmptyState
              title="No inventory records found"
              description="There are no inventory rates recorded for the selected dates. Please adjust the date filters."
              action={
                <Button variant="outline" size="small" onClick={() => setRangePreset(14)}>
                  Reset to 14-day window
                </Button>
              }
            />
          ) : (
            <div className="staff-inventory__groups">
              {/* Estate Rooms Group */}
              <section className="staff-inventory__group-card">
                <div className="staff-inventory__group-header">
                  <h2 className="staff-inventory__group-title">Estate Rooms</h2>
                  <span className="staff-inventory__group-meta">
                    Physical Capacity: 64 Keys &bull; {roomRates.length} days loaded
                  </span>
                </div>
                {renderRatesTable(roomRates, 'Estate Rooms', 64)}
              </section>

              {/* Private Villas Group */}
              <section className="staff-inventory__group-card">
                <div className="staff-inventory__group-header">
                  <h2 className="staff-inventory__group-title">Private Villas</h2>
                  <span className="staff-inventory__group-meta">
                    Physical Capacity: 12 Keys &bull; {villaRates.length} days loaded
                  </span>
                </div>
                {renderRatesTable(villaRates, 'Private Villas', 12)}
              </section>
            </div>
          )}
        </div>
      ),
    },
    {
      id: 'blackouts',
      label: '🚫 Blackout Dates',
      content: (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', marginTop: '1rem' }}>
          {/* Create Blackout Card */}
          <div className="staff-inventory__bulk-card">
            <div className="staff-inventory__bulk-header">
              <h2 className="staff-inventory__bulk-title">Add Blackout Date</h2>
              <p className="staff-inventory__bulk-subtitle">
                Block stay dates for the entire resort or a specific category. Changes immediately prevent new guest bookings without affecting existing reservations.
              </p>
            </div>

            <form onSubmit={handleCreateBlackout} className="staff-inventory__bulk-form">
              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="blackout-cat">
                  Applies To
                </label>
                <select
                  id="blackout-cat"
                  className="staff-inventory__select"
                  value={newBlackoutCategory}
                  onChange={(e) => setNewBlackoutCategory(e.target.value)}
                  disabled={isCreatingBlackout}
                >
                  <option value="ALL">All Resort (Resort-wide Buyout)</option>
                  <option value="ROOM">Estate Rooms (ROOM)</option>
                  <option value="VILLA">Private Villas (VILLA)</option>
                </select>
              </div>

              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="blackout-date">
                  Blackout Date
                </label>
                <input
                  id="blackout-date"
                  type="date"
                  className="staff-inventory__input"
                  value={newBlackoutDate}
                  min={SEED_MIN_DATE}
                  max={SEED_MAX_DATE}
                  onChange={(e) => setNewBlackoutDate(e.target.value)}
                  disabled={isCreatingBlackout}
                  required
                />
              </div>

              <div className="staff-inventory__filter-group staff-inventory__form-col-span-2">
                <label className="staff-inventory__label" htmlFor="blackout-reason">
                  Blackout Reason
                </label>
                <input
                  id="blackout-reason"
                  type="text"
                  placeholder="e.g. Christmas Buyout / Annual Estate Deep Clean"
                  className="staff-inventory__input"
                  value={newBlackoutReason}
                  onChange={(e) => setNewBlackoutReason(e.target.value)}
                  disabled={isCreatingBlackout}
                />
              </div>

              <div className="staff-inventory__bulk-submit">
                <Button
                  type="submit"
                  variant="primary"
                  size="medium"
                  disabled={isCreatingBlackout || !newBlackoutDate}
                >
                  {isCreatingBlackout ? 'Adding Blackout...' : 'Add Blackout Date'}
                </Button>
              </div>
            </form>

            {blackoutError && (
              <div className="staff-inventory__feedback">
                <ErrorBanner
                  title="Blackout Error"
                  message={blackoutError}
                  onDismiss={() => setBlackoutError(null)}
                />
              </div>
            )}

            {blackoutSuccess && (
              <div className="staff-inventory__feedback">
                <div
                  style={{
                    backgroundColor: 'var(--color-success-tint, #E4ECE3)',
                    color: 'var(--color-success, #2E5C31)',
                    padding: '0.75rem 1rem',
                    borderRadius: 'var(--radius-sm, 2px)',
                    fontWeight: 500,
                    fontSize: '0.875rem',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                  }}
                >
                  <span>✓ {blackoutSuccess}</span>
                  <button
                    type="button"
                    className="staff-inventory__dismiss-btn"
                    aria-label="Dismiss success message"
                    onClick={() => setBlackoutSuccess(null)}
                  >
                    ✕
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Blackout List Card */}
          <div className="staff-inventory__group-card">
            <div className="staff-inventory__group-header">
              <h2 className="staff-inventory__group-title">Existing Blackout Dates</h2>
              <span className="staff-inventory__group-meta">
                {blackouts.length} blackout {blackouts.length === 1 ? 'date' : 'dates'} configured
              </span>
            </div>

            {blackoutLoading ? (
              <Skeleton variant="rectangular" width="100%" height="200px" />
            ) : blackouts.length === 0 ? (
              <EmptyState
                title="No blackout dates"
                description="No blackout dates are currently configured. Guests can book whenever allotment and rates allow."
              />
            ) : (
              <>
                {/* Desktop Table View */}
                <div className="staff-inventory__table-wrapper">
                  <table className="staff-inventory__table">
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Scope / Category</th>
                        <th>Reason</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {blackouts.map((b) => (
                        <tr key={b.id}>
                          <td>
                            <strong>{formatDate(b.blackoutDate)}</strong>
                            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                              {b.blackoutDate}
                            </div>
                          </td>
                          <td>
                            {b.roomCategoryCode ? (
                              <Badge variant="neutral">{b.roomCategoryName || b.roomCategoryCode}</Badge>
                            ) : (
                              <Badge variant="warning">👑 All Resort (Buyout)</Badge>
                            )}
                          </td>
                          <td>{b.reason || 'Resort Blackout'}</td>
                          <td>
                            <button
                              type="button"
                              className="staff-inventory__delete-btn"
                              onClick={() => handleDeleteBlackout(b.id, b.blackoutDate)}
                              disabled={deletingBlackoutId === b.id}
                            >
                              {deletingBlackoutId === b.id ? 'Removing...' : 'Remove'}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Mobile Cards for Blackouts (<= 768px) */}
                <div className="staff-inventory__mobile-cards">
                  {blackouts.map((b) => (
                    <div key={`mobile-blackout-${b.id}`} className="staff-inventory__item-card">
                      <div className="staff-inventory__item-card-header">
                        <div>
                          <strong>{formatDate(b.blackoutDate)}</strong>
                          <div className="staff-inventory__item-card-sub">{b.blackoutDate}</div>
                        </div>
                        {b.roomCategoryCode ? (
                          <Badge variant="neutral">{b.roomCategoryName || b.roomCategoryCode}</Badge>
                        ) : (
                          <Badge variant="warning">👑 All Resort</Badge>
                        )}
                      </div>
                      <div className="staff-inventory__item-card-body">
                        <div className="staff-inventory__item-card-reason">
                          <span className="staff-inventory__label-muted">Reason: </span>
                          <span>{b.reason || 'Resort Blackout'}</span>
                        </div>
                      </div>
                      <div className="staff-inventory__item-card-actions">
                        <button
                          type="button"
                          className="staff-inventory__delete-btn staff-inventory__delete-btn--full"
                          onClick={() => handleDeleteBlackout(b.id, b.blackoutDate)}
                          disabled={deletingBlackoutId === b.id}
                        >
                          {deletingBlackoutId === b.id ? 'Removing...' : 'Remove Blackout'}
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        </div>
      ),
    },
    {
      id: 'promos',
      label: '🏷️ Promo Codes',
      content: (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', marginTop: '1rem' }}>
          {/* Create Promo Card */}
          <div className="staff-inventory__bulk-card">
            <div className="staff-inventory__bulk-header">
              <h2 className="staff-inventory__bulk-title">Create Promo Code</h2>
              <p className="staff-inventory__bulk-subtitle">
                Configure discount rates, validity windows, and minimum stay nights for guest bookings.
              </p>
            </div>

            <form onSubmit={handleCreatePromo} className="staff-inventory__bulk-form">
              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="promo-code">
                  Promo Code *
                </label>
                <input
                  id="promo-code"
                  type="text"
                  placeholder="e.g. FESTIVE20"
                  className="staff-inventory__input"
                  style={{ textTransform: 'uppercase' }}
                  value={newPromoCode}
                  onChange={(e) => setNewPromoCode(e.target.value.toUpperCase())}
                  disabled={isCreatingPromo}
                  required
                />
              </div>

              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="promo-type">
                  Discount Type *
                </label>
                <select
                  id="promo-type"
                  className="staff-inventory__select"
                  value={newPromoType}
                  onChange={(e) => setNewPromoType(e.target.value)}
                  disabled={isCreatingPromo}
                >
                  <option value="PERCENTAGE">Percentage (%)</option>
                  <option value="FLAT_INR">Flat INR (₹)</option>
                </select>
              </div>

              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="promo-val">
                  {newPromoType === 'PERCENTAGE' ? 'Discount Percentage (%) *' : 'Flat Discount (₹ INR) *'}
                </label>
                <input
                  id="promo-val"
                  type="number"
                  step="0.01"
                  min="0.01"
                  max={newPromoType === 'PERCENTAGE' ? 100 : undefined}
                  placeholder={newPromoType === 'PERCENTAGE' ? 'e.g. 15' : 'e.g. 1500'}
                  className="staff-inventory__input"
                  value={newPromoValue}
                  onChange={(e) => setNewPromoValue(e.target.value)}
                  disabled={isCreatingPromo}
                  required
                />
              </div>

              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="promo-from">
                  Valid From *
                </label>
                <input
                  id="promo-from"
                  type="date"
                  className="staff-inventory__input"
                  value={newPromoFrom}
                  min={SEED_MIN_DATE}
                  max={SEED_MAX_DATE}
                  onChange={(e) => setNewPromoFrom(e.target.value)}
                  disabled={isCreatingPromo}
                  required
                />
              </div>

              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="promo-to">
                  Valid To *
                </label>
                <input
                  id="promo-to"
                  type="date"
                  className="staff-inventory__input"
                  value={newPromoTo}
                  min={SEED_MIN_DATE}
                  max={SEED_MAX_DATE}
                  onChange={(e) => setNewPromoTo(e.target.value)}
                  disabled={isCreatingPromo}
                  required
                />
              </div>

              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="promo-nights">
                  Min Nights
                </label>
                <input
                  id="promo-nights"
                  type="number"
                  min="1"
                  className="staff-inventory__input"
                  value={newPromoMinNights}
                  onChange={(e) => setNewPromoMinNights(e.target.value)}
                  disabled={isCreatingPromo}
                />
              </div>

              <div className="staff-inventory__filter-group">
                <label className="staff-inventory__label" htmlFor="promo-uses">
                  Max Uses *
                </label>
                <input
                  id="promo-uses"
                  type="number"
                  min="1"
                  className="staff-inventory__input"
                  value={newPromoMaxUses}
                  onChange={(e) => setNewPromoMaxUses(e.target.value)}
                  disabled={isCreatingPromo}
                  required
                />
              </div>

              <div className="staff-inventory__bulk-submit">
                <Button
                  type="submit"
                  variant="primary"
                  size="medium"
                  disabled={isCreatingPromo || !newPromoCode || !newPromoValue}
                >
                  {isCreatingPromo ? 'Creating...' : 'Create Promo Code'}
                </Button>
              </div>
            </form>

            {promoError && (
              <div className="staff-inventory__feedback">
                <ErrorBanner
                  title="Promo Error"
                  message={promoError}
                  onDismiss={() => setPromoError(null)}
                />
              </div>
            )}

            {promoSuccess && (
              <div className="staff-inventory__feedback">
                <div
                  style={{
                    backgroundColor: 'var(--color-success-tint, #E4ECE3)',
                    color: 'var(--color-success, #2E5C31)',
                    padding: '0.75rem 1rem',
                    borderRadius: 'var(--radius-sm, 2px)',
                    fontWeight: 500,
                    fontSize: '0.875rem',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                  }}
                >
                  <span>✓ {promoSuccess}</span>
                  <button
                    type="button"
                    className="staff-inventory__dismiss-btn"
                    aria-label="Dismiss success message"
                    onClick={() => setPromoSuccess(null)}
                  >
                    ✕
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Promo List Card */}
          <div className="staff-inventory__group-card">
            <div className="staff-inventory__group-header">
              <h2 className="staff-inventory__group-title">Existing Promotional Codes</h2>
              <span className="staff-inventory__group-meta">
                {promos.length} codes registered
              </span>
            </div>

            {promoLoading ? (
              <Skeleton variant="rectangular" width="100%" height="200px" />
            ) : promos.length === 0 ? (
              <EmptyState
                title="No promo codes found"
                description="No promotional codes have been created yet."
              />
            ) : (
              <>
                {/* Desktop Table View */}
                <div className="staff-inventory__table-wrapper">
                  <table className="staff-inventory__table">
                    <thead>
                      <tr>
                        <th>Promo Code</th>
                        <th>Discount</th>
                        <th>Validity Window</th>
                        <th>Min Stay</th>
                        <th>Redemptions</th>
                        <th>Status</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {promos.map((p) => (
                        <tr key={p.id}>
                          <td>
                            <span className="staff-inventory__promo-code">{p.code}</span>
                          </td>
                          <td>
                            <strong>
                              {p.discountType === 'PERCENTAGE'
                                ? `${p.discountValue}% OFF`
                                : `₹${Number(p.discountValue).toLocaleString('en-IN')} OFF`}
                            </strong>
                          </td>
                          <td>
                            <div>{p.validFrom} &rarr; {p.validTo}</div>
                          </td>
                          <td>{p.minNights} {p.minNights === 1 ? 'night' : 'nights'}</td>
                          <td>{p.currentUses} / {p.maxUses}</td>
                          <td>
                            {p.isActive ? (
                              <Badge variant="success">Active</Badge>
                            ) : (
                              <Badge variant="neutral">Inactive</Badge>
                            )}
                          </td>
                          <td>
                            <button
                              type="button"
                              className="staff-inventory__toggle-btn"
                              onClick={() => handleTogglePromoActive(p.id, p.isActive, p.code)}
                              disabled={togglingPromoId === p.id}
                            >
                              {togglingPromoId === p.id
                                ? 'Updating...'
                                : p.isActive
                                ? 'Deactivate'
                                : 'Activate'}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Mobile Cards for Promos (<= 768px) */}
                <div className="staff-inventory__mobile-cards">
                  {promos.map((p) => (
                    <div key={`mobile-promo-${p.id}`} className="staff-inventory__item-card">
                      <div className="staff-inventory__item-card-header">
                        <span className="staff-inventory__promo-code">{p.code}</span>
                        {p.isActive ? (
                          <Badge variant="success">Active</Badge>
                        ) : (
                          <Badge variant="neutral">Inactive</Badge>
                        )}
                      </div>
                      <div className="staff-inventory__item-card-body">
                        <div className="staff-inventory__promo-card-value">
                          {p.discountType === 'PERCENTAGE'
                            ? `${p.discountValue}% OFF`
                            : `₹${Number(p.discountValue).toLocaleString('en-IN')} OFF`}
                        </div>
                        <div className="staff-inventory__item-card-sub">
                          Validity: {p.validFrom} &rarr; {p.validTo}
                        </div>
                        <div className="staff-inventory__promo-card-meta">
                          <span>Min Stay: {p.minNights} {p.minNights === 1 ? 'night' : 'nights'}</span>
                          <span>Uses: {p.currentUses} / {p.maxUses}</span>
                        </div>
                      </div>
                      <div className="staff-inventory__item-card-actions">
                        <button
                          type="button"
                          className="staff-inventory__toggle-btn staff-inventory__toggle-btn--full"
                          onClick={() => handleTogglePromoActive(p.id, p.isActive, p.code)}
                          disabled={togglingPromoId === p.id}
                        >
                          {togglingPromoId === p.id
                            ? 'Updating...'
                            : p.isActive
                            ? 'Deactivate Promo'
                            : 'Activate Promo'}
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        </div>
      ),
    },
  ];

  return (
    <div className="staff-inventory-page">
      {/* Header */}
      <header className="staff-inventory__header">
        <h1 className="staff-inventory__title">Rates, Allotment &amp; Promotions</h1>
        <p className="staff-inventory__subtitle">
          Manage daily rates, website allotments, resort blackout dates, and guest promotional codes.
        </p>
      </header>

      {/* Primary Tab Navigation */}
      <div className="staff-inventory__tabs">
        <Tabs
          items={tabItems}
          activeTab={activeTab}
          onChange={(newTab) => setActiveTab(newTab)}
        />
      </div>
    </div>
  );
}

export default StaffInventoryPage;
