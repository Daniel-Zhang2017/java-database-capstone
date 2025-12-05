// doctorCard.js

/**
 * Create a doctor card element with role-specific actions
 * @param {Object} doctor - Doctor object with details
 * @param {string} doctor.id - Doctor ID
 * @param {string} doctor.name - Doctor name
 * @param {string} doctor.specialization - Doctor specialization
 * @param {string} doctor.email - Doctor email
 * @param {string[]} doctor.availableTimes - Array of available times
 * @returns {HTMLElement} Doctor card element
 */
export function createDoctorCard(doctor) {
    // Create the main card container
    const card = document.createElement("div");
    card.classList.add("doctor-card");
    
    // Fetch the user's role
    const role = localStorage.getItem("userRole");
    
    // Create doctor info section
    const infoDiv = document.createElement("div");
    infoDiv.classList.add("doctor-info");
    
    // Create name element
    const name = document.createElement("h3");
    name.textContent = doctor.name;
    name.classList.add("doctor-name");
    
    // Create specialization element
    const specialization = document.createElement("p");
    specialization.textContent = doctor.specialization || "General Practitioner";
    specialization.classList.add("doctor-specialty");
    
    // Create email element
    const email = document.createElement("p");
    email.textContent = doctor.email;
    email.classList.add("doctor-email");
    
    // Create availability element
    const availability = document.createElement("p");
    availability.textContent = `Available: ${doctor.availableTimes ? doctor.availableTimes.join(", ") : "No times available"}`;
    availability.classList.add("doctor-availability");
    
    // Append all info elements
    infoDiv.appendChild(name);
    infoDiv.appendChild(specialization);
    infoDiv.appendChild(email);
    infoDiv.appendChild(availability);
    
    // Create button container
    const actionsDiv = document.createElement("div");
    actionsDiv.classList.add("card-actions");
    
    // Conditionally add buttons based on role
    if (role === "admin") {
        // Admin: Delete button
        const removeBtn = document.createElement("button");
        removeBtn.textContent = "Delete";
        removeBtn.classList.add("btn", "btn-delete");
        
        removeBtn.addEventListener("click", async () => {
            // 1. Confirm deletion
            const confirmDelete = confirm(`Are you sure you want to delete ${doctor.name}?`);
            if (!confirmDelete) return;
            
            // 2. Get token from localStorage
            const token = localStorage.getItem("token");
            if (!token) {
                alert("Session expired. Please log in again.");
                localStorage.removeItem("userRole");
                window.location.href = "/pages/adminLogin.html";
                return;
            }
            
            try {
                // 3. Call API to delete
                // Note: This assumes deleteDoctor is imported or available globally
                const result = await deleteDoctor(doctor.id, token);
                
                if (result.success) {
                    // 4. On success: remove the card from the DOM
                    card.style.opacity = "0";
                    card.style.transform = "translateY(-10px)";
                    
                    setTimeout(() => {
                        card.remove();
                    }, 300);
                    
                    // Optional: Show success message
                    alert(`${doctor.name} has been deleted successfully.`);
                } else {
                    alert(`Failed to delete doctor: ${result.message || "Unknown error"}`);
                }
            } catch (error) {
                console.error("Error deleting doctor:", error);
                alert("An error occurred while deleting the doctor.");
            }
        });
        
        actionsDiv.appendChild(removeBtn);
    } else if (role === "patient") {
        // Patient (not logged in): Book Now button with login prompt
        const bookNow = document.createElement("button");
        bookNow.textContent = "Book Now";
        bookNow.classList.add("btn", "btn-book");
        
        bookNow.addEventListener("click", () => {
            alert("Please login to book an appointment.");
            window.location.href = "/pages/patientLogin.html";
        });
        
        actionsDiv.appendChild(bookNow);
    } else if (role === "loggedPatient") {
        // Logged-in Patient: Book Now button with overlay
        const bookNow = document.createElement("button");
        bookNow.textContent = "Book Now";
        bookNow.classList.add("btn", "btn-book");
        
        bookNow.addEventListener("click", async (e) => {
            // Get patient token
            const token = localStorage.getItem("token");
            if (!token) {
                alert("Session expired. Please log in again.");
                localStorage.removeItem("userRole");
                window.location.href = "/pages/patientLogin.html";
                return;
            }
            
            try {
                // Fetch patient data
                // Note: This assumes getPatientData is imported or available globally
                const patientData = await getPatientData(token);
                
                if (!patientData) {
                    alert("Unable to fetch your patient information.");
                    return;
                }
                
                // Show booking overlay
                // Note: This assumes showBookingOverlay is imported or available globally
                showBookingOverlay(e, doctor, patientData);
            } catch (error) {
                console.error("Error fetching patient data:", error);
                alert("An error occurred while trying to book an appointment.");
            }
        });
        
        actionsDiv.appendChild(bookNow);
    } else {
        // Default: No actions for other roles
        const noAction = document.createElement("p");
        noAction.textContent = "No actions available";
        noAction.classList.add("no-action");
        actionsDiv.appendChild(noAction);
    }
    
    // Final assembly
    card.appendChild(infoDiv);
    card.appendChild(actionsDiv);
    
    return card;
}

