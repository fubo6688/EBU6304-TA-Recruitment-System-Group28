# TA 职位申请功能实现总结

## 📋 需求实现清单

### ✅ 需求 1: "Apply" 按钮仅对已登录的 TA 和开放职位启用
- **实现位置**：`ta-positions.html` 的 `applyPosition()` 函数
- **验证逻辑**：
  - 检查用户是否已登录（从 `localStorage.userId` 获取）
  - 检查用户角色是否为 "TA"（从 `localStorage.userRole` 获取）
  - 检查职位状态是否为 "open"
  - 如果条件不满足，显示相应的警告信息

**代码示例**：
```javascript
if (!userId) {
  taSystem.showMessage('Please log in first', 'warning');
  return;
}
if (userRole !== 'TA') {
  taSystem.showMessage('Only TAs can apply for positions', 'warning');
  return;
}
```

---

### ✅ 需求 2: 生成唯一的申请 ID（链接 TA ID 和职位 ID）
- **实现位置**：`api_server.py` 的 `handle_create_application()` 函数
- **生成规则**：`app{timestamp_in_milliseconds}`
- **链接方式**：申请记录结构中包含：
  - `applicationId`: 唯一的申请 ID
  - `taId`: 提交申请的 TA ID
  - `positionId`: 申请的职位 ID
  - 这三个字段组合唯一标识一个申请

**申请记录格式**：
```
app1774784816183|pos1774246445186|职位标题|20210002|TA名称|MO_ID|first|pending|2026-03-29|
```

**响应示例**：
```json
{
  "success": true,
  "applicationId": "app1774784816183",
  "application": {
    "id": "app1774784816183",
    "positionId": "pos1774246445186",
    "taId": "20210002",
    "taName": "李四",
    "status": "pending",
    "appliedDate": "2026-03-29"
  }
}
```

---

### ✅ 需求 3: 申请状态默认为 "Pending"，记录申请时间
- **实现位置**：`api_server.py` 的 `handle_create_application()` 函数
- **状态设置**：所有新申请的状态都硬编码为 `"pending"`
- **时间记录**：使用 `datetime.now().strftime("%Y-%m-%d")` 获取当前日期
- **持久化**：申请记录保存到 `data/applications.txt`

**代码示例**：
```python
today = datetime.now().strftime("%Y-%m-%d")
application_record = f"{app_id}|{position_id}|{pos_title}|{ta_id}|{ta_name}|{mo_id}|first|pending|{today}|"
```

---

### ✅ 需求 4: 防止重复申请（同一 TA + 同一职位）
- **实现位置**：`api_server.py` 的 `check_application_exists()` 和 `handle_create_application()` 函数
- **检查方法**：在处理申请前扫描现有的申请记录
- **拒绝逻辑**：如果发现相同的 `(positionId, taId)` 组合，返回错误信息

**验证响应示例**：
```json
{
  "success": false,
  "message": "You already applied for this position"
}
```

**测试命令**：
```bash
# 第一次申请（成功）
curl -X POST http://localhost:9091/api/applications \
  -d "taId=20210002&positionId=pos1774246445186"

# 第二次申请同一职位（被拒绝）
curl -X POST http://localhost:9091/api/applications \
  -d "taId=20210002&positionId=pos1774246445186"
# 响应：{"success": false, "message": "You already applied for this position"}
```

---

## 🔧 技术实现细节

### 后端 API 端点

#### 1. 获取职位列表
```
GET /api/positions?taId={userId}
```
**响应**：
- 返回所有 `status="open"` 的职位
- 包含 `taApplied` 字段表示该 TA 是否已申请过
- 用于在前端显示"Already Applied"按钮状态

#### 2. 提交职位申请
```
POST /api/applications
Content-Type: application/x-www-form-urlencoded

taId={userId}&positionId={positionId}
```

**验证流程**：
1. 检查 TA 是否已申请过该职位
2. 验证职位是否存在且状态为 "open"
3. 验证 TA ID 是否有效
4. 生成唯一申请 ID
5. 保存到 `applications.txt`

---

### 前端更新

