###MySQL Database Design
Here are the 6 core tables for the clinic management system:

Table: patients
id: INT, Primary Key, Auto Increment

first_name: VARCHAR(100), Not Null

last_name: VARCHAR(100), Not Null

date_of_birth: DATE, Not Null

gender: ENUM('Male', 'Female', 'Other', 'Prefer not to say'), Not Null

phone: VARCHAR(20), Not Null

email: VARCHAR(255), Unique

address: TEXT

emergency_contact: VARCHAR(100)

emergency_phone: VARCHAR(20)

blood_type: ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-', 'Unknown')

allergies: TEXT

created_at: TIMESTAMP, Default CURRENT_TIMESTAMP

updated_at: TIMESTAMP, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

Table: doctors
id: INT, Primary Key, Auto Increment

first_name: VARCHAR(100), Not Null

last_name: VARCHAR(100), Not Null

specialization: VARCHAR(255), Not Null

license_number: VARCHAR(50), Unique, Not Null

phone: VARCHAR(20), Not Null

email: VARCHAR(255), Unique, Not Null

office_room: VARCHAR(10)

is_active: BOOLEAN, Default True

hire_date: DATE, Not Null

created_at: TIMESTAMP, Default CURRENT_TIMESTAMP

updated_at: TIMESTAMP, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

Table: clinic_locations
id: INT, Primary Key, Auto Increment

name: VARCHAR(255), Not Null

address: TEXT, Not Null

phone: VARCHAR(20), Not Null

email: VARCHAR(255)

is_active: BOOLEAN, Default True

created_at: TIMESTAMP, Default CURRENT_TIMESTAMP

Table: appointments
id: INT, Primary Key, Auto Increment

doctor_id: INT, Foreign Key → doctors(id), Not Null

patient_id: INT, Foreign Key → patients(id), Not Null

location_id: INT, Foreign Key → clinic_locations(id), Not Null

appointment_date: DATE, Not Null

start_time: TIME, Not Null

end_time: TIME, Not Null

purpose: TEXT

status: ENUM('Scheduled', 'Completed', 'Cancelled', 'No-show'), Default 'Scheduled'

notes: TEXT

created_at: TIMESTAMP, Default CURRENT_TIMESTAMP

updated_at: TIMESTAMP, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

cancellation_reason: TEXT

cancelled_by: ENUM('Patient', 'Doctor', 'Admin')

Table: prescriptions
id: INT, Primary Key, Auto Increment

appointment_id: INT, Foreign Key → appointments(id), Not Null

patient_id: INT, Foreign Key → patients(id), Not Null

doctor_id: INT, Foreign Key → doctors(id), Not Null

medication_name: VARCHAR(255), Not Null

dosage: VARCHAR(100), Not Null

frequency: VARCHAR(100), Not Null

duration: VARCHAR(100), Not Null

instructions: TEXT

issued_date: DATE, Not Null

is_active: BOOLEAN, Default True

refills_remaining: INT, Default 0

created_at: TIMESTAMP, Default CURRENT_TIMESTAMP

Table: payments
id: INT, Primary Key, Auto Increment

appointment_id: INT, Foreign Key → appointments(id), Not Null

patient_id: INT, Foreign Key → patients(id), Not Null

amount: DECIMAL(10,2), Not Null

payment_method: ENUM('Cash', 'Credit Card', 'Debit Card', 'Insurance', 'Online'), Not Null

payment_status: ENUM('Pending', 'Completed', 'Partially Paid', 'Insurance Pending'), Default 'Pending'

insurance_provider: VARCHAR(255)

policy_number: VARCHAR(100)

payment_date: DATETIME

created_at: TIMESTAMP, Default CURRENT_TIMESTAMP

###MongoDB Collection Design
Collection: medical_notes
{
  "_id": ObjectId("64abc123456def7890123456"),
  "appointment_id": 156,
  "patient_id": 42,
  "doctor_id": 18,
  "date": ISODate("2024-01-15T14:30:00Z"),
  
  "subjective": {
    "chief_complaint": "Persistent headaches for 2 weeks",
    "history_of_present_illness": "Patient reports dull, throbbing pain in temples, worse in the afternoons. No nausea or vision changes. Started after stressful work project.",
    "review_of_systems": "Denies fever, neck stiffness, photophobia. Sleep: 5-6 hours/night. Stress level: High."
  },
  
  "objective": {
    "vital_signs": {
      "blood_pressure": "120/80",
      "heart_rate": 72,
      "temperature": 98.6,
      "oxygen_saturation": 98
    },
    "physical_exam": "Head: Normocephalic, atraumatic. Neck: Supple, no meningismus. Neurological: CN II-XII intact, normal gait."
  },
  
  "assessment": {
    "diagnosis": ["Tension-type headache", "Stress-related symptoms"],
    "icd_codes": ["G44.209", "F43.8"],
    "differential_diagnosis": ["Migraine", "Sinusitis", "Hypertension"]
  },
  
  "plan": {
    "recommendations": [
      "OTC ibuprofen 400mg as needed",
      "Stress management techniques",
      "Regular sleep schedule",
      "Follow-up in 4 weeks if symptoms persist"
    ],
    "referrals": [],
    "follow_up_required": true,
    "follow_up_days": 28
  },
  
  "attachments": [
    {
      "file_name": "headache_diary.pdf",
      "file_type": "pdf",
      "uploaded_at": ISODate("2024-01-15T14:35:00Z"),
      "size_kb": 245
    }
  ],
  
  "tags": ["headache", "stress", "neurology", "follow-up-required"],
  "privacy_level": "standard", // standard, sensitive, highly-sensitive
  "created_at": ISODate("2024-01-15T14:40:00Z"),
  "updated_at": ISODate("2024-01-15T14:40:00Z"),
  "version": 1
}

