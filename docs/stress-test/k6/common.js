// CC91 论坛压测 - 共享工具
//
// 提供 BASE_URL、登录 token 缓存、断言工具与随机数据生成器。
// 所有 scenario-*.js 文件 import 本模块。
//
// 重要：LoginResponse.java 实测返回 { accessToken, refreshToken, tokenType, expiresIn, ... }
//        （AuthController#login 直接返回 LoginResponse，未包装在 ApiResponse.data 中）

import http from 'k6/http';

// ──────────────────────────────────────────
// 基础配置（可由环境变量覆盖）
// ──────────────────────────────────────────

export const BASE_URL = (__ENV.BASE_URL || 'http://localhost:9000').replace(/\/+$/, '');

// 默认 15s × 500 VU，可被各 scenario 覆盖
export const DEFAULT_DURATION = __ENV.DURATION || '15s';
export const DEFAULT_VUS = parseInt(__ENV.VUS || '500', 10);

// ──────────────────────────────────────────
// 登录 + Token 缓存
// ──────────────────────────────────────────

// 进程级缓存：同一 (username, password) 只登录一次
const tokenCache = {};

/**
 * 登录并返回 JWT。同 (username, password) 在同一 k6 进程内只登录一次。
 *
 * @param {string} [username] - 默认读 USERNAME 环境变量，再退回 'admin'
 * @param {string} [password] - 默认读 PASSWORD 环境变量，再退回 'admin123'
 * @returns {string} JWT access token
 * @throws  登录失败时抛出，k6 会将 exec setup 失败标记为运行错误
 */
export function loginAndGetToken(username, password) {
  const user = username || __ENV.USERNAME || 'admin';
  const pass = password || __ENV.PASSWORD || 'admin123';
  const key = `${user}:${pass}`;
  if (tokenCache[key]) return tokenCache[key];

  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username: user, password: pass }),
    { headers: { 'Content-Type': 'application/json' }, timeout: '30s' },
  );

  if (res.status !== 200) {
    throw new Error(
      `login failed: HTTP ${res.status} body=${(res.body || '').slice(0, 300)}`,
    );
  }

  let body;
  try {
    body = JSON.parse(res.body);
  } catch (e) {
    throw new Error(`login: invalid JSON body: ${(res.body || '').slice(0, 300)}`);
  }

  // LoginResponse 直接返回（顶层 accessToken）；
  // 兼容部分包装形态 ApiResponse.data.accessToken
  const token =
    body.accessToken ||
    (body.data && body.data.accessToken) ||
    (body.data && body.data.token) ||
    body.token;

  if (!token) {
    throw new Error(`login: no access token in response: ${JSON.stringify(body).slice(0, 300)}`);
  }
  tokenCache[key] = token;
  return token;
}

/**
 * 构造带 Authorization 头的请求参数。
 * @param {string} token
 * @param {object} [extra] 额外 header
 */
export function authHeaders(token, extra) {
  return Object.assign(
    {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    extra || {},
  );
}

// ──────────────────────────────────────────
// 通用断言 / 工具
// ──────────────────────────────────────────

/**
 * 弱断言：状态码不符仅打 warn，不抛错，不影响 k6 失败率统计。
 * 真正的失败率由 http_req_failed（默认任何非 2xx 都算）+ threshold 控制。
 */
export function expectStatus(res, expected) {
  if (res.status !== expected) {
    // eslint-disable-next-line no-undef
    if (typeof console !== 'undefined' && console.warn) {
      console.warn(
        `expected ${expected}, got ${res.status}: ${(res.body || '').slice(0, 200)}`,
      );
    }
  }
}

/**
 * 生成简易伪 UUID，足够保证发帖标题/内容唯一性，不需要密码学强度。
 */
export function pseudoUuid() {
  const hex = '0123456789abcdef';
  let out = '';
  for (let i = 0; i < 32; i += 1) {
    if (i === 8 || i === 12 || i === 16 || i === 20) out += '-';
    out += hex[Math.floor(Math.random() * 16)];
  }
  return out;
}

/**
 * 默认 arrival-rate 限速 options：每秒 500 次请求 × 15s，最多 500 VU 并发。
 *
 * 为什么用 constant-arrival-rate 而不是 vus+duration：
 *   第二轮压测发现 vus=500 + 短连接 + 高 RPS 触发反馈循环（500 VU 全速发请求，
 *   失败响应立即重试，RPS 失控上升到 1000+），导致 Windows 客户端 ephemeral port
 *   在 TIME_WAIT 累积下耗尽，产生 17 万+ `connectex: No connection could be made`。
 *   arrival-rate 把 RPS 锁在 500/s，TIME_WAIT 累积上限约 500×240s=12 万，
 *   仍在 58K 端口范围内（实测当前 Windows dynamicport 是 1024-60000 = 58977）。
 *
 * thresholds: 失败率 < 5%、P99 < 5s。
 */
export function defaultOptions(overrides) {
  return Object.assign(
    {
      scenarios: {
        stress: {
          executor: 'constant-arrival-rate',
          rate: parseInt(__ENV.RATE || '500', 10),
          timeUnit: '1s',
          duration: __ENV.DURATION || DEFAULT_DURATION,
          preAllocatedVUs: parseInt(__ENV.VUS || String(DEFAULT_VUS), 10),
          maxVUs: parseInt(__ENV.MAX_VUS || '1000', 10),
        },
      },
      thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(99)<5000'],
      },
    },
    overrides || {},
  );
}
