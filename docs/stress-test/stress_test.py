"""
CC91 论坛系统压力测试脚本

基于 Python 标准库实现，无需安装第三方依赖。
测试场景：
  1. 只读基准测试（公开接口）
  2. 认证+读写混合测试
  3. 并发写入测试（发帖/评论）
  4. 单接口极限吞吐测试

用法：
  python stress_test.py [--base-url http://localhost:8080] [--concurrency 50] [--duration 30]
"""

import argparse
import concurrent.futures
import json
import statistics
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from typing import Optional


# ──────────────────────────────────────────
# 数据结构
# ──────────────────────────────────────────

@dataclass
class RequestResult:
    success: bool
    status_code: int
    latency_ms: float
    error: Optional[str] = None
    body: Optional[str] = None


@dataclass
class ScenarioResult:
    name: str
    total_requests: int = 0
    success_count: int = 0
    fail_count: int = 0
    latencies: list = field(default_factory=list)
    status_codes: dict = field(default_factory=dict)
    errors: list = field(default_factory=list)
    duration_s: float = 0.0

    @property
    def rps(self):
        return self.total_requests / self.duration_s if self.duration_s > 0 else 0

    @property
    def avg_latency_ms(self):
        return statistics.mean(self.latencies) if self.latencies else 0

    @property
    def p50_latency_ms(self):
        return self._percentile(50)

    @property
    def p90_latency_ms(self):
        return self._percentile(90)

    @property
    def p95_latency_ms(self):
        return self._percentile(95)

    @property
    def p99_latency_ms(self):
        return self._percentile(99)

    @property
    def max_latency_ms(self):
        return max(self.latencies) if self.latencies else 0

    @property
    def min_latency_ms(self):
        return min(self.latencies) if self.latencies else 0

    def _percentile(self, p):
        if not self.latencies:
            return 0
        sorted_lat = sorted(self.latencies)
        idx = int(len(sorted_lat) * p / 100)
        return sorted_lat[min(idx, len(sorted_lat) - 1)]


# ──────────────────────────────────────────
# HTTP 客户端
# ──────────────────────────────────────────

class ApiClient:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")

    def request(self, method, path, body=None, headers=None, token=None):
        url = f"{self.base_url}{path}"
        hdrs = {"Content-Type": "application/json"}
        if headers:
            hdrs.update(headers)
        if token:
            hdrs["Authorization"] = f"Bearer {token}"

        data = json.dumps(body).encode("utf-8") if body else None
        req = urllib.request.Request(url, data=data, headers=hdrs, method=method)

        start = time.perf_counter()
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                latency = (time.perf_counter() - start) * 1000
                resp_body = resp.read().decode("utf-8")
                return RequestResult(success=True, status_code=resp.status, latency_ms=latency, body=resp_body)
        except urllib.error.HTTPError as e:
            latency = (time.perf_counter() - start) * 1000
            resp_body = e.read().decode("utf-8", errors="replace")
            error = f"HTTP {e.code}: {resp_body[:1000]}" if resp_body else str(e)
            return RequestResult(
                success=False,
                status_code=e.code,
                latency_ms=latency,
                error=error,
                body=resp_body,
            )
        except Exception as e:
            latency = (time.perf_counter() - start) * 1000
            return RequestResult(success=False, status_code=0, latency_ms=latency, error=str(e))

    def get(self, path, token=None):
        return self.request("GET", path, token=token)

    def post(self, path, body=None, token=None):
        return self.request("POST", path, body=body, token=token)

    def put(self, path, body=None, token=None):
        return self.request("PUT", path, body=body, token=token)

    def delete(self, path, token=None):
        return self.request("DELETE", path, token=token)

    def login(self, username, password):
        try:
            req = urllib.request.Request(
                f"{self.base_url}/api/auth/login",
                data=json.dumps({"username": username, "password": password}).encode(),
                headers={"Content-Type": "application/json"},
                method="POST",
            )
            with urllib.request.urlopen(req, timeout=30) as resp:
                data = json.loads(resp.read().decode())
                # Response may be wrapped in ApiResponse {data: {accessToken}} or flat {accessToken}
                if "accessToken" in data:
                    return data["accessToken"]
                if "data" in data and isinstance(data["data"], dict):
                    return data["data"].get("accessToken")
                return None
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8", errors="replace")
            print(f"  登录失败: HTTP {e.code} — {body}")
            return None
        except Exception as e:
            print(f"  登录异常: {type(e).__name__}: {e}")
            return None


# ──────────────────────────────────────────
# 测试运行器
# ──────────────────────────────────────────

