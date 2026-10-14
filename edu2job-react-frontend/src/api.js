import axios from 'axios';

// Single API entry point: React talks ONLY to Spring Boot (never Flask or
// the database directly). Override with REACT_APP_API_URL in .env for
// non-local deployments; no secrets belong in frontend env vars.
const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
  timeout: 40000,
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Expired/invalid JWT: clear stale auth state and bounce to login.
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('jwt_token');
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  }
);

/** Human-friendly message for an API error (never a raw stack trace). */
export function apiErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (!error.response) {
    return 'Cannot reach the server. Check that the backend is running, then retry.';
  }
  const data = error.response.data || {};
  if (error.response.status === 400) {
    if (data.errors) {
      const first = Object.values(data.errors)[0];
      return first ? `Please check the form: ${first}` : 'Please check the highlighted fields.';
    }
    return data.message || 'Please check the highlighted fields.';
  }
  if (error.response.status === 401) return 'Your session has expired. Please log in again.';
  if (error.response.status === 403) return 'You do not have permission to do that.';
  if (error.response.status === 404) return 'The requested information was not found.';
  if (error.response.status === 429) return 'Too many requests. Please wait a moment and retry.';
  if (error.response.status === 503) {
    return data.message || 'The career recommendation service is not available yet.';
  }
  return data.message || fallback;
}

export default api;