// Import statements would typically be at the top of the file
// These are placeholder references - replace with actual imports
/**
 * Placeholder for deleteDoctor function
 * @param {string} doctorId 
 * @param {string} token 
 * @returns {Promise<Object>}
 */
async function deleteDoctor(doctorId, token) {
    // In actual implementation, import from doctorServices.js
    // import { deleteDoctor } from './doctorServices.js';
    console.log(`Deleting doctor ${doctorId}`);
    
    // Simulate API call
    return new Promise(resolve => {
        setTimeout(() => {
            resolve({ success: true, message: "Doctor deleted" });
        }, 500);
    });
}

/**
 * Placeholder for getPatientData function
 * @param {string} token 
 * @returns {Promise<Object>}
 */
async function getPatientData(token) {
    // In actual implementation, import from patientServices.js
    // import { getPatientData } from './patientServices.js';
    console.log("Fetching patient data");
    
    // Simulate API call
    return new Promise(resolve => {
        setTimeout(() => {
            resolve({
                id: "patient-123",
                name: "John Patient",
                email: "patient@example.com",
                phone: "123-456-7890"
            });
        }, 500);
    });
}

/**
 * Placeholder for showBookingOverlay function
 * @param {Event} event 
 * @param {Object} doctor 
 * @param {Object} patientData 
 */
function showBookingOverlay(event, doctor, patientData) {
    // In actual implementation, import from loggedPatient.js
    // import { showBookingOverlay } from './loggedPatient.js';
    console.log("Showing booking overlay", { doctor, patientData });
    
    // Create a simple overlay for demonstration
    const overlay = document.createElement("div");
    overlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 1000;
    `;
    
    const content = document.createElement("div");
    content.style.cssText = `
        background: white;
        padding: 30px;
        border-radius: 10px;
        max-width: 500px;
        width: 90%;
    `;
    
    content.innerHTML = `
        <h3>Book Appointment</h3>
        <p><strong>Doctor:</strong> ${doctor.name}</p>
        <p><strong>Specialty:</strong> ${doctor.specialization}</p>
        <p><strong>Patient:</strong> ${patientData.name}</p>
        <p><strong>Available Times:</strong> ${doctor.availableTimes ? doctor.availableTimes.join(", ") : "None"}</p>
        <button id="closeOverlay">Close</button>
    `;
    
    overlay.appendChild(content);
    document.body.appendChild(overlay);
    
    // Close overlay button
    content.querySelector("#closeOverlay").addEventListener("click", () => {
        overlay.remove();
    });
    
    // Close overlay when clicking outside
    overlay.addEventListener("click", (e) => {
        if (e.target === overlay) {
            overlay.remove();
        }
    });
}