import { openModal } from "../components/modals.js";
import { API_BASE_URL } from "../config/config.js";

const ADMIN_API = API_BASE_URL + '/admin/login';
const DOCTOR_API = API_BASE_URL + '/doctor/login';

// 添加角色选择函数
function selectRole(role) {
  switch(role) {
    case 'admin':
      window.location.href = '/adminDashboard/${encodeURIComponent(token)}';
      break;
    case 'doctor':
      window.location.href = '/doctorDashboard/${encodeURIComponent(token)}';
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

window.adminLoginHandler = async function () {
  const username = document.getElementById('admin-username')?.value;
  const password = document.getElementById('admin-password')?.value;

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
      localStorage.setItem('role', 'admin');
      // 直接跳转
      window.location.href = `/adminDashboard/${encodeURIComponent(result.token)}`;
    } else {
      const errorData = await response.json().catch(() => ({}));
      alert(errorData.error || 'Invalid credentials!');
    }
  } catch (error) {
    console.error('Admin login failed:', error);
    alert('Network error. Please try again later.');
  }
};

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
      localStorage.setItem('token', result.token);
      localStorage.setItem('role', 'doctor');
      // 直接跳转，不使用selectRole
      window.location.href = `/doctorDashboard/${encodeURIComponent(result.token)}`;
    } else {
      const errorData = await response.json().catch(() => ({}));
      alert(errorData.error || 'Invalid credentials!');
    }
  } catch (error) {
    console.error('Doctor login failed:', error);
    alert('Network error. Please try again later.');
  }
};