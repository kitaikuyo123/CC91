# CC91

一个正在开发中的论坛系统，**当前阶段已实现用户登录功能**，后续将陆续添加注册、发帖、评论等核心论坛功能。

## 功能特性

- **用户认证**
  - 基于 JWT 的安全登录
  - 使用 bcrypt 进行密码哈希
  - 速率限制防止暴力破解
  - 登录失败后账户锁定

- **用户注册**
  - 用户名唯一性验证
  - 邮箱格式验证
  - 密码强度要求（至少 6 位字符）
  - 6 位数字邮箱验证码

- **安全防护**
  - CORS 跨域支持
  - 输入验证和长度限制
  - 统一错误消息（防止用户枚举）
  - 验证码过期机制（10 分钟）

## 项目结构

```
CC91/
├── backend/             # Express.js API 服务器
│   ├── server.js        # 主程序文件
│   ├── tests/           # API 测试
│   └── package.json     # 后端依赖
├── frontend/            # 静态 HTML/CSS/JS 客户端
│   ├── index.html       # 登录/注册表单
│   ├── app.js           # 前端逻辑
│   └── style.css        # 样式文件
├── scripts/             # 工具脚本
└── logs/                # 应用日志
```

## 环境要求

- Node.js >= 14.x
- npm >= 6.x

## 安装

1. 克隆仓库：
```bash
git clone <repository-url>
cd LargeScale
```

2. 安装后端依赖：
```bash
cd backend
npm install
```

## 使用方法

### 开发环境

启动后端服务器：
```bash
cd backend
npm start
```

服务器将运行在 `http://localhost:3000`

### 打开前端页面

```bash
cd frontend
npx serve -p 8080
```

### API 接口

#### 登录
```
POST /api/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

#### 注册（发送验证码）
```
POST /api/register
Content-Type: application/json

{
  "username": "newuser",
  "email": "user@example.com",
  "password": "password123"
}
```

#### 验证邮箱
```
POST /api/verify-email
Content-Type: application/json

{
  "email": "user@example.com",
  "code": "123456"
}
```

## 测试

运行测试套件：
```bash
cd backend
npm test
```

监听模式运行测试：
```bash
npm run test:watch
```

生成测试覆盖率报告：
```bash
npm run test:coverage
```

## 默认测试账号

| 用户名  | 密码      |
|---------|-----------|
| admin   | admin123  |
| test    | test123   |

## 安全提示

**重要提示：** 这是一个演示项目。生产环境部署前请务必：

1. 修改 `server.js` 中的 `JWT_SECRET`
2. 使用真实的数据库（PostgreSQL、MongoDB 等）
3. 使用 Redis 存储会话/验证码
4. 实现真实的邮件发送功能（SMTP）
5. 启用 HTTPS/TLS
6. 使用环境变量存储敏感数据
7. 实现刷新令牌轮换机制
8. 添加更全面的输入验证

## 技术栈

- **后端**
  - Express.js - Web 框架
  - bcrypt - 密码哈希
  - jsonwebtoken - JWT 认证
  - Jest - 测试框架

- **前端**
  - 原生 JavaScript
  - CSS3

## 开源协议

MIT

## 贡献指南

1. Fork 本仓库
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request
