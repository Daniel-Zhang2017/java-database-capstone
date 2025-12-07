package com.project.back_end.services;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 1. **Add @Service Annotation**:
//    - The `@Service` annotation marks this class as a Spring service component, allowing Spring's container to manage it.
//    - This class contains the business logic related to managing prescriptions in the healthcare system.
@Service
public class PrescriptionService {

    // 2. **Constructor Injection for Dependencies**:
    //    - The `PrescriptionService` class depends on the `PrescriptionRepository` to interact with the database.
    private final PrescriptionRepository prescriptionRepository;

    // Constructor injection
    @Autowired
    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    // 3. **savePrescription Method**:
    //    - This method saves a new prescription to the database.
    //    - Before saving, it checks if a prescription already exists for the same appointment (using the appointment ID).
    @Transactional
    public ResponseEntity<Map<String, String>> savePrescription(Prescription prescription) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Validate prescription object
            if (prescription == null) {
                response.put("error", "Prescription cannot be null");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Check if appointmentId is provided
            if (prescription.getAppointmentId() == null) {
                response.put("error", "Appointment ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Check if a prescription already exists for this appointment
            boolean prescriptionExists = prescriptionRepository.existsByAppointmentId(prescription.getAppointmentId());
            if (prescriptionExists) {
                response.put("error", "A prescription already exists for this appointment");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Validate required fields
            if (prescription.getPatientId() == null) {
                response.put("error", "Patient ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            if (prescription.getDoctorId() == null) {
                response.put("error", "Doctor ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            if (prescription.getMedicationName() == null || prescription.getMedicationName().trim().isEmpty()) {
                response.put("error", "Medication name is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            if (prescription.getDosage() == null || prescription.getDosage().trim().isEmpty()) {
                response.put("error", "Dosage is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Set default values if not provided
            if (prescription.getStatus() == null || prescription.getStatus().trim().isEmpty()) {
                prescription.setStatus("active");
            }
            
            if (prescription.getRefillsRemaining() == 0) {
                prescription.setRefillsRemaining(0); // Default to no refills
            }
            
            // Set prescription date if not provided
            if (prescription.getPrescriptionDate() == null) {
                prescription.setPrescriptionDate(java.time.LocalDate.now());
            }
            
            // Save the prescription
            Prescription savedPrescription = prescriptionRepository.save(prescription);
            
            if (savedPrescription != null) {
                response.put("message", "Prescription saved successfully");
                response.put("prescriptionId", savedPrescription.getId());
                response.put("appointmentId", String.valueOf(savedPrescription.getAppointmentId()));
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                response.put("error", "Failed to save prescription");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to save prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 4. **getPrescription Method**:
    //    - Retrieves a prescription associated with a specific appointment based on the `appointmentId`.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPrescription(Long appointmentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate appointmentId
            if (appointmentId == null) {
                response.put("error", "Appointment ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Fetch prescription by appointment ID
            List<Prescription> prescriptions = prescriptionRepository.findByAppointmentId(appointmentId);
            
            if (prescriptions == null || prescriptions.isEmpty()) {
                response.put("message", "No prescription found for the given appointment");
                response.put("prescription", null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Return the first prescription (assuming one prescription per appointment)
            Prescription prescription = prescriptions.get(0);
            
            // Create a clean response map
            Map<String, Object> prescriptionDetails = new HashMap<>();
            prescriptionDetails.put("id", prescription.getId());
            prescriptionDetails.put("appointmentId", prescription.getAppointmentId());
            prescriptionDetails.put("patientId", prescription.getPatientId());
            prescriptionDetails.put("doctorId", prescription.getDoctorId());
            prescriptionDetails.put("medicationName", prescription.getMedicationName());
            prescriptionDetails.put("dosage", prescription.getDosage());
            prescriptionDetails.put("frequency", prescription.getFrequency());
            prescriptionDetails.put("duration", prescription.getDuration());
            prescriptionDetails.put("instructions", prescription.getInstructions());
            prescriptionDetails.put("prescriptionDate", prescription.getPrescriptionDate());
            prescriptionDetails.put("expirationDate", prescription.getExpirationDate());
            prescriptionDetails.put("refillsRemaining", prescription.getRefillsRemaining());
            prescriptionDetails.put("status", prescription.getStatus());
            prescriptionDetails.put("notes", prescription.getNotes());
            prescriptionDetails.put("createdAt", prescription.getCreatedAt());
            prescriptionDetails.put("updatedAt", prescription.getUpdatedAt());
            
            response.put("prescription", prescriptionDetails);
            response.put("message", "Prescription retrieved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to retrieve prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 5. **Additional useful methods for prescription management**:

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPrescriptionsByPatientId(Long patientId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (patientId == null) {
                response.put("error", "Patient ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            List<Prescription> prescriptions = prescriptionRepository.findByPatientId(patientId);
            
            if (prescriptions == null || prescriptions.isEmpty()) {
                response.put("message", "No prescriptions found for this patient");
                response.put("prescriptions", new java.util.ArrayList<>());
                return ResponseEntity.ok(response);
            }
            
            response.put("prescriptions", prescriptions);
            response.put("count", prescriptions.size());
            response.put("message", "Prescriptions retrieved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to retrieve prescriptions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPrescriptionsByDoctorId(Long doctorId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (doctorId == null) {
                response.put("error", "Doctor ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            List<Prescription> prescriptions = prescriptionRepository.findByDoctorId(doctorId);
            
            if (prescriptions == null || prescriptions.isEmpty()) {
                response.put("message", "No prescriptions found for this doctor");
                response.put("prescriptions", new java.util.ArrayList<>());
                return ResponseEntity.ok(response);
            }
            
            response.put("prescriptions", prescriptions);
            response.put("count", prescriptions.size());
            response.put("message", "Prescriptions retrieved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to retrieve prescriptions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> updatePrescription(String prescriptionId, Prescription updatedPrescription) {
        Map<String, String> response = new HashMap<>();
        
        try {
            if (prescriptionId == null || prescriptionId.trim().isEmpty()) {
                response.put("error", "Prescription ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Find existing prescription
            java.util.Optional<Prescription> existingPrescriptionOpt = prescriptionRepository.findById(prescriptionId);
            if (existingPrescriptionOpt.isEmpty()) {
                response.put("error", "Prescription not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Prescription existingPrescription = existingPrescriptionOpt.get();
            
            // Update fields if provided in updatedPrescription
            if (updatedPrescription.getMedicationName() != null) {
                existingPrescription.setMedicationName(updatedPrescription.getMedicationName());
            }
            
            if (updatedPrescription.getDosage() != null) {
                existingPrescription.setDosage(updatedPrescription.getDosage());
            }
            
            if (updatedPrescription.getFrequency() != null) {
                existingPrescription.setFrequency(updatedPrescription.getFrequency());
            }
            
            if (updatedPrescription.getDuration() != null) {
                existingPrescription.setDuration(updatedPrescription.getDuration());
            }
            
            if (updatedPrescription.getInstructions() != null) {
                existingPrescription.setInstructions(updatedPrescription.getInstructions());
            }
            
            if (updatedPrescription.getExpirationDate() != null) {
                existingPrescription.setExpirationDate(updatedPrescription.getExpirationDate());
            }
            
            if (updatedPrescription.getRefillsRemaining() >= 0) {
                existingPrescription.setRefillsRemaining(updatedPrescription.getRefillsRemaining());
            }
            
            if (updatedPrescription.getStatus() != null) {
                existingPrescription.setStatus(updatedPrescription.getStatus());
            }
            
            if (updatedPrescription.getNotes() != null) {
                existingPrescription.setNotes(updatedPrescription.getNotes());
            }
            
            // Update timestamp
            existingPrescription.setUpdatedAt(java.time.LocalDateTime.now());
            
            // Save updated prescription
            Prescription savedPrescription = prescriptionRepository.save(existingPrescription);
            
            response.put("message", "Prescription updated successfully");
            response.put("prescriptionId", savedPrescription.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to update prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> deletePrescription(String prescriptionId) {
        Map<String, String> response = new HashMap<>();
        
        try {
            if (prescriptionId == null || prescriptionId.trim().isEmpty()) {
                response.put("error", "Prescription ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Check if prescription exists
            if (!prescriptionRepository.existsById(prescriptionId)) {
                response.put("error", "Prescription not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Delete the prescription
            prescriptionRepository.deleteById(prescriptionId);
            
            response.put("message", "Prescription deleted successfully");
            response.put("deletedPrescriptionId", prescriptionId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to delete prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> searchPrescriptions(String searchTerm, Long patientId, Long doctorId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Prescription> prescriptions;
            
            if (patientId != null && doctorId != null) {
                prescriptions = prescriptionRepository.findByPatientIdAndDoctorId(patientId, doctorId);
            } else if (patientId != null) {
                prescriptions = prescriptionRepository.findByPatientId(patientId);
            } else if (doctorId != null) {
                prescriptions = prescriptionRepository.findByDoctorId(doctorId);
            } else {
                prescriptions = prescriptionRepository.findAll();
            }
            
            // Filter by search term if provided
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String finalSearchTerm = searchTerm.toLowerCase().trim();
                prescriptions = prescriptions.stream()
                    .filter(p -> 
                        (p.getMedicationName() != null && p.getMedicationName().toLowerCase().contains(finalSearchTerm)) ||
                        (p.getInstructions() != null && p.getInstructions().toLowerCase().contains(finalSearchTerm)) ||
                        (p.getNotes() != null && p.getNotes().toLowerCase().contains(finalSearchTerm))
                    )
                    .collect(java.util.stream.Collectors.toList());
            }
            
            response.put("prescriptions", prescriptions);
            response.put("count", prescriptions.size());
            response.put("message", "Prescriptions retrieved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to search prescriptions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getActivePrescriptions(Long patientId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (patientId == null) {
                response.put("error", "Patient ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            List<Prescription> allPrescriptions = prescriptionRepository.findByPatientId(patientId);
            
            // Filter active prescriptions (not expired and refills remaining)
            java.time.LocalDate today = java.time.LocalDate.now();
            List<Prescription> activePrescriptions = allPrescriptions.stream()
                .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getExpirationDate() == null || p.getExpirationDate().isAfter(today))
                .filter(p -> p.getRefillsRemaining() > 0)
                .collect(java.util.stream.Collectors.toList());
            
            response.put("prescriptions", activePrescriptions);
            response.put("count", activePrescriptions.size());
            response.put("message", "Active prescriptions retrieved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to retrieve active prescriptions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}