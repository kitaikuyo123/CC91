// 场景 2a：单接口极限 - GET /api/categories
//
// 500 VU × 15s，匿名集中打 categories 端点。对应旧 stress_test.py 极限吞吐之 categories。

import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, defaultOptions, expectStatus } from './common.js';

export const options = defaultOptions();

export default function () {
  group('GET /api/categories', () => {
    const res = http.get(`${BASE_URL}/api/categories`, { timeout: '30s' });
    expectStatus(res, 200);
    check(res, {
      'status is 2xx': (r) => r.status >= 200 && r.status < 300,
    });
  });
}
