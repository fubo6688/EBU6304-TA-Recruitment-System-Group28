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

  static register(payload) {
    return API.request("/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ ...payload, action: "register" })
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

  static changePassword(oldPassword, newPassword) {
    return API.request("/user/password", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ oldPassword, newPassword })
    });
  }

  static getPendingRegistrations() {
    return API.request("/user/pending-registrations");
  }

  static approveRegistration(userId, decision) {
    return API.request("/user/approve-registration", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ userId, decision })
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

  static updatePosition(payload) {
    return API.request("/position/update", {
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

  static publishPosition(positionId) {
    return API.request("/position/publish", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ positionId })
    });
  }

  static getReviewApplications(positionId) {
    const qs = positionId ? `?positionId=${encodeURIComponent(positionId)}` : "";
    return API.request(`/application/review-list${qs}`);
  }

  static getMyApplications(userId) {
    const qs = userId ? `?userId=${encodeURIComponent(userId)}` : "";
    return API.request(`/application/my-list${qs}`);
  }

  static reviewApplication(applicationId, decision, feedback = "") {
    return API.request("/application/review", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ applicationId, decision, feedback })
    });
  }

  static submitApplication(positionId, priority = "first") {
    return API.request("/application/submit", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: API.formBody({ positionId, priority })
    });
  }

  static getAdminDashboard() {
    return API.request("/admin/dashboard");
  }
}
