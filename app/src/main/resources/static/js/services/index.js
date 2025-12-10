import { openModal } from "../components/modals.js";
import { API_BASE_URL } from "../config/config.js";

const ADMIN_API = API_BASE_URL + '/admin/login';
const DOCTOR_API = API_BASE_URL + '/doctor/login';

// 添加角色选择函数
function selectRole(role) {
  switch(role) {
    case 'admin':
      window.location.href = '/admin/dashboard.html';
      break;
    case 'doctor':
      window.location.href = '/doctor/dashboard.html';
      break;
    default:
      console.error('Unknown role:', role);
      alert('Login successful but role not recognized!');
  }
}

window.onload = function () {
  const adminBtn = document.getElementById('adminLogin');
  const doctorBtn = document.getElementById('doctorLogin');

  if (adminBtn) {
    adminBtn.addEventListener('click', () => {
      openModal('adminLogin');
    });
  }

  if (doctorBtn) {
    doctorBtn.addEventListener('click', () => {
      openModal('doctorLogin');
    });
  }
};

// 管理员登录 - 使用不同ID
window.adminLoginHandler = async function () {
  // 方案A：使用不同ID
  const username = document.getElementById('admin-username')?.value;
  const password = document.getElementById('admin-password')?.value;

  // 或者方案B：在模态框内查找
  // const modal = document.querySelector('#adminLoginModal');
  // const username = modal?.querySelector('.username-input')?.value;
  // const password = modal?.querySelector('.password-input')?.value;

  if (!username || !password) {
    alert("Please enter both username and password.");
    return;
  }

  const admin = { username, password };

  try {
    const response = await fetch(ADMIN_API, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(admin)
    });

    if (response.ok) {
      const result = await response.json();
      localStorage.setItem('token', result.token);
      localStorage.setItem('role', 'admin'); // 保存角色信息
      selectRole('admin');
    } else {
      const errorData = await response.json().catch(() => ({}));
      alert(errorData.message || 'Invalid credentials!');
    }
  } catch (error) {
    console.error('Admin login failed:', error);
    alert('Network error. Please try again later. {}');
  }
};

// 医生登录 - 使用不同ID
window.doctorLoginHandler = async function () {
  const email = document.getElementById('doctor-email')?.value;
  const password = document.getElementById('doctor-password')?.value;

  if (!email || !password) {
    alert("Please enter both email and password.");
    return;
  }

  const doctor = { email, password };

  try {
    const response = await fetch(DOCTOR_API, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(doctor)
    });

    if (response.ok) {
      const result = await response.json();
      console.log('Doctor login result:', result);
      localStorage.setItem('token', result.token);
      localStorage.setItem('role', 'doctor'); // 保存角色信息
      selectRole('doctor');
    } else {
      const errorData = await response.json().catch(() => ({}));
      alert(errorData.message || 'Invalid credentials!');
    }
  } catch (error) {
    console.error('Doctor login failed:', error);
    alert('Network error. Please try again later.');
  }
};