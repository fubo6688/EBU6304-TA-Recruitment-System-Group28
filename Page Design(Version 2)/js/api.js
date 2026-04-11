class API {
  // 计算后端 API 基础地址，兼容不同部署路径。
  static getBaseUrl() {
    if (window.API_BASE_URL) {
      return window.API_BASE_URL;
    }

    const origin = window.location.origin;
    const path = window.location.pathname || "/";

    // Derive context path from URL path.
    // Examples:
    // /ta-system/login.html -> /ta-system/api
    // /myapp/pages/login.html -> /myapp/api
    // /login.html -> /api
    const segments = path.split("/").filter(Boolean);
    if (segments.length >= 2) {
      return `${origin}/${segments[0]}/api`;
    }

    if (segments.length === 1 && !segments[0].includes(".")) {
      return `${origin}/${segments[0]}/api`;
    }

    return `${origin}/api`;
  }

  // 统一请求入口：发送请求、解析 JSON、并将 HTTP 错误抛出。
  static async request(endpoint, options = {}) {
    const base = API.getBaseUrl();
    const url = `${base}${endpoint}`;
    const response = await fetch(url, {
      credentials: "include",
      ...options
    });

    const text = await response.text();
    let data;
    try {
      data = text ? JSON.parse(text) : {};
    } catch (e) {
      data = { success: false, message: text || "Invalid server response" };
    }

    if (!response.ok) {
      const msg = data && data.message ? data.message : `HTTP ${response.status}`;
      throw new Error(msg);
    }

    return data;
  }

  // 将普通对象编码为表单格式请求体。
  static formBody(params) {
    const form = new URLSearchParams();
    Object.keys(params).forEach((key) => {
      if (params[key] !== undefined && params[key] !== null) {
        form.append(key, params[key]);
      }
    });
    return form;
  }

  // 登录接口。
  static login(userId, password, role) {
    return API.request("/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ userId, password, role })
    });
  }

  // 获取会话状态。
  static session() {
    return API.request("/login");
  }

  // 登出接口。
  static logout() {
    return API.request("/login?action=logout");
  }

  // 获取当前用户资料。
  static getProfile() {
    return API.request("/user/profile");
  }

  // 更新用户资料。
  static updateProfile(payload) {
    return API.request("/user/profile", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody(payload)
    });
  }

  // 上传用户资料（multipart）。
  static async uploadProfile(formData) {
    const base = API.getBaseUrl();
    const response = await fetch(`${base}/user/profile`, {
      method: "POST",
      credentials: "include",
      body: formData
    });

    const text = await response.text();
    let data;
    try {
      data = text ? JSON.parse(text) : {};
    } catch (e) {
      data = { success: false, message: text || "Invalid server response" };
    }

    if (!response.ok) {
      const msg = data && data.message ? data.message : `HTTP ${response.status}`;
      throw new Error(msg);
    }
    return data;
  }

  // 查询指定 TA 资料。
  static getTaProfile(userId) {
    return API.request(`/user/ta-profile?userId=${encodeURIComponent(userId)}`);
  }

  // 生成简历预览地址。
  static getResumePreviewUrl(userId) {
    return `${API.getBaseUrl()}/user/resume?userId=${encodeURIComponent(userId)}`;
  }

  // 修改密码。
  static changePassword(oldPassword, newPassword) {
    return API.request("/user/password", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ oldPassword, newPassword })
    });
  }

  // 查询岗位列表。
  static getPositions() {
    return API.request("/position/list");
  }

  // 创建岗位。
  static createPosition(payload) {
    return API.request("/position/create", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody(payload)
    });
  }

  // 更新岗位状态。
  static updatePositionStatus(positionId, status) {
    return API.request("/position/status", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ positionId, status })
    });
  }

  // 获取申请列表（旧接口兼容）。
  static getApplications() {
    return API.request("/application/list");
  }

  // 获取审核申请列表。
  static getReviewApplications(positionId) {
    const qs = positionId ? `?positionId=${encodeURIComponent(positionId)}` : "";
    return API.request(`/application/review-list${qs}`);
  }

  // 提交申请。
  static submitApplication(positionId, priority = "first") {
    return API.request("/application/submit", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ positionId, priority })
    });
  }

  // 取消申请。
  static cancelApplication(applicationId) {
    return API.request("/application/cancel", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ applicationId })
    });
  }

  // 处理申请审核。
  static processApplication(applicationId, decision, feedback = "") {
    return API.request("/application/process", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ applicationId, decision, feedback })
    });
  }

  // 更新申请优先级。
  static updateApplicationPriority(applicationId, priority) {
    return API.request("/application/priority", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ applicationId, priority })
    });
  }

  // 获取通知列表。
  static getNotifications() {
    return API.request("/notification/list");
  }

  // 标记单条通知已读。
  static markNotificationRead(notificationId) {
    return API.request("/notification/read", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ notificationId })
    });
  }

  // 标记全部通知已读。
  static markAllNotificationsRead() {
    return API.request("/notification/read-all", {
      method: "POST"
    });
  }

  // 创建通知。
  static createNotification(payload) {
    return API.request("/notification/create", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody(payload)
    });
  }

  // 获取管理员分析数据。
  static adminAnalytics() {
    return API.request("/admin/analytics");
  }

  // 获取管理员用户列表。
  static adminUsers() {
    return API.request("/admin/users");
  }

  // 获取管理员日志列表。
  static adminLogs() {
    return API.request("/admin/logs");
  }

  // 更新用户状态（Admin）。
  static adminUpdateUserStatus(userId, status) {
    return API.request("/admin/user-status", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ userId, status })
    });
  }
}
