// TA 招聘系统公共前端逻辑
class TARecruitmentSystem {
  constructor() {
    // 页面级公共初始化：事件绑定 + 顶栏用户标识。
    this.initEventListeners();
    this.initTopbarBranding();
  }

  initEventListeners() {
    // 弹窗关闭：点击关闭按钮或遮罩层关闭弹窗
    document.addEventListener('click', (e) => {
      if (e.target.classList.contains('modal-close')) {
        e.target.closest('.modal').classList.remove('show');
      }
      if (e.target.classList.contains('modal')) {
        e.target.classList.remove('show');
      }
    });

    // 标签页切换
    document.addEventListener('click', (e) => {
      if (e.target.classList.contains('tab-button')) {
        const tabName = e.target.getAttribute('data-tab');
        const container = e.target.closest('.tabs-container');
        
        if (container) {
          container.querySelectorAll('.tab-button').forEach(btn => btn.classList.remove('active'));
          container.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
          
          e.target.classList.add('active');
          container.querySelector(`.tab-content[data-tab="${tabName}"]`).classList.add('active');
        }
      }
    });

    // 左侧菜单路由
    this.initMenuNavigation();

    // 退出登录确认
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', async () => {
        if (confirm('ConfirmwantLogoutLogin?？')) {
          if (typeof API !== 'undefined' && API.logout) {
            try {
              await API.logout();
            } catch (error) {
              // 忽略退出接口异常，仍清理本地会话并跳转登录页。
            }
          }
          localStorage.clear();
          window.location.href = 'login.html';
        }
      });
    }
  }

  initTopbarBranding() {
    const avatarElements = document.querySelectorAll('.topbar-avatar');
    if (!avatarElements.length) {
      return;
    }

    // 从 localStorage 计算头像缩写（优先用户名首字母，兜底角色首字母）。
    const userName = (localStorage.getItem('userName') || '').trim();
    const userRole = (localStorage.getItem('userRole') || 'U').trim();
    const fallback = userRole ? userRole.charAt(0).toUpperCase() : 'U';
    const initials = userName
      ? userName
          .split(/\s+/)
          .filter(Boolean)
          .slice(0, 2)
          .map((part) => part.charAt(0).toUpperCase())
          .join('')
      : fallback;

    avatarElements.forEach((avatar) => {
      avatar.textContent = initials || 'U';
      avatar.setAttribute('aria-label', userName ? `${userName} avatar` : 'User avatar');
      avatar.setAttribute('title', userName || userRole || 'User');
    });
  }

  // 菜单导航映射
  initMenuNavigation() {
    // TA 角色菜单
    const taMenuMap = {
      'My Profile': 'ta-profile.html',
      'Browse Positions': 'ta-positions.html',
      'My Applications': 'ta-applications.html',
      'Profile': 'profile.html'
    };

    // MO 角色菜单
    const moMenuMap = {
      'Position Management': 'mo-positions.html',
      'Application Review': 'mo-review.html',
      'Notifications': 'mo-notifications.html',
      'Profile': 'profile.html'
    };

    // Admin 角色菜单
    const adminMenuMap = {
      'Admin Dashboard': 'admin-dashboard.html',
      'Registration Approvals': 'admin-approvals.html',
      'Account Status': 'admin-account-status.html',
      'Profile': 'profile.html'
    };

    // 根据当前角色选择菜单映射表。
    const userRole = localStorage.getItem('userRole') || 'TA';
    const menuMap = userRole === 'TA' ? taMenuMap : (userRole === 'MO' ? moMenuMap : adminMenuMap);

    if (userRole === 'Admin') {
      this.ensureAdminSidebarMenu(adminMenuMap);
    }

    // 侧边栏菜单点击事件
    document.addEventListener('click', (e) => {
      if (e.target.classList.contains('sidebar-menu-item')) {
        const menuText = e.target.textContent.trim();
        // 关键路由变量：由菜单文案映射到页面地址。
        const page = menuMap[menuText];
        
        if (page) {
          // 更新当前激活菜单项
          document.querySelectorAll('.sidebar-menu-item').forEach(item => {
            item.classList.remove('active');
          });
          e.target.classList.add('active');
          
          // 跳转到目标页面
          window.location.href = page;
        } else if (userRole === 'TA' && menuText === 'Notifications') {
          this.showMessage('This page is not available yet.', 'info');
        }
      }
    });

    // 初始化当前页面对应的激活菜单
    this.setActiveMenu(menuMap);
  }

  ensureAdminSidebarMenu(adminMenuMap) {
    const titleEl = document.querySelector('.sidebar-title');
    const menuEl = document.querySelector('.sidebar-menu');
    if (!titleEl || !menuEl) {
      return;
    }

    const titleText = (titleEl.textContent || '').toLowerCase();
    if (!titleText.includes('admin')) {
      return;
    }

    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    // 在 Admin 页面强制重建菜单，确保新增入口（如 Account Status）可见。
    menuEl.innerHTML = Object.entries(adminMenuMap).map(([label, page]) => {
      const activeClass = page === currentPage ? ' active' : '';
      return `<li class="sidebar-menu-item${activeClass}">${label}</li>`;
    }).join('');
  }

  // 根据当前 URL 设置激活菜单
  setActiveMenu(menuMap) {
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    const currentMenuItem = Object.entries(menuMap).find(([, page]) => page === currentPage)?.[0];
    
    if (currentMenuItem) {
      document.querySelectorAll('.sidebar-menu-item').forEach(item => {
        if (item.textContent.trim() === currentMenuItem) {
          item.classList.add('active');
        } else {
          item.classList.remove('active');
        }
      });
    }
  }

  // 打开弹窗
  openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.add('show');
    }
  }

  // 关闭弹窗
  closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.remove('show');
    }
  }

  // 表单必填校验
  validateForm(formId) {
    const form = document.getElementById(formId);
    if (!form) return false;

    let isValid = true;
    const inputs = form.querySelectorAll('input[required], textarea[required], select[required]');

    inputs.forEach(input => {
      const formGroup = input.closest('.form-group');
      const errorEl = formGroup ? formGroup.querySelector('.form-error') : null;
      
      if (!input.value.trim()) {
        if (errorEl) {
          errorEl.textContent = `Please fill in${input.placeholder || input.name}`;
          errorEl.classList.add('show');
        }
        isValid = false;
      } else {
        if (errorEl) {
          errorEl.classList.remove('show');
        }
      }
    });

    return isValid;
  }

  // 右上角消息提示
  showMessage(message, type = 'info') {
    const alert = document.createElement('div');
    alert.className = `alert alert-${type}`;
    alert.innerHTML = `<span>${message}</span>`;
    alert.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 2000; animation: slideIn 0.3s ease;';
    
    document.body.appendChild(alert);
    
    setTimeout(() => {
      alert.style.animation = 'slideOut 0.3s ease';
      setTimeout(() => alert.remove(), 300);
    }, 3000);
  }

  // 文件上传预览
  setupFilePreview(inputId, previewId) {
    const input = document.getElementById(inputId);
    if (!input) return;

    input.addEventListener('change', (e) => {
      const file = e.target.files[0];
      if (file) {
        const reader = new FileReader();
        reader.onload = (event) => {
          const preview = document.getElementById(previewId);
          if (preview) {
            preview.innerHTML = `<img src="${event.target.result}" style="max-width: 100%; border-radius: 6px;"><p>${file.name}</p>`;
          }
        };
        reader.readAsDataURL(file);
      }
    });
  }

  // 时间表选择初始化
  initTimeTable() {
    const cells = document.querySelectorAll('.time-cell');
    cells.forEach(cell => {
      cell.addEventListener('click', () => {
        cell.classList.toggle('selected');
      });
    });
  }

  // 表格排序初始化
  initTableSort() {
    const tableSortState = {};
    document.addEventListener('click', (e) => {
      if (e.target.classList.contains('sortable')) {
        const table = e.target.closest('table');
        if (!table) return;
        
        const tableId = table.id || table.className;
        const column = Array.from(e.target.parentNode.children).indexOf(e.target);
        const rows = Array.from(table.querySelectorAll('tbody tr'));
        
        // 同一列连续点击时切换升/降序。
        const isAsc = tableSortState[tableId] !== column;
        tableSortState[tableId] = column;
        
        rows.sort((a, b) => {
          const aVal = a.children[column].textContent.trim();
          const bVal = b.children[column].textContent.trim();
          
          // 优先按数字排序，无法转数字时按文本排序。
          const aNum = parseFloat(aVal);
          const bNum = parseFloat(bVal);
          
          let result;
          if (!isNaN(aNum) && !isNaN(bNum)) {
            result = aNum - bNum; // 数字排序
          } else {
            result = aVal.localeCompare(bVal, 'zh-CN'); // 文本排序
          }
          
          return isAsc ? result : -result;
        });

        rows.forEach(row => table.querySelector('tbody').appendChild(row));
      }
    });
  }

  // 页面权限检查（本地角色兜底）
  checkPageAccess(requiredRole) {
    const userRole = localStorage.getItem('userRole');
    const allowedRoles = Array.isArray(requiredRole) ? requiredRole : [requiredRole];
    
    if (!userRole || !allowedRoles.includes(userRole)) {
      alert('You do not have permission to access this page');
      window.location.href = 'login.html';
    }
  }

  normalizeRole(role) {
    const value = (role || '').toString().trim().toLowerCase();
    if (value === 'ta') return 'TA';
    if (value === 'mo') return 'MO';
    if (value === 'admin') return 'Admin';
    return '';
  }

  async enforceSessionGuard(requiredRoles = []) {
    // 统一规范角色命名，避免 TA/ta、Admin/admin 混用导致误判。
    const allowed = (Array.isArray(requiredRoles) ? requiredRoles : [requiredRoles])
      .map((r) => this.normalizeRole(r))
      .filter(Boolean);

    const redirectToLogin = () => {
      try {
        localStorage.clear();
      } catch (error) {
      }
      window.location.href = 'login.html';
    };

    if (typeof API === 'undefined' || !API.session) {
      // 后端 session 接口不可用时，退化为本地角色校验。
      const localRole = this.normalizeRole(localStorage.getItem('userRole') || '');
      const localUserId = (localStorage.getItem('userId') || '').trim();
      if (!localUserId || (allowed.length && !allowed.includes(localRole))) {
        redirectToLogin();
        return false;
      }
      return true;
    }

    try {
      // 以服务端会话为准，防止前端缓存过期导致越权。
      const session = await API.session();
      if (!session || !session.loggedIn || !session.user) {
        redirectToLogin();
        return false;
      }

      const role = this.normalizeRole(session.user.userRole || session.user.role || '');
      if (!role || (allowed.length && !allowed.includes(role))) {
        redirectToLogin();
        return false;
      }

      localStorage.setItem('userRole', role);
      localStorage.setItem('userId', session.user.userId || '');
      localStorage.setItem('userName', session.user.userName || '');
      return true;
    } catch (error) {
      redirectToLogin();
      return false;
    }
  }

  // 搜索过滤初始化（带防抖）
  initSearch(searchInputId, tableId) {
    const searchInput = document.getElementById(searchInputId);
    if (!searchInput) return;

    let debounceTimer;
    searchInput.addEventListener('input', (e) => {
      clearTimeout(debounceTimer);
      
      // 防抖：避免每次按键都立即遍历整表。
      debounceTimer = setTimeout(() => {
        const searchTerm = e.target.value.toLowerCase();
        const table = document.getElementById(tableId);
        if (!table) return;
        
        const rows = table.querySelectorAll('tbody tr');
        rows.forEach(row => {
          const text = row.textContent.toLowerCase();
          const isMatch = !searchTerm || text.includes(searchTerm);
          row.style.display = isMatch ? '' : 'none';
        });
      }, 300);
    });
  }
}

