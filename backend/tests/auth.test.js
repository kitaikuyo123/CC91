/**
 * 后端 API 测试 - 登录接口
 *
 * 测试用例：
 * 1. 登录成功测试
 * 2. 用户不存在测试
 * 3. 密码错误测试
 * 4. 缺少字段测试
 *
 * 运行: npm test
 */

const request = require('supertest');
const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

// 创建独立的测试 app，避免端口冲突
function createTestApp() {
  const app = express();
  app.use(express.json());

  const JWT_SECRET = 'test-secret-key';
  const SALT_ROUNDS = 10;
  const loginAttempts = new Map();
  const MAX_ATTEMPTS = 5;
  const LOCKOUT_TIME = 15 * 60 * 1000;

  // 暴力破解防护中间件
  function rateLimitMiddleware(req, res, next) {
    const ip = req.ip || 'test-ip';
    const attempts = loginAttempts.get(ip) || { count: 0, lockUntil: 0 };

    if (Date.now() < attempts.lockUntil) {
      const remainingTime = Math.ceil((attempts.lockUntil - Date.now()) / 1000);
      return res.status(429).json({
        success: false,
        message: `账户已锁定，请 ${remainingTime} 秒后重试`
      });
    }

    next();
  }

  // 存储测试用户
  let users = [];

  // 初始化函数
  async function initUsers() {
    users = [
      { username: 'admin', passwordHash: await bcrypt.hash('admin123', SALT_ROUNDS) },
      { username: 'test', passwordHash: await bcrypt.hash('test123', SALT_ROUNDS) }
    ];
  }

  // POST /api/login 接口
  app.post('/api/login', rateLimitMiddleware, async (req, res) => {
    const { username, password } = req.body;
    const ip = req.ip || 'test-ip';

    // 检查必填字段
    if (!username || !password) {
      return res.status(400).json({
        success: false,
        message: '缺少用户名或密码'
      });
    }

    // 输入长度限制
    if (username.length > 50 || password.length > 100) {
      return res.status(400).json({
        success: false,
        message: '输入过长'
      });
    }

    // 查找用户
    const user = users.find(u => u.username === username);

    // 统一错误消息（防止用户枚举）
    const errorMessage = '用户名或密码错误';

    if (!user) {
      return res.status(401).json({
        success: false,
        message: errorMessage
      });
    }

    // 验证密码
    const isValidPassword = await bcrypt.compare(password, user.passwordHash);

    if (!isValidPassword) {
      // 记录失败尝试
      const attempts = loginAttempts.get(ip) || { count: 0, lockUntil: 0 };
      attempts.count += 1;

      if (attempts.count >= MAX_ATTEMPTS) {
        attempts.lockUntil = Date.now() + LOCKOUT_TIME;
        attempts.count = 0;
      }

      loginAttempts.set(ip, attempts);

      return res.status(401).json({
        success: false,
        message: errorMessage
      });
    }

    // 登录成功，清除失败记录
    loginAttempts.delete(ip);

    // 生成 JWT Token
    const token = jwt.sign(
      { username: user.username },
      JWT_SECRET,
      { expiresIn: '24h' }
    );

    res.status(200).json({
      success: true,
      message: '登录成功',
      token: token
    });
  });

  return { app, initUsers };
}

// ============================================
// 测试套件
// ============================================

