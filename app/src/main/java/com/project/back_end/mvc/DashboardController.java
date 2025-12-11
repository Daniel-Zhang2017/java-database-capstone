package com.project.back_end.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.back_end.services.TokenService;

@Controller
public class DashboardController {
    
    @Autowired
    private TokenService tokenService;

    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        System.out.println("\n\n");
        System.out.println("#############################################");
        System.out.println("# ADMIN DASHBOARD CONTROLLER CALLED! #");
        System.out.println("#############################################");
        System.out.println("TOKEN received: " + token);
        System.out.println("Time: " + java.time.LocalDateTime.now());
        
        try {
            System.out.println("Calling tokenService.validateToken...");
            boolean isValid = tokenService.validateToken(token, "admin");
            System.out.println("VALIDATION RESULT: " + isValid);
            
            if (isValid) {
                System.out.println("✓ SUCCESS - Returning admin/adminDashboard");
                System.out.println("#############################################\n\n");
                return "admin/adminDashboard";
            } else {
                System.out.println("✗ FAILED - Token invalid, redirecting to /");
                System.out.println("#############################################\n\n");
                return "redirect:/";
            }
        } catch (Exception e) {
            System.out.println("!!! EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            System.out.println("#############################################\n\n");
            return "redirect:/";
        }
    }

    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        System.out.println("\n\n");
        System.out.println("#############################################");
        System.out.println("# DOCTOR DASHBOARD CONTROLLER CALLED! #");
        System.out.println("#############################################");
        System.out.println("TOKEN received: " + token);
        System.out.println("Time: " + java.time.LocalDateTime.now());
        
        try {
            System.out.println("Calling tokenService.validateToken...");
            boolean isValid = tokenService.validateToken(token, "doctor");
            System.out.println("VALIDATION RESULT: " + isValid);
            
            if (isValid) {
                System.out.println("✓ SUCCESS - Returning doctor/doctorDashboard");
                System.out.println("#############################################\n\n");
                return "doctor/doctorDashboard";
            } else {
                System.out.println("✗ FAILED - Token invalid, redirecting to /");
                System.out.println("#############################################\n\n");
                return "redirect:/";
            }
        } catch (Exception e) {
            System.out.println("!!! EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            System.out.println("#############################################\n\n");
            return "redirect:/";
        }
    }
}