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