describe('POST /api/login', function() {
  let app, initUsers;

  // 所有测试前初始化
  beforeAll(async function() {
    const testApp = createTestApp();
    app = testApp.app;
    initUsers = testApp.initUsers;
    await initUsers();
  });

  // ============================================
  // 测试 1: 登录成功
  // ============================================
  describe('登录成功测试', function() {

    it('用 admin 用户登录应返回 200 和 token', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'admin', password: 'admin123' });

      expect(response.status).toBe(200);
      expect(response.body.success).toBe(true);
      expect(response.body.message).toBe('登录成功');
      expect(response.body.token).toBeDefined();
      expect(typeof response.body.token).toBe('string');
    });

    it('用 test 用户登录应返回 200 和 token', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'test', password: 'test123' });

      expect(response.status).toBe(200);
      expect(response.body.success).toBe(true);
      expect(response.body.token).toBeDefined();
    });

    it('返回的 token 应为有效的 JWT', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'admin', password: 'admin123' });

      const token = response.body.token;
      const decoded = jwt.decode(token);

      expect(decoded).toBeDefined();
      expect(decoded.username).toBe('admin');
      expect(decoded.exp).toBeDefined();
    });

  });

  // ============================================
  // 测试 2: 用户不存在
  // ============================================
  describe('用户不存在测试', function() {

    it('不存在的用户名应返回 401', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'nonexistent', password: 'password123' });

      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('用户名或密码错误');
    });

    it('随机用户名应返回 401', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'randomuser123', password: 'anypassword' });

      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
    });

    it('错误消息应不暴露用户是否存在', async function() {
      // 存在的用户 + 错误密码
      const response1 = await request(app)
        .post('/api/login')
        .send({ username: 'admin', password: 'wrongpassword' });

      // 不存在的用户
      const response2 = await request(app)
        .post('/api/login')
        .send({ username: 'nonexistent', password: 'wrongpassword' });

      // 消息应完全相同
      expect(response1.body.message).toBe(response2.body.message);
    });

  });

  // ============================================
  // 测试 3: 密码错误
  // ============================================
  describe('密码错误测试', function() {

    it('admin 用户错误密码应返回 401', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'admin', password: 'wrongpassword' });

      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('用户名或密码错误');
    });

    it('密码大小写敏感测试', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'admin', password: 'ADMIN123' });

      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
    });

    it('密码尾部多字符应失败', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'admin', password: 'admin123extra' });

      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
    });

  });

  // ============================================
  // 测试 4: 缺少字段
  // ============================================
  describe('缺少字段测试', function() {

    it('缺少 username 应返回 400', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ password: 'admin123' });

      expect(response.status).toBe(400);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('缺少用户名或密码');
    });

    it('缺少 password 应返回 400', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'admin' });

      expect(response.status).toBe(400);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('缺少用户名或密码');
    });

    it('缺少两个字段应返回 400', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({});

      expect(response.status).toBe(400);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('缺少用户名或密码');
    });

    it('空请求体应返回 400', async function() {
      const response = await request(app)
        .post('/api/login')
        .send();

      expect(response.status).toBe(400);
      expect(response.body.success).toBe(false);
    });

    it('username 为 null 应返回 400', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: null, password: 'admin123' });

      expect(response.status).toBe(400);
      expect(response.body.success).toBe(false);
    });

    it('password 为 null 应返回 400', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'admin', password: null });

      expect(response.status).toBe(400);
      expect(response.body.success).toBe(false);
    });

    it('空字符串 username 应返回 400', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: '', password: 'admin123' });

      expect(response.status).toBe(400);
      expect(response.body.success).toBe(false);
    });

    it('空字符串 password 应返回 400', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'admin', password: '' });

      expect(response.status).toBe(400);
      expect(response.body.success).toBe(false);
    });

  });

  // ============================================
  // 测试 5: 输入长度限制
  // ============================================
  describe('输入长度限制测试', function() {

    it('超长用户名应返回 400', async function() {
      const longUsername = 'a'.repeat(51);
      const response = await request(app)
        .post('/api/login')
        .send({ username: longUsername, password: 'password' });

      expect(response.status).toBe(400);
      expect(response.body.message).toBe('输入过长');
    });

    it('超长密码应返回 400', async function() {
      const longPassword = 'a'.repeat(101);
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'admin', password: longPassword });

      expect(response.status).toBe(400);
      expect(response.body.message).toBe('输入过长');
    });

    it('用户名刚好 50 字符应被接受', async function() {
      const response = await request(app)
        .post('/api/login')
        .send({ username: 'a'.repeat(50), password: 'password' });

      // 用户不存在会返回 401，但不是 400 长度错误
      expect(response.status).toBe(401);
    });

  });

});
