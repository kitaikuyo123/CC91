import { expect, afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';
import * as matchers from '@testing-library/jest-dom/matchers';

expect.extend(matchers);

// Mock LocalStorage to bypass Node.js 25+ global webstorage conflicts
class MockLocalStorage {
  private store: Record<string, string> = {};

  get length() {
    return Object.keys(this.store).length;
  }

  getItem(key: string) {
    return this.store[key] || null;
  }

  setItem(key: string, value: string) {
    this.store[key] = String(value);
  }

  removeItem(key: string) {
    delete this.store[key];
  }

  clear() {
    this.store = {};
  }

  key(index: number) {
    return Object.keys(this.store)[index] || null;
  }
}

const mockStorage = new MockLocalStorage();

Object.defineProperty(globalThis, 'localStorage', {
  value: mockStorage,
  writable: true,
  configurable: true,
});

if (typeof window !== 'undefined') {
  Object.defineProperty(window, 'localStorage', {
    value: mockStorage,
    writable: true,
    configurable: true,
  });
}

afterEach(() => {
  cleanup();
  mockStorage.clear();
});
