// 场景 6：帖子详情 + 评论列表并发读取
//
// 500 VU × 15s。匿名访问（详情/评论 GET 都是公开端点）。
// 每个 iteration 按顺序轮询 GET /api/posts/{id} 和 GET /api/posts/{id}/comments。
// 对应旧 stress_test.py scenario_post_detail_with_comments。
//
// post_id 默认取 1，可通过环境变量 POST_ID 覆盖。
// 旧脚本会动态拉 /api/posts?page=0&size=1 取第一条，这里改成可配置，
// 因为在 setup 阶段拉一次（单次 HTTP）更稳定。

import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, defaultOptions, expectStatus } from './common.js';

export const options = defaultOptions();

const POST_ID = parseInt(__ENV.POST_ID || '1', 10);

// setup：若用户未显式指定 POST_ID，尝试从 /api/posts 取第一条，
// 失败则回退到 1（与旧脚本一致）。
export function setup() {
  if (__ENV.POST_ID) {
    return { postId: POST_ID };
  }
  try {
    const res = http.get(`${BASE_URL}/api/posts?page=0&size=1`, { timeout: '10s' });
    if (res.status === 200) {
      const body = JSON.parse(res.body);
      const content =
        (body && body.content) ||
        (body && body.data && body.data.content) ||
        [];
      if (Array.isArray(content) && content.length > 0 && content[0].id != null) {
        return { postId: content[0].id };
      }
    }
  } catch (e) {
    // ignore，回退默认
  }
  return { postId: 1 };
}

export default function (data) {
  const postId = (data && data.postId) || 1;

  if (__ITER % 2 === 0) {
    group(`GET /api/posts/${postId}`, () => {
      const res = http.get(`${BASE_URL}/api/posts/${postId}`, { timeout: '30s' });
      expectStatus(res, 200);
      check(res, {
        'status is 2xx': (r) => r.status >= 200 && r.status < 300,
      });
    });
  } else {
    group(`GET /api/posts/${postId}/comments`, () => {
      const res = http.get(`${BASE_URL}/api/posts/${postId}/comments`, { timeout: '30s' });
      expectStatus(res, 200);
      check(res, {
        'status is 2xx': (r) => r.status >= 200 && r.status < 300,
      });
    });
  }
}
