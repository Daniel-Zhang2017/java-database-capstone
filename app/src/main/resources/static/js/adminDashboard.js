import { openModal } from '../components/modals.js';
import { getDoctors, filterDoctors, saveDoctor } from './services/doctorServices.js';
import { createDoctorCard } from '../components/doctorCard.js';

// Attach a click listener to the "Add Doctor" button
document.getElementById('addDocBtn').addEventListener('click', () => {
    openModal('addDoctor');
});

// When the DOM is fully loaded, load all doctor cards
document.addEventListener('DOMContentLoaded', () => {
    loadDoctorCards();
    
    // Attach event listeners for search and filter
    document.getElementById("searchBar").addEventListener("input", filterDoctorsOnChange);
    document.getElementById("filterTime").addEventListener("change", filterDoctorsOnChange);
    document.getElementById("filterSpecialty").addEventListener("change", filterDoctorsOnChange);
});

/**
 * Function: loadDoctorCards
 * Purpose: Fetch all doctors and display them as cards
 */
async function loadDoctorCards() {
    try {
        const doctors = await getDoctors();
        renderDoctorCards(doctors);
    } catch (error) {
        console.error('Error loading doctor cards:', error);
        // Optionally show an error message to the user
        const contentDiv = document.getElementById("content");
        contentDiv.innerHTML = `<p class="error-message">Failed to load doctors. Please try again later.</p>`;
    }
}

/**
 * Function: filterDoctorsOnChange
 * Purpose: Filter doctors based on name, available time, and specialty
 */
async function filterDoctorsOnChange() {
    try {
        // Read values from search bar and filters
        const name = document.getElementById("searchBar").value.trim();
        const time = document.getElementById("filterTime").value;
        const specialty = document.getElementById("filterSpecialty").value;
        
        // Normalize empty values to null or empty string
        const nameParam = name === '' ? null : name;
        const timeParam = time === '' ? null : time;
        const specialtyParam = specialty === '' ? null : specialty;
        
        // Call filterDoctors service function
        const result = await filterDoctors(nameParam, timeParam, specialtyParam);
        
        if (result.doctors && result.doctors.length > 0) {
            renderDoctorCards(result.doctors);
        } else {
            // Show "No doctors found" message
            const contentDiv = document.getElementById("content");
            contentDiv.innerHTML = `<p class="no-doctors-message">No doctors found with the given filters.</p>`;
        }
    } catch (error) {
        console.error('Error filtering doctors:', error);
        alert('Failed to filter doctors. Please try again.');
    }
}

/**
 * Function: renderDoctorCards
 * Purpose: A helper function to render a list of doctors passed to it
 * @param {Array} doctors - Array of doctor objects
 */
function renderDoctorCards(doctors) {
    const contentDiv = document.getElementById("content");
    
    // Clear current content
    contentDiv.innerHTML = "";
    
    // If no doctors, show message
    if (!doctors || doctors.length === 0) {
        contentDiv.innerHTML = `<p class="no-doctors-message">No doctors available.</p>`;
        return;
    }
    
    // Create and append doctor cards
    doctors.forEach(doctor => {
        const doctorCard = createDoctorCard(doctor);
        contentDiv.appendChild(doctorCard);
    });
}

/**
 * Function: adminAddDoctor
 * Purpose: Collect form data and add a new doctor to the system
 */
window.adminAddDoctor = async function() {
    try {
        // Collect input values from the modal form
        const name = document.getElementById("doctorName").value.trim();
        const email = document.getElementById("doctorEmail").value.trim();
        const phone = document.getElementById("doctorPhone").value.trim();
        const password = document.getElementById("doctorPassword").value;
        const specialty = document.getElementById("doctorSpecialty").value.trim();
        const availability = document.getElementById("doctorAvailability").value.trim();
        
        // Collect checkbox values for availability days (if present)
        const availableDays = [];
        const dayCheckboxes = document.querySelectorAll('input[name="availableDays"]:checked');
        dayCheckboxes.forEach(checkbox => {
            availableDays.push(checkbox.value);
        });
        
        // Validate required fields
        if (!name || !email || !phone || !password || !specialty || !availability) {
            alert('Please fill in all required fields.');
            return;
        }
        
        // Validate email format
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            alert('Please enter a valid email address.');
            return;
        }
        
        // Validate phone number (basic validation)
        const phoneRegex = /^[0-9+\-\s()]{10,}$/;
        if (!phoneRegex.test(phone)) {
            alert('Please enter a valid phone number.');
            return;
        }
        
        // Retrieve the authentication token from localStorage
        const token = localStorage.getItem('token');
        
        if (!token) {
            alert('Authentication required. Please login again.');
            return;
        }
        
        // Build a doctor object with the form values
        const doctor = {
            name,
            email,
            phone,
            password, // Note: In production, password should be hashed on the client or server-side
            specialty,
            availability,
            availableDays: availableDays.length > 0 ? availableDays : undefined
        };
        
        // Call saveDoctor service function
        const result = await saveDoctor(doctor, token);
        
        if (result.success) {
            // Show success message
            alert(result.message || 'Doctor added successfully!');
            
            // Close the modal (assuming closeModal function exists)
            const closeModalBtn = document.querySelector('.modal .close-button') || 
                                document.querySelector('[data-modal-close="addDoctor"]');
            if (closeModalBtn) {
                closeModalBtn.click();
            }
            
            // Clear form fields
            document.getElementById("doctorName").value = '';
            document.getElementById("doctorEmail").value = '';
            document.getElementById("doctorPhone").value = '';
            document.getElementById("doctorPassword").value = '';
            document.getElementById("doctorSpecialty").value = '';
            document.getElementById("doctorAvailability").value = '';
            
            // Uncheck all day checkboxes
            document.querySelectorAll('input[name="availableDays"]').forEach(checkbox => {
                checkbox.checked = false;
            });
            
            // Reload the doctor list
            loadDoctorCards();
        } else {
            alert(result.message || 'Failed to add doctor. Please try again.');
        }
    } catch (error) {
        console.error('Error adding doctor:', error);
        alert('An unexpected error occurred. Please try again.');
    }
};

// Make functions available globally if needed
window.loadDoctorCards = loadDoctorCards;
window.filterDoctorsOnChange = filterDoctorsOnChange;
window.renderDoctorCards = renderDoctorCards;