#### 1. API 客户端方法更新 (`js/api.js`)
```javascript
// 获取职位（可选传入 taId 以检查已申请）
static getPositions(taId = null) {
  const qs = taId ? `?taId=${encodeURIComponent(taId)}` : "";
  return API.request(`/positions${qs}`);
}

// 提交申请
static submitApplication(positionId, taId, priority = "first") {
  return API.request("/applications", {
    method: "POST",
    body: API.formBody({ positionId, taId, priority })
  });
}
```

#### 2. 页面逻辑更新 (`ta-positions.html`)
- 页面加载时获取当前 TA ID（从 `localStorage.userId`）
- 调用 API 加载职位列表时传递 TA ID
- 根据 `taApplied` 标志显示不同的按钮状态
- 申请前检查是否已登录及角色验证

---

## 📊 数据持久化

### 应用文件结构
```
data/
├── applications.txt    # 申请记录（新增支持）
├── positions.txt       # 职位列表
└── users.txt          # 用户数据
```

### 申请记录格式
```
{appId}|{positionId}|{positionTitle}|{taId}|{taName}|{moId}|{priority}|{status}|{appliedDate}|{comment}
```

**示例**：
```
app1774784816183|pos1774246445186|sssssss|20210002|李四|M001|first|pending|2026-03-29|
```

---

## 🧪 测试用例

### 测试场景 1: 成功申请职位
```bash
curl -X POST http://localhost:9091/api/applications \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "taId=20210002&positionId=pos1774246445186"
```
**预期结果**：申请成功，返回 `applicationId`

### 测试场景 2: 防止重复申请
```bash
# 同一申请再试一次
curl -X POST http://localhost:9091/api/applications \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "taId=20210002&positionId=pos1774246445186"
```
**预期结果**：返回错误 "You already applied for this position"

### 测试场景 3: 申请已关闭职位
```bash
curl -X POST http://localhost:9091/api/applications \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "taId=20210001&positionId=pos_closed"
```
**预期结果**：返回错误 "This position is no longer open"

### 测试场景 4: 获取职位列表（含申请状态）
```bash
curl "http://localhost:9091/api/positions?taId=20210001"
```
**预期结果**：返回职位列表，每个职位包含 `taApplied` 字段

---

## 🚀 使用流程

1. **用户登录**
   - TA 用户登录系统
   - 系统将 `userId` 和 `userRole` 保存至 `localStorage`

2. **浏览职位**
   - 进入"Browse Positions"（尤其是 `ta-positions.html`）
   - 前端加载职位列表，API 返回该 TA 的申请状态

3. **提交申请**
   - 点击"Apply Now"按钮
   - 前端验证登录状态和角色
   - 调用 API 提交申请
   - 成功后刷新职位列表，已申请职位显示"Already Applied"

4. **申请跟踪**
   - TA 可在"My Applications"页面查看历史申请
   - 每个申请显示状态、申请日期和职位信息

---

## ✨ 功能特性

| 特性 | 实现状态 | 说明 |
|------|--------|------|
| 职位申请 | ✅ 完成 | TA 可一键申请开放职位 |
| 防重复申请 | ✅ 完成 | 同一职位同一 TA 仅可申请一次 |
| 状态跟踪 | ✅ 完成 | 申请状态默认为 "pending" |
| 鉴权验证 | ✅ 完成 | 仅已登录 TA 可申请开放职位 |
| 数据持久化 | ✅ 完成 | 申请记录保存至 `applications.txt` |
| 唯一 ID 生成 | ✅ 完成 | 基于时间戳生成唯一申请 ID |

---

## 📁 修改的文件清单

1. **`api_server.py`** - 新增 `/api/positions` 和 `/api/applications` 端点
2. **`js/api.js`** - 更新 `getPositions()`, `submitApplication()`, `getApplications()` 方法
3. **`ta-positions.html`** - 修改 `applyPosition()` 和 `loadPositions()` 逻辑
4. **`js/config.js`** - 配置 API 地址

---

## 🎯 访问地址

- **前端应用**：`http://localhost:8001`
- **登录页面**：`http://localhost:8001/login.html`
- **职位页面**：`http://localhost:8001/ta-positions.html`（登录后访问）
- **API 服务**：`http://localhost:9091`

---

## 📝 TA 测试账户

| 用户ID | 密码 | 名称 |
|--------|------|------|
| 20210001 | 123456 | 张三 |
| 20210002 | 123456 | 李四 |

