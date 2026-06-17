// 场景 2b：单接口极限 - GET /api/posts?page=0&size=20
//
// 500 VU × 15s。已知 50 并发旧基线下此端点出现过 3 次 30s 超时；
// 500 并发预计失败率会上升，报告里如实记录，不调 threshold 掩盖。

import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, defaultOptions, expectStatus } from './common.js';

export const options = defaultOptions();

export default function () {
  group('GET /api/posts', () => {
    const res = http.get(`${BASE_URL}/api/posts?page=0&size=20`, { timeout: '30s' });
    expectStatus(res, 200);
    check(res, {
      'status is 2xx': (r) => r.status >= 200 && r.status < 300,
    });
  });
}
