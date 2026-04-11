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

  // 统一请求入口：发送请求、解析 JSON、并将 HTTP 错误转换为异常。
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

  // 将普通对象编码为 x-www-form-urlencoded 请求体。
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

  // 注册接口。
  static register(payload) {
    return API.request("/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ ...payload, action: "register" })
    });
  }

  // 获取当前会话状态。
  static session() {
    return API.request("/login");
  }

  // 根据账号查询角色提示，用于登录页角色自动回填。
  static getLoginRoleHint(userId) {
    const qs = `?action=role-hint&userId=${encodeURIComponent(userId || "")}`;
    return API.request(`/login${qs}`);
  }

  // 登出接口。
  static logout() {
    return API.request("/login?action=logout");
  }

  // 获取当前用户资料。
  static getProfile() {
    return API.request("/user/profile");
  }

  // 更新资料（表单编码方式）。
  static updateProfile(payload) {
    return API.request("/user/profile", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody(payload)
    });
  }

  // 上传资料（multipart），用于头像/简历文件提交。
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

  // 获取指定 TA 资料（供 MO/Admin 查看）。
  static getTaProfile(userId) {
    return API.request(`/user/ta-profile?userId=${encodeURIComponent(userId)}`);
  }

  // 构建简历预览地址。
  static getResumePreviewUrl(userId) {
    return `${API.getBaseUrl()}/user/resume?userId=${encodeURIComponent(userId)}`;
  }

  // 打开简历预览：先鉴别响应是否为 PDF，再以内存 URL 方式打开。
  static async openResume(userId) {
    const url = API.getResumePreviewUrl(userId);
    const popup = window.open('about:blank', '_blank');
    try {
      const response = await fetch(url, { credentials: 'include' });
      const contentType = (response.headers.get('content-type') || '').toLowerCase();
      if (!response.ok || !contentType.includes('pdf')) {
        let message = `HTTP ${response.status}`;
        try {
          const text = await response.text();
          if (text) {
            const data = JSON.parse(text);
            message = data.message || text;
          }
        } catch (error) {
        }
        if (popup) popup.close();
        throw new Error(message || 'Resume not found');
      }

      const blob = await response.blob();
      const blobUrl = URL.createObjectURL(blob);
      if (popup) {
        popup.location.href = blobUrl;
        popup.addEventListener('beforeunload', () => URL.revokeObjectURL(blobUrl), { once: true });
      } else {
        window.location.href = blobUrl;
        setTimeout(() => URL.revokeObjectURL(blobUrl), 60000);
      }
      return true;
    } catch (error) {
      if (popup) popup.close();
      throw error;
    }
  }

  // 修改密码接口。
  static changePassword(oldPassword, newPassword) {
    return API.request("/user/password", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ oldPassword, newPassword })
    });
  }

  // 获取待审批注册列表（Admin）。
  static getPendingRegistrations() {
    return API.request("/user/pending-registrations");
  }

  // 审批注册请求（Admin）。
  static approveRegistration(userId, decision) {
    return API.request("/user/approve-registration", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ userId, decision })
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

  // 更新岗位信息。
  static updatePosition(payload) {
    return API.request("/position/update", {
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

  // 发布岗位（强制 open 并触发通知）。
  static publishPosition(positionId) {
    return API.request("/position/publish", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ positionId })
    });
  }

  // 获取审核列表（MO/Admin）。
  static getReviewApplications(positionId) {
    const qs = positionId ? `?positionId=${encodeURIComponent(positionId)}` : "";
    return API.request(`/application/review-list${qs}`);
  }

  // 获取我的申请（TA 或 Admin 指定用户）。
  static getMyApplications(userId) {
    const qs = userId ? `?userId=${encodeURIComponent(userId)}` : "";
    return API.request(`/application/my-list${qs}`);
  }

  // 审核申请。
  static reviewApplication(applicationId, decision, feedback = "") {
    return API.request("/application/review", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ applicationId, decision, feedback })
    });
  }

  // 提交岗位申请。
  static submitApplication(positionId, priority = "first") {
    return API.request("/application/submit", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ positionId, priority })
    });
  }

  // 获取管理员看板数据。
  static getAdminDashboard() {
    return API.request("/admin/dashboard");
  }
}
