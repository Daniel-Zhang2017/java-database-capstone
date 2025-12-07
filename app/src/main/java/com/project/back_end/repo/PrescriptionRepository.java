package com.project.back_end.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.project.back_end.models.Prescription;

import java.util.List;

// 1. Extend MongoRepository:
//    - The repository extends MongoRepository<Prescription, String>, which provides basic CRUD functionality for MongoDB.
//    - This allows the repository to perform operations like save, delete, update, and find without needing to implement these methods manually.
//    - MongoRepository is tailored for working with MongoDB, unlike JpaRepository which is used for relational databases.
@Repository
public interface PrescriptionRepository extends MongoRepository<Prescription, String> {

    // 2. Custom Query Method:

    //    - **findByAppointmentId**:
    //      - This method retrieves a list of prescriptions associated with a specific appointment.
    //      - Return type: List<Prescription>
    //      - Parameters: Long appointmentId
    //      - MongoRepository automatically derives the query from the method name, in this case, it will find prescriptions by the appointment ID.
    List<Prescription> findByAppointmentId(Long appointmentId);

    // 3. Additional useful methods using Spring Data MongoDB naming conventions:

    // Find prescriptions by patient ID
    List<Prescription> findByPatientId(Long patientId);

    // Find prescriptions by doctor ID
    List<Prescription> findByDoctorId(Long doctorId);

    // Find prescriptions by patient ID and doctor ID
    List<Prescription> findByPatientIdAndDoctorId(Long patientId, Long doctorId);

    // Find prescriptions by patient ID and appointment ID
    List<Prescription> findByPatientIdAndAppointmentId(Long patientId, Long appointmentId);

    // Find prescriptions by medication name (case-insensitive)
    List<Prescription> findByMedicationNameContainingIgnoreCase(String medicationName);

    // Find prescriptions by medication name exact match
    List<Prescription> findByMedicationName(String medicationName);

    // Find prescriptions by prescription date range
    List<Prescription> findByPrescriptionDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);

    // Find prescriptions by status (e.g., "active", "completed", "cancelled")
    List<Prescription> findByStatus(String status);

    // Find prescriptions by patient ID and status
    List<Prescription> findByPatientIdAndStatus(Long patientId, String status);

    // Find prescriptions by doctor ID and status
    List<Prescription> findByDoctorIdAndStatus(Long doctorId, String status);

    // Find prescriptions by refills remaining greater than zero
    List<Prescription> findByRefillsRemainingGreaterThan(int refills);

    // Find prescriptions with refills remaining
    List<Prescription> findByRefillsRemainingGreaterThanEqual(int refills);

    // Find prescriptions by dosage
    List<Prescription> findByDosage(String dosage);

    // Find prescriptions by frequency
    List<Prescription> findByFrequency(String frequency);

    // Count prescriptions by patient ID
    long countByPatientId(Long patientId);

    // Count prescriptions by doctor ID
    long countByDoctorId(Long doctorId);

    // Check if prescription exists for an appointment
    boolean existsByAppointmentId(Long appointmentId);

    // Check if prescription exists for a patient
    boolean existsByPatientId(Long patientId);

    // Delete prescriptions by appointment ID
    void deleteByAppointmentId(Long appointmentId);

    // Delete prescriptions by patient ID
    void deleteByPatientId(Long patientId);

    // Delete prescriptions by doctor ID
    void deleteByDoctorId(Long doctorId);

    // Find prescriptions with notes containing specific text (case-insensitive)
    List<Prescription> findByNotesContainingIgnoreCase(String notes);

    // Find prescriptions by patient ID sorted by prescription date descending
    List<Prescription> findByPatientIdOrderByPrescriptionDateDesc(Long patientId);

    // Find prescriptions by doctor ID sorted by prescription date descending
    List<Prescription> findByDoctorIdOrderByPrescriptionDateDesc(Long doctorId);

    // Find active prescriptions (not expired and refills remaining)
    // Custom query might be needed for complex logic
    // @Query("{ 'expirationDate': { $gte: ?0 }, 'refillsRemaining': { $gt: 0 } }")
    // List<Prescription> findActivePrescriptions(java.time.LocalDate currentDate);

    // 4. Custom query using @Query annotation for more complex queries
    // @Query("{ 'patientId': ?0, 'medicationName': { $regex: ?1, $options: 'i' } }")
    // List<Prescription> findByPatientIdAndMedicationNameLike(Long patientId, String medicationName);

    // 5. Aggregation query example for statistics
    // @Aggregation(pipeline = {
    //     "{ $group: { _id: '$doctorId', totalPrescriptions: { $sum: 1 } } }",
    //     "{ $sort: { totalPrescriptions: -1 } }"
    // })
    // List<DoctorPrescriptionStats> getPrescriptionStatsByDoctor();
}

// 6. Optional: DTO for aggregation results
// class DoctorPrescriptionStats {
//     private Long doctorId;
//     private Long totalPrescriptions;
//     
//     // Constructor, getters, and setters
// }