class StressTestRunner:
    def __init__(self, base_url, concurrency, duration_s):
        self.client = ApiClient(base_url)
        self.concurrency = concurrency
        self.duration_s = duration_s
        self.results: list[ScenarioResult] = []
        self._stop_event = threading.Event()
        self._counter_lock = threading.Lock()
        self._created_post_ids: list[int] = []
        self._post_ids_lock = threading.Lock()

    def _run_concurrent(self, name, worker_fn, max_workers=None):
        """在 duration_s 秒内用 max_workers 个线程并发执行 worker_fn"""
        if max_workers is None:
            max_workers = self.concurrency

        result = ScenarioResult(name=name)
        self._stop_event.clear()
        start = time.perf_counter()

        with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as pool:
            deadline = start + self.duration_s
            futures = {pool.submit(worker_fn) for _ in range(max_workers)}

            while futures:
                timeout = max(0.0, deadline - time.perf_counter())
                if timeout == 0:
                    self._stop_event.set()
                    break

                done, futures = concurrent.futures.wait(
                    futures,
                    timeout=min(0.1, timeout),
                    return_when=concurrent.futures.FIRST_COMPLETED,
                )

                for f in done:
                    r: RequestResult = f.result()
                    result.total_requests += 1
                    if r.success:
                        result.success_count += 1
                    else:
                        result.fail_count += 1
                        if r.error:
                            result.errors.append(r.error)
                    result.latencies.append(r.latency_ms)
                    result.status_codes[r.status_code] = result.status_codes.get(r.status_code, 0) + 1

                    if time.perf_counter() < deadline:
                        futures.add(pool.submit(worker_fn))

            self._stop_event.set()

            for f in concurrent.futures.as_completed(futures, timeout=60):
                r: RequestResult = f.result()
                result.total_requests += 1
                if r.success:
                    result.success_count += 1
                else:
                    result.fail_count += 1
                    if r.error:
                        result.errors.append(r.error)
                result.latencies.append(r.latency_ms)
                result.status_codes[r.status_code] = result.status_codes.get(r.status_code, 0) + 1

        result.duration_s = time.perf_counter() - start
        self.results.append(result)
        return result

    def _run_fixed_count(self, name, worker_fn, count, max_workers=None):
        """执行固定次数的请求"""
        if max_workers is None:
            max_workers = min(self.concurrency, count)

        result = ScenarioResult(name=name)
        start = time.perf_counter()

        with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as pool:
            futures = [pool.submit(worker_fn, i) for i in range(count)]
            for f in concurrent.futures.as_completed(futures, timeout=120):
                r: RequestResult = f.result()
                result.total_requests += 1
                if r.success:
                    result.success_count += 1
                else:
                    result.fail_count += 1
                    if r.error:
                        result.errors.append(r.error)
                result.latencies.append(r.latency_ms)
                result.status_codes[r.status_code] = result.status_codes.get(r.status_code, 0) + 1

        result.duration_s = time.perf_counter() - start
        self.results.append(result)
        return result

    # ── 场景 1：只读基准（公开接口，无认证） ──

    def scenario_readonly_baseline(self):
        print(f"\n{'='*60}")
        print(f"场景 1：只读基准测试（公开接口）")
        print(f"并发数: {self.concurrency}  持续: {self.duration_s}s")
        print(f"{'='*60}")

        endpoints = [
            ("GET /api/categories", lambda: self.client.get("/api/categories")),
            ("GET /api/posts", lambda: self.client.get("/api/posts")),
            ("GET /api/announcements", lambda: self.client.get("/api/announcements")),
        ]
        idx = [0]
        idx_lock = threading.Lock()

        def worker():
            if self._stop_event.is_set():
                return RequestResult(success=True, status_code=0, latency_ms=0)
            with idx_lock:
                ep_name, fn = endpoints[idx[0] % len(endpoints)]
                idx[0] += 1
            return fn()

        return self._run_concurrent("只读基准测试（公开接口）", worker)

    # ── 场景 2：单接口极限吞吐 ──

    def scenario_single_endpoint_throughput(self):
        print(f"\n{'='*60}")
        print(f"场景 2：单接口极限吞吐测试")
        print(f"{'='*60}")

        results = []
        for label, path in [
            ("GET /api/categories", "/api/categories"),
            ("GET /api/posts", "/api/posts"),
            ("GET /api/announcements", "/api/announcements"),
        ]:
            r = self._run_concurrent(
                f"极限吞吐 - {label}",
                lambda p=path: self.client.get(p),
            )
            results.append(r)
        return results

    # ── 场景 3：认证流程压力测试 ──

    def scenario_auth_stress(self):
        print(f"\n{'='*60}")
        print(f"场景 3：认证流程压力测试")
        print(f"{'='*60}")

        # 先获取一个 token
        token = self.client.login("admin", "admin123")
        if not token:
            print("  跳过：无法获取 JWT Token（后端未启动或凭据错误）")
            return None

        def worker():
            r = self.client.post("/api/auth/login", {"username": "admin", "password": "admin123"})
            return r

        return self._run_concurrent("认证流程压力测试（POST /api/auth/login）", worker)

    # ── 场景 4：认证用户读写混合 ──

    def _record_post_id(self, result):
        """从响应体中提取帖子 ID 并记录"""
        if not result.success or not result.body:
            return
        try:
            data = json.loads(result.body)
            post_id = None
            if isinstance(data, dict):
                if "id" in data:
                    post_id = data["id"]
                elif "data" in data and isinstance(data["data"], dict):
                    post_id = data["data"].get("id")
            if post_id is not None:
                with self._post_ids_lock:
                    self._created_post_ids.append(post_id)
        except (json.JSONDecodeError, KeyError):
            pass

    def scenario_mixed_readwrite(self):
        print(f"\n{'='*60}")
        print(f"场景 4：认证用户读写混合（80%读 20%写）")
        print(f"{'='*60}")

        token = self.client.login("admin", "admin123")
        if not token:
            print("  跳过：无法获取 JWT Token")
            return None

        runner = self

        def worker(c):
            if c % 5 == 0:
                # 20% 写操作
                body = {
                    "title": f"压测帖子-{c}-{int(time.time() * 1000)}",
                    "content": "这是一条压力测试自动创建的帖子内容，用于测试并发写入性能。",
                    "categoryId": 1,
                }
                r = self.client.post("/api/posts", body, token=token)
                runner._record_post_id(r)
                return r
            else:
                # 80% 读操作
                return self.client.get("/api/posts", token=token)

        return self._run_fixed_count(
            "认证用户读写混合（80%读 20%写）",
            worker,
            count=self.concurrency * 20,
        )

    # ── 场景 5：并发写入帖子 ──

    def scenario_concurrent_write(self):
        print(f"\n{'='*60}")
        print(f"场景 5：并发写入测试（发帖）")
        print(f"{'='*60}")

        token = self.client.login("admin", "admin123")
        if not token:
            print("  跳过：无法获取 JWT Token")
            return None

        runner = self

        def worker(i):
            body = {
                "title": f"压测帖子-{i}-{int(time.time())}",
                "content": f"压力测试第 {i} 条并发写入帖子。",
                "categoryId": 1,
            }
            r = self.client.post("/api/posts", body, token=token)
            runner._record_post_id(r)
            return r

        return self._run_fixed_count(
            "并发写入测试（发帖）",
            worker,
            count=self.concurrency * 10,
        )

    # ── 场景 6：帖子详情 + 评论读取 ──

    def scenario_post_detail_with_comments(self):
        print(f"\n{'='*60}")
        print(f"场景 6：帖子详情 + 评论列表并发读取")
        print(f"{'='*60}")

        # 先获取一个存在的帖子 ID
        try:
            req = urllib.request.Request(f"{self.client.base_url}/api/posts?page=0&size=1")
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode())
                content = data.get("content", [])
                post_id = content[0]["id"] if content else 1
        except Exception:
            post_id = 1

        counter = [0]
        counter_lock = threading.Lock()

        def worker():
            with counter_lock:
                c = counter[0]
                counter[0] += 1
            if c % 2 == 0:
                return self.client.get(f"/api/posts/{post_id}")
            else:
                return self.client.get(f"/api/posts/{post_id}/comments")

        return self._run_concurrent(
            f"帖子详情+评论（post_id={post_id}）",
            worker,
        )

    # ── 运行全部场景 ──

    def run_all(self):
        print("=" * 60)
        print("CC91 论坛系统 — 压力测试")
        print(f"目标: {self.client.base_url}")
        print(f"并发数: {self.concurrency}")
        print(f"每个场景持续时间: {self.duration_s}s")
        print(f"开始时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 60)

        # 预检：后端是否可达
        try:
            req = urllib.request.Request(f"{self.client.base_url}/api/categories")
            with urllib.request.urlopen(req, timeout=5) as resp:
                print(f"预检通过：后端可达 (HTTP {resp.status})\n")
        except Exception as e:
            print(f"预检失败：后端不可达 — {e}")
            print("请先启动微服务（Eureka → Gateway → 业务服务），Gateway 端口: 9000")
            sys.exit(1)

        self.scenario_readonly_baseline()
        self.scenario_single_endpoint_throughput()
        self.scenario_auth_stress()
        self.scenario_mixed_readwrite()
        self.scenario_concurrent_write()
        self.scenario_post_detail_with_comments()

        self.print_summary()

        # 自动清理本次压测产生的帖子
        self.cleanup_created_posts()

        return self.results

    # ── 清理 ──

    def cleanup_created_posts(self):
        """删除本次压测产生的帖子"""
        if not self._created_post_ids:
            print("\n本次未产生测试帖子，跳过清理。")
            return

        token = self.client.login("admin", "admin123")
        if not token:
            print("\n清理跳过：无法获取 JWT Token")
            return

        ids = list(self._created_post_ids)
        total = len(ids)
        deleted = 0
        failed = 0

        print(f"\n{'='*60}")
        print(f"清理：删除本次压测产生的 {total} 篇帖子")
        print(f"{'='*60}")

        for post_id in ids:
            r = self.client.delete(f"/api/posts/{post_id}", token=token)
            if r.success:
                deleted += 1
            else:
                failed += 1

        self._created_post_ids.clear()
        print(f"清理完成：删除 {deleted} 篇，失败 {failed} 篇")

    def cleanup_legacy_posts(self):
        """搜索并删除历史压测遗留的垃圾帖子"""
        token = self.client.login("admin", "admin123")
        if not token:
            print("清理跳过：无法获取 JWT Token")
            return

        print(f"\n{'='*60}")
        print('搜索并清理历史压测垃圾帖子（标题含"压测帖子"）')
        print(f"{'='*60}")

        all_ids = []
        page = 0
        while True:
            try:
                url = f"{self.client.base_url}/api/posts/search?keyword=%E5%8E%8B%E6%B5%8B%E5%B8%96%E5%AD%90&page={page}&size=50"
                req = urllib.request.Request(url)
                with urllib.request.urlopen(req, timeout=15) as resp:
                    data = json.loads(resp.read().decode("utf-8"))
                    content = data.get("content", [])
                    for post in content:
                        title = post.get("title", "")
                        if "压测帖子" in title:
                            all_ids.append(post["id"])
                    total_pages = data.get("totalPages", 1)
                    page += 1
                    if page >= total_pages:
                        break
            except Exception as e:
                print(f"搜索失败：{e}")
                break

        if not all_ids:
            print("未发现历史垃圾帖子。")
            return

        print(f"发现 {len(all_ids)} 篇历史垃圾帖子，开始删除...")
        deleted = 0
        for post_id in all_ids:
            r = self.client.delete(f"/api/posts/{post_id}", token=token)
            if r.success:
                deleted += 1

        print(f"清理完成：删除 {deleted}/{len(all_ids)} 篇历史垃圾帖子")

    def print_summary(self):
        print("\n")
        print("=" * 80)
        print("压力测试总结报告")
        print("=" * 80)
        print(f"{'场景':<35} {'总请求':>8} {'成功':>8} {'失败':>8} {'RPS':>10} {'平均ms':>10} {'P90ms':>8} {'P99ms':>8}")
        print("-" * 80)

        for r in self.results:
            print(
                f"{r.name:<35} "
                f"{r.total_requests:>8} "
                f"{r.success_count:>8} "
                f"{r.fail_count:>8} "
                f"{r.rps:>10.1f} "
                f"{r.avg_latency_ms:>10.1f} "
                f"{r.p90_latency_ms:>8.1f} "
                f"{r.p99_latency_ms:>8.1f}"
            )

        print("=" * 80)

    def to_dict(self):
        return [
            {
                "name": r.name,
                "total_requests": r.total_requests,
                "success_count": r.success_count,
                "fail_count": r.fail_count,
                "rps": round(r.rps, 2),
                "avg_latency_ms": round(r.avg_latency_ms, 2),
                "p50_latency_ms": round(r.p50_latency_ms, 2),
                "p90_latency_ms": round(r.p90_latency_ms, 2),
                "p95_latency_ms": round(r.p95_latency_ms, 2),
                "p99_latency_ms": round(r.p99_latency_ms, 2),
                "min_latency_ms": round(r.min_latency_ms, 2),
                "max_latency_ms": round(r.max_latency_ms, 2),
                "duration_s": round(r.duration_s, 2),
                "status_codes": r.status_codes,
                "errors": r.errors[:10],
            }
            for r in self.results
        ]


# ──────────────────────────────────────────
# 入口
# ──────────────────────────────────────────

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="CC91 论坛系统压力测试")
    parser.add_argument("--base-url", default="http://localhost:9000", help="后端地址（微服务 Gateway）")
    parser.add_argument("--concurrency", type=int, default=50, help="并发线程数")
    parser.add_argument("--duration", type=int, default=10, help="每个场景持续时间（秒）")
    parser.add_argument("--output", default="docs/stress-test/stress_test_result.json", help="结果输出文件")
    parser.add_argument("--cleanup-only", action="store_true", help="仅清理历史垃圾帖子（不运行测试）")
    args = parser.parse_args()

    runner = StressTestRunner(args.base_url, args.concurrency, args.duration)

    if args.cleanup_only:
        runner.cleanup_legacy_posts()
    else:
        results = runner.run_all()
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(runner.to_dict(), f, ensure_ascii=False, indent=2)
        print(f"\n结果已保存至: {args.output}")
