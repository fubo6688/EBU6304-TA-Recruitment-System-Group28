# TA 招聘系统 - 后端部署指南

## 项目结构

```
backend/
├── WEB-INF/
│   ├── web.xml              # Tomcat 配置文件
│   ├── classes/             # 编译后的 Java 类
│   └── lib/                 # 依赖库
├── src/
│   └── com/ta/
│       ├── servlet/         # Servlet 类
│       ├── model/           # 数据模型
│       └── util/            # 工具类
├── data/                    # 数据文件存储目录
├── build.bat                # 编译脚本
└── README.md                # 本文件
```

## 前置要求

1. **Java JDK 8+**
   - 下载：https://www.oracle.com/java/technologies/downloads/
   - 验证：`javac -version`

2. **Tomcat 9.0+**
   - 下载：https://tomcat.apache.org/download-90.cgi
   - 解压到任意位置（如 `C:\Program Files\Tomcat`)

3. **JSON 库**
   - 下载：https://mvnrepository.com/artifact/org.json/json
   - 放到 `WEB-INF/lib/json-xxx.jar`

## 部署步骤

### 1. 编译 Java 源代码

在 `backend` 目录下运行：

```bash
build.bat
```

或手动编译：

```bash
javac -d WEB-INF\classes src\com\ta\model\*.java src\com\ta\util\*.java src\com\ta\servlet\*.java
```

### 2. 下载依赖库

下载 JSON 库并放到 `WEB-INF/lib/`:

- 访问：https://mvnrepository.com/artifact/org.json/json
- 下载最新版本 JAR 文件（如 `json-20240303.jar`）
- 放到 `c:\Users\ROG\Desktop\PAGE\backend\WEB-INF\lib\`

### 3. 配置 Tomcat

#### 方式 A：直接部署（推荐用于学习）

1. 进入 Tomcat 的 `webapps` 目录
2. 创建文件夹 `ta-system`
3. 复制整个 `backend` 目录的内容到 `webapps/ta-system`
4. 目录结构应该是：
   ```
   webapps/ta-system/
   ├── WEB-INF/
   ├── index.html      (复制前端文件)
   ├── css/
   ├── js/
   └── ...
   ```

#### 方式 B：使用 Build 目录（推荐用于生产）

1. 右键项目 → 选择 "Run as" → "Run on Server"
2. 或使用 IDE 的部署功能

### 4. 配置前端连接后端

编辑 `js/script.js`，添加后端 API 基础 URL：

```javascript
const API_BASE_URL = 'http://localhost:8080/ta-system/api';
```

### 5. 启动 Tomcat

#### Windows 启动

```bash
cd C:\Program Files\Tomcat\bin
startup.bat
```

#### 验证启动

访问 `http://localhost:8080` 看到 Tomcat 欢迎页面说明启动成功。

### 6. 访问系统

打开浏览器访问：

```
http://localhost:8080/ta-system/
```

## API 端点

### 用户认证

- **POST /api/login**
  - 参数: `userId`, `password`
  - 返回: `{success: bool, message: string, user: object}`

### 用户信息

- **GET /api/user/profile**
  - 获取当前用户信息
  - 返回: `{userId, userName, email, role, qmId}`

- **PUT /api/user/password**
  - 修改密码
  - 参数: `oldPassword`, `newPassword`

### 职位管理

- **GET /api/position/list**
  - 获取所有职位列表
  - 返回: 职位数组

- **POST /api/position/create**
  - 创建新职位（仅 MO）

### 应用管理

- **GET /api/application/list**
  - 获取用户的应用列表

- **POST /api/application/submit**
  - 提交职位应用

### 通知

- **GET /api/notification/list**
  - 获取用户通知

- **POST /api/notification/create**
  - 创建新通知

## 默认测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| ta001 | 123456 | TA |
| ta002 | 123456 | TA |
| mo001 | 123456 | MO |
| admin001 | admin123 | Admin |

## 数据存储

所有数据存储在 `backend/data/` 目录：

- `users.txt` - 用户列表
- `positions.txt` - 职位列表
- `{userId}_notifications.txt` - 用户通知

## 常见问题

### Q: 报错 "Web application at context path [/ta-system] has been marked as failed to start"

A: 检查：
1. Java 类是否编译成功
2. `WEB-INF/lib/` 中是否有 json-xxx.jar
3. 路径中不能有中文字符

### Q: 404 错误访问不到 API

A: 确认：
1. Tomcat 已启动
2. 访问的 URL 路径正确
3. web.xml 中的映射配置正确

### Q: 无法登录

A: 检查：
1. 账号和密码是否正确（见上表）
2. `backend/data/users.txt` 是否存在
3. 检查浏览器控制台的错误信息

## 扩展功能

如需添加更多功能：

1. 在 `src/com/ta/servlet/` 中创建新的 Servlet
2. 在 `web.xml` 中配置新的 Servlet 映射
3. 修改前端代码调用新 API

## 性能优化（生产环境）

1. **使用数据库**替换纯文本存储
   - 替换 `DataManager` 中的文件操作为 JDBC/数据库操作
   - 推荐使用 MySQL 或 PostgreSQL

2. **添加缓存**
   - 使用 Redis 缓存用户信息和职位列表

3. **连接池**
   - 使用 HikariCP 管理数据库连接

4. **对象关系映射**
   - 使用 MyBatis 或 Hibernate

## 联系方式

如有问题，查看 Tomcat logs：
- Windows: `C:\Program Files\Tomcat\logs\catalina.out`
- 提供日志信息以便诊断

---

**版本**: 1.0  
**更新**: 2026-03-16
