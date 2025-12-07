package com.project.back_end.services;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenService {
    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    
    private static final String CLAIM_USER_TYPE = "userType";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USER_NAME = "userName";
    
    @Value("${jwt.secret:defaultSecretKeyForDevelopmentOnlyChangeInProduction}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}") // 默认24小时
    private long accessTokenExpiration;
    
    @Value("${jwt.refresh-expiration:604800000}") // 默认7天
    private long refreshTokenExpiration;
    
    public TokenService(AdminRepository adminRepository,
                       DoctorRepository doctorRepository,
                       PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    // ==================== 令牌生成 ====================
    
    public String generateAccessToken(String userType, String identifier, Long userId, String userName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_TYPE, userType);
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USER_NAME, userName);
        
        Date now = Date.from(Instant.now());
        Date expiryDate = Date.from(Instant.now().plusMillis(accessTokenExpiration));
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(identifier)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String generateRefreshToken(String identifier) {
        Date now = Date.from(Instant.now());
        Date expiryDate = Date.from(Instant.now().plusMillis(refreshTokenExpiration));
        
        return Jwts.builder()
                .setSubject(identifier)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    // 向后兼容的方法
    public String generateToken(String identifier) {
        // 为向后兼容，假设这是患者token
        return generateAccessToken("patient", identifier, null, null);
    }
    
    public String generateDoctorToken(Long doctorId, String email) {
        Doctor doctor = doctorRepository.findByEmail(email);
        return generateAccessToken("doctor", email, doctorId, 
            doctor != null ? doctor.getName() : null);
    }
    
    // ==================== 令牌解析 ====================
    
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            logger.warn("Token expired: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            logger.warn("Invalid token: {}", e.getMessage());
            throw new RuntimeException("Invalid token", e);
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            throw new RuntimeException("Token validation failed", e);
        }
    }
    
    public String extractIdentifier(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }
    
    public String getUserTypeFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get(CLAIM_USER_TYPE, String.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get(CLAIM_USER_ID, Long.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    public String getUserNameFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get(CLAIM_USER_NAME, String.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    // 向后兼容的方法
    public String extractEmailFromToken(String token) {
        return extractIdentifier(token);
    }
    
    // ==================== 令牌验证 ====================
    
    public boolean validateToken(String token, String expectedUserType) {
        try {
            String userType = getUserTypeFromToken(token);
            if (!StringUtils.hasText(userType) || !userType.equalsIgnoreCase(expectedUserType)) {
                return false;
            }
            
            String identifier = extractIdentifier(token);
            if (!StringUtils.hasText(identifier)) {
                return false;
            }
            
            // 验证用户是否存在
            return isUserExists(userType, identifier);
            
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean isUserExists(String userType, String identifier) {
        return switch (userType.toLowerCase()) {
            case "admin" -> adminRepository.findByUsername(identifier) != null;
            case "doctor" -> doctorRepository.findByEmail(identifier) != null;
            case "patient" -> patientRepository.findByEmail(identifier) != null;
            default -> false;
        };
    }
    
    public boolean isValidToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ==================== 令牌信息获取 ====================
    
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
    }
    
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }
    
    public Date getIssuedAtDateFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getIssuedAt();
        } catch (Exception e) {
            return null;
        }
    }
    
    // ==================== 令牌刷新 ====================
    
    public String refreshAccessToken(String refreshToken) {
        try {
            String identifier = extractIdentifier(refreshToken);
            if (!StringUtils.hasText(identifier)) {
                return null;
            }
            
            // 验证用户类型（这里需要存储用户类型，或者通过identifier推断）
            // 简化处理：使用原token的用户类型
            // 实际应该从数据库查询用户信息
            
            // 生成新的access token
            return generateToken(identifier);
            
        } catch (Exception e) {
            logger.error("Failed to refresh token: {}", e.getMessage());
            return null;
        }
    }
    
    // ==================== 辅助方法 ====================
    
    public Map<String, Object> getTokenInfo(String token) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            Claims claims = extractAllClaims(token);
            
            info.put("identifier", claims.getSubject());
            info.put("userType", claims.get(CLAIM_USER_TYPE));
            info.put("userId", claims.get(CLAIM_USER_ID));
            info.put("userName", claims.get(CLAIM_USER_NAME));
            info.put("issuedAt", claims.getIssuedAt());
            info.put("expiration", claims.getExpiration());
            info.put("valid", true);
            
        } catch (Exception e) {
            info.put("valid", false);
            info.put("error", e.getMessage());
        }
        
        return info;
    }
    
    public long getTokenValidityInSeconds(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            if (expiration == null) {
                return 0;
            }
            
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining / 1000);
            
        } catch (Exception e) {
            return 0;
        }
    }
}