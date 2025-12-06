package com.project.back_end.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

@Controller
public class DashboardController {

    // 2. Autowire the Shared Service:
    //    - Inject the common `Service` class, which provides the token validation logic used to authorize access to dashboards.
    @Autowired
    private Service service;

    // 3. Define the `adminDashboard` Method:
    //    - Handles HTTP GET requests to `/adminDashboard/{token}`.
    //    - Accepts an admin's token as a path variable.
    //    - Validates the token using the shared service for the `"admin"` role.
    //    - If the token is valid (i.e., no errors returned), forwards the user to the `"admin/adminDashboard"` view.
    //    - If invalid, redirects to the root URL, likely the login or home page.
    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        Map<String, String> validationErrors = service.validateToken(token, "admin");
        
        if (validationErrors.isEmpty()) {
            return "admin/adminDashboard";
        } else {
            return "redirect:http://localhost:8080";
        }
    }

    // 4. Define the `doctorDashboard` Method:
    //    - Handles HTTP GET requests to `/doctorDashboard/{token}`.
    //    - Accepts a doctor's token as a path variable.
    //    - Validates the token using the shared service for the `"doctor"` role.
    //    - If the token is valid, forwards the user to the `"doctor/doctorDashboard"` view.
    //    - If the token is invalid, redirects to the root URL.
    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        Map<String, String> validationErrors = service.validateToken(token, "doctor");
        
        if (validationErrors.isEmpty()) {
            return "doctor/doctorDashboard";
        } else {
            return "redirect:http://localhost:8080";
        }
    }
}