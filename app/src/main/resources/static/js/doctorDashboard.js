import { getAllAppointments } from './services/appointmentRecordService.js';
import { createPatientRow } from './components/patientRows.js';

// Get the table body where patient rows will be added
const tableBody = document.getElementById('patientTableBody');

// Initialize selectedDate with today's date in 'YYYY-MM-DD' format
let selectedDate = new Date().toISOString().split('T')[0];

// Get the saved token from localStorage (used for authenticated API calls)
const token = localStorage.getItem('token') || '';

// Initialize patientName to null (used for filtering by name)
let patientName = null;

// Add an 'input' event listener to the search bar
document.getElementById('searchBar').addEventListener('input', function() {
    // Trim and check the input value
    const searchValue = this.value.trim();
    
    if (searchValue) {
        patientName = searchValue;
    } else {
        patientName = "null"; // As expected by backend for no filter
    }
    
    // Reload the appointments list with the updated filter
    loadAppointments();
});

// Add a click listener to the "Today" button
document.getElementById('todayButton').addEventListener('click', function() {
    // Set selectedDate to today's date
    const today = new Date().toISOString().split('T')[0];
    selectedDate = today;
    
    // Update the date picker UI to match
    const datePicker = document.getElementById('datePicker');
    if (datePicker) {
        datePicker.value = today;
    }
    
    // Reload the appointments for today
    loadAppointments();
});

// Add a change event listener to the date picker
document.getElementById('datePicker').addEventListener('change', function() {
    // Update selectedDate with the new value
    selectedDate = this.value;
    
    // Reload the appointments for that specific date
    loadAppointments();
});

/**
 * Function: loadAppointments
 * Purpose: Fetch and display appointments based on selected date and optional patient name
 */
async function loadAppointments() {
    try {
        // Step 1: Call getAllAppointments with selectedDate, patientName, and token
        const appointments = await getAllAppointments(selectedDate, patientName, token);
        
        // Step 2: Clear the table body content before rendering new rows
        tableBody.innerHTML = '';
        
        // Step 3: If no appointments are returned
        if (!appointments || appointments.length === 0) {
            const messageRow = document.createElement('tr');
            messageRow.innerHTML = `
                <td colspan="6" class="no-appointments-message">
                    No Appointments found for ${selectedDate}.
                </td>
            `;
            tableBody.appendChild(messageRow);
            return;
        }
        
        // Step 4: If appointments exist
        appointments.forEach(appointment => {
            // Construct a 'patient' object with id, name, phone, and email
            const patient = {
                id: appointment.id || appointment.appointmentId || `app-${Date.now()}`,
                name: appointment.patientName || appointment.name || 'N/A',
                phone: appointment.patientPhone || appointment.phone || 'N/A',
                email: appointment.patientEmail || appointment.email || 'N/A',
                appointmentTime: appointment.appointmentTime || appointment.time || 'N/A',
                status: appointment.status || 'Scheduled',
                // Include additional appointment details if needed
                appointmentDate: appointment.appointmentDate || selectedDate,
                doctorId: appointment.doctorId || 'N/A',
                reason: appointment.reason || 'Checkup'
            };
            
            // Call createPatientRow to generate a table row for the appointment
            const patientRow = createPatientRow(patient);
            
            // Append each row to the table body
            tableBody.appendChild(patientRow);
        });
        
    } catch (error) {
        console.error('Error loading appointments:', error);
        
        // Step 5: Catch and handle any errors during fetch
        tableBody.innerHTML = '';
        const errorRow = document.createElement('tr');
        errorRow.innerHTML = `
            <td colspan="6" class="error-message">
                Error loading appointments. Try again later.
            </td>
        `;
        tableBody.appendChild(errorRow);
    }
}

// When the page is fully loaded (DOMContentLoaded)
document.addEventListener('DOMContentLoaded', function() {
    // Initialize date picker with today's date
    const datePicker = document.getElementById('datePicker');
    if (datePicker) {
        datePicker.value = selectedDate;
        datePicker.max = new Date().toISOString().split('T')[0]; // Optional: restrict to today or past dates
    }
    
    // Call renderContent() (assumes it sets up the UI layout)
    if (typeof renderContent === 'function') {
        renderContent();
    }
    
    // Call loadAppointments() to display today's appointments by default
    loadAppointments();
});

// Make loadAppointments available globally if needed
window.loadAppointments = loadAppointments;