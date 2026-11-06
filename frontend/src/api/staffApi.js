const BASE_URL = import.meta.env.VITE_API_URL || '';

function getAuthHeaders(token) {
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

export async function staffLogin(email, password) {
  const url = `${BASE_URL}/api/v1/auth/login`;
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });

  if (!res.ok) {
    let errorMsg = 'Invalid email or password';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {
      // Fallback message
    }
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return await res.json();
}

export async function fetchStaffProfile(token) {
  const url = `${BASE_URL}/api/v1/admin/me`;
  const res = await fetch(url, {
    headers: getAuthHeaders(token),
  });

  if (!res.ok) {
    const err = new Error('Session expired or unauthorized');
    err.status = res.status;
    throw err;
  }

  return await res.json();
}

export async function fetchStaffMembers(token) {
  const url = `${BASE_URL}/api/v1/admin/staff`;
  const res = await fetch(url, {
    headers: getAuthHeaders(token),
  });

  if (!res.ok) {
    const err = new Error('Unable to load staff directory');
    err.status = res.status;
    throw err;
  }

  return await res.json();
}

export async function fetchAdminEnquiries(token, filters = {}) {
  const params = new URLSearchParams();
  if (filters.category) params.append('category', filters.category);
  if (filters.status) params.append('status', filters.status);
  if (filters.assigned_to) params.append('assigned_to', filters.assigned_to);
  if (filters.channel) params.append('channel', filters.channel);
  if (filters.min_confidence) params.append('min_confidence', filters.min_confidence);

  const queryString = params.toString() ? `?${params.toString()}` : '';
  const url = `${BASE_URL}/api/v1/admin/enquiries${queryString}`;

  const res = await fetch(url, {
    headers: getAuthHeaders(token),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to load enquiries';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return await res.json();
}

export async function fetchAdminEnquiryDetail(token, id) {
  const url = `${BASE_URL}/api/v1/admin/enquiries/${id}`;
  const res = await fetch(url, {
    headers: getAuthHeaders(token),
  });

  if (!res.ok) {
    let errorMsg = 'Unable to load enquiry details';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return await res.json();
}

export async function routeAdminEnquiry(token, id, payload) {
  const url = `${BASE_URL}/api/v1/admin/enquiries/${id}/route`;
  const res = await fetch(url, {
    method: 'PATCH',
    headers: getAuthHeaders(token),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to reroute enquiry';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return await res.json();
}

export async function approveAdminDraft(token, id, payload) {
  const url = `${BASE_URL}/api/v1/admin/enquiries/${id}/approve-draft`;
  const res = await fetch(url, {
    method: 'POST',
    headers: getAuthHeaders(token),
    body: JSON.stringify(payload || {}),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to approve draft';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return await res.json();
}

export async function discardAdminDraft(token, id) {
  const url = `${BASE_URL}/api/v1/admin/enquiries/${id}/discard-draft`;
  const res = await fetch(url, {
    method: 'POST',
    headers: getAuthHeaders(token),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to discard draft';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return await res.json();
}


export async function simulateAdminEnquiry(token, payload) {
  const url = `${BASE_URL}/api/v1/admin/enquiries/simulate`;
  const res = await fetch(url, {
    method: 'POST',
    headers: getAuthHeaders(token),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to simulate enquiry';
    let errorData = null;
    try {
      errorData = await res.json();
      if (errorData && errorData.message) errorMsg = errorData.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    err.data = errorData;
    throw err;
  }

  return await res.json();
}

export async function fetchInventoryRates(token, from, to) {
  const params = new URLSearchParams();
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  const url = `${BASE_URL}/api/v1/admin/inventory-rates?${params.toString()}`;
  const res = await fetch(url, {
    headers: getAuthHeaders(token),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to fetch inventory rates';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return await res.json();
}

export async function bulkUpdateInventoryRates(token, payload) {
  const url = `${BASE_URL}/api/v1/admin/inventory-rates/bulk`;
  const res = await fetch(url, {
    method: 'PUT',
    headers: getAuthHeaders(token),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to update inventory rates';
    let errorData = null;
    try {
      errorData = await res.json();
      if (errorData && errorData.message) errorMsg = errorData.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    err.data = errorData;
    throw err;
  }

  return await res.json();
}


export async function fetchAdminBlackouts(token, from, to) {
  const params = new URLSearchParams();
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  const query = params.toString() ? `?${params.toString()}` : '';
  const url = `${BASE_URL}/api/v1/admin/blackouts${query}`;
  const res = await fetch(url, {
    headers: getAuthHeaders(token),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to fetch blackout dates';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return await res.json();
}

export async function createAdminBlackout(token, payload) {
  const url = `${BASE_URL}/api/v1/admin/blackouts`;
  const res = await fetch(url, {
    method: 'POST',
    headers: getAuthHeaders(token),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to create blackout date';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return await res.json();
}

export async function deleteAdminBlackout(token, id) {
  const url = `${BASE_URL}/api/v1/admin/blackouts/${id}`;
  const res = await fetch(url, {
    method: 'DELETE',
    headers: getAuthHeaders(token),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to delete blackout date';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return true;
}

export async function fetchAdminPromos(token) {
  const url = `${BASE_URL}/api/v1/admin/promos`;
  const res = await fetch(url, {
    headers: getAuthHeaders(token),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to fetch promo codes';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return await res.json();
}

export async function createAdminPromo(token, payload) {
  const url = `${BASE_URL}/api/v1/admin/promos`;
  const res = await fetch(url, {
    method: 'POST',
    headers: getAuthHeaders(token),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to create promo code';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return await res.json();
}

export async function updateAdminPromoActive(token, id, active) {
  const url = `${BASE_URL}/api/v1/admin/promos/${id}/active`;
  const res = await fetch(url, {
    method: 'PATCH',
    headers: getAuthHeaders(token),
    body: JSON.stringify({ active }),
  });

  if (!res.ok) {
    let errorMsg = 'Failed to update promo status';
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch (e) {}
    const err = new Error(errorMsg);
    err.status = res.status;
    throw err;
  }

  return await res.json();
}
