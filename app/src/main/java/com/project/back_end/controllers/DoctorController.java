package com.project.back_end.controllers;

import com.project.back_end.models.Doctor;
import com.project.back_end.DTO.Login;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.MainService;
import com.project.back_end.repo.doctorRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("${api.path}" + "doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final MainService service;

    @Autowired
    public DoctorController(DoctorService doctorService, MainService service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    // 获取医生可用时间
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<?> getDoctorAvailability(
            @PathVariable String user,
            @PathVariable Long doctorId,
            @PathVariable String date,
            @PathVariable String token) {
        
        // 验证token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, user);
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // 解析日期
            LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
            
            // 调用Service方法
            List<String> availability = doctorService.getDoctorAvailability(doctorId, parsedDate);
            
            if (availability.isEmpty()) {
                // 检查医生是否存在
                Doctor doctor = doctorService.getDoctorById(doctorId);
                if (doctor == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Doctor not found"));
                }
                return ResponseEntity.ok(Map.of(
                        "message", "No available slots for the selected date",
                        "availableSlots", List.of()
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                    "availableSlots", availability,
                    "doctorId", doctorId,
                    "date", date,
                    "totalSlots", availability.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch doctor availability: " + e.getMessage()));
        }
    }

    // 获取所有医生列表
    @GetMapping
    public ResponseEntity<?> getDoctors() {
        try {
            // 调用Service方法获取医生列表
            List<Doctor> doctors = doctorService.getDoctors();
            
            if (doctors.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "No doctors found",
                        "doctors", List.of(),
                        "count", 0
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                    "doctors", doctors,
                    "count", doctors.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve doctors: " + e.getMessage()));
        }
    }

    // 添加新医生（管理员权限）
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> saveDoctor(
            @PathVariable String token,
            @RequestBody Doctor doctor) {
        
        // 验证管理员token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "admin");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // 检查医生是否已存在
            Doctor existingDoctorByEmail = doctorRepository.findByEmail(doctor.getEmail());
            if (existingDoctorByEmail != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Doctor with this email already exists"));
            }
            
            Doctor existingDoctorByPhone = doctorRepository.findByPhone(doctor.getPhone());
            if (existingDoctorByPhone != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Doctor with this phone number already exists"));
            }
            
            // 调用Service方法保存医生
            int result = doctorService.saveDoctor(doctor);
            
            if (result == 1) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("message", "Doctor added successfully"));
            } else if (result == -1) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Doctor already exists"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to add doctor"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save doctor: " + e.getMessage()));
        }
    }

    // 医生登录
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> doctorLogin(@RequestBody Login login) {
        try {
            // 调用Service方法进行登录验证
            ResponseEntity<Map<String, String>> response = doctorService.validateDoctor(login);
            return response;
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed: " + e.getMessage()));
        }
    }

    // 更新医生信息（管理员权限）
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateDoctor(
            @PathVariable String token,
            @RequestBody Doctor doctor) {
        
        // 验证管理员token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "admin");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // 调用Service方法更新医生
            int result = doctorService.updateDoctor(doctor);
            
            if (result == 1) {
                return ResponseEntity.ok(Map.of("message", "Doctor updated successfully"));
            } else if (result == -1) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Doctor not found or duplicate email/phone"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update doctor"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update doctor: " + e.getMessage()));
        }
    }

    // 删除医生（管理员权限）
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> deleteDoctor(
            @PathVariable Long id,
            @PathVariable String token) {
        
        // 验证管理员token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "admin");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // 调用Service方法删除医生
            int result = doctorService.deleteDoctor(id);
            
            if (result == 1) {
                return ResponseEntity.ok(Map.of("message", "Doctor deleted successfully"));
            } else if (result == -1) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Doctor not found with id " + id));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to delete doctor"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete doctor: " + e.getMessage()));
        }
    }

    // 筛选医生
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<?> filterDoctors(
            @PathVariable(required = false) String name,
            @PathVariable(required = false) String time,
            @PathVariable(required = false) String speciality) {
        
        try {
            // 处理可选参数
            String nameParam = "null".equalsIgnoreCase(name) ? null : name;
            String timeParam = "null".equalsIgnoreCase(time) ? null : time;
            String specialityParam = "null".equalsIgnoreCase(speciality) ? null : speciality;
            
            // 调用Service方法筛选医生
            Map<String, Object> filteredDoctors = doctorService.filterDoctorsByNameSpecilityandTime(
                    nameParam, specialityParam, timeParam);
            
            if (filteredDoctors.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(filteredDoctors);
            }
            
            return ResponseEntity.ok(filteredDoctors);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to filter doctors: " + e.getMessage()));
        }
    }

    // 通过ID获取医生详情
    @GetMapping("/{id}/{token}")
    public ResponseEntity<?> getDoctorById(
            @PathVariable Long id,
            @PathVariable String token) {
        
        // 验证token（管理员或医生本人）
        ResponseEntity<Map<String, String>> adminValidation = service.validateToken(token, "admin");
        ResponseEntity<Map<String, String>> doctorValidation = service.validateToken(token, "doctor");
        
        if (adminValidation.getStatusCode() != HttpStatus.OK && doctorValidation.getStatusCode() != HttpStatus.OK) {
            // 检查是否是医生在访问自己的信息
            String email = service.extractEmailFromToken(token);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized access"));
            }
            
            // 验证医生是否是访问自己的信息
            Doctor doctor = doctorRepository.findByEmail(email);
            if (doctor == null || !doctor.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }
        }
        
        try {
            // 调用Service方法获取医生
            Doctor doctor = doctorService.getDoctorById(id);
            
            if (doctor == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Doctor not found"));
            }
            
            return ResponseEntity.ok(Map.of(
                    "doctor", doctor,
                    "message", "Doctor retrieved successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve doctor: " + e.getMessage()));
        }
    }

    // 医生更新自己的个人资料
    @PutMapping("/profile/{token}")
    public ResponseEntity<Map<String, String>> updateDoctorProfile(
            @PathVariable String token,
            @RequestBody Doctor doctor) {
        
        // 提取医生邮箱
        String email = service.extractEmailFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
        
        try {
            // 查找当前医生
            Doctor currentDoctor = doctorRepository.findByEmail(email);
            if (currentDoctor == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Doctor not found"));
            }
            
            // 确保只能更新自己的信息
            doctor.setId(currentDoctor.getId());
            
            // 更新医生信息
            int result = doctorService.updateDoctor(doctor);
            
            if (result == 1) {
                return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
            } else if (result == -1) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Email or phone already in use"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update profile"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update profile: " + e.getMessage()));
        }
    }

    // 搜索医生
    @GetMapping("/search/{token}")
    public ResponseEntity<?> searchDoctors(
            @PathVariable String token,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String location) {
        
        // 验证token（任何人都可以搜索医生）
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            tokenValidation = service.validateToken(token, "doctor");
            if (tokenValidation.getStatusCode() != HttpStatus.OK) {
                tokenValidation = service.validateToken(token, "admin");
                if (tokenValidation.getStatusCode() != HttpStatus.OK) {
                    return tokenValidation;
                }
            }
        }
        
        try {
            // 调用Service方法搜索医生
            Map<String, Object> searchResult = doctorService.searchDoctors(name, specialty, location);
            
            if (searchResult.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(searchResult);
            }
            
            return ResponseEntity.ok(searchResult);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to search doctors: " + e.getMessage()));
        }
    }

    // 获取活跃医生列表
    @GetMapping("/active/{token}")
    public ResponseEntity<?> getActiveDoctors(@PathVariable String token) {
        
        // 验证token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            tokenValidation = service.validateToken(token, "doctor");
            if (tokenValidation.getStatusCode() != HttpStatus.OK) {
                tokenValidation = service.validateToken(token, "admin");
                if (tokenValidation.getStatusCode() != HttpStatus.OK) {
                    return tokenValidation;
                }
            }
        }
        
        try {
            // 调用Service方法获取活跃医生
            List<Doctor> activeDoctors = doctorService.getActiveDoctors();
            
            return ResponseEntity.ok(Map.of(
                    "doctors", activeDoctors,
                    "count", activeDoctors.size(),
                    "message", "Active doctors retrieved successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve active doctors: " + e.getMessage()));
        }
    }

    // 获取所有专业列表
    @GetMapping("/specialties/{token}")
    public ResponseEntity<?> getAllSpecialties(@PathVariable String token) {
        
        // 验证token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            tokenValidation = service.validateToken(token, "doctor");
            if (tokenValidation.getStatusCode() != HttpStatus.OK) {
                tokenValidation = service.validateToken(token, "admin");
                if (tokenValidation.getStatusCode() != HttpStatus.OK) {
                    return tokenValidation;
                }
            }
        }
        
        try {
            // 调用Service方法获取专业列表
            List<String> specialties = doctorService.getAllSpecialties();
            
            return ResponseEntity.ok(Map.of(
                    "specialties", specialties,
                    "count", specialties.size(),
                    "message", "Specialties retrieved successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve specialties: " + e.getMessage()));
        }
    }

    // 健康检查
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "Doctor service is running"));
    }

    // 更新医生状态（管理员权限）
    @PutMapping("/status/{doctorId}/{status}/{token}")
    public ResponseEntity<Map<String, String>> updateDoctorStatus(
            @PathVariable Long doctorId,
            @PathVariable String status,
            @PathVariable String token) {
        
        // 验证管理员token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "admin");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // 调用Service方法更新状态
            int result = doctorService.updateDoctorStatus(doctorId, status);
            
            if (result == 1) {
                return ResponseEntity.ok(Map.of(
                        "message", "Doctor status updated successfully",
                        "doctorId", String.valueOf(doctorId),
                        "status", status
                ));
            } else if (result == -1) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Doctor not found"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update doctor status"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update doctor status: " + e.getMessage()));
        }
    }
}