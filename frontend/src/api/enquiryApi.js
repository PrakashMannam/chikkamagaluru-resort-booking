const BASE_URL = import.meta.env.VITE_API_URL || '';

export async function submitPublicEnquiry(payload) {
  const url = `${BASE_URL}/api/v1/public/enquiries`;
  try {
    const res = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      let errorMsg = 'Unable to submit your enquiry. Please check your details and try again.';
      try {
        const data = await res.json();
        if (data && data.message) {
          errorMsg = data.message;
        }
      } catch (e) {
        // fallback to status code messaging
      }
      const err = new Error(errorMsg);
      err.status = res.status;
      throw err;
    }

    return await res.json();
  } catch (err) {
    if (err.status) throw err;
    const networkErr = new Error('Connection to the estate concierge service is unavailable. Please check your connection and tap Retry.');
    networkErr.isNetwork = true;
    throw networkErr;
  }
}
