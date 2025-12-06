package com.project.back_end.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.back_end.model.Patient;

// 1. Extend JpaRepository:
//    - The repository extends JpaRepository<Patient, Long>, which provides basic CRUD functionality.
//    - This allows the repository to perform operations like save, delete, update, and find without needing to implement these methods manually.
//    - JpaRepository also includes features like pagination and sorting.
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // 2. Custom Query Methods:

    //    - **findByEmail**:
    //      - This method retrieves a Patient by their email address.
    //      - Return type: Patient
    //      - Parameters: String email
    Patient findByEmail(String email);

    //    - **findByEmailOrPhone**:
    //      - This method retrieves a Patient by either their email or phone number, allowing flexibility for the search.
    //      - Return type: Patient
    //      - Parameters: String email, String phone
    Patient findByEmailOrPhone(String email, String phone);

    // 3. Additional useful methods using Spring Data naming conventions:

    // Find patient by email ignoring case
    Patient findByEmailIgnoreCase(String email);

    // Find patient by phone number
    Patient findByPhone(String phone);

    // Find patient by phone number ignoring formatting variations
    Patient findByPhoneContaining(String phoneSegment);

    // Find patients by name (exact match)
    Patient findByName(String name);

    // Find patients by partial name match (case-insensitive)
    Patient findByNameIgnoreCase(String name);

    // Find patients by partial name containing search term
    Patient findByNameContainingIgnoreCase(String name);

    // Find patients by address containing search term
    Patient findByAddressContainingIgnoreCase(String address);

    // Check if patient exists by email
    boolean existsByEmail(String email);

    // Check if patient exists by email ignoring case
    boolean existsByEmailIgnoreCase(String email);

    // Check if patient exists by phone number
    boolean existsByPhone(String phone);

    // Check if patient exists by either email or phone
    boolean existsByEmailOrPhone(String email, String phone);

    // Find patients by email or name (for broader search)
    Patient findByEmailOrName(String email, String name);

    // Find patients by email or name ignoring case
    Patient findByEmailIgnoreCaseOrNameIgnoreCase(String email, String name);

    // Find all patients ordered by name
    Patient findAllByOrderByNameAsc();

    // Find patients by city (assuming address contains city information)
    // If you have a separate city field, you can use: findByCity(String city)
    Patient findByAddressContaining(String city);

    // Find patients with upcoming appointments (assuming there's a relationship with appointments)
    // This would require a join query, but for simple cases:
    // @Query("SELECT p FROM Patient p WHERE EXISTS (SELECT a FROM Appointment a WHERE a.patient.id = p.id AND a.status = 0)")
    // List<Patient> findPatientsWithUpcomingAppointments();

    // Count patients by registration month/year (for analytics)
    // @Query("SELECT COUNT(p) FROM Patient p WHERE MONTH(p.registrationDate) = :month AND YEAR(p.registrationDate) = :year")
    // long countByRegistrationMonthAndYear(@Param("month") int month, @Param("year") int year);

    // 4. Complex query for comprehensive patient search
    // @Query("SELECT p FROM Patient p WHERE " +
    //        "(:email IS NULL OR p.email = :email) AND " +
    //        "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
    //        "(:phone IS NULL OR p.phone LIKE CONCAT('%', :phone, '%'))")
    // List<Patient> searchPatients(
    //        @Param("email") String email,
    //        @Param("name") String name,
    //        @Param("phone") String phone
    // );
}