package com.project.back_end.services;

import com.project.back_end.model.Appointment;
import com.project.back_end.model.Doctor;
import com.project.back_end.model.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// 1. **Add @Service Annotation**:
//    - To indicate that this class is a service layer class for handling business logic.
//    - The `@Service` annotation should be added before the class declaration to mark it as a Spring service component.
@Service
public class AppointmentService {

    // 2. **Constructor Injection for Dependencies**:
    //    - The `AppointmentService` class requires several dependencies like `AppointmentRepository`, `Service`, `TokenService`, `PatientRepository`, and `DoctorRepository`.
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService;

    // Constructor injection
    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                             PatientRepository patientRepository,
                             DoctorRepository doctorRepository,
                             TokenService tokenService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
    }

    // 4. **Book Appointment Method**:
    //    - Responsible for saving the new appointment to the database.
    //    - If the save operation fails, it returns `0`; otherwise, it returns `1`.
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            // Validate appointment data before saving
            if (appointment == null) {
                return 0;
            }
            
            // Check if doctor exists
            Optional<Doctor> doctorOptional = doctorRepository.findById(appointment.getDoctor().getId());
            if (doctorOptional.isEmpty()) {
                return 0;
            }
            
            // Check if patient exists
            Optional<Patient> patientOptional = patientRepository.findById(appointment.getPatient().getId());
            if (patientOptional.isEmpty()) {
                return 0;
            }
            
            // Check if appointment time is in the future
            if (appointment.getAppointmentTime().isBefore(LocalDateTime.now())) {
                return 0;
            }
            
            // Check if doctor already has an appointment at this time
            boolean appointmentExists = appointmentRepository.existsByDoctorIdAndAppointmentTime(
                appointment.getDoctor().getId(), appointment.getAppointmentTime());
            
            if (appointmentExists) {
                return 0;
            }
            
            // Set default status if not provided
            if (appointment.getStatus() == 0) {
                appointment.setStatus(1); // 1 typically means "scheduled" or "booked"
            }
            
            // Save the appointment
            Appointment savedAppointment = appointmentRepository.save(appointment);
            return savedAppointment != null ? 1 : 0;
            
        } catch (Exception e) {
            // Log the exception
            e.printStackTrace();
            return 0;
        }
    }

    // 5. **Update Appointment Method**:
    //    - This method is used to update an existing appointment based on its ID.
    //    - It validates whether the patient ID matches, checks if the appointment is available for updating, and ensures that the doctor is available at the specified time.
    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Check if appointment exists
            Optional<Appointment> existingAppointmentOpt = appointmentRepository.findById(appointment.getId());
            if (existingAppointmentOpt.isEmpty()) {
                response.put("error", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Appointment existingAppointment = existingAppointmentOpt.get();
            
            // Validate appointment update
            if (!validateAppointmentForUpdate(existingAppointment, appointment)) {
                response.put("error", "Invalid appointment update");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Check if doctor exists
            Optional<Doctor> doctorOptional = doctorRepository.findById(appointment.getDoctor().getId());
            if (doctorOptional.isEmpty()) {
                response.put("error", "Doctor not found");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Check if new appointment time is available (excluding the current appointment)
            boolean timeConflict = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                    appointment.getDoctor().getId(),
                    appointment.getAppointmentTime().minusMinutes(30),
                    appointment.getAppointmentTime().plusMinutes(30)
                ).stream()
                .anyMatch(a -> !a.getId().equals(appointment.getId()));
            
            if (timeConflict) {
                response.put("error", "Doctor already has an appointment at this time");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            
            // Update appointment
            existingAppointment.setDoctor(appointment.getDoctor());
            existingAppointment.setAppointmentTime(appointment.getAppointmentTime());
            existingAppointment.setStatus(appointment.getStatus());
            
            // Update end time (if you have such logic)
            existingAppointment.setEndTime(appointment.getAppointmentTime().plusHours(1));
            
            appointmentRepository.save(existingAppointment);
            
            response.put("message", "Appointment updated successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to update appointment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Helper method to validate appointment update
    private boolean validateAppointmentForUpdate(Appointment existing, Appointment updated) {
        // Check if the appointment is in the future (can't update past appointments)
        if (existing.getAppointmentTime().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        // Check if the appointment is not already cancelled or completed
        // Assuming status 3 = cancelled, 2 = completed
        if (existing.getStatus() == 3 || existing.getStatus() == 2) {
            return false;
        }
        
        // Ensure the appointment time is in the future
        if (updated.getAppointmentTime().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        return true;
    }

    // 6. **Cancel Appointment Method**:
    //    - This method cancels an appointment by deleting it from the database.
    //    - It ensures the patient who owns the appointment is trying to cancel it and handles possible errors.
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Extract patient ID from token
            Long patientIdFromToken = tokenService.extractPatientId(token);
            
            if (patientIdFromToken == null) {
                response.put("error", "Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Find appointment
            Optional<Appointment> appointmentOptional = appointmentRepository.findById(id);
            if (appointmentOptional.isEmpty()) {
                response.put("error", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Appointment appointment = appointmentOptional.get();
            
            // Check if the patient owns the appointment
            if (!appointment.getPatient().getId().equals(patientIdFromToken)) {
                response.put("error", "You are not authorized to cancel this appointment");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Check if appointment can be cancelled (e.g., not in the past)
            if (appointment.getAppointmentTime().isBefore(LocalDateTime.now())) {
                response.put("error", "Cannot cancel past appointments");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Instead of deleting, update status to cancelled (status = 3)
            appointment.setStatus(3); // 3 typically means "cancelled"
            appointmentRepository.save(appointment);
            
            // Alternative: If you want to actually delete:
            // appointmentRepository.delete(appointment);
            
            response.put("message", "Appointment cancelled successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to cancel appointment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 7. **Get Appointments Method**:
    //    - This method retrieves a list of appointments for a specific doctor on a particular day, optionally filtered by the patient's name.
    @Transactional(readOnly = true)
    public Map<String, Object> getAppointments(String pname, LocalDate date, String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract doctor ID from token
            Long doctorIdFromToken = tokenService.extractDoctorId(token);
            
            if (doctorIdFromToken == null) {
                response.put("error", "Invalid token or not a doctor");
                return response;
            }
            
            // Calculate start and end of the day
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            
            // Get appointments for the doctor on the given date
            List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorIdFromToken, startOfDay, endOfDay);
            
            // Filter by patient name if provided
            if (pname != null && !pname.trim().isEmpty()) {
                String searchName = pname.toLowerCase().trim();
                appointments = appointments.stream()
                    .filter(appointment -> {
                        String patientName = appointment.getPatient().getName();
                        return patientName != null && 
                               patientName.toLowerCase().contains(searchName);
                    })
                    .collect(Collectors.toList());
            }
            
            // Convert to DTOs or keep as entities
            response.put("appointments", appointments);
            response.put("count", appointments.size());
            response.put("date", date.toString());
            response.put("doctorId", doctorIdFromToken);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to retrieve appointments: " + e.getMessage());
        }
        
        return response;
    }

    // 8. **Change Status Method**:
    //    - This method updates the status of an appointment by changing its value in the database.
    @Transactional
    public ResponseEntity<Map<String, String>> changeStatus(long appointmentId, int newStatus, String token) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Verify authorization (doctor or admin)
            Long doctorId = tokenService.extractDoctorId(token);
            Long adminId = tokenService.extractAdminId(token);
            
            if (doctorId == null && adminId == null) {
                response.put("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Find appointment
            Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
            if (appointmentOptional.isEmpty()) {
                response.put("error", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Appointment appointment = appointmentOptional.get();
            
            // If doctor is changing status, verify they are the appointment's doctor
            if (doctorId != null && !appointment.getDoctor().getId().equals(doctorId)) {
                response.put("error", "You can only change status of your own appointments");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Validate status transition
            if (!isValidStatusTransition(appointment.getStatus(), newStatus)) {
                response.put("error", "Invalid status transition");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Update status
            appointment.setStatus(newStatus);
            appointmentRepository.save(appointment);
            
            response.put("message", "Appointment status updated successfully");
            response.put("newStatus", String.valueOf(newStatus));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to update appointment status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Helper method to validate status transitions
    private boolean isValidStatusTransition(int currentStatus, int newStatus) {
        // Define valid status transitions
        // Example: 1=scheduled, 2=in-progress, 3=completed, 4=cancelled
        
        // Can't change from completed or cancelled
        if (currentStatus == 3 || currentStatus == 4) {
            return false;
        }
        
        // Allow all transitions except invalid ones
        return newStatus >= 1 && newStatus <= 4;
    }
    
    // Additional useful methods
    
    @Transactional(readOnly = true)
    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }
    
    @Transactional(readOnly = true)
    public List<Appointment> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }
    
    @Transactional(readOnly = true)
    public List<Appointment> getUpcomingAppointments(Long patientId) {
        return appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(patientId, 1)
            .stream()
            .filter(appointment -> appointment.getAppointmentTime().isAfter(LocalDateTime.now()))
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Appointment getAppointmentById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId).orElse(null);
    }
    
    @Transactional
    public boolean rescheduleAppointment(Long appointmentId, LocalDateTime newTime, String token) {
        try {
            // Extract patient ID from token
            Long patientId = tokenService.extractPatientId(token);
            
            // Find appointment
            Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
            if (appointmentOptional.isEmpty()) {
                return false;
            }
            
            Appointment appointment = appointmentOptional.get();
            
            // Verify patient owns the appointment
            if (!appointment.getPatient().getId().equals(patientId)) {
                return false;
            }
            
            // Check if new time is available
            boolean timeConflict = appointmentRepository.existsByDoctorIdAndAppointmentTime(
                appointment.getDoctor().getId(), newTime);
            
            if (timeConflict) {
                return false;
            }
            
            // Update appointment time
            appointment.setAppointmentTime(newTime);
            appointment.setEndTime(newTime.plusHours(1));
            appointmentRepository.save(appointment);
            
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}