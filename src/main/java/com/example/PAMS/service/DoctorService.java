package com.example.PAMS.service;

import com.example.PAMS.dto.DoctorDto;
import com.example.PAMS.entities.Doctor;
import com.example.PAMS.entities.User;
import com.example.PAMS.repository.DoctorRepository;
import com.example.PAMS.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DoctorService {

        private final DoctorRepository doctorRepository;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorRepository doctorRepository,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
            this.doctorRepository = doctorRepository;
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
    }
        //Register Doctor
        public String registerDoctor(@ModelAttribute("doctor") DoctorDto doctorDto) {
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

            return "redirect:/login?registered";
        }


    public void updateDoctor(DoctorDto doctorDto) {
        Doctor doctor = doctorRepository.findById(doctorDto.getDoctorId())
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found"));

        // Update editable fields
        doctor.setPhone(doctorDto.getPhone());
        doctor.setAvailability(buildAvailabilityJson(doctorDto));

        // Update password only if provided
        if (doctorDto.getPassword() != null && !doctorDto.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(doctorDto.getPassword());
            doctor.setPassword(encodedPassword);

            // Also update associated user's password
            User user = userRepository.findByDoctor(doctor)
                    .orElseThrow(() -> new EntityNotFoundException("User account not found"));
            user.setPassword(encodedPassword);
            userRepository.save(user);
        }

        doctorRepository.save(doctor);
    }


        private String buildAvailabilityJson(DoctorDto dto) {
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
            } catch ( JsonProcessingException e) {
                System.out.println(e.getMessage());
                return "{}";
            }
        }
}
