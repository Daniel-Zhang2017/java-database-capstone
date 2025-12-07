package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentService {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);
    
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService;
    private final DoctorService doctorService;
    
    // 预约状态枚举
    public enum AppointmentStatus {
        SCHEDULED(1, "已预约"),
        CONFIRMED(2, "已确认"),
        COMPLETED(3, "已完成"),
        CANCELLED(4, "已取消"),
        NO_SHOW(5, "未到场");
        
        private final int code;
        private final String description;
        
        AppointmentStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public int getCode() { return code; }
        public String getDescription() { return description; }
        
        public static AppointmentStatus fromCode(int code) {
            return Arrays.stream(values())
                    .filter(status -> status.code == code)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("无效的状态码: " + code));
        }
    }
    
    // 预约常量
    private static final int MIN_ADVANCE_HOURS = 2;
    private static final int APPOINTMENT_DURATION_MINUTES = 60;
    private static final int CONFLICT_BUFFER_MINUTES = 30;
    
    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                            PatientRepository patientRepository,
                            DoctorRepository doctorRepository,
                            TokenService tokenService,
                            DoctorService doctorService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
        this.doctorService = doctorService;
    }
    
    // ==================== 核心业务方法 ====================
    
    /**
     * 预约挂号
     */
    @Transactional
    public ResponseEntity<Map<String, Object>> bookAppointment(AppointmentDTO appointmentDTO) {
        try {
            // 1. 验证令牌
            String token = appointmentDTO.getToken();
            if (!tokenService.validateToken(token, "patient")) {
                return errorResponse("无效或未授权的令牌", HttpStatus.UNAUTHORIZED);
            }
            
            // 2. 获取患者信息
            Long patientId = tokenService.getUserIdFromToken(token);
            if (patientId == null) {
                return errorResponse("无法获取患者信息", HttpStatus.BAD_REQUEST);
            }
            
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (patientOpt.isEmpty()) {
                return errorResponse("患者不存在", HttpStatus.NOT_FOUND);
            }
            
            Patient patient = patientOpt.get();
            
            // 3. 验证预约数据
            ValidationResult validation = validateAppointmentData(appointmentDTO);
            if (!validation.isValid()) {
                return validation.getErrorResponse();
            }
            
            // 4. 获取医生信息
            Optional<Doctor> doctorOpt = doctorRepository.findById(appointmentDTO.getDoctorId());
            if (doctorOpt.isEmpty()) {
                return errorResponse("医生不存在", HttpStatus.NOT_FOUND);
            }
            
            Doctor doctor = doctorOpt.get();
            
            // 5. 检查预约时间可用性
            LocalDateTime appointmentTime = appointmentDTO.getAppointmentTime();
            if (!isTimeSlotAvailable(doctor.getId(), appointmentTime)) {
                return conflictResponse("医生在该时间段不可用", 
                    doctorService.getDoctorAvailability(doctor.getId(), appointmentTime.toLocalDate()));
            }
            
            // 6. 创建预约
            Appointment appointment = createAppointment(appointmentDTO, patient, doctor);
            
            Appointment savedAppointment = appointmentRepository.save(appointment);
            
            // 7. 构建成功响应
            return successResponse("预约成功", Map.of(
                "appointmentId", savedAppointment.getId(),
                "appointmentTime", savedAppointment.getAppointmentTime(),
                "status", savedAppointment.getStatus(),
                "doctorName", doctor.getName(),
                "patientName", patient.getName(),
                "confirmationNumber", generateConfirmationNumber(savedAppointment)
            ), HttpStatus.CREATED);
            
        } catch (Exception e) {
            logger.error("预约失败", e);
            return errorResponse("预约失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // ==================== 查询方法 ====================
    
    /**
     * 获取患者预约列表
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientAppointments(String token, 
                                                                     AppointmentQuery query) {
        try {
            // 验证患者令牌
            if (!tokenService.validateToken(token, "patient")) {
                return errorResponse("无效的令牌", HttpStatus.UNAUTHORIZED);
            }
            
            Long patientId = tokenService.getUserIdFromToken(token);
            if (patientId == null) {
                return errorResponse("无法获取患者信息", HttpStatus.BAD_REQUEST);
            }
            
            // 构建查询条件
            List<Appointment> appointments = findAppointments(patientId, null, query);
            
            // 统计数据
            AppointmentStatistics stats = calculateStatistics(appointments);
            
            return successResponse("获取成功", Map.of(
                "appointments", appointments.stream()
                    .map(this::convertToAppointmentResponse)
                    .collect(Collectors.toList()),
                "totalCount", appointments.size(),
                "statistics", stats,
                "patientId", patientId
            ));
            
        } catch (Exception e) {
            logger.error("获取预约列表失败", e);
            return errorResponse("获取失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 获取医生预约列表
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getDoctorAppointments(String token,
                                                                    AppointmentQuery query) {
        try {
            // 验证医生令牌
            if (!tokenService.validateToken(token, "doctor")) {
                return errorResponse("无效的令牌", HttpStatus.UNAUTHORIZED);
            }
            
            Long doctorId = tokenService.getUserIdFromToken(token);
            if (doctorId == null) {
                return errorResponse("无法获取医生信息", HttpStatus.BAD_REQUEST);
            }
            
            // 构建查询条件
            List<Appointment> appointments = findAppointments(null, doctorId, query);
            
            // 统计数据
            AppointmentStatistics stats = calculateStatistics(appointments);
            
            return successResponse("获取成功", Map.of(
                "appointments", appointments.stream()
                    .map(this::convertToAppointmentResponse)
                    .collect(Collectors.toList()),
                "totalCount", appointments.size(),
                "statistics", stats,
                "doctorId", doctorId
            ));
            
        } catch (Exception e) {
            logger.error("获取医生预约列表失败", e);
            return errorResponse("获取失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // ==================== 预约管理 ====================
    
    /**
     * 取消预约
     */
    @Transactional
    public ResponseEntity<Map<String, Object>> cancelAppointment(Long appointmentId, String token, String reason) {
        try {
            // 验证权限
            AuthorizationResult auth = authorizeAppointmentAccess(appointmentId, token, "patient");
            if (!auth.isAuthorized()) {
                return auth.getErrorResponse();
            }
            
            Appointment appointment = auth.getAppointment();
            
            // 检查是否可以取消
            if (!isCancellable(appointment)) {
                return errorResponse("该预约无法取消", HttpStatus.BAD_REQUEST);
            }
            
            // 更新状态
            appointment.setStatus(AppointmentStatus.CANCELLED.getCode());
            appointment.setCancellationReason(reason);
            appointment.setCancellationTime(LocalDateTime.now());
            
            appointmentRepository.save(appointment);
            
            return successResponse("预约已取消", Map.of(
                "appointmentId", appointmentId,
                "cancellationTime", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("取消预约失败", e);
            return errorResponse("取消失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 确认预约
     */
    @Transactional
    public ResponseEntity<Map<String, Object>> confirmAppointment(Long appointmentId, String token) {
        try {
            // 验证权限（医生可以确认）
            AuthorizationResult auth = authorizeAppointmentAccess(appointmentId, token, "doctor");
            if (!auth.isAuthorized()) {
                return auth.getErrorResponse();
            }
            
            Appointment appointment = auth.getAppointment();
            
            // 检查是否可以确认
            if (!isConfirmable(appointment)) {
                return errorResponse("该预约无法确认", HttpStatus.BAD_REQUEST);
            }
            
            // 更新状态
            appointment.setStatus(AppointmentStatus.CONFIRMED.getCode());
            appointmentRepository.save(appointment);
            
            return successResponse("预约已确认", Map.of(
                "appointmentId", appointmentId,
                "status", AppointmentStatus.CONFIRMED.getCode()
            ));
            
        } catch (Exception e) {
            logger.error("确认预约失败", e);
            return errorResponse("确认失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 重新安排预约
     */
    @Transactional
    public ResponseEntity<Map<String, Object>> rescheduleAppointment(Long appointmentId, 
                                                                   RescheduleRequest request) {
        try {
            // 验证权限
            AuthorizationResult auth = authorizeAppointmentAccess(appointmentId, request.getToken(), "patient");
            if (!auth.isAuthorized()) {
                return auth.getErrorResponse();
            }
            
            Appointment appointment = auth.getAppointment();
            
            // 检查是否可以重新安排
            if (!isReschedulable(appointment)) {
                return errorResponse("该预约无法重新安排", HttpStatus.BAD_REQUEST);
            }
            
            // 验证新时间
            ValidationResult timeValidation = validateNewAppointmentTime(
                request.getNewTime(), appointment.getDoctor().getId());
            if (!timeValidation.isValid()) {
                return timeValidation.getErrorResponse();
            }
            
            // 更新预约时间
            appointment.setAppointmentTime(request.getNewTime());
            appointment.setEndTime(request.getNewTime().plusMinutes(APPOINTMENT_DURATION_MINUTES));
            appointmentRepository.save(appointment);
            
            return successResponse("预约已重新安排", Map.of(
                "appointmentId", appointmentId,
                "newAppointmentTime", request.getNewTime()
            ));
            
        } catch (Exception e) {
            logger.error("重新安排预约失败", e);
            return errorResponse("重新安排失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // ==================== 验证方法 ====================
    
    private ValidationResult validateAppointmentData(AppointmentDTO dto) {
        if (dto == null) {
            return ValidationResult.error("预约数据不能为空", HttpStatus.BAD_REQUEST);
        }
        
        if (dto.getDoctorId() == null) {
            return ValidationResult.error("医生ID不能为空", HttpStatus.BAD_REQUEST);
        }
        
        if (dto.getAppointmentTime() == null) {
            return ValidationResult.error("预约时间不能为空", HttpStatus.BAD_REQUEST);
        }
        
        // 检查预约时间是否在未来
        if (dto.getAppointmentTime().isBefore(LocalDateTime.now().plusHours(MIN_ADVANCE_HOURS))) {
            return ValidationResult.error(String.format("预约必须至少提前%d小时", MIN_ADVANCE_HOURS), 
                HttpStatus.BAD_REQUEST);
        }
        
        return ValidationResult.valid();
    }
    
    private boolean isTimeSlotAvailable(Long doctorId, LocalDateTime appointmentTime) {
        LocalDateTime startWindow = appointmentTime.minusMinutes(CONFLICT_BUFFER_MINUTES);
        LocalDateTime endWindow = appointmentTime.plusMinutes(CONFLICT_BUFFER_MINUTES);
        
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(
            doctorId, startWindow, endWindow, AppointmentStatus.CANCELLED.getCode());
        
        return conflicts.isEmpty();
    }
    
    // ==================== 辅助方法 ====================
    
    private Appointment createAppointment(AppointmentDTO dto, Patient patient, Doctor doctor) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentTime(dto.getAppointmentTime());
        appointment.setEndTime(dto.getAppointmentTime().plusMinutes(APPOINTMENT_DURATION_MINUTES));
        appointment.setStatus(AppointmentStatus.SCHEDULED.getCode());
        appointment.setCondition(dto.getCondition());
        appointment.setNotes(dto.getNotes());
        appointment.setCreatedAt(LocalDateTime.now());
        return appointment;
    }
    
    private String generateConfirmationNumber(Appointment appointment) {
        return String.format("APT-%d-%s-%06d",
            appointment.getDoctor().getId(),
            appointment.getAppointmentTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
            appointment.getId());
    }
    
    private boolean isCancellable(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.CANCELLED.getCode() ||
            appointment.getStatus() == AppointmentStatus.COMPLETED.getCode()) {
            return false;
        }
        
        // 不能在预约开始前太短时间取消
        return appointment.getAppointmentTime()
            .isAfter(LocalDateTime.now().plusHours(MIN_ADVANCE_HOURS));
    }
    
    private boolean isConfirmable(Appointment appointment) {
        return appointment.getStatus() == AppointmentStatus.SCHEDULED.getCode();
    }
    
    private boolean isReschedulable(Appointment appointment) {
        return appointment.getStatus() == AppointmentStatus.SCHEDULED.getCode() ||
               appointment.getStatus() == AppointmentStatus.CONFIRMED.getCode();
    }
    
    // ==================== DTO和内部类 ====================
    
    // 预约数据DTO
    public static class AppointmentDTO {
        private String token;
        private Long doctorId;
        private LocalDateTime appointmentTime;
        private String condition;
        private String notes;
        
        // getters and setters
    }
    
    // 查询条件DTO
    public static class AppointmentQuery {
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer status;
        private String searchKeyword;
        private String sortBy;
        private boolean ascending = true;
        
        // getters and setters
    }
    
    // 重新安排请求DTO
    public static class RescheduleRequest {
        private String token;
        private LocalDateTime newTime;
        
        // getters and setters
    }
    
    // 响应DTO
    public static class AppointmentResponse {
        private Long id;
        private LocalDateTime appointmentTime;
        private LocalDateTime endTime;
        private Integer status;
        private String statusDescription;
        private String doctorName;
        private String doctorSpecialty;
        private String patientName;
        private String condition;
        private String notes;
        private String confirmationNumber;
        
        // getters and setters
    }
    
    // 统计信息
    public static class AppointmentStatistics {
        private long total;
        private long scheduled;
        private long confirmed;
        private long completed;
        private long cancelled;
        private long noShow;
        private double completionRate;
        private double cancellationRate;
        
        // getters and setters
    }
    
    // 验证结果
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final HttpStatus errorStatus;
        
        private ValidationResult(boolean valid, String errorMessage, HttpStatus errorStatus) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.errorStatus = errorStatus;
        }
        
        static ValidationResult valid() {
            return new ValidationResult(true, null, null);
        }
        
        static ValidationResult error(String message, HttpStatus status) {
            return new ValidationResult(false, message, status);
        }
        
        boolean isValid() { return valid; }
        
        ResponseEntity<Map<String, Object>> getErrorResponse() {
            return errorResponse(errorMessage, errorStatus);
        }
    }
    
    // 授权结果
    private class AuthorizationResult {
        private final boolean authorized;
        private final Appointment appointment;
        private final String errorMessage;
        private final HttpStatus errorStatus;
        
        private AuthorizationResult(boolean authorized, Appointment appointment, 
                                   String errorMessage, HttpStatus errorStatus) {
            this.authorized = authorized;
            this.appointment = appointment;
            this.errorMessage = errorMessage;
            this.errorStatus = errorStatus;
        }
        
        boolean isAuthorized() { return authorized; }
        Appointment getAppointment() { return appointment; }
        
        ResponseEntity<Map<String, Object>> getErrorResponse() {
            return errorResponse(errorMessage, errorStatus);
        }
    }
    
    private AuthorizationResult authorizeAppointmentAccess(Long appointmentId, String token, String requiredRole) {
        // 验证令牌
        if (!tokenService.validateToken(token, requiredRole)) {
            return new AuthorizationResult(false, null, "未授权的访问", HttpStatus.UNAUTHORIZED);
        }
        
        // 查找预约
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            return new AuthorizationResult(false, null, "预约不存在", HttpStatus.NOT_FOUND);
        }
        
        Appointment appointment = appointmentOpt.get();
        Long userId = tokenService.getUserIdFromToken(token);
        
        // 验证用户权限
        boolean hasAccess = switch (requiredRole) {
            case "patient" -> appointment.getPatient().getId().equals(userId);
            case "doctor" -> appointment.getDoctor().getId().equals(userId);
            case "admin" -> true; // 管理员有所有权限
            default -> false;
        };
        
        if (!hasAccess) {
            return new AuthorizationResult(false, null, "没有访问权限", HttpStatus.FORBIDDEN);
        }
        
        return new AuthorizationResult(true, appointment, null, null);
    }
    
    // ==================== 响应构建器 ====================
    
    private ResponseEntity<Map<String, Object>> successResponse(String message, Map<String, Object> data) {
        return successResponse(message, data, HttpStatus.OK);
    }
    
    private ResponseEntity<Map<String, Object>> successResponse(String message, 
                                                               Map<String, Object> data,
                                                               HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return ResponseEntity.status(status).body(response);
    }
    
    private ResponseEntity<Map<String, Object>> errorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(response);
    }
    
    private ResponseEntity<Map<String, Object>> conflictResponse(String message, 
                                                                List<String> availableSlots) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("availableSlots", availableSlots);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    // 其他辅助方法（需要实现）
    private List<Appointment> findAppointments(Long patientId, Long doctorId, AppointmentQuery query) {
        // 实现查询逻辑
        return Collections.emptyList();
    }
    
    private AppointmentStatistics calculateStatistics(List<Appointment> appointments) {
        // 实现统计逻辑
        return new AppointmentStatistics();
    }
    
    private AppointmentResponse convertToAppointmentResponse(Appointment appointment) {
        // 实现转换逻辑
        return new AppointmentResponse();
    }
}