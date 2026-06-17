// 场景 4：认证用户读写混合（80% 读 + 20% 写）
//
// 需要 JWT（admin/admin123），token 在 setup 阶段登录一次后透传给所有 VU。
// 默认 500 VU × 15s。每个 iteration 用 Math.random() 切换读/写：
//   - 80% GET /api/posts?page=0&size=20
//   - 20% POST /api/posts
// 对应旧 stress_test.py scenario_mixed_readwrite（旧版按 count%5，新版用概率更真实）。

import http from 'k6/http';
import { check, group } from 'k6';
import {
  BASE_URL,
  authHeaders,
  defaultOptions,
  expectStatus,
  loginAndGetToken,
  pseudoUuid,
} from './common.js';

export const options = defaultOptions();

// setup 在 k6 init 阶段执行一次，token 透传给所有 VU/iteration。
// loginAndGetToken 自带进程级缓存，setup 只会真实登录一次。
export function setup() {
  const token = loginAndGetToken();
  return { token };
}

export default function (data) {
  const token = data && data.token;
  if (!token) {
    // setup 未提供 token（k6 inspect 等场景），跳过本次 iteration
    return;
  }

  const isWrite = Math.random() < 0.2;

  if (isWrite) {
    group('POST /api/posts', () => {
      const payload = JSON.stringify({
        title: `压测帖子-mix-${pseudoUuid()}`,
        content: '这是一条压力测试自动创建的帖子内容，用于测试并发写入性能。',
        categoryId: 1,
        status: 'PUBLISHED',
      });
      const res = http.post(`${BASE_URL}/api/posts`, payload, {
        headers: authHeaders(token),
        timeout: '30s',
      });
      expectStatus(res, 200);
      check(res, {
        'status is 2xx': (r) => r.status >= 200 && r.status < 300,
      });
    });
  } else {
    group('GET /api/posts', () => {
      const res = http.get(`${BASE_URL}/api/posts?page=0&size=20`, {
        headers: { Authorization: `Bearer ${token}` },
        timeout: '30s',
      });
      expectStatus(res, 200);
      check(res, {
        'status is 2xx': (r) => r.status >= 200 && r.status < 300,
      });
    });
  }
}
