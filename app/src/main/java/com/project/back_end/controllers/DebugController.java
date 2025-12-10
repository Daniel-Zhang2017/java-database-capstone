package com.project.back_end.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.project.back_end.models.Admin;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.services.TokenService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*")  // 添加跨域支持
public class DebugController {
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private TokenService tokenService;
    
    // 1. 系统状态检查
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
            
            // 显示数据库中的管理员用户
            List<Admin> allAdmins = adminRepository.findAll();
            if (!allAdmins.isEmpty()) {
                List<String> usernames = new java.util.ArrayList<>();
                for (Admin admin : allAdmins) {
                    usernames.add(admin.getUsername());
                }
                response.put("available_usernames", usernames);
            }
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
        
        // 添加simple-login端点信息
        response.put("simple_login_endpoint", "http://localhost:8080/api/debug/simple-login");
        response.put("simple_login_method", "POST");
        response.put("simple_login_content_type", "application/json");
        response.put("simple_login_example_request", "{\"username\":\"admin\",\"password\":\"admin123\"}");
        
        return ResponseEntity.ok(response);
    }
    
    // 2. 管理员登录接口（硬编码验证）
    @PostMapping("/simple-login")
    public ResponseEntity<Map<String, Object>> simpleLogin(@RequestBody Map<String, String> loginData) {
        System.out.println("=== [DebugController] 收到登录请求 ===");
        System.out.println("时间: " + new Date());
        System.out.println("数据: " + loginData);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (loginData == null) {
                response.put("error", "请求数据为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            String username = loginData.get("username");
            String password = loginData.get("password");
            
            System.out.println("用户名: '" + username + "'");
            System.out.println("密码: '" + password + "'");
            
            // 硬编码验证
            if ("admin".equals(username) && "admin123".equals(password)) {
                System.out.println("硬编码验证成功！");
                
                response.put("status", "success");
                response.put("message", "登录成功（硬编码验证）");
                response.put("token", "debug-jwt-token-" + System.currentTimeMillis() + "-" + username);
                response.put("username", username);
                response.put("role", "admin");
                response.put("timestamp", new Date());
                
                return ResponseEntity.ok(response);
            } else {
                System.out.println("硬编码验证失败");
                response.put("status", "error");
                response.put("message", "用户名或密码错误");
                response.put("suggestion", "使用: admin / admin123");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (Exception e) {
            System.err.println("登录异常: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "服务器错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // 3. 数据库验证的管理员登录
    @PostMapping("/database-login")
    public ResponseEntity<Map<String, Object>> databaseLogin(@RequestBody Map<String, String> loginData) {
        System.out.println("=== [DebugController] 数据库登录请求 ===");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (loginData == null) {
                response.put("error", "请求数据为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            String username = loginData.get("username");
            String password = loginData.get("password");
            
            System.out.println("查询用户: " + username);
            
            // 从数据库查询用户
            Admin admin = adminRepository.findByUsername(username);
            
            if (admin == null) {
                System.out.println("用户不存在: " + username);
                response.put("status", "error");
                response.put("message", "用户不存在");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            System.out.println("找到用户: " + admin.getUsername());
            System.out.println("数据库密码: '" + admin.getPassword() + "'");
            System.out.println("输入密码: '" + password + "'");
            
            // 验证密码
            if (admin.getPassword() != null && admin.getPassword().equals(password)) {
                System.out.println("数据库验证成功！");
                
                // 生成token
                String token = tokenService.generateToken(admin.getUsername());
                
                response.put("status", "success");
                response.put("message", "登录成功（数据库验证）");
                response.put("token", token);
                response.put("username", admin.getUsername());
                //response.put("user_id", admin.getId());
                response.put("timestamp", new Date());
                
                return ResponseEntity.ok(response);
            } else {
                System.out.println("密码不匹配");
                response.put("status", "error");
                response.put("message", "密码错误");
                
                // 修复语法错误：使用Map创建debug信息
                Map<String, Object> debugInfo = new HashMap<>();
                debugInfo.put("stored_password_length", admin.getPassword() != null ? admin.getPassword().length() : 0);
                debugInfo.put("input_password_length", password != null ? password.length() : 0);
                response.put("debug", debugInfo);
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (Exception e) {
            System.err.println("数据库登录异常: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "服务器错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // 4. 测试simple-login端点
    @GetMapping("/test-simple-login")
    public ResponseEntity<Map<String, Object>> testSimpleLogin() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 使用RestTemplate测试自己的端点
            RestTemplate restTemplate = new RestTemplate();
            
            // 准备请求数据
            Map<String, String> requestData = new HashMap<>();
            requestData.put("username", "admin");
            requestData.put("password", "admin123");
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestData, headers);
            
            // 发送请求到自己的simple-login端点
            ResponseEntity<Map> apiResponse = restTemplate.postForEntity(
                "http://localhost:8080/api/debug/simple-login", 
                request, 
                Map.class
            );
            
            response.put("test_endpoint", "/api/debug/simple-login");
            response.put("request_data", requestData);
            response.put("response_status", apiResponse.getStatusCodeValue());
            response.put("response_body", apiResponse.getBody());
            response.put("test_result", "SUCCESS");
            
        } catch (Exception e) {
            response.put("test_endpoint", "/api/debug/simple-login");
            response.put("test_result", "FAILED");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    // 5. 获取所有端点信息
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> listEndpoints() {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("GET /api/debug/status", "检查系统状态");
        endpoints.put("POST /api/debug/simple-login", "简单登录（硬编码验证）");
        endpoints.put("POST /api/debug/database-login", "数据库验证登录");
        endpoints.put("GET /api/debug/test-simple-login", "测试simple-login端点");
        endpoints.put("GET /api/debug/endpoints", "查看所有可用端点");
        endpoints.put("GET /api/debug/database", "查看数据库数据");
        
        response.put("available_endpoints", endpoints);
        response.put("note", "POST端点需要使用application/json格式");
        response.put("example_request", "{\"username\":\"admin\",\"password\":\"admin123\"}");
        
        return ResponseEntity.ok(response);
    }
    
    // 6. 查看数据库数据
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> showDatabaseData() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Admin> allAdmins = adminRepository.findAll();
            
            if (allAdmins.isEmpty()) {
                response.put("message", "数据库中没有管理员数据");
                response.put("suggestion", "请先通过数据库工具添加管理员账户");
            } else {
                List<Map<String, Object>> adminList = new java.util.ArrayList<>();
                
                for (Admin admin : allAdmins) {
                    Map<String, Object> adminInfo = new HashMap<>();
                    //adminInfo.put("id", admin.getId());
                    adminInfo.put("username", admin.getUsername());
                    adminInfo.put("password", admin.getPassword());
                    adminInfo.put("password_length", admin.getPassword() != null ? admin.getPassword().length() : 0);
                    adminList.add(adminInfo);
                }
                
                response.put("admins", adminList);
                response.put("total_count", allAdmins.size());
                response.put("recommendation", "使用第一个用户的用户名进行登录测试");
            }
            
        } catch (Exception e) {
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    // 7. 健康检查端点
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "医院管理系统后端");
        response.put("timestamp", new Date());
        response.put("version", "1.0");
        return ResponseEntity.ok(response);
    }
    
    // 8. 测试GET方式登录（方便浏览器直接测试）
    @GetMapping("/login-test")
    public ResponseEntity<Map<String, Object>> loginTest(
            @RequestParam(required = false, defaultValue = "admin") String username,
            @RequestParam(required = false, defaultValue = "admin123") String password) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("method", "GET");
        response.put("endpoint", "/api/debug/login-test");
        response.put("username", username);
        response.put("password", password);
        response.put("timestamp", new Date());
        
        // 硬编码验证
        if ("admin".equals(username) && "admin123".equals(password)) {
            response.put("status", "success");
            response.put("message", "登录成功（GET方式测试）");
            response.put("token", "get-test-token-" + System.currentTimeMillis());
        } else {
            response.put("status", "error");
            response.put("message", "用户名或密码错误");
        }
        
        return ResponseEntity.ok(response);
    }
}