import { API_BASE_URL } from "../config/config.js";

// Define a constant for the doctor-related base endpoint
const DOCTOR_API = API_BASE_URL + '/doctor';

/**
 * Function: getDoctors
 * Purpose: Fetch the list of all doctors from the API
 * Returns: Array of doctors or empty array if error occurs
 */
export async function getDoctors() {
    try {
        const response = await fetch(DOCTOR_API);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        // Assuming the response contains a 'doctors' array
        return data.doctors || [];
    } catch (error) {
        console.error('Error fetching doctors:', error);
        return []; // Return empty array to avoid breaking frontend
    }
}

/**
 * Function: deleteDoctor
 * Purpose: Delete a specific doctor using their ID and an authentication token
 * Parameters:
 *   id - Doctor's unique identifier
 *   token - Authentication token for admin authorization
 * Returns: Object with success status and message
 */
export async function deleteDoctor(id, token) {
    try {
        // Construct the full endpoint URL with ID and token
        const deleteUrl = `${DOCTOR_API}/${id}/${token}`;
        
        const response = await fetch(deleteUrl, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        
        return {
            success: response.ok,
            message: result.message || (response.ok ? 'Doctor deleted successfully' : 'Failed to delete doctor')
        };
    } catch (error) {
        console.error('Error deleting doctor:', error);
        return {
            success: false,
            message: 'Network error occurred while deleting doctor'
        };
    }
}

/**
 * Function: saveDoctor
 * Purpose: Save (create) a new doctor using a POST request
 * Parameters:
 *   doctor - Object containing doctor details (name, email, availability, specialty, etc.)
 *   token - Authentication token for admin authorization
 * Returns: Object with success status and message
 */
export async function saveDoctor(doctor, token) {
    try {
        // Construct the endpoint URL with token
        const saveUrl = `${DOCTOR_API}/${token}`;
        
        const response = await fetch(saveUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(doctor)
        });
        
        const result = await response.json();
        
        return {
            success: response.ok,
            message: result.message || (response.ok ? 'Doctor saved successfully' : 'Failed to save doctor')
        };
    } catch (error) {
        console.error('Error saving doctor:', error);
        return {
            success: false,
            message: 'Network error occurred while saving doctor'
        };
    }
}

/**
 * Function: filterDoctors
 * Purpose: Fetch doctors based on filtering criteria (name, time, and specialty)
 * Parameters:
 *   name - Doctor's name to filter by (can be null or empty string)
 *   time - Availability time to filter by (can be null or empty string)
 *   specialty - Medical specialty to filter by (can be null or empty string)
 * Returns: Object with doctors array (filtered results)
 */
export async function filterDoctors(name, time, specialty) {
    try {
        // Construct the filter URL with parameters
        // Note: Adjust the URL structure based on your backend API design
        let filterUrl = `${DOCTOR_API}/filter`;
        
        // Add parameters as needed - this depends on your backend API design
        // Here's an example approach using query parameters:
        const params = new URLSearchParams();
        
        if (name && name.trim() !== '') {
            params.append('name', name);
        }
        
        if (time && time.trim() !== '') {
            params.append('time', time);
        }
        
        if (specialty && specialty.trim() !== '') {
            params.append('specialty', specialty);
        }
        
        // If we have any parameters, append them to the URL
        const queryString = params.toString();
        if (queryString) {
            filterUrl += `?${queryString}`;
        }
        
        const response = await fetch(filterUrl);
        
        if (response.ok) {
            const data = await response.json();
            // Assuming the response contains a 'doctors' array
            return {
                doctors: data.doctors || []
            };
        } else {
            console.error('Error filtering doctors. Status:', response.status);
            return {
                doctors: []
            };
        }
    } catch (error) {
        console.error('Error filtering doctors:', error);
        alert('Failed to filter doctors. Please try again.');
        return {
            doctors: []
        };
    }
}