// 页面加载完成后挂载全局系统实例
document.addEventListener('DOMContentLoaded', () => {
  window.taSystem = new TARecruitmentSystem();
});

// 动画与提示框样式定义
const style = document.createElement('style');
style.innerHTML = `
  @keyframes slideIn {
    from {
      transform: translateX(400px);
      opacity: 0;
    }
    to {
      transform: translateX(0);
      opacity: 1;
    }
  }
  
  @keyframes slideOut {
    from {
      transform: translateX(0);
      opacity: 1;
    }
    to {
      transform: translateX(400px);
      opacity: 0;
    }
  }

  /* 提示框样式 */
  .alert {
    padding: 12px 16px;
    border-radius: 4px;
    font-size: 14px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    max-width: 300px;
    word-wrap: break-word;
  }

  .alert-info {
    background-color: #e0e7ff;
    color: #1e40af;
    border-left: 4px solid #1e40af;
  }

  .alert-success {
    background-color: #d1fae5;
    color: #059669;
    border-left: 4px solid #059669;
  }

  .alert-danger {
    background-color: #fee2e2;
    color: #dc2626;
    border-left: 4px solid #dc2626;
  }

  .alert-warning {
    background-color: #fef3c7;
    color: #d97706;
    border-left: 4px solid #d97706;
  }
`;
document.head.appendChild(style);



