package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// 1. **Add @Service Annotation**:
//    - The `@Service` annotation is used to mark this class as a Spring service component. 
//    - It will be managed by Spring's container and used for business logic related to patients and appointments.
@Service
public class PatientService {

    // 2. **Constructor Injection for Dependencies**:
    //    - The `PatientService` class has dependencies on `PatientRepository`, `AppointmentRepository`, and `TokenService`.
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    // Constructor injection
    @Autowired
    public PatientService(PatientRepository patientRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // 3. **createPatient Method**:
    //    - Creates a new patient in the database. It saves the patient object using the `PatientRepository`.
    //    - If the patient is successfully saved, the method returns `1`; otherwise, it logs the error and returns `0`.
    @Transactional
    public int createPatient(Patient patient) {
        try {
            // Check if patient with same email already exists
            Patient existingPatient = patientRepository.findByEmail(patient.getEmail());
            if (existingPatient != null) {
                // Patient with this email already exists
                return -1; // Conflict
            }
            
            // Check if patient with same phone already exists
            existingPatient = patientRepository.findByPhone(patient.getPhone());
            if (existingPatient != null) {
                // Patient with this phone already exists
                return -2; // Phone conflict
            }
            
            // Set default values if not provided
            if (patient.getStatus() == null) {
                patient.setStatus("active");
            }
            
            // Save the patient
            Patient savedPatient = patientRepository.save(patient);
            return savedPatient != null ? 1 : 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Internal error
        }
    }

    // 4. **getPatientAppointment Method**:
    //    - Retrieves a list of appointments for a specific patient, based on their ID.
    //    - The appointments are then converted into `AppointmentDTO` objects for easier consumption by the API client.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract email from token
            String patientEmail = tokenService.extractEmail(token);
            if (patientEmail == null) {
                response.put("error", "Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Find patient by email from token
            Patient patientFromToken = patientRepository.findByEmail(patientEmail);
            if (patientFromToken == null) {
                response.put("error", "Patient not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Verify that the patient ID from token matches the requested ID
            if (!patientFromToken.getId().equals(id)) {
                response.put("error", "Unauthorized access to patient appointments");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Get appointments for the patient
            List<Appointment> appointments = appointmentRepository.findByPatientId(id);
            
            // Convert appointments to DTOs
            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                .map(this::convertToAppointmentDTO)
                .collect(Collectors.toList());
            
            response.put("appointments", appointmentDTOs);
            response.put("count", appointmentDTOs.size());
            response.put("patientId", id);
            response.put("patientName", patientFromToken.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to retrieve appointments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 5. **filterByCondition Method**:
    //    - Filters appointments for a patient based on the condition (e.g., "past" or "future").
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate condition
            if (condition == null || (!condition.equalsIgnoreCase("past") && !condition.equalsIgnoreCase("future"))) {
                response.put("error", "Invalid condition. Use 'past' or 'future'.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Get all appointments for the patient
            List<Appointment> allAppointments = appointmentRepository.findByPatientId(id);
            
            // Filter based on condition
            List<Appointment> filteredAppointments;
            LocalDateTime now = LocalDateTime.now();
            
            if (condition.equalsIgnoreCase("past")) {
                filteredAppointments = allAppointments.stream()
                    .filter(appointment -> appointment.getAppointmentTime().isBefore(now))
                    .collect(Collectors.toList());
            } else { // future
                filteredAppointments = allAppointments.stream()
                    .filter(appointment -> appointment.getAppointmentTime().isAfter(now) || 
                                           appointment.getAppointmentTime().isEqual(now))
                    .collect(Collectors.toList());
            }
            
            // Convert to DTOs
            List<AppointmentDTO> appointmentDTOs = filteredAppointments.stream()
                .map(this::convertToAppointmentDTO)
                .collect(Collectors.toList());
            
            response.put("appointments", appointmentDTOs);
            response.put("count", appointmentDTOs.size());
            response.put("condition", condition);
            response.put("patientId", id);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to filter appointments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 6. **filterByDoctor Method**:
    //    - Filters appointments for a patient based on the doctor's name.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate inputs
            if (name == null || name.trim().isEmpty()) {
                response.put("error", "Doctor name is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Get appointments for the patient
            List<Appointment> allAppointments = appointmentRepository.findByPatientId(patientId);
            
            // Filter by doctor name (case-insensitive partial match)
            List<Appointment> filteredAppointments = allAppointments.stream()
                .filter(appointment -> {
                    Doctor doctor = appointment.getDoctor();
                    return doctor != null && 
                           doctor.getName() != null &&
                           doctor.getName().toLowerCase().contains(name.toLowerCase().trim());
                })
                .collect(Collectors.toList());
            
            // Convert to DTOs
            List<AppointmentDTO> appointmentDTOs = filteredAppointments.stream()
                .map(this::convertToAppointmentDTO)
                .collect(Collectors.toList());
            
            response.put("appointments", appointmentDTOs);
            response.put("count", appointmentDTOs.size());
            response.put("doctorName", name);
            response.put("patientId", patientId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to filter appointments by doctor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 7. **filterByDoctorAndCondition Method**:
    //    - Filters appointments based on both the doctor's name and the condition (past or future) for a specific patient.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition, String name, long patientId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate condition
            if (condition == null || (!condition.equalsIgnoreCase("past") && !condition.equalsIgnoreCase("future"))) {
                response.put("error", "Invalid condition. Use 'past' or 'future'.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Get all appointments for the patient
            List<Appointment> allAppointments = appointmentRepository.findByPatientId(patientId);
            
            LocalDateTime now = LocalDateTime.now();
            
            // Filter by both doctor name and condition
            List<Appointment> filteredAppointments = allAppointments.stream()
                .filter(appointment -> {
                    // Check doctor name
                    Doctor doctor = appointment.getDoctor();
                    boolean doctorMatches = doctor != null && 
                                           doctor.getName() != null &&
                                           doctor.getName().toLowerCase().contains(name.toLowerCase().trim());
                    
                    if (!doctorMatches) return false;
                    
                    // Check condition
                    if (condition.equalsIgnoreCase("past")) {
                        return appointment.getAppointmentTime().isBefore(now);
                    } else { // future
                        return appointment.getAppointmentTime().isAfter(now) || 
                               appointment.getAppointmentTime().isEqual(now);
                    }
                })
                .collect(Collectors.toList());
            
            // Convert to DTOs
            List<AppointmentDTO> appointmentDTOs = filteredAppointments.stream()
                .map(this::convertToAppointmentDTO)
                .collect(Collectors.toList());
            
            response.put("appointments", appointmentDTOs);
            response.put("count", appointmentDTOs.size());
            response.put("condition", condition);
            response.put("doctorName", name);
            response.put("patientId", patientId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to filter appointments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 8. **getPatientDetails Method**:
    //    - Retrieves patient details using the `tokenService` to extract the patient's email from the provided token.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract email from token
            String patientEmail = tokenService.extractEmail(token);
            if (patientEmail == null) {
                response.put("error", "Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Find patient by email
            Patient patient = patientRepository.findByEmail(patientEmail);
            if (patient == null) {
                response.put("error", "Patient not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Return patient details (excluding sensitive information)
            Map<String, Object> patientDetails = new HashMap<>();
            patientDetails.put("id", patient.getId());
            patientDetails.put("name", patient.getName());
            patientDetails.put("email", patient.getEmail());
            patientDetails.put("phone", patient.getPhone());
            patientDetails.put("address", patient.getAddress());
            patientDetails.put("dateOfBirth", patient.getDateOfBirth());
            patientDetails.put("gender", patient.getGender());
            patientDetails.put("emergencyContact", patient.getEmergencyContact());
            patientDetails.put("medicalHistory", patient.getMedicalHistory());
            patientDetails.put("status", patient.getStatus());
            patientDetails.put("registrationDate", patient.getRegistrationDate());
            
            response.put("patient", patientDetails);
            response.put("message", "Patient details retrieved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to retrieve patient details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Helper method to convert Appointment to AppointmentDTO
    private AppointmentDTO convertToAppointmentDTO(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        
        Doctor doctor = appointment.getDoctor();
        Patient patient = appointment.getPatient();
        
        return new AppointmentDTO(
            appointment.getId(),
            doctor != null ? doctor.getId() : null,
            doctor != null ? doctor.getName() : "Unknown Doctor",
            patient != null ? patient.getId() : null,
            patient != null ? patient.getName() : "Unknown Patient",
            patient != null ? patient.getEmail() : null,
            patient != null ? patient.getPhone() : null,
            patient != null ? patient.getAddress() : null,
            appointment.getAppointmentTime(),
            appointment.getStatus()
        );
    }

    // Additional useful methods
    
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientById(Long id, String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract email from token
            String patientEmail = tokenService.extractEmail(token);
            if (patientEmail == null) {
                response.put("error", "Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Find patient by ID
            Optional<Patient> patientOptional = patientRepository.findById(id);
            if (patientOptional.isEmpty()) {
                response.put("error", "Patient not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Patient patient = patientOptional.get();
            
            // Verify that the patient from token matches the requested patient
            Patient patientFromToken = patientRepository.findByEmail(patientEmail);
            if (patientFromToken == null || !patientFromToken.getId().equals(id)) {
                response.put("error", "Unauthorized access to patient information");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Return patient details
            Map<String, Object> patientDetails = new HashMap<>();
            patientDetails.put("id", patient.getId());
            patientDetails.put("name", patient.getName());
            patientDetails.put("email", patient.getEmail());
            patientDetails.put("phone", patient.getPhone());
            patientDetails.put("address", patient.getAddress());
            patientDetails.put("dateOfBirth", patient.getDateOfBirth());
            patientDetails.put("gender", patient.getGender());
            patientDetails.put("emergencyContact", patient.getEmergencyContact());
            patientDetails.put("medicalHistory", patient.getMedicalHistory());
            patientDetails.put("status", patient.getStatus());
            patientDetails.put("registrationDate", patient.getRegistrationDate());
            
            response.put("patient", patientDetails);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to retrieve patient: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @Transactional
    public ResponseEntity<Map<String, Object>> updatePatient(Long id, Patient updatedPatient, String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract email from token
            String patientEmail = tokenService.extractEmail(token);
            if (patientEmail == null) {
                response.put("error", "Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Find patient by ID
            Optional<Patient> patientOptional = patientRepository.findById(id);
            if (patientOptional.isEmpty()) {
                response.put("error", "Patient not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Patient patient = patientOptional.get();
            
            // Verify that the patient from token matches the patient to update
            Patient patientFromToken = patientRepository.findByEmail(patientEmail);
            if (patientFromToken == null || !patientFromToken.getId().equals(id)) {
                response.put("error", "Unauthorized to update this patient");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Update patient fields
            if (updatedPatient.getName() != null) {
                patient.setName(updatedPatient.getName());
            }
            if (updatedPatient.getPhone() != null) {
                // Check if phone already exists for another patient
                Patient existingWithPhone = patientRepository.findByPhone(updatedPatient.getPhone());
                if (existingWithPhone != null && !existingWithPhone.getId().equals(id)) {
                    response.put("error", "Phone number already in use");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }
                patient.setPhone(updatedPatient.getPhone());
            }
            if (updatedPatient.getAddress() != null) {
                patient.setAddress(updatedPatient.getAddress());
            }
            if (updatedPatient.getDateOfBirth() != null) {
                patient.setDateOfBirth(updatedPatient.getDateOfBirth());
            }
            if (updatedPatient.getGender() != null) {
                patient.setGender(updatedPatient.getGender());
            }
            if (updatedPatient.getEmergencyContact() != null) {
                patient.setEmergencyContact(updatedPatient.getEmergencyContact());
            }
            if (updatedPatient.getMedicalHistory() != null) {
                patient.setMedicalHistory(updatedPatient.getMedicalHistory());
            }
            
            // Save updated patient
            Patient savedPatient = patientRepository.save(patient);
            
            response.put("message", "Patient updated successfully");
            response.put("patientId", savedPatient.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to update patient: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @Transactional
    public ResponseEntity<Map<String, Object>> deletePatient(Long id, String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract email from token
            String patientEmail = tokenService.extractEmail(token);
            if (patientEmail == null) {
                response.put("error", "Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Find patient by ID
            Optional<Patient> patientOptional = patientRepository.findById(id);
            if (patientOptional.isEmpty()) {
                response.put("error", "Patient not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Patient patient = patientOptional.get();
            
            // Verify that the patient from token matches the patient to delete
            Patient patientFromToken = patientRepository.findByEmail(patientEmail);
            if (patientFromToken == null || !patientFromToken.getId().equals(id)) {
                response.put("error", "Unauthorized to delete this patient");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Delete patient's appointments first
            List<Appointment> patientAppointments = appointmentRepository.findByPatientId(id);
            appointmentRepository.deleteAll(patientAppointments);
            
            // Delete the patient
            patientRepository.delete(patient);
            
            response.put("message", "Patient deleted successfully");
            response.put("deletedPatientId", id);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to delete patient: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> searchPatients(String searchTerm) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Patient> patients = new ArrayList<>();
            
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                patients = patientRepository.findAll();
            } else {
                // Try to find by email
                Patient byEmail = patientRepository.findByEmail(searchTerm);
                if (byEmail != null) {
                    patients.add(byEmail);
                }
                
                // Try to find by partial name (case-insensitive)
                Patient byName = patientRepository.findByNameContainingIgnoreCase(searchTerm);
                if (byName != null && !patients.contains(byName)) {
                    patients.add(byName);
                }
                
                // Try to find by phone
                Patient byPhone = patientRepository.findByPhone(searchTerm);
                if (byPhone != null && !patients.contains(byPhone)) {
                    patients.add(byPhone);
                }
                
                // If still empty, try broader search
                if (patients.isEmpty()) {
                    patients = patientRepository.findAll().stream()
                        .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                     p.getEmail() != null && p.getEmail().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                     p.getPhone() != null && p.getPhone().contains(searchTerm))
                        .collect(Collectors.toList());
                }
            }
            
            response.put("patients", patients);
            response.put("count", patients.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to search patients: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}