package com.project.back_end.controllers;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("${api.path}" + "prescription")
public class PrescriptionController {
    
    private final PrescriptionService prescriptionService;
    private final Service service;

    // 1. Set Up the Controller Class:
    //    - Annotate the class with `@RestController` to define it as a REST API controller.
    //    - Use `@RequestMapping("${api.path}prescription")` to set the base path for all prescription-related endpoints.
    //    - This controller manages creating and retrieving prescriptions tied to appointments.

    // 2. Autowire Dependencies:
    //    - Inject `PrescriptionService` to handle logic related to saving and fetching prescriptions.
    //    - Inject the shared `Service` class for token validation and role-based access control.
    @Autowired
    public PrescriptionController(PrescriptionService prescriptionService, Service service) {
        this.prescriptionService = prescriptionService;
        this.service = service;
    }

    // 3. Define the `savePrescription` Method:
    //    - Handles HTTP POST requests to save a new prescription for a given appointment.
    //    - Accepts a validated `Prescription` object in the request body and a doctor's token as a path variable.
    //    - Validates the token for the `"doctor"` role.
    //    - If the token is valid, updates the status of the corresponding appointment to reflect that a prescription has been added.
    //    - Delegates the saving logic to `PrescriptionService` and returns a response indicating success or failure.
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> savePrescription(
            @PathVariable String token,
            @RequestBody Prescription prescription) {
        
        // Validate doctor token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // Extract doctor email from token for validation
            String doctorEmail = service.extractEmailFromToken(token);
            if (doctorEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token"));
            }
            
            // Validate that the prescription belongs to this doctor
            if (!prescriptionService.validateDoctorForPrescription(prescription.getAppointmentId(), doctorEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not authorized to write prescriptions for this appointment"));
            }
            
            // Save the prescription
            Prescription savedPrescription = prescriptionService.savePrescription(prescription);
            if (savedPrescription != null) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of(
                                "message", "Prescription saved successfully",
                                "prescriptionId", String.valueOf(savedPrescription.getId()),
                                "appointmentId", String.valueOf(savedPrescription.getAppointmentId())
                        ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to save prescription"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save prescription: " + e.getMessage()));
        }
    }

    // 4. Define the `getPrescription` Method:
    //    - Handles HTTP GET requests to retrieve a prescription by its associated appointment ID.
    //    - Accepts the appointment ID and a doctor's token as path variables.
    //    - Validates the token for the `"doctor"` role using the shared service.
    //    - If the token is valid, fetches the prescription using the `PrescriptionService`.
    //    - Returns the prescription details or an appropriate error message if validation fails.
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescription(
            @PathVariable Long appointmentId,
            @PathVariable String token) {
        
        // Validate doctor token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // Extract doctor email from token for validation
            String doctorEmail = service.extractEmailFromToken(token);
            if (doctorEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token"));
            }
            
            // Check if doctor is authorized to view this prescription
            if (!prescriptionService.validateDoctorForPrescription(appointmentId, doctorEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not authorized to view this prescription"));
            }
            
            Map<String, Object> prescription = prescriptionService.getPrescription(appointmentId);
            
            if (prescription.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(prescription);
            }
            
            return ResponseEntity.ok(prescription);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve prescription: " + e.getMessage()));
        }
    }

    // Additional endpoint: Get prescription for patient (patient can view their own prescriptions)
    @GetMapping("/patient/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescriptionForPatient(
            @PathVariable Long appointmentId,
            @PathVariable String token) {
        
        // Validate patient token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // Extract patient email from token
            String patientEmail = service.extractEmailFromToken(token);
            if (patientEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token"));
            }
            
            // Check if patient is authorized to view this prescription
            if (!prescriptionService.validatePatientForPrescription(appointmentId, patientEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not authorized to view this prescription"));
            }
            
            Map<String, Object> prescription = prescriptionService.getPrescription(appointmentId);
            
            if (prescription.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(prescription);
            }
            
            return ResponseEntity.ok(prescription);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve prescription: " + e.getMessage()));
        }
    }

    // Additional endpoint: Update prescription
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updatePrescription(
            @PathVariable String token,
            @RequestBody Prescription prescription) {
        
        // Validate doctor token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // Extract doctor email from token for validation
            String doctorEmail = service.extractEmailFromToken(token);
            if (doctorEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token"));
            }
            
            // Validate that the prescription belongs to this doctor
            if (!prescriptionService.validateDoctorForPrescription(prescription.getAppointmentId(), doctorEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not authorized to update this prescription"));
            }
            
            Prescription updatedPrescription = prescriptionService.updatePrescription(prescription);
            if (updatedPrescription != null) {
                return ResponseEntity.ok(Map.of(
                        "message", "Prescription updated successfully",
                        "prescriptionId", String.valueOf(updatedPrescription.getId())
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Prescription not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update prescription: " + e.getMessage()));
        }
    }

    // Additional endpoint: Get all prescriptions for a doctor
    @GetMapping("/doctor/{doctorId}/{token}")
    public ResponseEntity<?> getDoctorPrescriptions(
            @PathVariable Long doctorId,
            @PathVariable String token) {
        
        // Validate doctor token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // Extract doctor email from token for validation
            String doctorEmail = service.extractEmailFromToken(token);
            if (doctorEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token"));
            }
            
            // Validate that the doctor is accessing their own prescriptions
            if (!prescriptionService.validateDoctorOwnership(doctorId, doctorEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not authorized to view these prescriptions"));
            }
            
            Map<String, Object> prescriptions = prescriptionService.getPrescriptionsByDoctorId(doctorId);
            return ResponseEntity.ok(prescriptions);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve prescriptions: " + e.getMessage()));
        }
    }

    // Additional endpoint: Health check
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "Prescription service is running"));
    }
}