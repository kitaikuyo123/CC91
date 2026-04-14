# CC91

一个正在开发中的论坛系统，采用现代化技术栈构建。

## 技术栈

### 后端
- **框架**: Spring Boot 3.x
- **安全**: Spring Security (JWT 认证)
- **数据库**: MySQL 8.0+
- **ORM**: Spring Data JPA / Hibernate
- **测试**: JUnit 5, MockMvc, Mockito
- **构建**: Maven

### 前端
- **框架**: React 19
- **路由**: React Router 6+
- **HTTP**: Axios
- **状态**: Context API / React Query
- **测试**: React Testing Library, Vitest
- **构建**: Vite

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
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/cc91/
│   │       ├── controller/     # REST 控制器
│   │       ├── service/        # 业务逻辑层
│   │       ├── repository/     # 数据访问层
│   │       ├── entity/         # JPA 实体
│   │       ├── dto/            # 数据传输对象
│   │       ├── security/       # 安全配置
│   │       └── config/         # 配置类
│   ├── src/main/resources/
│   │   ├── application.yml     # 应用配置
│   │   └── db/migration/       # 数据库迁移脚本
│   ├── src/test/java/          # 单元测试
│   └── pom.xml                 # Maven 配置
├── frontend/                   # React 前端
│   ├── src/
│   │   ├── components/         # React 组件
│   │   ├── pages/              # 页面组件
│   │   ├── hooks/              # 自定义 Hooks
│   │   ├── api/                # API 调用
│   │   ├── context/            # Context 状态
│   │   └── utils/              # 工具函数
│   ├── index.html
│   └── package.json
└── scripts/                    # 工具脚本
```

## 环境要求

- **后端**: JDK 17+, Maven 3.6+, MySQL 8.0+
- **前端**: Node.js 18+, npm 9+

## 安装

### 1. 克隆仓库
```bash
git clone <repository-url>
cd LargeScale
```

### 2. 数据库设置
```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE cc91_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'cc91_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON cc91_db.* TO 'cc91_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 后端设置
```bash
cd backend
# 配置数据库连接
cp src/main/resources/application.yml.example src/main/resources/application.yml
# 编辑 application.yml 设置数据库连接信息

# 安装依赖并启动
mvn clean install
mvn spring-boot:run
```

服务器将运行在 `http://localhost:8080`

### 4. 前端设置
```bash
cd frontend
npm install
npm run dev
```

前端将运行在 `http://localhost:5173`

## API 接口

### 登录
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### 注册（发送验证码）
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "email": "user@example.com",
  "password": "password123"
}
```

### 验证邮箱
```
POST /api/auth/verify-email
Content-Type: application/json

{
  "email": "user@example.com",
  "code": "123456"
}
```

## 测试

### 后端测试
```bash
cd backend
mvn test
```

### 前端测试
```bash
cd frontend
npm test
```

### 覆盖率报告
```bash
# 后端
cd backend
mvn jacoco:report

# 前端
cd frontend
npm run coverage
```

## 默认测试账号

| 用户名  | 密码      |
|---------|-----------|
| admin   | admin123  |
| test    | test123   |

## 安全提示

**重要提示：** 这是一个演示项目。生产环境部署前请务必：

1. 修改 JWT_SECRET 密钥
2. 使用环境变量存储敏感数据
3. 配置 HTTPS/TLS
4. 实现刷新令牌轮换机制
5. 添加更全面的输入验证
6. 配置防火墙和速率限制
7. 定期更新依赖包

## 开源协议

MIT

## 贡献指南

1. Fork 本仓库
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request
