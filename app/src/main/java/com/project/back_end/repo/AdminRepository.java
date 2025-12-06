package com.project.back_end.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.back_end.model.Admin;

// 1. Extend JpaRepository:
//    - The repository extends JpaRepository<Admin, Long>, which gives it basic CRUD functionality.
//    - The methods such as save, delete, update, and find are inherited without the need for explicit implementation.
//    - JpaRepository also includes pagination and sorting features.
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    // 2. Custom Query Method:
    //    - **findByUsername**:
    //      - This method allows you to find an Admin by their username.
    //      - Return type: Admin
    //      - Parameter: String username
    //      - It will return an Admin entity that matches the provided username.
    //      - If no Admin is found with the given username, it returns null.
    Admin findByUsername(String username);

    // Optional: Additional query methods can be added here as needed
    // For example:
    // - findByEmail(String email)
    // - findByUsernameAndPassword(String username, String password)
    // - findByRole(String role)
}