/*
  Step-by-Step Explanation of Header Section Rendering

  This code dynamically renders the header section of the page based on the user's role, session status, and available actions (such as login, logout, or role-switching).
*/

// Define the renderHeader function
function renderHeader() {
  // 2. Select the Header Div
  const headerDiv = document.getElementById("header");
  if (!headerDiv) return;

  // 3. Check if the Current Page is the Root Page
  if (window.location.pathname.endsWith("/")) {
    localStorage.removeItem("userRole");
    headerDiv.innerHTML = `
      <header class="header">
        <div class="logo-section">
          <img src="../assets/images/logo/logo.png" alt="Hospital CRM Logo" class="logo-img">
          <span class="logo-title">Hospital CMS</span>
        </div>
      </header>`;
    return;
  }

  // 4. Retrieve the User's Role and Token from LocalStorage
  const role = localStorage.getItem("userRole");
  const token = localStorage.getItem("token");

  // 6. Handle Session Expiry or Invalid Login
  if ((role === "loggedPatient" || role === "admin" || role === "doctor") && !token) {
    localStorage.removeItem("userRole");
    alert("Session expired or invalid login. Please log in again.");
    window.location.href = "/";
    return;
  }

  // 5. Initialize Header Content
  let headerContent = `<header class="header">
    <div class="logo-section">
      <img src="../assets/images/logo/logo.png" alt="Hospital CRM Logo" class="logo-img">
      <span class="logo-title">Hospital CMS</span>
    </div>
    <nav class="nav-section">`;

  // 7. Add Role-Specific Header Content
  if (role === "admin") {
    headerContent += `
      <button id="addDocBtn" class="nav-btn admin-btn" onclick="openModal('addDoctor')">Add Doctor</button>
      <button class="nav-btn logout-btn" onclick="logout()">Logout</button>`;
  } else if (role === "doctor") {
    headerContent += `
      <button class="nav-btn home-btn" onclick="window.location.href='/pages/doctorDashboard.html'">Home</button>
      <button class="nav-btn logout-btn" onclick="logout()">Logout</button>`;
  } else if (role === "patient") {
    headerContent += `
      <button id="patientLogin" class="nav-btn login-btn">Login</button>
      <button id="patientSignup" class="nav-btn signup-btn">Sign Up</button>`;
  } else if (role === "loggedPatient") {
    headerContent += `
      <button class="nav-btn home-btn" onclick="window.location.href='/pages/loggedPatientDashboard.html'">Home</button>
      <button class="nav-btn appointments-btn" onclick="window.location.href='/pages/patientAppointments.html'">Appointments</button>
      <button class="nav-btn logout-btn" onclick="logoutPatient()">Logout</button>`;
  } else {
    // Default header for non-logged-in users on other pages
    headerContent += `
      <button id="doctorLogin" class="nav-btn login-btn">Doctor Login</button>
      <button id="adminLogin" class="nav-btn login-btn">Admin Login</button>
      <button id="patientLogin" class="nav-btn login-btn">Patient Login</button>
      <button id="patientSignup" class="nav-btn signup-btn">Sign Up</button>`;
  }

  // 9. Close the Header Section
  headerContent += `</nav></header>`;

  // 10. Render the Header Content
  headerDiv.innerHTML = headerContent;

  // 11. Attach Event Listeners to Header Buttons
  attachHeaderButtonListeners();
}

// 12. Attach Event Listeners to Header Buttons
function attachHeaderButtonListeners() {
  // Doctor Login Button
  const doctorLoginBtn = document.getElementById("doctorLogin");
  if (doctorLoginBtn) {
    doctorLoginBtn.addEventListener("click", () => {
      openModal("doctorLogin");
    });
  }

  // Admin Login Button
  const adminLoginBtn = document.getElementById("adminLogin");
  if (adminLoginBtn) {
    adminLoginBtn.addEventListener("click", () => {
      openModal("adminLogin");
    });
  }

  // Patient Login Button
  const patientLoginBtn = document.getElementById("patientLogin");
  if (patientLoginBtn) {
    patientLoginBtn.addEventListener("click", () => {
      openModal("patientLogin");
    });
  }

  // Patient Signup Button
  const patientSignupBtn = document.getElementById("patientSignup");
  if (patientSignupBtn) {
    patientSignupBtn.addEventListener("click", () => {
      openModal("patientSignup");
    });
  }

  // Add Doctor Button (Admin only)
  const addDocBtn = document.getElementById("addDocBtn");
  if (addDocBtn) {
    addDocBtn.addEventListener("click", () => {
      openModal("addDoctor");
    });
  }
}

// 13. Logout Function for Admin/Doctor
function logout() {
  // Clear all session data
  localStorage.removeItem("token");
  localStorage.removeItem("userRole");
  localStorage.removeItem("userId");
  localStorage.removeItem("doctorId");
  
  // Redirect to home page
  window.location.href = "/";
}

// 14. Logout Function for Patient
function logoutPatient() {
  // Clear patient-specific session data
  localStorage.removeItem("token");
  localStorage.removeItem("userRole");
  localStorage.removeItem("patientId");
  
  // Redirect to patient dashboard or home
  window.location.href = "/pages/patientDashboard.html";
}

// 15. Role Selection Function
function selectRole(role) {
  localStorage.setItem("userRole", role);
  switch (role) {
    case "doctor":
      window.location.href = "/pages/doctorDashboard.html";
      break;
    case "admin":
      window.location.href = "/pages/adminDashboard.html";
      break;
    case "patient":
      window.location.href = "/pages/patientDashboard.html";
      break;
    default:
      window.location.href = "/";
  }
}

// 16. Modal Opening Helper Function
function openModal(modalType) {
  // This would typically call a modal rendering function
  // For now, we'll log the action
  console.log(`Opening modal: ${modalType}`);
  
  // In a real implementation, this would show the appropriate modal
  // Example: showModal(modalType);
  
  // For demonstration, we'll redirect to appropriate pages
  switch (modalType) {
    case "doctorLogin":
      window.location.href = "/pages/doctorLogin.html";
      break;
    case "adminLogin":
      window.location.href = "/pages/adminLogin.html";
      break;
    case "patientLogin":
      window.location.href = "/pages/patientLogin.html";
      break;
    case "patientSignup":
      window.location.href = "/pages/patientSignup.html";
      break;
    case "addDoctor":
      // This would typically open a modal
      console.log("Add Doctor modal should open");
      break;
  }
}

// 17. Initialize Header on Page Load
document.addEventListener("DOMContentLoaded", renderHeader);

// 18. Also export functions for module usage if needed
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    renderHeader,
    attachHeaderButtonListeners,
    logout,
    logoutPatient,
    selectRole,
    openModal
  };
}