# CC91 校园论坛系统 — 环境搭建指南

> 验证环境：Windows 11 / Java 24 / Maven 3.9.9 / Node.js 24.11.0 / MySQL 8.0.41  
> 验证日期：2026-06-05  
> 验证人：2f4f4f（DevOps）

---

## 前置要求

| 依赖 | 版本 | 验证命令 |
|------|------|----------|
| Java JDK | 17+ | `java -version` |
| Maven | 3.6+ | `mvn -v` |
| Node.js | 18+ | `node -v` |
| npm | 9+ | `npm -v` |
| MySQL | 8.0+ | `mysql --version` |
| Git | 2.30+ | `git --version` |

---

## 1. 克隆项目

```bash
git clone https://github.com/kitaikuyo123/CC91.git
cd CC91
git checkout develop
```

**注意：** 最新代码在 `develop` 分支，`main` 分支只有旧版登录注册功能。

---

## 2. 数据库准备

```sql
-- 登录 MySQL（替换为你的 root 密码）
mysql -u root -p

-- 创建数据库
CREATE DATABASE cc91_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

或一行命令完成：
```bash
mysql -u root -p你的密码 -e "CREATE DATABASE cc91_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

---

## 3. 后端配置与启动

### 3.1 创建本地配置文件

在 `backend/src/main/resources/` 下创建 `application-local.yml`：

```yaml
spring:
  datasource:
    username: root
    password: 你的MySQL密码

  mail:
    username: 
    password: 

app:
  mail:
    console-log-only: true   # 本地开发不发邮件，验证码打印到控制台
  dev-data:
    enabled: true
```

**说明：** 项目使用环境变量管理配置，但本地开发建议用 `application-local.yml` 覆盖，避免每次启动都输环境变量。

### 3.2 编译并启动

```bash
cd backend
mvn clean package -DskipTests
java -jar target/cc91-backend-1.0.0.jar --spring.profiles.active=local
```

**预期输出：**
```
BUILD SUCCESS
Successfully validated 12 migrations
Tomcat started on port 8080
CC91 Forum Backend is running on http://localhost:8080
```

---

## 4. 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端将运行在 http://localhost:5173

**默认测试账号：**
| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | ADMIN |
| user | user123 | USER |

---

## 5. 生产构建验证

```bash
cd frontend
npm run build
```

**预期产物：** `frontend/dist/` 目录包含：
- `index.html`
- `assets/`（JS、CSS、图片）

> 注意：生产构建后 Vite proxy 不再生效，需独立部署后端或使用 Nginx 反向代理。

---

## 6. 常见问题排查

### Q1: `Port 8080 was already in use`

**原因：** 之前启动的后端进程还在运行。  
**解决：**
```bash
taskkill /F /IM java.exe
```
然后重新启动后端。

### Q2: `Access denied for user 'root'@'localhost'`

**原因：** MySQL 密码错误或用户无权限。  
**解决：** 确认 `application-local.yml` 中的密码正确，且 root 用户有 `cc91_db` 访问权限。

### Q3: Flyway 迁移报错 `Checksum mismatch`

**原因：** 修改了已执行的 `.sql` 迁移文件。  
**解决：** 不要修改已执行的迁移！如需修改，先删库重建：
```sql
DROP DATABASE cc91_db;
CREATE DATABASE cc91_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Q4: 前端 `npm run build` 报 `window.matchMedia is not a function`

**原因：** Vitest/jsdom 环境缺少 matchMedia。  
**解决：** 在 `frontend/src/test/setup.ts` 中添加 matchMedia mock（详见项目测试配置）。

---

## 7. 干净环境构建验证（DevOps checklist）

- [x] 新目录 `git clone` 后 `mvn clean package -DskipTests` 编译成功  
- [x] 空数据库上 Flyway V1~V12 全部迁移成功  
- [x] `npm ci && npm run build` 打包成功，产出 `dist/` 目录  
- [x] 后端启动后访问 `http://localhost:8080` 返回正常响应  

> 验证日期：2026-06-05  
> 验证人：2f4f4f
