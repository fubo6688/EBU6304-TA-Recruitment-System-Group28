// TA Recruitment System - Common Functions
class TARecruitmentSystem {
  // 构造函数：初始化全局交互事件。
  constructor() {
    this.initEventListeners();
  }

  // 绑定全局交互事件：弹窗、标签切换、菜单和退出登录。
  initEventListeners() {
    // Modal box closesbutton
    document.addEventListener('click', (e) => {
      if (e.target.classList.contains('modal-close')) {
        e.target.closest('.modal').classList.remove('show');
      }
      if (e.target.classList.contains('modal')) {
        e.target.classList.remove('show');
      }
    });

    // Tab switching
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

    // Menu navigation
    this.initMenuNavigation();

    // Logoutconfirm
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', async () => {
        if (confirm('ConfirmwantLogoutLogin?？')) {
          if (typeof API !== 'undefined' && API.logout) {
            try {
              await API.logout();
            } catch (error) {
              // Ignore logout API errors and still clear local state.
            }
          }
          localStorage.clear();
          window.location.href = 'login.html';
        }
      });
    }
  }

  // 初始化侧边栏菜单映射与点击跳转。
  initMenuNavigation() {
    // TA RoleMenu mapping
    const taMenuMap = {
      'My Profile': 'ta-profile.html',
      'Browse Positions': 'ta-positions.html',
      'My Applications': 'ta-applications.html',
      'Profile': 'profile.html',
      'Notifications': 'notification.html'
    };

    // MO RoleMenu mapping
    const moMenuMap = {
      'Position Management': 'mo-positions.html',
      'Application Review': 'mo-review.html',
      'Profile': 'profile.html',
      'Notifications': 'notification.html'
    };

    // AdminMenu mapping
    const adminMenuMap = {
      'Analytics': 'admin-analytics.html',
      'User Management': 'admin-users.html',
      'System Logs': 'admin-logs.html',
      'Profile': 'profile.html',
      'Notifications': 'notification.html'
    };

    // Get userRole
    const userRole = localStorage.getItem('userRole') || 'TA';
    const menuMap = userRole === 'TA' ? taMenuMap : (userRole === 'MO' ? moMenuMap : adminMenuMap);

    // Add menu item click event
    document.addEventListener('click', (e) => {
      if (e.target.classList.contains('sidebar-menu-item')) {
        const menuText = e.target.textContent.trim();
        const page = menuMap[menuText];
        
        if (page) {
          // Update active menu item
          document.querySelectorAll('.sidebar-menu-item').forEach(item => {
            item.classList.remove('active');
          });
          e.target.classList.add('active');
          
          // Navigate to page
          window.location.href = page;
        }
      }
    });

    // Set the active menu of the current page
    this.setActiveMenu(menuMap);
  }

  // 根据当前页面 URL 高亮菜单项。
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

  // 打开指定弹窗。
  openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.add('show');
    }
  }

  // 关闭指定弹窗。
  closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.remove('show');
    }
  }

  // 校验表单必填字段并提示错误信息。
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

  // 显示浮动提示消息。
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

  // 绑定文件预览（图片 + 文件名）。
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

  // 初始化时间表可选单元格。
  initTimeTable() {
    const cells = document.querySelectorAll('.time-cell');
    cells.forEach(cell => {
      cell.addEventListener('click', () => {
        cell.classList.toggle('selected');
      });
    });
  }

  // 初始化表格排序功能，支持数字和字符串。
  initTableSort() {
    const tableSortState = {};
    document.addEventListener('click', (e) => {
      if (e.target.classList.contains('sortable')) {
        const table = e.target.closest('table');
        if (!table) return;
        
        const tableId = table.id || table.className;
        const column = Array.from(e.target.parentNode.children).indexOf(e.target);
        const rows = Array.from(table.querySelectorAll('tbody tr'));
        
        // Switch sort direction
        const isAsc = tableSortState[tableId] !== column;
        tableSortState[tableId] = column;
        
        rows.sort((a, b) => {
          const aVal = a.children[column].textContent.trim();
          const bVal = b.children[column].textContent.trim();
          
          // Try numerical sorting
          const aNum = parseFloat(aVal);
          const bNum = parseFloat(bVal);
          
          let result;
          if (!isNaN(aNum) && !isNaN(bNum)) {
            result = aNum - bNum; // Number sorting
          } else {
            result = aVal.localeCompare(bVal, 'zh-CN'); // String sorting
          }
          
          return isAsc ? result : -result;
        });

        rows.forEach(row => table.querySelector('tbody').appendChild(row));
      }
    });
  }

  // 本地权限校验，不通过跳转登录页。
  checkPageAccess(requiredRole) {
    const userRole = localStorage.getItem('userRole');
    const allowedRoles = Array.isArray(requiredRole) ? requiredRole : [requiredRole];
    
    if (!userRole || !allowedRoles.includes(userRole)) {
      alert('You do not have permission to access this page');
      window.location.href = 'login.html';
    }
  }

  // 初始化搜索过滤（含防抖）。
  initSearch(searchInputId, tableId) {
    const searchInput = document.getElementById(searchInputId);
    if (!searchInput) return;

    let debounceTimer;
    searchInput.addEventListener('input', (e) => {
      clearTimeout(debounceTimer);
      
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

// Initialized after the page is loaded
document.addEventListener('DOMContentLoaded', () => {
  window.taSystem = new TARecruitmentSystem();
});

// animation andTipBox style definition
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

  /* Tipbox style */
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



