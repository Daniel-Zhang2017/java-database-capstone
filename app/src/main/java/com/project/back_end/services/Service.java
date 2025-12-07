package com.project.back_end.services;

import com.project.back_end.models.*;
import com.project.back_end.repositories.*;
import com.project.back_end.services.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class Service {
    
    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    @Autowired
    public Service(TokenService tokenService,
                   AdminRepository adminRepository,
                   DoctorRepository doctorRepository,
                   PatientRepository patientRepository,
                   DoctorService doctorService,
                   PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    // 1. **@Service Annotation**
    // The @Service annotation marks this class as a service component in Spring. This allows Spring to automatically detect it through component scanning
    // and manage its lifecycle, enabling it to be injected into controllers or other services using @Autowired or constructor injection.

    // 2. **Constructor Injection for Dependencies**
    // The constructor injects all required dependencies (TokenService, Repositories, and other Services). This approach promotes loose coupling, improves testability,
    // and ensures that all required dependencies are provided at object creation time.

    // 3. **validateToken Method**
    // This method checks if the provided JWT token is valid for a specific user. It uses the TokenService to perform the validation.
    // If the token is invalid or expired, it returns a 401 Unauthorized response with an appropriate error message. This ensures security by preventing
    // unauthorized access to protected resources.
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> response = new HashMap<>();
        
        try {
            boolean isValid = tokenService.validateToken(token, user);
            
            if (!isValid) {
                response.put("error", "Invalid or expired token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            response.put("message", "Token is valid");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Token validation failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // 4. **validateAdmin Method**
    // This method validates the login credentials for an admin user.
    // - It first searches the admin repository using the provided username.
    // - If an admin is found, it checks if the password matches.
    // - If the password is correct, it generates and returns a JWT token (using the admin's username) with a 200 OK status.
    // - If the password is incorrect, it returns a 401 Unauthorized status with an error message.
    // - If no admin is found, it also returns a 401 Unauthorized.
    // - If any unexpected error occurs during the process, a 500 Internal Server Error response is returned.
    // This method ensures that only valid admin users can access secured parts of the system.
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> response = new HashMap<>();
        
        try {
            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            
            if (admin == null) {
                response.put("error", "Admin not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            if (admin.getPassword().equals(receivedAdmin.getPassword())) {
                String token = tokenService.generateToken(admin.getUsername());
                response.put("token", token);
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "Invalid password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (Exception e) {
            response.put("error", "An error occurred during admin validation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 5. **filterDoctor Method**
    // This method provides filtering functionality for doctors based on name, specialty, and available time slots.
    // - It supports various combinations of the three filters.
    // - If none of the filters are provided, it returns all available doctors.
    // This flexible filtering mechanism allows the frontend or consumers of the API to search and narrow down doctors based on user criteria.
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Doctor> filteredDoctors;
            
            if (name != null && !name.isEmpty() && specialty != null && !specialty.isEmpty() && time != null && !time.isEmpty()) {
                // Filter by name, specialty, and time
                filteredDoctors = doctorService.filterDoctorsByNameSpecialityAndTime(name, specialty, time);
            } else if (name != null && !name.isEmpty() && specialty != null && !specialty.isEmpty()) {
                // Filter by name and specialty
                filteredDoctors = doctorService.filterDoctorsByNameAndSpeciality(name, specialty);
            } else if (name != null && !name.isEmpty() && time != null && !time.isEmpty()) {
                // Filter by name and time
                filteredDoctors = doctorService.filterDoctorsByNameAndTime(name, time);
            } else if (specialty != null && !specialty.isEmpty() && time != null && !time.isEmpty()) {
                // Filter by specialty and time
                filteredDoctors = doctorService.filterDoctorsBySpecialityAndTime(specialty, time);
            } else if (name != null && !name.isEmpty()) {
                // Filter by name only
                filteredDoctors = doctorService.filterDoctorsByName(name);
            } else if (specialty != null && !specialty.isEmpty()) {
                // Filter by specialty only
                filteredDoctors = doctorService.filterDoctorsBySpeciality(specialty);
            } else if (time != null && !time.isEmpty()) {
                // Filter by time only
                filteredDoctors = doctorService.filterDoctorsByTime(time);
            } else {
                // No filters provided, return all doctors
                filteredDoctors = doctorService.getAllDoctors();
            }
            
            response.put("doctors", filteredDoctors);
            response.put("count", filteredDoctors.size());
            
        } catch (Exception e) {
            response.put("error", "Failed to filter doctors: " + e.getMessage());
        }
        
        return response;
    }

    // 6. **validateAppointment Method**
    // This method validates if the requested appointment time for a doctor is available.
    // - It first checks if the doctor exists in the repository.
    // - Then, it retrieves the list of available time slots for the doctor on the specified date.
    // - It compares the requested appointment time with the start times of these slots.
    // - If a match is found, it returns 1 (valid appointment time).
    // - If no matching time slot is found, it returns 0 (invalid).
    // - If the doctor doesn't exist, it returns -1.
    // This logic prevents overlapping or invalid appointment bookings.
    public int validateAppointment(Appointment appointment) {
        try {
            Optional<Doctor> doctorOptional = doctorRepository.findById(appointment.getDoctorId());
            
            if (!doctorOptional.isPresent()) {
                return -1; // Doctor doesn't exist
            }
            
            Doctor doctor = doctorOptional.get();
            List<String> availableSlots = doctorService.getDoctorAvailability(doctor.getId(), appointment.getAppointmentDate());
            
            // Check if the appointment time matches any available slot
            for (String slot : availableSlots) {
                if (slot.equals(appointment.getAppointmentTime())) {
                    return 1; // Valid appointment time
                }
            }
            
            return 0; // Time unavailable
            
        } catch (Exception e) {
            // In case of any error, consider appointment as invalid
            return 0;
        }
    }

    // 7. **validatePatient Method**
    // This method checks whether a patient with the same email or phone number already exists in the system.
    // - If a match is found, it returns false (indicating the patient is not valid for new registration).
    // - If no match is found, it returns true.
    // This helps enforce uniqueness constraints on patient records and prevent duplicate entries.
    public boolean validatePatient(Patient patient) {
        try {
            Patient existingPatient = patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone());
            return existingPatient == null; // Return true if patient doesn't exist (valid for registration)
        } catch (Exception e) {
            // In case of error, assume patient exists to prevent duplicate registration
            return false;
        }
    }

    // 8. **validatePatientLogin Method**
    // This method handles login validation for patient users.
    // - It looks up the patient by email.
    // - If found, it checks whether the provided password matches the stored one.
    // - On successful validation, it generates a JWT token and returns it with a 200 OK status.
    // - If the password is incorrect or the patient doesn't exist, it returns a 401 Unauthorized with a relevant error.
    // - If an exception occurs, it returns a 500 Internal Server Error.
    // This method ensures only legitimate patients can log in and access their data securely.
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> response = new HashMap<>();
        
        try {
            Patient patient = patientRepository.findByEmail(login.getEmail());
            
            if (patient == null) {
                response.put("error", "Patient not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            if (patient.getPassword().equals(login.getPassword())) {
                String token = tokenService.generateToken(patient.getEmail());
                response.put("token", token);
                response.put("patientId", String.valueOf(patient.getId()));
                response.put("name", patient.getName());
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "Invalid password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (Exception e) {
            response.put("error", "An error occurred during patient login");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 9. **filterPatient Method**
    // This method filters a patient's appointment history based on condition and doctor name.
    // - It extracts the email from the JWT token to identify the patient.
    // - Depending on which filters (condition, doctor name) are provided, it delegates the filtering logic to PatientService.
    // - If no filters are provided, it retrieves all appointments for the patient.
    // This flexible method supports patient-specific querying and enhances user experience on the client side.
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract patient email from token
            String patientEmail = tokenService.extractEmailFromToken(token);
            
            if (patientEmail == null) {
                response.put("error", "Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Get patient by email
            Patient patient = patientRepository.findByEmail(patientEmail);
            if (patient == null) {
                response.put("error", "Patient not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            List<Appointment> filteredAppointments;
            
            if (condition != null && !condition.isEmpty() && name != null && !name.isEmpty()) {
                // Filter by both condition and doctor name
                filteredAppointments = patientService.filterByDoctorAndCondition(patient.getId(), name, condition);
            } else if (condition != null && !condition.isEmpty()) {
                // Filter by condition only
                filteredAppointments = patientService.filterByCondition(patient.getId(), condition);
            } else if (name != null && !name.isEmpty()) {
                // Filter by doctor name only
                filteredAppointments = patientService.filterByDoctor(patient.getId(), name);
            } else {
                // No filters, get all appointments for the patient
                filteredAppointments = patientService.getAllAppointments(patient.getId());
            }
            
            response.put("appointments", filteredAppointments);
            response.put("count", filteredAppointments.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Failed to filter appointments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}