package com.project.back_end.controllers;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.MainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("${api.path}" + "prescription")
public class PrescriptionController {
    
    private final PrescriptionService prescriptionService;
    private final MainService service;

    @Autowired
    public PrescriptionController(PrescriptionService prescriptionService, MainService service) {
        this.prescriptionService = prescriptionService;
        this.service = service;
    }

    // 保存处方
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> savePrescription(
            @PathVariable String token,
            @RequestBody Prescription prescription) {
        
        // 验证医生token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // 提取医生邮箱用于验证
            String doctorEmail = service.extractEmailFromToken(token);
            if (doctorEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token"));
            }
            
            // 注意：需要实现validateDoctorForPrescription方法或在Service中添加
            // 暂时注释掉，直到Service中实现该方法
            
            // Save the prescription using service method
            ResponseEntity<Map<String, String>> response = prescriptionService.savePrescription(prescription);
            
            // 如果保存成功，可以更新相关状态（如果需要）
            if (response.getStatusCode() == HttpStatus.CREATED) {
                // 这里可以添加额外的逻辑，比如更新appointment状态
            }
            
            return response;
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to save prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 根据预约ID获取处方（医生访问）
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescription(
            @PathVariable Long appointmentId,
            @PathVariable String token) {
        
        // 验证医生token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // 提取医生邮箱用于验证
            String doctorEmail = service.extractEmailFromToken(token);
            if (doctorEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token"));
            }
            
            // 注意：需要实现验证逻辑
            // 暂时直接调用Service方法
            
            ResponseEntity<Map<String, Object>> response = prescriptionService.getPrescription(appointmentId);
            return response;
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 患者查看自己的处方
    @GetMapping("/patient/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescriptionForPatient(
            @PathVariable Long appointmentId,
            @PathVariable String token) {
        
        // 验证患者token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // 提取患者邮箱
            String patientEmail = service.extractEmailFromToken(token);
            if (patientEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token"));
            }
            
            // 注意：需要实现患者验证逻辑
            // 暂时直接调用Service方法
            
            ResponseEntity<Map<String, Object>> response = prescriptionService.getPrescription(appointmentId);
            return response;
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 更新处方
    @PutMapping("/{prescriptionId}/{token}")
    public ResponseEntity<Map<String, String>> updatePrescription(
            @PathVariable String prescriptionId,
            @PathVariable String token,
            @RequestBody Prescription updatedPrescription) {
        
        // 验证医生token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // 提取医生邮箱用于验证
            String doctorEmail = service.extractEmailFromToken(token);
            if (doctorEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token"));
            }
            
            // 使用Service中的正确方法签名
            ResponseEntity<Map<String, String>> response = prescriptionService.updatePrescription(prescriptionId, updatedPrescription);
            return response;
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 根据医生ID获取处方列表
    @GetMapping("/doctor/{doctorId}/{token}")
    public ResponseEntity<?> getDoctorPrescriptions(
            @PathVariable Long doctorId,
            @PathVariable String token) {
        
        // 验证医生token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            // 提取医生邮箱用于验证
            String doctorEmail = service.extractEmailFromToken(token);
            if (doctorEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token"));
            }
            
            ResponseEntity<Map<String, Object>> response = prescriptionService.getPrescriptionsByDoctorId(doctorId);
            return response;
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve prescriptions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 根据患者ID获取处方列表
    @GetMapping("/patient/list/{patientId}/{token}")
    public ResponseEntity<?> getPatientPrescriptions(
            @PathVariable Long patientId,
            @PathVariable String token) {
        
        // 验证患者token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            ResponseEntity<Map<String, Object>> response = prescriptionService.getPrescriptionsByPatientId(patientId);
            return response;
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve prescriptions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 删除处方
    @DeleteMapping("/{prescriptionId}/{token}")
    public ResponseEntity<Map<String, String>> deletePrescription(
            @PathVariable String prescriptionId,
            @PathVariable String token) {
        
        // 验证医生token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            ResponseEntity<Map<String, String>> response = prescriptionService.deletePrescription(prescriptionId);
            return response;
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to delete prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 搜索处方
    @GetMapping("/search/{token}")
    public ResponseEntity<?> searchPrescriptions(
            @PathVariable String token,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId) {
        
        // 验证token（医生或患者都可以搜索）
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            // 如果不是医生，尝试验证患者token
            tokenValidation = service.validateToken(token, "patient");
            if (tokenValidation.getStatusCode() != HttpStatus.OK) {
                return tokenValidation;
            }
        }
        
        try {
            ResponseEntity<Map<String, Object>> response = prescriptionService.searchPrescriptions(searchTerm, patientId, doctorId);
            return response;
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to search prescriptions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 获取有效处方
    @GetMapping("/active/{patientId}/{token}")
    public ResponseEntity<?> getActivePrescriptions(
            @PathVariable Long patientId,
            @PathVariable String token) {
        
        // 验证患者token
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation;
        }
        
        try {
            ResponseEntity<Map<String, Object>> response = prescriptionService.getActivePrescriptions(patientId);
            return response;
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve active prescriptions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 健康检查
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "Prescription service is running"));
    }

    // 新端点：批量获取处方状态
    @GetMapping("/batch-status/{token}")
    public ResponseEntity<?> getBatchPrescriptionStatus(
            @PathVariable String token,
            @RequestParam("ids") String prescriptionIds) {
        
        // 验证token（医生或患者）
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            tokenValidation = service.validateToken(token, "patient");
            if (tokenValidation.getStatusCode() != HttpStatus.OK) {
                return tokenValidation;
            }
        }
        
        try {
            // 这里可以添加批量查询逻辑
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Batch status check endpoint");
            response.put("note", "需要实现具体的批量查询逻辑");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to check batch status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}