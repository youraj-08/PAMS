package com.example.PAMS.controller;

import com.example.PAMS.dto.AdminDto;
import com.example.PAMS.dto.DoctorDto;
import com.example.PAMS.entities.Admin;
import com.example.PAMS.entities.Doctor;
import com.example.PAMS.entities.Patient;
import com.example.PAMS.exception.ResourceNotFoundException;
import com.example.PAMS.repository.AdminRepository;
import com.example.PAMS.repository.DoctorRepository;
import com.example.PAMS.repository.PatientRepository;
import com.example.PAMS.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;

    public AdminController(PatientRepository patientRepository, DoctorRepository doctorRepository, AdminRepository adminRepository, UserRepository userRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("patientCount", patientRepository.count());
        model.addAttribute("doctorCount", doctorRepository.count());
        model.addAttribute("adminCount", adminRepository.count());
        return "admin-dashboard";
    }

    // Patient Management
    @GetMapping("/patients")
    public String listPatients(Model model) {
        model.addAttribute("patients", patientRepository.findAll());
        return "admin-patients";
    }

    @PostMapping("/patients/delete/{id}")
    public String deletePatient(@PathVariable("id") Integer id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        // Delete associated user
        userRepository.findByPatient(patient).ifPresent(userRepository::delete);

        patientRepository.delete(patient);
        return "redirect:/admin/patients?deleted=true";
    }
    @GetMapping("/patients/edit/{id}")
    public String editPatientForm(@PathVariable("id") Integer id, Model model) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        model.addAttribute("patient", patient);
        return "admin-edit-patient"; // Create this template
    }

    @PostMapping("/patients/update/{id}")
    public String updatePatient(@PathVariable("id") Integer id,
                                @ModelAttribute Patient patient) {
        Patient existing = patientRepository.findById(id).orElseThrow();
        existing.setPhone(patient.getPhone());
        // Update other fields as needed
        patientRepository.save(existing);
        return "redirect:/admin/patients?updated=true";
    }

    // Doctor Management
    @GetMapping("/doctors")
    public String listDoctors(Model model) {
        model.addAttribute("doctors", doctorRepository.findAll());
        return "admin-doctors";
    }

    @PostMapping("/doctors/delete/{id}")
    public String deleteDoctor(@PathVariable("id") Integer id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        // Delete associated user
        userRepository.findByDoctor(doctor).ifPresent(userRepository::delete);

        doctorRepository.delete(doctor);
        return "redirect:/admin/doctors?deleted=true";
    }

    @GetMapping("/doctors/edit/{id}")
    public String editDoctorForm(@PathVariable("id") Integer id, Model model) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        model.addAttribute("doctor", doctor);
        model.addAttribute("availability", parseAvailability(doctor.getAvailability()));
        return "admin-edit-doctor";
    }

    @PostMapping("/doctors/update/{id}")
    public String updateDoctor(@PathVariable("id") Integer id,
                               @ModelAttribute DoctorDto updateDto) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow();
        doctor.setPhone(updateDto.getPhone());
        doctor.setAvailability(buildAvailabilityJson(updateDto));
        doctorRepository.save(doctor);
        return "redirect:/admin/doctors?updated=true";
    }

    // Admin Management
    @GetMapping("/admins")
    public String listAdmins(Model model) {
        List<Admin> admins = adminRepository.findAll();
        List<AdminDto> adminDtos = admins.stream().map(admin -> {
            String role = admin.getUser() != null ? admin.getUser().getRole() : "NO_ROLE";
            return new AdminDto(
                    admin.getAdminId(),
                    admin.getName(),
                    admin.getEmail(),
                    role
            );
        }).collect(Collectors.toList());

        model.addAttribute("admins", adminDtos);
        return "admin-admins";
    }

    @PostMapping("/admins/delete/{id}")
    public String deleteAdmin(@PathVariable("id") Integer id,
                              Authentication authentication) {
        // Prevent self-deletion
        String currentEmail = authentication.getName();
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (admin.getEmail().equals(currentEmail)) {
            return "redirect:/admin/admins?error=self_delete";
        }

        // Delete associated user
        userRepository.findByAdmin(admin).ifPresent(userRepository::delete);

        adminRepository.delete(admin);
        return "redirect:/admin/admins?deleted=true";
    }

    // Helper methods for availability
    private Map<String, Map<String, String>> parseAvailability(String availabilityJson) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(availabilityJson,
                    new TypeReference<Map<String, Map<String, String>>>(){});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private String buildAvailabilityJson(DoctorDto dto) {
        Map<String, Map<String, String>> availability = new LinkedHashMap<>();

        availability.put("Monday", Map.of("start", dto.getMondayStart(), "end", dto.getMondayEnd()));
        availability.put("Tuesday", Map.of("start", dto.getTuesdayStart(), "end", dto.getTuesdayEnd()));
        availability.put("Wednesday", Map.of("start", dto.getWednesdayStart(), "end", dto.getWednesdayEnd()));
        availability.put("Thursday", Map.of("start", dto.getThursdayStart(), "end", dto.getThursdayEnd()));
        availability.put("Friday", Map.of("start", dto.getFridayStart(), "end", dto.getFridayEnd()));

        if (dto.getSaturdayStart() != null && !dto.getSaturdayStart().isEmpty()) {
            availability.put("Saturday", Map.of("start", dto.getSaturdayStart(), "end", dto.getSaturdayEnd()));
        }

        if (dto.getSundayStart() != null && !dto.getSundayStart().isEmpty()) {
            availability.put("Sunday", Map.of("start", dto.getSundayStart(), "end", dto.getSundayEnd()));
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(availability);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}

