package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.services.TokenService;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// 1. **Add @Service Annotation**:
//    - This class should be annotated with `@Service` to indicate that it is a service layer class.
//    - The `@Service` annotation marks this class as a Spring-managed bean for business logic.
@Service
public class DoctorService {

    // 2. **Constructor Injection for Dependencies**:
    //    - The `DoctorService` class depends on `DoctorRepository`, `AppointmentRepository`, and `TokenService`.
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    // Time slot definitions
    private static final List<String> ALL_TIME_SLOTS = List.of(
        "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "12:00", "12:30", "13:00", "13:30", "14:00", "14:30",
        "15:00", "15:30", "16:00", "16:30", "17:00", "17:30"
    );

    // Constructor injection
    @Autowired
    public DoctorService(DoctorRepository doctorRepository,
                        AppointmentRepository appointmentRepository,
                        TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // 4. **getDoctorAvailability Method**:
    //    - Retrieves the available time slots for a specific doctor on a particular date and filters out already booked slots.
    @Transactional(readOnly = true)
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        List<String> availableSlots = new ArrayList<>(ALL_TIME_SLOTS);
        
        try {
            // Validate doctor exists
            Optional<Doctor> doctorOptional = doctorRepository.findById(doctorId);
            if (doctorOptional.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Calculate start and end of the day
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            
            // Get appointments for the doctor on the given date
            List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay);
            
            // Filter out booked time slots
            List<String> bookedSlots = appointments.stream()
                .map(appointment -> {
                    LocalTime appointmentTime = appointment.getAppointmentTime().toLocalTime();
                    return appointmentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                })
                .collect(Collectors.toList());
            
            availableSlots.removeAll(bookedSlots);
            
            // Also consider doctor's working hours (assuming 9 AM to 6 PM)
            // You can customize this based on doctor's specific working hours
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
        
        return availableSlots;
    }

    // 5. **saveDoctor Method**:
    //    - Used to save a new doctor record in the database after checking if a doctor with the same email already exists.
    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            // Check if doctor with same email already exists
            Doctor existingDoctor = doctorRepository.findByEmail(doctor.getEmail());
            if (existingDoctor != null) {
                return -1; // Doctor already exists
            }
            
            // Check if doctor with same phone already exists
            existingDoctor = doctorRepository.findByPhone(doctor.getPhone());
            if (existingDoctor != null) {
                return -1; // Phone number already registered
            }
            
            // Set default values if not provided
            if (doctor.getStatus() == null) {
                doctor.setStatus("active"); // Default status
            }
            
            // Save the doctor
            Doctor savedDoctor = doctorRepository.save(doctor);
            return savedDoctor != null ? 1 : 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Internal error
        }
    }

    // 6. **updateDoctor Method**:
    //    - Updates an existing doctor's details in the database. If the doctor doesn't exist, it returns `-1`.
    @Transactional
    public int updateDoctor(Doctor doctor) {
        try {
            // Check if doctor exists
            Optional<Doctor> existingDoctorOpt = doctorRepository.findById(doctor.getId());
            if (existingDoctorOpt.isEmpty()) {
                return -1; // Doctor not found
            }
            
            Doctor existingDoctor = existingDoctorOpt.get();
            
            // Check if email is being changed and if new email already exists
            if (!existingDoctor.getEmail().equals(doctor.getEmail())) {
                Doctor doctorWithNewEmail = doctorRepository.findByEmail(doctor.getEmail());
                if (doctorWithNewEmail != null) {
                    return -1; // New email already in use
                }
            }
            
            // Check if phone is being changed and if new phone already exists
            if (!existingDoctor.getPhone().equals(doctor.getPhone())) {
                Doctor doctorWithNewPhone = doctorRepository.findByPhone(doctor.getPhone());
                if (doctorWithNewPhone != null) {
                    return -1; // New phone already in use
                }
            }
            
            // Update doctor details
            existingDoctor.setName(doctor.getName());
            existingDoctor.setEmail(doctor.getEmail());
            existingDoctor.setPhone(doctor.getPhone());
            existingDoctor.setSpecialty(doctor.getSpecialty());
            existingDoctor.setQualification(doctor.getQualification());
            existingDoctor.setExperience(doctor.getExperience());
            existingDoctor.setLocation(doctor.getLocation());
            existingDoctor.setAvailableTimes(doctor.getAvailableTimes());
            existingDoctor.setStatus(doctor.getStatus());
            
            doctorRepository.save(existingDoctor);
            return 1;
            
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Internal error
        }
    }

    // 7. **getDoctors Method**:
    //    - Fetches all doctors from the database. It is marked with `@Transactional` to ensure that the collection is properly loaded.
    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        try {
            return doctorRepository.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // 8. **deleteDoctor Method**:
    //    - Deletes a doctor from the system along with all appointments associated with that doctor.
    @Transactional
    public int deleteDoctor(long id) {
        try {
            // Check if doctor exists
            Optional<Doctor> doctorOptional = doctorRepository.findById(id);
            if (doctorOptional.isEmpty()) {
                return -1; // Doctor not found
            }
            
            // Delete all appointments associated with this doctor
            appointmentRepository.deleteAllByDoctorId(id);
            
            // Delete the doctor
            doctorRepository.deleteById(id);
            
            return 1;
            
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Internal error
        }
    }

    // 9. **validateDoctor Method**:
    //    - Validates a doctor's login by checking if the email and password match an existing doctor record.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Find doctor by email
            Doctor doctor = doctorRepository.findByEmail(login.getIdentifier());
            if (doctor == null) {
                // Try case-insensitive search
                doctor = doctorRepository.findByEmailIgnoreCase(login.getIdentifier());
            }
            
            if (doctor == null) {
                response.put("error", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Check if doctor is active
            if ("inactive".equals(doctor.getStatus())) {
                response.put("error", "Doctor account is inactive");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Validate password (assuming password is stored as plain text or hashed)
            // In real application, you should use password encoder
            if (!doctor.getPassword().equals(login.getPassword())) {
                response.put("error", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Generate token
            String token = tokenService.generateDoctorToken(doctor.getId(), doctor.getEmail());
            
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("doctorId", String.valueOf(doctor.getId()));
            response.put("doctorName", doctor.getName());
            response.put("specialty", doctor.getSpecialty());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 10. **findDoctorByName Method**:
    //    - Finds doctors based on partial name matching and returns the list of doctors with their available times.
    @Transactional(readOnly = true)
    public Map<String, Object> findDoctorByName(String name) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Doctor> doctors;
            if (name == null || name.trim().isEmpty()) {
                doctors = doctorRepository.findAll();
            } else {
                doctors = doctorRepository.findByNameLike(name.trim());
            }
            
            response.put("doctors", doctors);
            response.put("count", doctors.size());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to search doctors: " + e.getMessage());
        }
        
        return response;
    }

    // 11. **filterDoctorsByNameSpecilityandTime Method**:
    //    - Filters doctors based on their name, specialty, and availability during a specific time (AM/PM).
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Doctor> doctors;
            
            if ((name == null || name.trim().isEmpty()) && (specialty == null || specialty.trim().isEmpty())) {
                doctors = doctorRepository.findAll();
            } else if (name != null && !name.trim().isEmpty() && specialty != null && !specialty.trim().isEmpty()) {
                doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name.trim(), specialty.trim());
            } else if (name != null && !name.trim().isEmpty()) {
                doctors = doctorRepository.findByNameContainingIgnoreCase(name.trim());
            } else {
                doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty.trim());
            }
            
            // Filter by time if specified
            if (amOrPm != null && !amOrPm.trim().isEmpty()) {
                doctors = filterDoctorByTime(doctors, amOrPm.trim());
            }
            
            response.put("doctors", doctors);
            response.put("count", doctors.size());
            response.put("filters", Map.of(
                "name", name != null ? name : "",
                "specialty", specialty != null ? specialty : "",
                "time", amOrPm != null ? amOrPm : ""
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to filter doctors: " + e.getMessage());
        }
        
        return response;
    }

    // 12. **filterDoctorByTime Method**:
    //    - Filters a list of doctors based on whether their available times match the specified time period (AM/PM).
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        if (doctors == null || doctors.isEmpty() || amOrPm == null || amOrPm.trim().isEmpty()) {
            return doctors;
        }
        
        return doctors.stream()
            .filter(doctor -> {
                List<String> availableTimes = doctor.getAvailableTimes();
                if (availableTimes == null || availableTimes.isEmpty()) {
                    return false;
                }
                
                // Filter based on AM/PM
                return availableTimes.stream()
                    .anyMatch(time -> {
                        if (time == null) return false;
                        
                        try {
                            int hour = Integer.parseInt(time.split(":")[0]);
                            
                            if ("AM".equalsIgnoreCase(amOrPm)) {
                                return hour < 12; // Morning hours (before 12 PM)
                            } else if ("PM".equalsIgnoreCase(amOrPm)) {
                                return hour >= 12; // Afternoon/Evening hours (12 PM and later)
                            }
                        } catch (NumberFormatException e) {
                            return false;
                        }
                        
                        return false;
                    });
            })
            .collect(Collectors.toList());
    }

    // 13. **filterDoctorByNameAndTime Method**:
    //    - Filters doctors based on their name and the specified time period (AM/PM).
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Doctor> doctors;
            
            if (name == null || name.trim().isEmpty()) {
                doctors = doctorRepository.findAll();
            } else {
                doctors = doctorRepository.findByNameContainingIgnoreCase(name.trim());
            }
            
            // Filter by time if specified
            if (amOrPm != null && !amOrPm.trim().isEmpty()) {
                doctors = filterDoctorByTime(doctors, amOrPm.trim());
            }
            
            response.put("doctors", doctors);
            response.put("count", doctors.size());
            response.put("filters", Map.of(
                "name", name != null ? name : "",
                "time", amOrPm != null ? amOrPm : ""
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to filter doctors: " + e.getMessage());
        }
        
        return response;
    }

    // 14. **filterDoctorByNameAndSpecility Method**:
    //    - Filters doctors by name and specialty.
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specialty) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Doctor> doctors;
            
            if ((name == null || name.trim().isEmpty()) && (specialty == null || specialty.trim().isEmpty())) {
                doctors = doctorRepository.findAll();
            } else if (name != null && !name.trim().isEmpty() && specialty != null && !specialty.trim().isEmpty()) {
                doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name.trim(), specialty.trim());
            } else if (name != null && !name.trim().isEmpty()) {
                doctors = doctorRepository.findByNameContainingIgnoreCase(name.trim());
            } else {
                doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty.trim());
            }
            
            response.put("doctors", doctors);
            response.put("count", doctors.size());
            response.put("filters", Map.of(
                "name", name != null ? name : "",
                "specialty", specialty != null ? specialty : ""
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to filter doctors: " + e.getMessage());
        }
        
        return response;
    }

    // 15. **filterDoctorByTimeAndSpecility Method**:
    //    - Filters doctors based on their specialty and availability during a specific time period (AM/PM).
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specialty, String amOrPm) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Doctor> doctors;
            
            if (specialty == null || specialty.trim().isEmpty()) {
                doctors = doctorRepository.findAll();
            } else {
                doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty.trim());
            }
            
            // Filter by time if specified
            if (amOrPm != null && !amOrPm.trim().isEmpty()) {
                doctors = filterDoctorByTime(doctors, amOrPm.trim());
            }
            
            response.put("doctors", doctors);
            response.put("count", doctors.size());
            response.put("filters", Map.of(
                "specialty", specialty != null ? specialty : "",
                "time", amOrPm != null ? amOrPm : ""
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to filter doctors: " + e.getMessage());
        }
        
        return response;
    }

    // 16. **filterDoctorBySpecility Method**:
    //    - Filters doctors based on their specialty.
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorBySpecility(String specialty) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Doctor> doctors;
            
            if (specialty == null || specialty.trim().isEmpty()) {
                doctors = doctorRepository.findAll();
            } else {
                doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty.trim());
            }
            
            response.put("doctors", doctors);
            response.put("count", doctors.size());
            response.put("specialty", specialty != null ? specialty : "");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to filter doctors: " + e.getMessage());
        }
        
        return response;
    }

    // 17. **filterDoctorsByTime Method**:
    //    - Filters all doctors based on their availability during a specific time period (AM/PM).
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Doctor> doctors = doctorRepository.findAll();
            
            // Filter by time if specified
            if (amOrPm != null && !amOrPm.trim().isEmpty()) {
                doctors = filterDoctorByTime(doctors, amOrPm.trim());
            }
            
            response.put("doctors", doctors);
            response.put("count", doctors.size());
            response.put("time", amOrPm != null ? amOrPm : "");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to filter doctors: " + e.getMessage());
        }
        
        return response;
    }

    // Additional useful methods
    
    @Transactional(readOnly = true)
    public Doctor getDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId).orElse(null);
    }
    
    @Transactional(readOnly = true)
    public List<Doctor> getActiveDoctors() {
        return doctorRepository.findAll().stream()
            .filter(doctor -> "active".equals(doctor.getStatus()))
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<String> getAllSpecialties() {
        return doctorRepository.findAll().stream()
            .map(Doctor::getSpecialty)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> searchDoctors(String name, String specialty, String location) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Doctor> doctors = doctorRepository.searchDoctors(name, specialty, location);
            response.put("doctors", doctors);
            response.put("count", doctors.size());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to search doctors: " + e.getMessage());
        }
        
        return response;
    }
    
    @Transactional
    public int updateDoctorStatus(Long doctorId, String status) {
        try {
            Optional<Doctor> doctorOptional = doctorRepository.findById(doctorId);
            if (doctorOptional.isEmpty()) {
                return -1;
            }
            
            Doctor doctor = doctorOptional.get();
            doctor.setStatus(status);
            doctorRepository.save(doctor);
            return 1;
            
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}