Collection: patient_messages
{
  "_id": ObjectId("64abc123456def7890123457"),
  "thread_id": "thread_42_18",
  "participants": [
    { "user_id": 42, "role": "patient", "name": "Jane Doe" },
    { "user_id": 18, "role": "doctor", "name": "Dr. Smith" }
  ],
  
  "messages": [
    {
      "message_id": "msg_001",
      "sender_id": 42,
      "sender_role": "patient",
      "content": "Hi Dr. Smith, I've been experiencing some side effects from the medication - mild nausea. Should I continue taking it?",
      "attachments": [],
      "read_by_recipient": true,
      "timestamp": ISODate("2024-01-16T09:15:00Z")
    },
    {
      "message_id": "msg_002",
      "sender_id": 18,
      "sender_role": "doctor",
      "content": "Hi Jane, please take the medication with food. If nausea persists after 2 days, reduce to half dose and let me know.",
      "attachments": [
        {
          "type": "educational",
          "title": "Managing Medication Side Effects",
          "url": "/resources/medication-guide.pdf"
        }
      ],
      "read_by_recipient": true,
      "timestamp": ISODate("2024-01-16T14:30:00Z")
    }
  ],
  
  "metadata": {
    "related_appointment": 156,
    "related_prescription": 89,
    "urgency": "routine", // emergency, urgent, routine
    "status": "active", // active, archived, resolved
    "last_activity": ISODate("2024-01-16T14:30:00Z"),
    "message_count": 2
  },
  
  "created_at": ISODate("2024-01-16T09:15:00Z"),
  "updated_at": ISODate("2024-01-16T14:30:00Z")
}

Collection: audit_logs
{
  "_id": ObjectId("64abc123456def7890123458"),
  "event_type": "prescription_modified",
  "timestamp": ISODate("2024-01-15T16:45:23Z"),
  
  "actor": {
    "id": 18,
    "type": "doctor",
    "name": "Dr. Robert Smith",
    "ip_address": "192.168.1.100"
  },
  
  "target": {
    "id": 89,
    "type": "prescription",
    "patient_id": 42,
    "patient_name": "Jane Doe"
  },
  
  "action": "dosage_adjusted",
  
  "changes": {
    "before": {
      "dosage": "20mg",
      "frequency": "once daily",
      "refills_remaining": 1
    },
    "after": {
      "dosage": "10mg",
      "frequency": "twice daily",
      "refills_remaining": 2
    },
    "reason": "Patient reported side effects, adjusting to lower split dose"
  },
  
  "context": {
    "appointment_id": 156,
    "location": "Main Clinic",
    "device": "web_browser",
    "user_agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
  },
  
  "compliance": {
    "hipaa_relevant": true,
    "consent_verified": true,
    "legal_hold": false
  }
}

Collection: patient_feedback
{
  "_id": ObjectId("64abc123456def7890123459"),
  "appointment_id": 156,
  "patient_id": 42,
  "doctor_id": 18,
  "location_id": 3,
  
  "ratings": {
    "overall": 4.5,
    "wait_time": 4,
    "doctor_communication": 5,
    "staff_courtesy": 4,
    "facility_cleanliness": 5,
    "ease_of_appointment": 3
  },
  
  "feedback": {
    "what_went_well": [
      "Doctor listened carefully to my concerns",
      "Explanation was clear and thorough",
      "Staff was friendly and professional"
    ],
    "improvement_suggestions": [
      "Waiting area could use more seating",
      "Parking validation process was confusing"
    ],
    "comments": "Dr. Smith was excellent - very patient and addressed all my questions. The wait was a bit longer than expected though."
  },
  
  "sentiment": {
    "overall_sentiment": "positive",
    "keywords": ["excellent", "patient", "thorough", "friendly", "long wait"],
    "sentiment_score": 0.85
  },
  
  "metadata": {
    "channel": "post_appointment_email",
    "response_requested": false,
    "follow_up_action": "none",
    "anonymous": false,
    "verified_patient": true
  },
  
  "timestamps": {
    "appointment_date": ISODate("2024-01-15T00:00:00Z"),
    "feedback_submitted": ISODate("2024-01-16T10:30:00Z"),
    "reviewed_by_staff": ISODate("2024-01-17T09:15:00Z")
  }
}

Design Philosophy & Decisions
1. References vs Embedding
Medical Notes: Contains both IDs (for MySQL joins) and some duplicated data (names) for readability and offline access

Messages: Embeds conversation history for performance - entire thread can be fetched in one query

Audit Logs: Self-contained with all context needed for compliance reporting

