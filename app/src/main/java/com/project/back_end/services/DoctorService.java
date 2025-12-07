package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {
    private static final Logger logger = LoggerFactory.getLogger(DoctorService.class);
    
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;
    
    // Time slot definitions
    private static final List<String> ALL_TIME_SLOTS = List.of(
        "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "12:00", "12:30", "13:00", "13:30", "14:00", "14:30",
        "15:00", "15:30", "16:00", "16:30", "17:00", "17:30"
    );
    
    // Status constants
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_INACTIVE = "inactive";
    
    // Response codes
    public static final int SUCCESS = 1;
    public static final int FAILURE = 0;
    public static final int DOCTOR_NOT_FOUND = -1;
    public static final int DOCTOR_ALREADY_EXISTS = -2;
    
    @Autowired
    public DoctorService(DoctorRepository doctorRepository,
                        AppointmentRepository appointmentRepository,
                        TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }
    
    // ==================== Doctor CRUD Operations ====================
    
    /**
     * Get all doctors
     */
    @Transactional(readOnly = true)
    public List<Doctor> getAllDoctors() {
        try {
            return doctorRepository.findAll();
        } catch (Exception e) {
            logger.error("Error fetching all doctors", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get doctor by ID
     */
    @Transactional(readOnly = true)
    public Optional<Doctor> getDoctorById(Long doctorId) {
        try {
            return doctorRepository.findById(doctorId);
        } catch (Exception e) {
            logger.error("Error fetching doctor by ID: {}", doctorId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Create new doctor
     */
    @Transactional
    public int createDoctor(Doctor doctor) {
        try {
            // Check if doctor with same email already exists
            if (doctorRepository.existsByEmail(doctor.getEmail())) {
                return DOCTOR_ALREADY_EXISTS;
            }
            
            // Check if doctor with same phone already exists
            if (doctorRepository.existsByPhone(doctor.getPhone())) {
                return DOCTOR_ALREADY_EXISTS;
            }
            
            // Set default values
            if (!StringUtils.hasText(doctor.getStatus())) {
                doctor.setStatus(STATUS_ACTIVE);
            }
            
            Doctor savedDoctor = doctorRepository.save(doctor);
            return savedDoctor != null ? SUCCESS : FAILURE;
            
        } catch (Exception e) {
            logger.error("Error creating doctor", e);
            return FAILURE;
        }
    }
    
    /**
     * Update existing doctor
     */
    @Transactional
    public int updateDoctor(Doctor doctor) {
        try {
            // Check if doctor exists
            Optional<Doctor> existingDoctorOpt = doctorRepository.findById(doctor.getId());
            if (existingDoctorOpt.isEmpty()) {
                return DOCTOR_NOT_FOUND;
            }
            
            Doctor existingDoctor = existingDoctorOpt.get();
            
            // Validate email uniqueness if changed
            if (!existingDoctor.getEmail().equals(doctor.getEmail()) && 
                doctorRepository.existsByEmail(doctor.getEmail())) {
                return DOCTOR_ALREADY_EXISTS;
            }
            
            // Validate phone uniqueness if changed
            if (!existingDoctor.getPhone().equals(doctor.getPhone()) && 
                doctorRepository.existsByPhone(doctor.getPhone())) {
                return DOCTOR_ALREADY_EXISTS;
            }
            
            // Update doctor fields
            updateDoctorFields(existingDoctor, doctor);
            doctorRepository.save(existingDoctor);
            return SUCCESS;
            
        } catch (Exception e) {
            logger.error("Error updating doctor: {}", doctor.getId(), e);
            return FAILURE;
        }
    }
    
    /**
     * Delete doctor
     */
    @Transactional
    public int deleteDoctor(Long doctorId) {
        try {
            // Check if doctor exists
            if (!doctorRepository.existsById(doctorId)) {
                return DOCTOR_NOT_FOUND;
            }
            
            // Delete all appointments associated with this doctor
            appointmentRepository.deleteByDoctorId(doctorId);
            
            // Delete the doctor
            doctorRepository.deleteById(doctorId);
            return SUCCESS;
            
        } catch (Exception e) {
            logger.error("Error deleting doctor: {}", doctorId, e);
            return FAILURE;
        }
    }
    
    /**
     * Update doctor status
     */
    @Transactional
    public int updateDoctorStatus(Long doctorId, String status) {
        try {
            Optional<Doctor> doctorOptional = doctorRepository.findById(doctorId);
            if (doctorOptional.isEmpty()) {
                return DOCTOR_NOT_FOUND;
            }
            
            Doctor doctor = doctorOptional.get();
            doctor.setStatus(status);
            doctorRepository.save(doctor);
            return SUCCESS;
            
        } catch (Exception e) {
            logger.error("Error updating doctor status: {}", doctorId, e);
            return FAILURE;
        }
    }
    
    // ==================== Search and Filter Operations ====================
    
    /**
     * Search doctors with multiple criteria
     */
    @Transactional(readOnly = true)
    public List<Doctor> searchDoctors(String name, String specialty, String timePeriod) {
        try {
            // Start with all doctors
            List<Doctor> doctors = doctorRepository.findAll();
            
            // Apply filters
            doctors = filterDoctors(doctors, name, specialty, timePeriod);
            
            return doctors;
            
        } catch (Exception e) {
            logger.error("Error searching doctors", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Filter doctors with all criteria (for backward compatibility)
     */
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorsByNameSpecialityAndTime(String name, String specialty, String timePeriod) {
        return searchDoctors(name, specialty, timePeriod);
    }
    
    /**
     * Filter doctors by name and specialty
     */
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorsByNameAndSpeciality(String name, String specialty) {
        return searchDoctors(name, specialty, null);
    }
    
    /**
     * Filter doctors by name and time
     */
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorsByNameAndTime(String name, String timePeriod) {
        return searchDoctors(name, null, timePeriod);
    }
    
    /**
     * Filter doctors by specialty and time
     */
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorsBySpecialityAndTime(String specialty, String timePeriod) {
        return searchDoctors(null, specialty, timePeriod);
    }
    
    /**
     * Filter doctors by name only
     */
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorsByName(String name) {
        try {
            if (!StringUtils.hasText(name)) {
                return doctorRepository.findAll();
            }
            return doctorRepository.findByNameContainingIgnoreCase(name.trim());
        } catch (Exception e) {
            logger.error("Error filtering doctors by name: {}", name, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Filter doctors by specialty only
     */
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorsBySpeciality(String specialty) {
        try {
            if (!StringUtils.hasText(specialty)) {
                return doctorRepository.findAll();
            }
            return doctorRepository.findBySpecialtyIgnoreCase(specialty.trim());
        } catch (Exception e) {
            logger.error("Error filtering doctors by specialty: {}", specialty, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Filter doctors by time period only
     */
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorsByTime(String timePeriod) {
        try {
            List<Doctor> doctors = doctorRepository.findAll();
            return filterByTimePeriod(doctors, timePeriod);
        } catch (Exception e) {
            logger.error("Error filtering doctors by time: {}", timePeriod, e);
            return Collections.emptyList();
        }
    }
    
    // ==================== Availability Management ====================
    
    /**
     * Get doctor availability for a specific date
     */
    @Transactional(readOnly = true)
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        try {
            // Validate doctor exists
            if (!doctorRepository.existsById(doctorId)) {
                return Collections.emptyList();
            }
            
            List<String> availableSlots = new ArrayList<>(ALL_TIME_SLOTS);
            
            // Get appointments for the doctor on the given date
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            
            List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay);
            
            // Filter out booked time slots
            List<String> bookedSlots = appointments.stream()
                .map(appointment -> appointment.getAppointmentTime().toLocalTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm")))
                .collect(Collectors.toList());
            
            availableSlots.removeAll(bookedSlots);
            
            return availableSlots;
            
        } catch (Exception e) {
            logger.error("Error getting doctor availability: doctorId={}, date={}", doctorId, date, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Check if time slot is available for a doctor
     */
    @Transactional(readOnly = true)
    public boolean isTimeSlotAvailable(Long doctorId, LocalDate date, String timeSlot) {
        List<String> availableSlots = getDoctorAvailability(doctorId, date);
        return availableSlots.contains(timeSlot);
    }
    
    // ==================== Authentication ====================
    
    /**
     * Validate doctor login
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> validateDoctorLogin(Login login) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Find doctor by email (case-insensitive)
            Doctor doctor = doctorRepository.findByEmailIgnoreCase(login.getIdentifier());
            if (doctor == null) {
                response.put("error", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Check if doctor is active
            if (STATUS_INACTIVE.equals(doctor.getStatus())) {
                response.put("error", "Doctor account is inactive");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Validate password (TODO: Implement password encryption)
            if (!doctor.getPassword().equals(login.getPassword())) {
                response.put("error", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Generate token
            String token = tokenService.generateDoctorToken(doctor.getId(), doctor.getEmail());
            
            // Build successful response
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("doctorId", doctor.getId());
            response.put("doctorName", doctor.getName());
            response.put("specialty", doctor.getSpecialty());
            response.put("email", doctor.getEmail());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during doctor login", e);
            response.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Generic filter method
     */
    private List<Doctor> filterDoctors(List<Doctor> doctors, String name, String specialty, String timePeriod) {
        if (doctors == null || doctors.isEmpty()) {
            return Collections.emptyList();
        }
        
        return doctors.stream()
            .filter(doctor -> filterByName(doctor, name))
            .filter(doctor -> filterBySpecialty(doctor, specialty))
            .filter(doctor -> filterByTimePeriod(doctor, timePeriod))
            .collect(Collectors.toList());
    }
    
    private boolean filterByName(Doctor doctor, String name) {
        if (!StringUtils.hasText(name)) {
            return true;
        }
        return doctor.getName().toLowerCase().contains(name.toLowerCase());
    }
    
    private boolean filterBySpecialty(Doctor doctor, String specialty) {
        if (!StringUtils.hasText(specialty)) {
            return true;
        }
        return doctor.getSpecialty().equalsIgnoreCase(specialty);
    }
    
    private boolean filterByTimePeriod(Doctor doctor, String timePeriod) {
        if (!StringUtils.hasText(timePeriod)) {
            return true;
        }
        List<String> availableTimes = doctor.getAvailableTimes();
        if (availableTimes == null || availableTimes.isEmpty()) {
            return false;
        }
        
        return availableTimes.stream().anyMatch(time -> isTimeInPeriod(time, timePeriod));
    }
    
    private List<Doctor> filterByTimePeriod(List<Doctor> doctors, String timePeriod) {
        return doctors.stream()
            .filter(doctor -> filterByTimePeriod(doctor, timePeriod))
            .collect(Collectors.toList());
    }
    
    private boolean isTimeInPeriod(String time, String period) {
        try {
            int hour = Integer.parseInt(time.split(":")[0]);
            
            if ("AM".equalsIgnoreCase(period)) {
                return hour < 12;
            } else if ("PM".equalsIgnoreCase(period)) {
                return hour >= 12;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Update doctor fields
     */
    private void updateDoctorFields(Doctor existingDoctor, Doctor newData) {
        if (StringUtils.hasText(newData.getName())) {
            existingDoctor.setName(newData.getName());
        }
        if (StringUtils.hasText(newData.getEmail())) {
            existingDoctor.setEmail(newData.getEmail());
        }
        if (StringUtils.hasText(newData.getPhone())) {
            existingDoctor.setPhone(newData.getPhone());
        }
        if (StringUtils.hasText(newData.getSpecialty())) {
            existingDoctor.setSpecialty(newData.getSpecialty());
        }
        if (StringUtils.hasText(newData.getQualification())) {
            existingDoctor.setQualification(newData.getQualification());
        }
        if (newData.getExperience() > 0) {
            existingDoctor.setExperience(newData.getExperience());
        }
        if (StringUtils.hasText(newData.getLocation())) {
            existingDoctor.setLocation(newData.getLocation());
        }
        if (newData.getAvailableTimes() != null) {
            existingDoctor.setAvailableTimes(newData.getAvailableTimes());
        }
        if (StringUtils.hasText(newData.getStatus())) {
            existingDoctor.setStatus(newData.getStatus());
        }
    }
    
    // ==================== Additional Helper Methods ====================
    
    /**
     * Get all active doctors
     */
    @Transactional(readOnly = true)
    public List<Doctor> getActiveDoctors() {
        try {
            return doctorRepository.findByStatus(STATUS_ACTIVE);
        } catch (Exception e) {
            logger.error("Error fetching active doctors", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all unique specialties
     */
    @Transactional(readOnly = true)
    public List<String> getAllSpecialties() {
        try {
            return doctorRepository.findAll().stream()
                .map(Doctor::getSpecialty)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching specialties", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Search doctors with advanced criteria
     */
    @Transactional(readOnly = true)
    public List<Doctor> searchDoctorsAdvanced(String name, String specialty, String location, 
                                             Integer minExperience, Integer maxExperience) {
        try {
            return doctorRepository.findAll().stream()
                .filter(doctor -> filterByName(doctor, name))
                .filter(doctor -> filterBySpecialty(doctor, specialty))
                .filter(doctor -> filterByLocation(doctor, location))
                .filter(doctor -> filterByExperience(doctor, minExperience, maxExperience))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error in advanced doctor search", e);
            return Collections.emptyList();
        }
    }
    
    private boolean filterByLocation(Doctor doctor, String location) {
        if (!StringUtils.hasText(location)) {
            return true;
        }
        return doctor.getLocation() != null && 
               doctor.getLocation().toLowerCase().contains(location.toLowerCase());
    }
    
    private boolean filterByExperience(Doctor doctor, Integer minExperience, Integer maxExperience) {
        if (minExperience != null && doctor.getExperience() < minExperience) {
            return false;
        }
        if (maxExperience != null && doctor.getExperience() > maxExperience) {
            return false;
        }
        return true;
    }
}