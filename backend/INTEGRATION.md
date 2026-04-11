# 前后端集成指南

## 快速开始

### 环境要求

- Java JDK 8+ 
- Tomcat 9.0+
- 现代浏览器 (Chrome, Firefox, Edge)

### 一分钟快速部署

#### 1. 编译后端代码

```bash
cd C:\Users\ROG\Desktop\PAGE\backend
deploy.bat
```

#### 2. 下载 JSON 库

访问 https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar

或使用 curl：

```bash
curl -o WEB-INF/lib/json-20240303.jar https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar
```

#### 3. 部署到 Tomcat

```bash
# 假设 Tomcat 在 C:\Program Files\Tomcat

xcopy /E /I /Y . "C:\Program Files\Tomcat\webapps\ta-system"
```

#### 4. 启动 Tomcat

```bash
cd "C:\Program Files\Tomcat\bin"
startup.bat
```

#### 5. 打开浏览器

```
http://localhost:8080/ta-system/
```

## 系统架构

```
┌─────────────────────────────────────────────┐
│           Web Browser (前端)                 │
│    HTML5 + CSS3 + JavaScript                │
│  (localStorage 本地存储用户会话)            │
└───────────────────┬─────────────────────────┘
                    │ HTTP/JSON
                    │
┌───────────────────▼─────────────────────────┐
│         Tomcat Web Server                   │
├─────────────────────────────────────────────┤
│ Servlet Layer (处理业务逻辑)                 │
│  ├─ LoginServlet (登录认证)                  │
│  ├─ UserServlet (用户管理)                   │
│  ├─ PositionServlet (职位管理)               │
│  ├─ ApplicationServlet (应用管理)            │
│  └─ NotificationServlet (通知管理)           │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│     Data Layer (纯文本存储)                  │
│  data/                                      │
│  ├─ users.txt (用户数据)                     │
│  ├─ positions.txt (职位数据)                 │
│  └─ {userId}_notifications.txt (通知数据)   │
└─────────────────────────────────────────────┘
```

## 关键 API

### 用户认证

登录后，系统会自动设置 session，之后所有请求都会自动携带会话信息。

**请求:**
```
POST /api/login
Content-Type: application/x-www-form-urlencoded

userId=ta001&password=123456
```

**响应:**
```json
{
  "success": true,
  "message": "登录成功",
  "user": {
    "userId": "ta001",
    "userName": "张三",
    "userRole": "TA",
    "email": "zhangsan@example.com"
  }
}
```

### 获取用户信息

```
GET /api/user/profile
```

**响应:**
```json
{
  "userId": "ta001",
  "userName": "张三",
  "email": "zhangsan@example.com",
  "role": "TA",
  "qmId": "20210001"
}
```

### 职位列表

```
GET /api/position/list
```

**响应:**
```json
[
  {
    "id": "pos001",
    "title": "数据结构助教",
    "department": "计算机学院",
    "salary": "2000-3000元/月",
    "description": "负责数据结构课程的教学辅导",
    "requirements": "计算机相关专业，有教学经验优先"
  }
]
```

## 前端集成代码

### 修改登录接口

编辑 `login.html` 或 `login.js`：

```javascript
// 替换原来的 localStorage 验证逻辑
const loginForm = document.getElementById('loginForm');
loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  
  const userId = document.querySelector('input[name="userId"]').value;
  const password = document.querySelector('input[name="password"]').value;
  
  try {
    const response = await fetch('http://localhost:8080/ta-system/api/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: `userId=${userId}&password=${password}`,
      credentials: 'include' // 发送 session cookie
    });
    
    const data = await response.json();
    
    if (data.success) {
      // 保存用户信息到 localStorage
      localStorage.setItem('userId', data.user.userId);
      localStorage.setItem('userName', data.user.userName);
      localStorage.setItem('userRole', data.user.userRole);
      
      // 跳转到首页
      window.location.href = 'index.html';
    } else {
      alert(data.message);
    }
  } catch (error) {
    console.error('登录请求失败:', error);
    alert('网络错误，请检查后端服务是否启动');
  }
});
```

### 创建通用 API 工具类

在 `js/api.js` 中添加：

```javascript
class API {
  static BASE_URL = 'http://localhost:8080/ta-system/api';

  static async request(endpoint, options = {}) {
    const response = await fetch(`${this.BASE_URL}${endpoint}`, {
      ...options,
      credentials: 'include', // 自动携带 session
      headers: {
        'Content-Type': 'application/json',
        ...options.headers
      }
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return response.json();
  }

  // 用户相关
  static getProfile() {
    return this.request('/user/profile');
  }

  static changePassword(oldPassword, newPassword) {
    return this.request('/user/password', {
      method: 'PUT',
      body: JSON.stringify({ oldPassword, newPassword })
    });
  }

  // 职位相关
  static getPositions() {
    return this.request('/position/list');
  }

  // 应用相关  
  static getApplications() {
    return this.request('/application/list');
  }

  // 通知相关
  static getNotifications() {
    return this.request('/notification/list');
  }
}

// 使用示例
API.getProfile().then(user => {
  console.log('用户信息:', user);
});
```

## 故障排除

### 问题 1: CORS 错误

**错误信息:** 
```
Access to XMLHttpRequest at 'http://localhost:8080/ta-system/api/login' 
from origin 'file://C:...' has been blocked by CORS policy
```

**解决:**
确保已部署到 Tomcat（不能使用 file:// 协议），或在后端添加 CORS 头（已包含）。

### 问题 2: 404 Not Found

**错误信息:**
```
GET http://localhost:8080/ta-system/api/login 404 (Not Found)
```

**排查:**
1. 检查 Tomcat 是否启动
2. 检查部署路径是否正确
3. 检查 java 编译是否成功
4. 查看 Tomcat logs

### 问题 3: Session 失效

用户登录后跳转到新页面就失效了。

**原因:** 未设置 `credentials: 'include'`

**修复:** 所有 fetch 请求都加上：
```javascript
credentials: 'include'
```

## 数据持久化

当前系统使用文本文件存储数据。生产环境建议升级到数据库：

### 升级到 MySQL

1. 修改 `DataManager.java`，使用 JDBC 连接 MySQL
2. 编写 SQL 建表脚本
3. 在 `web.xml` 中配置数据库连接参数

示例数据库架构：

```sql
CREATE TABLE users (
  user_id VARCHAR(20) PRIMARY KEY,
  user_name VARCHAR(100),
  email VARCHAR(100),
  password VARCHAR(100),
  role VARCHAR(20),
  qm_id VARCHAR(20),
  status VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE positions (
  position_id VARCHAR(20) PRIMARY KEY,
  title VARCHAR(100),
  department VARCHAR(100),
  salary VARCHAR(50),
  description TEXT,
  requirements TEXT,
  created_at TIMESTAMP
);

CREATE TABLE applications (
  application_id VARCHAR(20) PRIMARY KEY,
  user_id VARCHAR(20),
  position_id VARCHAR(20),
  status VARCHAR(20),
  applied_date TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  FOREIGN KEY (position_id) REFERENCES positions(position_id)
);
```

## 下一步

- [ ] 集成前端和后端
- [ ] 部署到服务器
- [ ] 添加数据库支持
- [ ] 实现更复杂的业务逻辑
- [ ] 添加单元测试
- [ ] 性能优化和缓存

---

**技术栈:**
- 前端: HTML5 + CSS3 + Vanilla JavaScript
- 后端: Java Servlet + JSP
- 数据: 纯文本 (可升级到 MySQL/PostgreSQL)
- 服务器: Apache Tomcat 9.0+
