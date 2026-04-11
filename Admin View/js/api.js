class API {
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

  static formBody(params) {
    const form = new URLSearchParams();
    Object.keys(params).forEach((key) => {
      if (params[key] !== undefined && params[key] !== null) {
        form.append(key, params[key]);
      }
    });
    return form;
  }

  static login(userId, password, role) {
    return API.request("/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ userId, password, role })
    });
  }

  static session() {
    return API.request("/login");
  }

  static logout() {
    return API.request("/login?action=logout");
  }

  static getProfile() {
    return API.request("/user/profile");
  }

  static updateProfile(payload) {
    return API.request("/user/profile", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody(payload)
    });
  }

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

  static getTaProfile(userId) {
    return API.request(`/user/ta-profile?userId=${encodeURIComponent(userId)}`);
  }

  static getResumePreviewUrl(userId) {
    return `${API.getBaseUrl()}/user/resume?userId=${encodeURIComponent(userId)}`;
  }

  static changePassword(oldPassword, newPassword) {
    return API.request("/user/password", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ oldPassword, newPassword })
    });
  }

  static getPositions() {
    return API.request("/position/list");
  }

  static createPosition(payload) {
    return API.request("/position/create", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody(payload)
    });
  }

  static updatePositionStatus(positionId, status) {
    return API.request("/position/status", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ positionId, status })
    });
  }

  static getApplications() {
    return API.request("/application/list");
  }

  static getReviewApplications(positionId) {
    const qs = positionId ? `?positionId=${encodeURIComponent(positionId)}` : "";
    return API.request(`/application/review-list${qs}`);
  }

  static submitApplication(positionId, priority = "first") {
    return API.request("/application/submit", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ positionId, priority })
    });
  }

  static cancelApplication(applicationId) {
    return API.request("/application/cancel", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ applicationId })
    });
  }

  static processApplication(applicationId, decision, feedback = "") {
    return API.request("/application/process", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ applicationId, decision, feedback })
    });
  }

  static updateApplicationPriority(applicationId, priority) {
    return API.request("/application/priority", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ applicationId, priority })
    });
  }

  static getNotifications() {
    return API.request("/notification/list");
  }

  static markNotificationRead(notificationId) {
    return API.request("/notification/read", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ notificationId })
    });
  }

  static markAllNotificationsRead() {
    return API.request("/notification/read-all", {
      method: "POST"
    });
  }

  static createNotification(payload) {
    return API.request("/notification/create", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody(payload)
    });
  }

  static adminAnalytics() {
    return API.request("/admin/analytics");
  }

  static adminUsers() {
    return API.request("/admin/users");
  }

  static adminLogs() {
    return API.request("/admin/logs");
  }

  static adminUpdateUserStatus(userId, status) {
    return API.request("/admin/user-status", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ userId, status })
    });
  }

  static adminExportPreview(params = {}) {
    const q = new URLSearchParams();
    Object.keys(params || {}).forEach((key) => {
      const value = params[key];
      if (value !== undefined && value !== null && String(value).trim() !== "") {
        q.append(key, String(value));
      }
    });
    return API.request(`/admin/export?${q.toString()}`);
  }

  static async adminExportCsv(params = {}) {
    const base = API.getBaseUrl();
    const q = new URLSearchParams();
    Object.keys(params || {}).forEach((key) => {
      const value = params[key];
      if (value !== undefined && value !== null && String(value).trim() !== "") {
        q.append(key, String(value));
      }
    });
    q.set("format", "csv");

    const response = await fetch(`${base}/admin/export?${q.toString()}`, {
      method: "GET",
      credentials: "include"
    });

    if (!response.ok) {
      let message = `HTTP ${response.status}`;
      try {
        const text = await response.text();
        const data = text ? JSON.parse(text) : {};
        message = data.message || message;
      } catch (e) {
      }
      throw new Error(message);
    }

    const blob = await response.blob();
    const disposition = response.headers.get("Content-Disposition") || "";
    const match = disposition.match(/filename=\"?([^\";]+)\"?/i);
    const fileName = match && match[1] ? match[1] : `ta_export_${Date.now()}.csv`;
    return { blob, fileName };
  }
}
