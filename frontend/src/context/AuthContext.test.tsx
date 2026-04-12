import { describe, it, expect, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from '../context/AuthContext';

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('should provide initial auth state', () => {
    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    });

    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });

  it('should login user and store token', async () => {
    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    });

    result.current.login('testuser', 'fake-token');

    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.user).toEqual({ username: 'testuser', email: '' });
      expect(localStorage.getItem('access_token')).toBe('fake-token');
    });
  });

  it('should logout user and clear token', async () => {
    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    });

    result.current.login('testuser', 'fake-token');
    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(true);
    });

    result.current.logout();
    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.user).toBeNull();
      expect(localStorage.getItem('access_token')).toBeNull();
    });
  });

  it('should restore auth state from localStorage', () => {
    localStorage.setItem('access_token', 'stored-token');
    localStorage.setItem('user', JSON.stringify({ username: 'storeduser', email: '' }));

    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    });

    waitFor(() => {
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.user).toEqual({ username: 'storeduser', email: '' });
    });
  });
});
