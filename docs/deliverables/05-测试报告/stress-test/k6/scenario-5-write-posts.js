// 场景 5：固定 500 次并发写入帖子
//
// 例外配置：iterations: 500, vus: 500（每个 VU 跑 1 次即停），不加 duration。
// 对应旧 stress_test.py scenario_concurrent_write。
//
// 注意：本场景会向数据库写入 500 篇"压测帖子"。
// 不在脚本内自动清理（避免 teardown 阶段再次撞 Tomcat 队列）。
// 如需清理，单独跑：python ../stress_test.py --cleanup-only
// 或手动 DELETE /api/posts/{id}。

import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, authHeaders, expectStatus, loginAndGetToken, pseudoUuid } from './common.js';

export const options = {
  // 固定 500 个 iteration，500 个 VU 每个跑 1 次
  iterations: 500,
  vus: 500,
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(99)<5000'],
  },
};

export function setup() {
  const token = loginAndGetToken();
  return { token };
}

export default function (data) {
  const token = data && data.token;
  if (!token) return;

  group('POST /api/posts', () => {
    const payload = JSON.stringify({
      title: `压测帖子-write-${pseudoUuid()}`,
      content: `压力测试第 ${__ITER + 1} 条并发写入帖子。这是一段较长的内容用于模拟真实发帖场景。`,
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
}
