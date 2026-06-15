import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { AuthProvider, useAuth } from '../../context/AuthContext';

describe('AuthContext', () => {
  beforeEach(() => {
    sessionStorage.clear();
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

    act(() => {
      result.current.login('testuser', 'fake-token');
    });

    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.user).toEqual({ username: 'testuser', email: '', role: 'USER' });
      expect(sessionStorage.getItem('access_token')).toBe('fake-token');
    });
  });

  it('should logout user and clear token', async () => {
    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    });

    act(() => {
      result.current.login('testuser', 'fake-token');
    });
    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(true);
    });

    act(() => {
      result.current.logout();
    });
    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.user).toBeNull();
      expect(sessionStorage.getItem('access_token')).toBeNull();
    });
  });

  it('should restore auth state from sessionStorage', async () => {
    sessionStorage.setItem('access_token', 'stored-token');
    sessionStorage.setItem('user', JSON.stringify({ username: 'storeduser', email: '' }));

    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    });

    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.user).toEqual({ username: 'storeduser', email: '' });
    });
  });
});
