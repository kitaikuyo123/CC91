const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

const app = express();
const PORT = 3000;
const JWT_SECRET = 'your-secret-key-change-in-production';
const SALT_ROUNDS = 10;

// 登录尝试记录（简单的内存存储，生产环境应使用 Redis）
const loginAttempts = new Map();
const MAX_ATTEMPTS = 5;
const LOCKOUT_TIME = 15 * 60 * 1000; // 15 分钟

// 中间件
app.use(express.json());

// 暴力破解防护中间件
function rateLimitMiddleware(req, res, next) {
  const ip = req.ip || req.connection.remoteAddress;
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

// 哈希密码（初始化时使用）
async function hashPassword(password) {
  return bcrypt.hash(password, SALT_ROUNDS);
}

// 初始化测试用户（使用哈希密码）
let users = [];
(async () => {
  users = [
    { username: 'admin', passwordHash: await hashPassword('admin123') },
    { username: 'test', passwordHash: await hashPassword('test123') }
  ];
  console.log('Users initialized with hashed passwords');
})();

// POST /api/login 接口
app.post('/api/login', rateLimitMiddleware, async (req, res) => {
  const { username, password } = req.body;
  const ip = req.ip || req.connection.remoteAddress;

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

// 启动服务器
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
