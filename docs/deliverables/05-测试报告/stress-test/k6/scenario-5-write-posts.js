// 场景 5：500 QPS 稳定写入帖子（arrival-rate）
//
// 改动理由（Task 1 MVP）：
//   原配置 iterations: 500 + vus: 500 是"一次性瞬时打满"，500 个 VU 同一时刻全部
//   冲到 /api/posts，并发几乎不可控，无法稳定暴露后端在 500 QPS 持续负载下的
//   表现（连接池排队、Tomcat 线程调度抖动都被瞬时压力掩盖）。
//   改为 constant-arrival-rate，以恒定 500 req/s 持续 5s 打入，能更真实地
//   反映"500 并发稳定写入"的目标场景，p(99) 与 failure rate 也更稳定可读。
//
// 注意：本场景会向数据库写入约 2500 篇"压测帖子"（500/s × 5s）。
// 不在脚本内自动清理（避免 teardown 阶段再次撞 Tomcat 队列）。
// 如需清理，单独跑：python ../stress_test.py --cleanup-only
// 或手动 DELETE /api/posts/{id}。

import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, authHeaders, expectStatus, loginAndGetToken, pseudoUuid } from './common.js';

export const options = {
  // 以恒定 500 req/s 持续 5s 打入（共约 2500 次请求）
  // preAllocatedVUs=100 预热避免冷启动毛刺；maxVUs=500 兜底防止排队丢请求
  scenarios: {
    concurrent_writes: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '1s',
      duration: '5s',
      preAllocatedVUs: 100,
      maxVUs: 500,
    },
  },
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
