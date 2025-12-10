package com.project.back_end.services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class TokenService {

    @Value("${jwt.secret}")
    private String secret;

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    
    public TokenService(AdminRepository adminRepository, 
                       DoctorRepository doctorRepository, 
                       PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    // Return type changed to SecretKey to fix verifyWith(...) issue
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 原方法 - 只根据邮箱生成token
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7))
                .signWith(getSigningKey()) // clean & modern
                .compact();
    }    

    /**
     * 新增方法 - 根据用户类型和用户名生成token
     * @param userType 用户类型: "admin", "doctor", "patient"
     * @param username 用户名/邮箱
     * @return JWT token字符串
     */
    public String generateToken(String userType, String username) {
        // 验证参数
        if (userType == null || username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户类型和用户名不能为空");
        }
        
        // 验证用户类型是否合法
        if (!userType.equals("admin") && !userType.equals("doctor") && !userType.equals("patient")) {
            throw new IllegalArgumentException("用户类型必须是: admin, doctor 或 patient");
        }
        
        // 验证用户是否存在
        if (!userExists(userType, username)) {
            throw new IllegalArgumentException("用户不存在: " + userType + " - " + username);
        }
        
        // 在JWT中添加用户类型作为自定义声明
        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", userType);
        
        return Jwts.builder()
                .subject(username)
                .claims(claims) // 添加自定义声明
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7)) // 7天有效期
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * 重载方法 - 接受三个参数（兼容您的调用方式）
     * 第一个参数可以忽略，第二个参数是用户类型，第三个参数是用户名
     */
    public String generateToken(Object ignored, String userType, String username) {
        // 忽略第一个参数，直接调用两个参数的方法
        return generateToken(userType, username);
    }
    
    /**
     * 检查用户是否存在
     */
    private boolean userExists(String userType, String username) {
        switch (userType) {
            case "admin":
                Admin admin = adminRepository.findByUsername(username);
                return admin != null;
            case "doctor":
                Doctor doctor = doctorRepository.findByEmail(username);
                return doctor != null;
            case "patient":
                Patient patient = patientRepository.findByEmail(username);
                return patient != null;
            default:
                return false;
        }
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // No more error now
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    
    /**
     * 新增方法 - 从token中提取用户类型
     */
    public String extractUserType(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userType", String.class);
    }

    public boolean validateToken(String token, String user) {
        try {
            String extracted = extractEmail(token);
            if(user.equals("admin")) {
                Admin admin = adminRepository.findByUsername(extracted);
                return admin != null;
            } else if(user.equals("doctor")) {
                Doctor doctor = doctorRepository.findByEmail(extracted);
                return doctor != null;
            } else if(user.equals("patient")) {
                Patient patient = patientRepository.findByEmail(extracted);
                return patient != null;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 新增方法 - 增强版token验证，自动从token中提取用户类型
     */
    public boolean validateTokenEnhanced(String token) {
        try {
            String username = extractEmail(token);
            String userType = extractUserType(token);
            
            if (userType == null) {
                // 如果token中没有userType声明，回退到旧的验证逻辑
                return validateToken(token, "admin") || 
                       validateToken(token, "doctor") || 
                       validateToken(token, "patient");
            }
            
            // 根据token中的用户类型进行验证
            switch (userType) {
                case "admin":
                    Admin admin = adminRepository.findByUsername(username);
                    return admin != null;
                case "doctor":
                    Doctor doctor = doctorRepository.findByEmail(username);
                    return doctor != null;
                case "patient":
                    Patient patient = patientRepository.findByEmail(username);
                    return patient != null;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}