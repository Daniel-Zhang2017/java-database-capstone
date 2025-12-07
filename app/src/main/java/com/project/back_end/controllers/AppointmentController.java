package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.MainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final MainService service;

    // 1. Set Up the Controller Class:
    //    - Annotate the class with `@RestController` to define it as a REST API controller.
    //    - Use `@RequestMapping("/appointments")` to set a base path for all appointment-related endpoints.
    //    - This centralizes all routes that deal with booking, updating, retrieving, and canceling appointments.

    // 2. Autowire Dependencies:
    //    - Inject `AppointmentService` for handling the business logic specific to appointments.
    //    - Inject the general `Service` class, which provides shared functionality like token validation and appointment checks.
    @Autowired
    public AppointmentController(AppointmentService appointmentService, MainService service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

    // 3. Define the `getAppointments` Method:
    //    - Handles HTTP GET requests to fetch appointments based on date and patient name.
    //    - Takes the appointment date, patient name, and token as path variables.
    //    - First validates the token for role `"doctor"` using the `Service`.
    //    - If the token is valid, returns appointments for the given patient on the specified date.
    //    - If the token is invalid or expired, responds with the appropriate message and status code.
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<?> getAppointments(
            @PathVariable String date,
            @PathVariable String patientName,
            @PathVariable String token) {
        
        // Validate doctor token
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "doctor");
        if (validationResponse.getStatusCode() != HttpStatus.OK) {
            return validationResponse;
        }
        
        // Get appointments
        try {
            Map<String, Object> appointments = appointmentService.getAppointments(date, patientName);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve appointments: " + e.getMessage()));
        }
    }

    // 4. Define the `bookAppointment` Method:
    //    - Handles HTTP POST requests to create a new appointment.
    //    - Accepts a validated `Appointment` object in the request body and a token as a path variable.
    //    - Validates the token for the `"patient"` role.
    //    - Uses service logic to validate the appointment data (e.g., check for doctor availability and time conflicts).
    //    - Returns success if booked, or appropriate error messages if the doctor ID is invalid or the slot is already taken.
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> bookAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment) {
        
        // Validate patient token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        // Validate appointment data
        int validationResult = service.validateAppointment(appointment);
        
        if (validationResult == -1) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Doctor does not exist"));
        } else if (validationResult == 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Appointment time is unavailable"));
        }
        
        // Book the appointment
        try {
            Appointment bookedAppointment = appointmentService.bookAppointment(appointment);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "Appointment booked successfully",
                            "appointmentId", String.valueOf(bookedAppointment.getId()),
                            "status", "BOOKED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to book appointment: " + e.getMessage()));
        }
    }

    // 5. Define the `updateAppointment` Method:
    //    - Handles HTTP PUT requests to modify an existing appointment.
    //    - Accepts a validated `Appointment` object and a token as input.
    //    - Validates the token for `"patient"` role.
    //    - Delegates the update logic to the `AppointmentService`.
    //    - Returns an appropriate success or failure response based on the update result.
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment) {
        
        // Validate patient token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        // Validate appointment data if it's being rescheduled
        if (appointment.getAppointmentDate() != null || appointment.getAppointmentTime() != null) {
            int validationResult = service.validateAppointment(appointment);
            
            if (validationResult == -1) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Doctor does not exist"));
            } else if (validationResult == 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Appointment time is unavailable"));
            }
        }
        
        // Update the appointment
        try {
            boolean updated = appointmentService.updateAppointment(appointment);
            if (updated) {
                return ResponseEntity.ok(Map.of("message", "Appointment updated successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Appointment not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update appointment: " + e.getMessage()));
        }
    }

    // 6. Define the `cancelAppointment` Method:
    //    - Handles HTTP DELETE requests to cancel a specific appointment.
    //    - Accepts the appointment ID and a token as path variables.
    //    - Validates the token for `"patient"` role to ensure the user is authorized to cancel the appointment.
    //    - Calls `AppointmentService` to handle the cancellation process and returns the result.
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> cancelAppointment(
            @PathVariable Long id,
            @PathVariable String token) {
        
        // Validate patient token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        // Cancel the appointment
        try {
            boolean cancelled = appointmentService.cancelAppointment(id);
            if (cancelled) {
                return ResponseEntity.ok(Map.of("message", "Appointment cancelled successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Appointment not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cancel appointment: " + e.getMessage()));
        }
    }

    // Additional endpoint: Get all appointments for a specific patient (for patient dashboard)
    @GetMapping("/patient/{patientId}/{token}")
    public ResponseEntity<?> getPatientAppointments(
            @PathVariable Long patientId,
            @PathVariable String token) {
        
        // Validate patient token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            Map<String, Object> appointments = appointmentService.getAppointmentsByPatientId(patientId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve patient appointments: " + e.getMessage()));
        }
    }

    // Additional endpoint: Get all appointments for a specific doctor (for doctor dashboard)
    @GetMapping("/doctor/{doctorId}/{token}")
    public ResponseEntity<?> getDoctorAppointments(
            @PathVariable Long doctorId,
            @PathVariable String token) {
        
        // Validate doctor token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            Map<String, Object> appointments = appointmentService.getAppointmentsByDoctorId(doctorId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve doctor appointments: " + e.getMessage()));
        }
    }
}