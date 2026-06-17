#!/usr/bin/env node
//
// 将一个或多个 k6 JSON 输出文件（--out json=results/sN.json）转换为旧版
// stress_test_result.json 兼容的数组结构。
//
// 输入：k6 的 NDJSON 流，每行一个对象：
//   - {type:"Metric", metric:"<name>", data:{name, type, contains, ...}}  指标定义
//   - {type:"Point",  metric:"<name>", data:{time, value, tags}}          指标样本
//   注意：metric 名字在【顶层】 obj.metric，不在 obj.data.metric。
//   k6 0.40+ 的 Metric 行还会带 quantiles 字段，但我们不依赖它，
//   而是自己收集 http_req_duration 的所有 Point 样本算 P50/P90/P95/P99，
//   与旧 Python 脚本的统计口径完全一致。
//
// 用法：
//   node parse-results.js results/*.json > stress_test_result_500.json
//   node parse-results.js results/scenario-2c-announcements.json   # 单场景
//
// 输出：JSON 数组（每输入文件一项），字段与 stress_test.py ScenarioResult 对齐。
//
// 场景名映射：默认取文件名（去掉 scenario- 前缀和 .json），
//            若结果里有 tag source / scenario 则优先用之。

'use strict';

const fs = require('fs');

// 文件名 → 旧报告里的中文场景名
const SCENARIO_NAME_MAP = {
  'scenario-1-read-baseline': '只读基准测试（公开接口）',
  'scenario-2a-categories': '极限吞吐 - GET /api/categories',
  'scenario-2b-posts': '极限吞吐 - GET /api/posts',
  'scenario-2c-announcements': '极限吞吐 - GET /api/announcements',
  'scenario-4-mixed-read-write': '认证用户读写混合（80%读 20%写）',
  'scenario-5-write-posts': '并发写入测试（发帖）',
  'scenario-6-post-detail-comments': '帖子详情+评论并发读取',
};

function percentile(sortedAsc, p) {
  if (!sortedAsc.length) return 0;
  // 与旧 Python 脚本 _percentile 对齐：
  //   idx = int(len * p / 100)
  const idx = Math.floor((sortedAsc.length * p) / 100);
  return sortedAsc[Math.min(idx, sortedAsc.length - 1)];
}

function parseFile(path) {
  const raw = fs.readFileSync(path, 'utf8');
  const lines = raw.split(/\r?\n/);

  const durations = []; // http_req_duration 样本（毫秒，k6 默认单位）
  const statusCodes = {}; // { "200": 123, "0": 5, ... }  0 表示连接级失败
  let httpReqs = 0;
  let httpReqFailed = 0;
  let iterations = 0;
  let firstTime = Infinity;
  let lastTime = 0;
  const errors = new Set();

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) continue;
    let obj;
    try {
      obj = JSON.parse(trimmed);
    } catch (e) {
      continue;
    }
    if (!obj || !obj.type || !obj.data) continue;
    const d = obj.data;
    // metric 名字在顶层 obj.metric（Point 行 + Metric 行都如此），
    // 不在 obj.data.metric —— 旧代码找错了位置，导致 Point 全部解析失败。
    const metricName = obj.metric;

    if (obj.type === 'Point') {
      const ts = Date.parse(d.time || '');
      if (!Number.isNaN(ts)) {
        if (ts < firstTime) firstTime = ts;
        if (ts > lastTime) lastTime = ts;
      }

      if (metricName === 'http_req_duration') {
        // k6 时间类 metric 单位是毫秒
        durations.push(Number(d.value) || 0);
      } else if (metricName === 'http_reqs') {
        httpReqs += 1;
      } else if (metricName === 'http_req_failed') {
        // value=1 表示该请求被标记为失败
        if (Number(d.value) > 0) {
          httpReqFailed += 1;
        }
      } else if (metricName === 'iterations') {
        if (Number(d.value) > 0) {
          iterations += 1;
        }
      }

      const tags = d.tags || {};

      // 状态码统计：以 http_reqs 的 status tag 为准（每个 HTTP 请求一次）
      if (metricName === 'http_reqs') {
        const st = tags.status || '0';
        statusCodes[st] = (statusCodes[st] || 0) + 1;
      }

      // http_req_failed 仅用于错误信息收集，不重复计入状态码（k6 的 http_reqs
      // 已记录了底层状态；对于连接级失败，k6 会发一个 status="" 或 0 的 http_reqs 样本）
      if (metricName === 'http_req_failed' && Number(d.value) > 0) {
        if (tags.error) errors.add(tags.error);
        if (tags.error_code) errors.add(`error_code=${tags.error_code}`);
      }
    }
  }

  const durationS = firstTime === Infinity || lastTime <= firstTime
    ? 0
    : (lastTime - firstTime) / 1000;

  // total_requests：以 http_reqs 为准（每个 HTTP 请求一次），
  // 退回到 iterations（无 HTTP 请求的纯迭代场景）
  const totalRequests = httpReqs || iterations;

  // success_count：k6 的 http_req_failed=1 表示失败，反推成功数
  const failCount = httpReqFailed;
  const successCount = Math.max(0, totalRequests - failCount);

  durations.sort((a, b) => a - b);
  const sum = durations.reduce((acc, v) => acc + v, 0);
  const avg = durations.length ? sum / durations.length : 0;
  const min = durations.length ? durations[0] : 0;
  const max = durations.length ? durations[durations.length - 1] : 0;

  // 场景名：优先从输入文件名推断
  const baseName = path.replace(/^.*[\\/]/, '').replace(/\.json$/, '');
  const name = SCENARIO_NAME_MAP[baseName] || baseName;

  const rps = durationS > 0 ? totalRequests / durationS : 0;

  return {
    name,
    source_file: baseName,
    total_requests: totalRequests,
    success_count: successCount,
    fail_count: failCount,
    rps: Math.round(rps * 100) / 100,
    avg_latency_ms: Math.round(avg * 100) / 100,
    p50_latency_ms: Math.round(percentile(durations, 50) * 100) / 100,
    p90_latency_ms: Math.round(percentile(durations, 90) * 100) / 100,
    p95_latency_ms: Math.round(percentile(durations, 95) * 100) / 100,
    p99_latency_ms: Math.round(percentile(durations, 99) * 100) / 100,
    min_latency_ms: Math.round(min * 100) / 100,
    max_latency_ms: Math.round(max * 100) / 100,
    duration_s: Math.round(durationS * 100) / 100,
    status_codes: statusCodes,
    errors: Array.from(errors).slice(0, 10),
  };
}

function main() {
  const argv = process.argv.slice(2);
  if (argv.length === 0) {
    process.stderr.write('usage: node parse-results.js <k6-json> [<k6-json> ...]\n');
    process.exit(2);
  }

  const out = argv.map((p) => {
    try {
      return parseFile(p);
    } catch (e) {
      process.stderr.write(`[WARN] failed to parse ${p}: ${e.message}\n`);
      return null;
    }
  }).filter(Boolean);

  process.stdout.write(JSON.stringify(out, null, 2));
  process.stdout.write('\n');
}

main();
