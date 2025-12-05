import { openModal } from '../components/modals.js';
import { API_BASE_URL } from '../config/config.js';

// Define API endpoints
const ADMIN_API = API_BASE_URL + '/admin';
const DOCTOR_API = API_BASE_URL + '/doctor/login';

// Setup button event listeners
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

// Admin login handler
window.adminLoginHandler = async function() {
    try {
        // Get input values
        const username = document.getElementById('adminUsername').value;
        const password = document.getElementById('adminPassword').value;
        
        // Create admin object
        const admin = { username, password };
        
        // Send POST request
        const response = await fetch(ADMIN_API, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(admin)
        });
        
        if (response.ok) {
            const data = await response.json();
            
            // Store token in localStorage
            if (data.token) {
                localStorage.setItem('token', data.token);
                // Call helper function to save role
                selectRole('admin');
            } else {
                alert('Invalid response from server');
            }
        } else {
            alert('Invalid credentials!');
        }
    } catch (error) {
        console.error('Login error:', error);
        alert('An error occurred during login. Please try again.');
    }
};

// Doctor login handler
window.doctorLoginHandler = async function() {
    try {
        // Get input values
        const email = document.getElementById('doctorEmail').value;
        const password = document.getElementById('doctorPassword').value;
        
        // Create doctor object
        const doctor = { email, password };
        
        // Send POST request
        const response = await fetch(DOCTOR_API, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(doctor)
        });
        
        if (response.ok) {
            const data = await response.json();
            
            // Store token in localStorage
            if (data.token) {
                localStorage.setItem('token', data.token);
                // Call helper function to save role
                selectRole('doctor');
            } else {
                alert('Invalid response from server');
            }
        } else {
            alert('Invalid credentials!');
        }
    } catch (error) {
        console.error('Login error:', error);
        alert('An error occurred during login. Please try again.');
    }
};