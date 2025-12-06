package com.project.back_end.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.back_end.model.Doctor;

import java.util.List;

// 1. Extend JpaRepository:
//    - The repository extends JpaRepository<Doctor, Long>, which gives it basic CRUD functionality.
//    - This allows the repository to perform operations like save, delete, update, and find without needing to implement these methods manually.
//    - JpaRepository also includes features like pagination and sorting.
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // 2. Custom Query Methods:

    //    - **findByEmail**:
    //      - This method retrieves a Doctor by their email.
    //      - Return type: Doctor
    //      - Parameters: String email
    Doctor findByEmail(String email);

    //    - **findByNameLike**:
    //      - This method retrieves a list of Doctors whose name contains the provided search string.
    //      - Uses CONCAT for flexible pattern matching with wildcards
    //      - Return type: List<Doctor>
    //      - Parameters: String name
    @Query("SELECT d FROM Doctor d WHERE d.name LIKE CONCAT('%', :name, '%')")
    List<Doctor> findByNameLike(@Param("name") String name);

    //    - **findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase**:
    //      - This method retrieves a list of Doctors where the name contains the search string (case-insensitive) 
    //        and the specialty matches exactly (case-insensitive).
    //      - It combines both fields for a more specific search.
    //      - Return type: List<Doctor>
    //      - Parameters: String name, String specialty
    @Query("SELECT d FROM Doctor d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND LOWER(d.specialty) = LOWER(:specialty)")
    List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
            @Param("name") String name,
            @Param("specialty") String specialty
    );

    //    - **findBySpecialtyIgnoreCase**:
    //      - This method retrieves a list of Doctors with the specified specialty, ignoring case sensitivity.
    //      - Return type: List<Doctor>
    //      - Parameters: String specialty
    @Query("SELECT d FROM Doctor d WHERE LOWER(d.specialty) = LOWER(:specialty)")
    List<Doctor> findBySpecialtyIgnoreCase(@Param("specialty") String specialty);

    // 3. Additional useful methods using Spring Data naming conventions:

    // Find doctor by email ignoring case
    Doctor findByEmailIgnoreCase(String email);

    // Find doctors by partial name match (case-insensitive) using naming convention
    List<Doctor> findByNameContainingIgnoreCase(String name);

    // Find doctors by exact name (case-sensitive)
    Doctor findByName(String name);

    // Find doctors by exact name ignoring case
    Doctor findByNameIgnoreCase(String name);

    // Find doctors by phone number
    Doctor findByPhone(String phone);

    // Check if a doctor exists with the given email
    boolean existsByEmail(String email);

    // Check if a doctor exists with the given email ignoring case
    boolean existsByEmailIgnoreCase(String email);

    // Find doctors by multiple specialties (exact match, case-insensitive)
    List<Doctor> findBySpecialtyInIgnoreCase(List<String> specialties);

    // Find doctors with pagination support
    // This is inherited from JpaRepository but can be customized
    // Example: Page<Doctor> findByNameContaining(String name, Pageable pageable);

    // 4. Complex query combining multiple filters
    @Query("SELECT d FROM Doctor d WHERE " +
           "(:name IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:specialty IS NULL OR LOWER(d.specialty) = LOWER(:specialty)) AND " +
           "(:location IS NULL OR LOWER(d.location) LIKE LOWER(CONCAT('%', :location, '%')))")
    List<Doctor> searchDoctors(
            @Param("name") String name,
            @Param("specialty") String specialty,
            @Param("location") String location
    );

    // 5. Count queries for statistics
    long countBySpecialtyIgnoreCase(String specialty);
    
    // Count active doctors (assuming there's an 'active' boolean field)
    // long countByActiveTrue();
}