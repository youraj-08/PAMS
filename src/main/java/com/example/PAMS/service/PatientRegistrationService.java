package com.example.PAMS.service;

import com.example.PAMS.dto.DoctorRegistrationDto;
import com.example.PAMS.entities.Doctor;
import com.example.PAMS.entities.Patient;
import com.example.PAMS.entities.User;
import com.example.PAMS.repository.DoctorRepository;
import com.example.PAMS.repository.PatientRepository;
import com.example.PAMS.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ModelAttribute;

@Service
public class RegistrationService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PatientRepository patientRepository;
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    public void registerPatient(Patient patient) {
        // Encrypt password
        String encodedPassword = passwordEncoder.encode(patient.getPassword());
        patient.setPassword(encodedPassword);

        // Save patient
        Patient savedPatient = patientRepository.save(patient);

        // Create user
        User user = new User();
        user.setEmail(patient.getEmail());
        user.setPassword(encodedPassword);
        user.setRole("PATIENT");
        user.setPatient(savedPatient);

        userRepository.save(user);
    }

    public void registerDoctor(@ModelAttribute("doctor") DoctorRegistrationDto doctorDto) {
        // Create Doctor entity
        Doctor doctor = new Doctor();
        doctor.setName(doctorDto.getName());
        doctor.setEmail(doctorDto.getEmail());
        doctor.setPhone(doctorDto.getPhone());
        doctor.setSpecialization(doctorDto.getSpecialization());

        // Set availability as JSON
        doctor.setAvailability(buildAvailabilityJson(doctorDto));

        // Encode password
        String encodedPassword = passwordEncoder.encode(doctorDto.getPassword());
        doctor.setPassword(encodedPassword);

        // Save doctor
        Doctor savedDoctor = doctorRepository.save(doctor);

        // Create User entity
        User user = new User();
        user.setEmail(doctorDto.getEmail());
        user.setPassword(encodedPassword);
        user.setRole("DOCTOR");
        user.setDoctor(savedDoctor);

        userRepository.save(user);

    }

    private String buildAvailabilityJson(DoctorRegistrationDto dto) {
        // Build JSON structure for availability
        Map<String, Map<String, String>> availability = new LinkedHashMap<>();

        availability.put("Monday", Map.of(
                "start", dto.getMondayStart(),
                "end", dto.getMondayEnd()
        ));

        availability.put("Tuesday", Map.of(
                "start", dto.getTuesdayStart(),
                "end", dto.getTuesdayEnd()
        ));

        availability.put("Wednesday", Map.of(
                "start", dto.getWednesdayStart(),
                "end", dto.getWednesdayEnd()
        ));

        availability.put("Thursday", Map.of(
                "start", dto.getThursdayStart(),
                "end", dto.getThursdayEnd()
        ));

        availability.put("Friday", Map.of(
                "start", dto.getFridayStart(),
                "end", dto.getFridayEnd()
        ));

        if (dto.getSaturdayStart() != null && !dto.getSaturdayStart().isEmpty()) {
            availability.put("Saturday", Map.of(
                    "start", dto.getSaturdayStart(),
                    "end", dto.getSaturdayEnd()
            ));
        }

        if (dto.getSundayStart() != null && !dto.getSundayStart().isEmpty()) {
            availability.put("Sunday", Map.of(
                    "start", dto.getSundayStart(),
                    "end", dto.getSundayEnd()
            ));
        }

        // Convert to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(availability);
        } catch (JsonProcessingException e) {
            // Fallback to empty JSON object if conversion fails
            return "{}";
        }
    }
}// Similar methods for doctor/admin registration

