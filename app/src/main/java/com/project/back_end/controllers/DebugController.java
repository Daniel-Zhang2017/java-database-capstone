package com.project.back_end.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.models.Admin;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.services.TokenService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private TokenService tokenService;
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkStatus() {
        Map<String, Object> response = new HashMap<>();
        
        // 检查Repository
        response.put("adminRepository", adminRepository != null ? "OK" : "NULL");
        
        // 检查TokenService
        response.put("tokenService", tokenService != null ? "OK" : "NULL");
        
        // 测试数据库查询
        try {
            long count = adminRepository.count();
            response.put("database_connection", "OK");
            response.put("total_admins", count);
        } catch (Exception e) {
            response.put("database_connection", "ERROR: " + e.getMessage());
        }
        
        // 测试生成token
        if (tokenService != null) {
            try {
                String token = tokenService.generateToken("test-user");
                response.put("token_generation", "OK");
                response.put("sample_token", token);
            } catch (Exception e) {
                response.put("token_generation", "ERROR: " + e.getMessage());
            }
        }
        
        return ResponseEntity.ok(response);
    }
}