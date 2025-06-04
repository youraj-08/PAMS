package com.example.PAMS.controller;

import com.example.PAMS.dto.AdminDto;
import com.example.PAMS.dto.DoctorDto;
import com.example.PAMS.entities.Admin;
import com.example.PAMS.entities.Doctor;
import com.example.PAMS.entities.Patient;
import com.example.PAMS.entities.User;
import com.example.PAMS.exception.ResourceNotFoundException;
import com.example.PAMS.repository.AdminRepository;
import com.example.PAMS.repository.DoctorRepository;
import com.example.PAMS.repository.PatientRepository;
import com.example.PAMS.repository.UserRepository;
import com.example.PAMS.service.DoctorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final DoctorService doctorService;

    public AdminController(PatientRepository patientRepository, DoctorRepository doctorRepository, AdminRepository adminRepository, UserRepository userRepository, DoctorService doctorService) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.doctorService = doctorService;
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

    @Transactional
    @PostMapping("/patients/delete/{id}")
    public String deletePatient(@PathVariable("id") Integer id) {
        try {
            Patient patient = patientRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
            //user.setPatient(null);
            userRepository.findByPatient_PatientId(id).ifPresent(userRepository::delete);

            patientRepository.delete(patient);
            return "redirect:/admin/patients?deleted=true";
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return "redirect:/admin/patients?error=true";
        }
    }


    @GetMapping("/patients/edit/{id}")
    public String editPatientForm(@PathVariable("id") Integer id, Model model) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        model.addAttribute("patient", patient);
        return "admin-edit-patient";
    }

    // Update the updatePatient method
    @PostMapping("/patients/update/{id}")
    public String updatePatient(@PathVariable("id") Integer id,
                                @Valid @ModelAttribute("patient") Patient updatedPatient,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            System.out.println(result.getAllErrors());
            return "admin-edit-patient";
        }

        Patient existingPatient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        User existingUser = userRepository.findByEmail(existingPatient.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        existingUser.setEmail(updatedPatient.getEmail());

        // Update all fields
        existingPatient.setName(updatedPatient.getName());
        existingPatient.setEmail(updatedPatient.getEmail());
        existingPatient.setPhone(updatedPatient.getPhone());
        existingPatient.setAddress(updatedPatient.getAddress());
        existingPatient.setDob(updatedPatient.getDob());

        patientRepository.save(existingPatient);
        userRepository.save(existingUser);

        redirectAttributes.addFlashAttribute("successMessage", "Patient updated successfully!");
        return "redirect:/admin/patients";
    }

    // Doctor Management
    @GetMapping("/doctors")
    public String listDoctors(Model model) {
        model.addAttribute("doctors", doctorRepository.findAll());
        return "admin-doctors";
    }

    @Transactional
    @PostMapping("/doctors/delete/{id}")
    public String deleteDoctor(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            // 1. Find the doctor
            Doctor doctor = doctorRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));

            // 2. Find and delete the associated user
            Optional<User> userOptional = userRepository.findByDoctor(doctor);
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // Break the relationship
                user.setDoctor(null);
                doctor.setUser(null);  // Add this if your Doctor entity has a 'user' field

                // Save the updated user to break relationship
                userRepository.save(user);

                // Delete the user
                userRepository.delete(user);
            }

            // 3. Now delete the doctor
            doctorRepository.delete(doctor);

            redirectAttributes.addFlashAttribute("successMessage", "Doctor deleted successfully!");
            return "redirect:/admin/doctors";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting doctor: " + ex.getMessage());
            ex.printStackTrace();
            return "redirect:/admin/doctors";
        }
    }


    @GetMapping("/doctors/edit/{id}")
    public String editDoctorForm(@PathVariable("id") Integer id, Model model) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        DoctorDto doctorDto = convertToDto(doctor);
        model.addAttribute("doctor", doctorDto);
        return "admin-edit-doctor";
    }

    @PostMapping("/doctors/update/{id}")
    public String updateDoctor(@PathVariable("id") Integer id,
                               @ModelAttribute("doctor") DoctorDto doctorDto,
                               RedirectAttributes redirectAttributes) {
        try {
            doctorDto.setDoctorId(id);
            doctorService.updateDoctor(doctorDto);
            redirectAttributes.addFlashAttribute("successMessage", "Doctor details updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating doctor.");
            System.out.println( e.getMessage());
        }
        return "redirect:/admin/doctors";
    }


     //Admin Management
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

    @Transactional
    @PostMapping("/admins/delete/{id}")
    public String deleteAdmin(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            // 1. Find the admin
            Admin admin = adminRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + id));

            // 2. Find and delete the associated user using ID
            Optional<User> userOptional = userRepository.findByAdmin_AdminId(id);
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // Break the relationship
                user.setAdmin(null);
                admin.setUser(null);

                userRepository.save(user);
                userRepository.delete(user);
            }

            // 3. Delete the admin
            adminRepository.delete(admin);

            redirectAttributes.addFlashAttribute("successMessage", "Admin deleted successfully!");
            return "redirect:/admin/admins";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting admin: " + ex.getMessage());
            ex.printStackTrace();
            return "redirect:/admin/admins";
        }
    }

    private DoctorDto convertToDto(Doctor doctor) {

        DoctorDto dto = new DoctorDto();
        ObjectMapper objectMapper = new ObjectMapper();

        dto.setDoctorId(doctor.getDoctorId());
        dto.setName(doctor.getName());
        dto.setEmail(doctor.getEmail());
        dto.setPhone(doctor.getPhone());
        dto.setSpecialization(doctor.getSpecialization());

        // Parse availability JSON
        if (doctor.getAvailability() != null) {
            try {
                Map<String, Map<String, String>> availability = objectMapper.readValue(
                        doctor.getAvailability(),
                        new TypeReference<Map<String, Map<String, String>>>() {}
                );

                // Set DTO fields from JSON
                availability.forEach((day, times) -> {
                    switch (day.toLowerCase()) {
                        case "monday" -> {
                            dto.setMondayStart(times.get("start"));
                            dto.setMondayEnd(times.get("end"));
                        }
                        case "tuesday" -> {
                            dto.setTuesdayStart(times.get("start"));
                            dto.setTuesdayEnd(times.get("end"));
                        }
                        case "wednesday" -> {
                            dto.setWednesdayStart(times.get("start"));
                            dto.setWednesdayEnd(times.get("end"));
                        }
                        case "thursday" -> {
                            dto.setThursdayStart(times.get("start"));
                            dto.setThursdayEnd(times.get("end"));
                        }
                        case "friday" -> {
                            dto.setFridayStart(times.get("start"));
                            dto.setFridayEnd(times.get("end"));
                        }
                        case "saturday" -> {
                            dto.setSaturdayStart(times.get("start"));
                            dto.setSaturdayEnd(times.get("end"));
                        }
                        case "sunday" -> {
                            dto.setSundayStart(times.get("start"));
                            dto.setSundayEnd(times.get("end"));
                        }

                    }
                });
            } catch (JsonProcessingException e) {
                System.out.println(e.getMessage());
            }
        }
        return dto;
    }
}

