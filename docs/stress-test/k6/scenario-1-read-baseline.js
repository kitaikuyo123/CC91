// 场景 1：只读基准测试（公开接口轮询）
//
// 500 VU × 15s，匿名访问 /api/categories、/api/posts、/api/announcements 三个公开端点，
// 每个 VU 在三个端点之间轮询。对应旧 stress_test.py scenario_readonly_baseline。

import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, defaultOptions, expectStatus } from './common.js';

export const options = defaultOptions();

const ENDPOINTS = [
  { name: 'GET /api/categories', url: `${BASE_URL}/api/categories` },
  { name: 'GET /api/posts', url: `${BASE_URL}/api/posts?page=0&size=20` },
  { name: 'GET /api/announcements', url: `${BASE_URL}/api/announcements` },
];

export default function () {
  // 轮询：每个 iteration 按顺序取下一个端点
  // k6 在每个 VU 内部维护独立的 exec.scenario.iterationInTest，
  // 这里用全局计数器足够（轮询顺序不要求严格公平）
  const ep = ENDPOINTS[__ITER % ENDPOINTS.length];
  group(ep.name, () => {
    const res = http.get(ep.url, { timeout: '30s' });
    expectStatus(res, 200);
    check(res, {
      'status is 2xx': (r) => r.status >= 200 && r.status < 300,
    });
  });
}
