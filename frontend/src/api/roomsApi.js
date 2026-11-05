const BASE_URL = import.meta.env.VITE_API_URL || '';

export async function fetchAvailability({
  checkIn,
  checkOut,
  rooms = 1,
  adults = 2,
  children = 0,
}) {
  const params = new URLSearchParams({
    checkIn,
    checkOut,
    rooms: String(rooms),
    adults: String(adults),
    children: String(children),
  });

  const url = `${BASE_URL}/api/v1/public/rooms/availability?${params.toString()}`;

  try {
    const res = await fetch(url, {
      headers: {
        Accept: 'application/json',
      },
    });

    if (!res.ok) {
      let message = 'Unable to refresh estate rates. Please try again.';
      if (res.status === 409) {
        message = 'The requested room was just reserved. Please select another category.';
      } else if (res.status === 429) {
        message = 'Please wait a minute before requesting updated rates.';
      } else if (res.status === 422) {
        message = 'The requested dates are outside our estate booking calendar.';
      } else if (res.status === 503) {
        message = 'Reservations system is currently busy. Please try again.';
      }
      const err = new Error(message);
      err.status = res.status;
      throw err;
    }

    return await res.json();
  } catch (err) {
    if (err.status) throw err;
    const networkErr = new Error('Connection to the estate reservations service is unavailable. Tap Retry to check live rates.');
    networkErr.isNetwork = true;
    throw networkErr;
  }
}

export async function createBooking(payload, idempotencyKey) {
  const url = `${BASE_URL}/api/v1/public/bookings`;
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };
  if (idempotencyKey) {
    headers['Idempotency-Key'] = idempotencyKey;
  }

  try {
    const res = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });

    const body = await res.json().catch(() => ({}));

    if (!res.ok) {
      let message = body.message || 'Unable to create booking hold. Please try again.';
      if (res.status === 400 && body.details) {
        const detailMsgs = Object.values(body.details).join('; ');
        message = detailMsgs || body.message || 'Please check all required booking fields.';
      } else if (res.status === 409) {
        message = body.message || 'The selected accommodation is no longer available. Another guest may have just reserved it.';
      } else if (res.status === 422) {
        message = body.message || 'The promo code entered is invalid or does not match booking criteria.';
      } else if (res.status === 429) {
        message = body.message || 'Active booking hold limit reached (maximum 2 pending holds). Please complete or cancel an existing hold.';
      } else if (res.status === 503) {
        message = 'The booking service is momentarily busy. Please tap retry.';
      }
      const err = new Error(message);
      err.status = res.status;
      err.details = body.details;
      err.serverBody = body;
      throw err;
    }

    return body;
  } catch (err) {
    if (err.status) throw err;
    const networkErr = new Error('Connection to the reservation service timed out. Please tap retry.');
    networkErr.isNetwork = true;
    throw networkErr;
  }
}

export async function simulatePayment(bookingRef, token, status = 'SUCCESS', details = {}) {
  const url = `${BASE_URL}/api/v1/public/payments/${encodeURIComponent(bookingRef)}/mock-webhook`;
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };
  if (token) {
    headers['X-Payment-Token'] = token;
  }

  const payload = {
    oneTimePaymentToken: token,
    status,
    mockGatewayRef: details.mockGatewayRef || `MOCK-${Date.now().toString(36).toUpperCase()}`,
    paymentMethod: details.paymentMethod || 'UPI_TEST',
  };

  try {
    const res = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });

    const body = await res.json().catch(() => ({}));

    if (!res.ok) {
      let message = body.message || 'Payment simulation could not be processed.';
      if (res.status === 409) {
        if (body.error === 'Booking Hold Expired' || body.message?.includes('expired')) {
          message = 'Your 10-minute hold has expired. The held allotment has been released.';
        } else if (body.error === 'Payment Token Already Used' || body.message?.includes('already been used')) {
          message = 'This payment token has already been processed.';
        }
      }
      const err = new Error(message);
      err.status = res.status;
      err.serverBody = body;
      throw err;
    }

    return body;
  } catch (err) {
    if (err.status) throw err;
    const networkErr = new Error('Unable to reach payment simulation service. Please tap retry.');
    networkErr.isNetwork = true;
    throw networkErr;
  }
}

export async function cancelHold(bookingRef, token) {
  const url = `${BASE_URL}/api/v1/public/bookings/${encodeURIComponent(bookingRef)}/cancel-hold`;
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };
  if (token) {
    headers['X-Payment-Token'] = token;
  }

  try {
    const res = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify({ token }),
    });

    const body = await res.json().catch(() => ({}));

    if (!res.ok) {
      const err = new Error(body.message || 'Unable to cancel booking hold.');
      err.status = res.status;
      err.serverBody = body;
      throw err;
    }

    return body;
  } catch (err) {
    if (err.status) throw err;
    const networkErr = new Error('Network error while cancelling hold. Please tap retry.');
    networkErr.isNetwork = true;
    throw networkErr;
  }
}

export async function fetchBooking(bookingRef) {
  const url = `${BASE_URL}/api/v1/public/bookings/${encodeURIComponent(bookingRef)}`;

  try {
    const res = await fetch(url, {
      headers: {
        Accept: 'application/json',
      },
    });

    const body = await res.json().catch(() => ({}));

    if (!res.ok) {
      let message = body.message || 'Unable to retrieve booking details.';
      if (res.status === 404) {
        message = `No booking found for reference "${bookingRef}".`;
      }
      const err = new Error(message);
      err.status = res.status;
      err.serverBody = body;
      throw err;
    }

    return body;
  } catch (err) {
    if (err.status) throw err;
    const networkErr = new Error('Connection error while fetching booking details. Tap Retry to reload.');
    networkErr.isNetwork = true;
    throw networkErr;
  }
}
