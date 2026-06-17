// 场景 2c：单接口极限 - GET /api/announcements
//
// 500 VU × 15s，匿名。

import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, defaultOptions, expectStatus } from './common.js';

export const options = defaultOptions();

export default function () {
  group('GET /api/announcements', () => {
    const res = http.get(`${BASE_URL}/api/announcements`, { timeout: '30s' });
    expectStatus(res, 200);
    check(res, {
      'status is 2xx': (r) => r.status >= 200 && r.status < 300,
    });
  });
}
