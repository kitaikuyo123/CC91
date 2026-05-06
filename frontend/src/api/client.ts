import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';

/**
 * Axios client configuration
 * Communicates with backend API
 */

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Request interceptor - auto-attach JWT token
 */
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('access_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Tracks whether a token refresh is currently in progress.
 * Used to prevent multiple concurrent 401 responses from triggering
 * multiple refresh requests simultaneously.
 */
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

/**
 * Processes all queued requests that failed due to 401,
 * either retrying them with the new token or rejecting them.
 */
function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token!);
    }
  });
  failedQueue = [];
}

/**
 * Response interceptor - handle 401 with token refresh
 *
 * On 401:
 * 1. If not already refreshing, attempt to refresh the token
 * 2. If refresh succeeds, update stored tokens and retry the original request
 * 3. If refresh fails, clear tokens and redirect to login
 * 4. If already refreshing, queue the request to be retried after refresh completes
 */
client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Only attempt refresh for 401 errors on non-auth endpoints
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Don't try to refresh if the failing request is itself a refresh request
      if (originalRequest.url === '/auth/refresh') {
        clearTokensAndRedirect();
        return Promise.reject(error);
      }

      // If already refreshing, queue this request
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`;
              resolve(client(originalRequest));
            },
            reject,
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const storedRefreshToken = localStorage.getItem('refresh_token');
        if (!storedRefreshToken) {
          throw new Error('No refresh token available');
        }

        // Use bare axios to avoid interceptor loop
        const response = await axios.post(
          `${client.defaults.baseURL}/auth/refresh`,
          { refreshToken: storedRefreshToken }
        );

        const { accessToken, refreshToken: newRefreshToken } = response.data;

        // Store new tokens
        localStorage.setItem('access_token', accessToken);
        if (newRefreshToken) {
          localStorage.setItem('refresh_token', newRefreshToken);
        }

        // Process queued requests
        processQueue(null, accessToken);

        // Retry original request with new token
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return client(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        clearTokensAndRedirect();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

function clearTokensAndRedirect() {
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
  localStorage.removeItem('user');
  // Only redirect if not already on login/register pages
  if (!window.location.pathname.startsWith('/login') &&
      !window.location.pathname.startsWith('/register') &&
      !window.location.pathname.startsWith('/forgot-password') &&
      !window.location.pathname.startsWith('/reset-password')) {
    window.location.href = '/login';
  }
}

export default client;
